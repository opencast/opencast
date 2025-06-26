package org.opencastproject.maintenance.impl;

import org.opencastproject.maintenance.api.MaintenanceService;
import org.opencastproject.maintenance.impl.perstistance.MaintenanceEntity;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component(
    immediate = true,
    service = { MaintenanceService.class },
    property = {
        "service.description=Maintenance Service"
    }
)
public class MaintenanceServiceImpl implements MaintenanceService {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceServiceImpl.class);

  private boolean readOnlyActive = false;
  private LocalDateTime startDate;
  private LocalDateTime endDate;

  private  ScheduledExecutorService maintenanceScheduler;
  private ScheduledFuture<?> maintenanceStartFuture;
  private ScheduledFuture<?> maintenanceEndFuture;
  private MaintenanceEntity currentScheduledMaintenanceEntity;

  /** The MaintenanceServiceDatabase for saving the maintenance schedule */
  protected MaintenanceServiceDatabase maintenanceDatabase;

  /** The ServiceRegistry for getting access to the hosts */
  protected ServiceRegistry serviceRegistry;

  @Activate
  @Modified
  public void activate() {
    logger.info("Activated/Modified Maintenance Service.");
    synchronizeSchedulerAndDatabase();
    logger.info("Finished activating/updating Maintenance Service");
  }

  private void synchronizeSchedulerAndDatabase() {
    logger.info("Checking if maintenance is running or scheduled and if we have to synchronize with the database.");
    if (isMaintenanceRunning()) {
      logger.info("Maintenance is in progress. Doing nothing.");
    } else {
      logger.info("No maintenance in progress. Checking if maintenance is scheduled in database.");
      MaintenanceEntity maintenanceEntity = maintenanceDatabase.loadSchedule();
      boolean databaseEntryExists = maintenanceEntity != null;

      // Case 1: Not in OC and not in Database -> just do nothing
      if (!isMaintenanceScheduled() && !databaseEntryExists) {
        logger.info("No maintenance schedule found in database or in Opencast memory. Nothing to do.");

      // Case 2: Not in OC but in Database -> activate scheduler with data from database
      } else if (!isMaintenanceScheduled() && databaseEntryExists) {
        logger.info("Maintenance schedule found in database, but not in Opencast memory. Activating scheduler.");
        scheduleMaintenance(maintenanceEntity);

      // Case 3: In OC but not in Database -> weird case, but we synchronize with
      // the database and deactivate the scheduler
      } else if (isMaintenanceScheduled() && !databaseEntryExists) {
        logger.info("Maintenance schedule not found in database, but in Opencast memory. Deactivating scheduler.");
        shutdownScheduler();

      // Case 4: In OC and in Database -> Check if both are in sync
      } else if (isMaintenanceScheduled() && databaseEntryExists) {
        logger.info("Maintenance schedule found in database and in Opencast memory. Checking if they are in sync.");
      if (maintenanceEntity.fieldsEqual(currentScheduledMaintenanceEntity)) {
        logger.info("Maintenance schedule is in sync. Nothing to do.");
      } else {
          logger.info("Maintenance schedule is not in sync. Shutting down scheduler and re-activating it.");
          shutdownScheduler();
          scheduleMaintenance(maintenanceEntity);
        }
      }
    }
  }

  private void scheduleMaintenance(MaintenanceEntity maintenanceEntity) {
    scheduleMaintenance(maintenanceEntity.isActivateMaintenance(), maintenanceEntity.isActivateReadOnly(),
        maintenanceEntity.getStartDate(), maintenanceEntity.getEndDate());
  }


  @Override
  public void scheduleMaintenance(boolean activateMaintenance, boolean activateReadOnly,
      LocalDateTime startDate, LocalDateTime endDate) {

    logger.info("Scheduling maintenance with following parameters: "
            + "Put Server into maintenance mode: {}, Enable read-only mode: {}, Start Date: {}, End Date: {}",
        activateMaintenance, activateReadOnly, startDate, endDate);

    if (isMaintenanceRunning()) {
      throw new IllegalStateException("Tried to schedule maintenance while maintenance is already running.");
    }

    if (isMaintenanceScheduled()) {
      throw new IllegalStateException("Tried to schedule maintenance while maintenance is already scheduled.");
    }

    // Calculate delay and duration
    long initialDelay = ChronoUnit.MINUTES.between(LocalDateTime.now(), startDate);
    long duration = ChronoUnit.MINUTES.between(startDate, endDate);

    if (LocalDateTime.now().isAfter(startDate)) { // The start date is in the past (maintenance in progress or finished)
      if (LocalDateTime.now().isAfter(endDate)) { // The end date is in the past (maintenance finished)
        throw new IllegalStateException("Tried to schedule maintenance with dates in the past. "
            + "Start date: " + startDate + ", End date: " + endDate);
      } else { // The end date is in the future (maintenance in progress)
        initialDelay = 0;
      }
    }

    // Save the schedule to database
    currentScheduledMaintenanceEntity = maintenanceDatabase.saveSchedule(
        activateMaintenance, activateReadOnly, startDate, endDate);

    // save dates locally
    this.startDate = startDate;
    this.endDate = endDate;

    // Schedule the maintenance
    maintenanceScheduler = Executors.newScheduledThreadPool(1);
    maintenanceStartFuture = maintenanceScheduler.schedule(() -> {
      // duration == delay of execution of exitMaintenanceMode()
      boolean success = enterMaintenanceMode();
      if (success) {
        maintenanceEndFuture = maintenanceScheduler.schedule(this::exitMaintenanceMode, duration, TimeUnit.MINUTES);
      }
    }, initialDelay, TimeUnit.MINUTES);
  }

  public boolean enterMaintenanceMode() {
    logger.info("Entering maintenance mode");
    if (currentScheduledMaintenanceEntity.isActivateMaintenance()) {
      try {
        for (HostRegistration host : serviceRegistry.getHostRegistrations()) {
          serviceRegistry.setMaintenanceStatus(host.getBaseUrl(), true);
        }
      } catch (Exception e) {
        logger.error("Error while entering maintenance mode.", e);
        return false;
      }
    }
    readOnlyActive = currentScheduledMaintenanceEntity.isActivateReadOnly();
    return true;
  }

  public void exitMaintenanceMode() {
    logger.info("Exiting maintenance mode");
    if (currentScheduledMaintenanceEntity.isActivateMaintenance()) {
      try {
        for (HostRegistration host : serviceRegistry.getHostRegistrations()) {
          serviceRegistry.setMaintenanceStatus(host.getBaseUrl(), false);
        }
      } catch (Exception e) {
        logger.error("Error while exiting maintenance mode.", e);
      }
    }

    try {
      maintenanceDatabase.removeSchedule();
    } catch (Exception e) {
      logger.error("Error while removing maintenance schedule from database.", e);
    }

    resetMaintenanceState();
  }

  private void resetMaintenanceState() {
    readOnlyActive = false;
    maintenanceStartFuture = null;
    maintenanceEndFuture = null;
    currentScheduledMaintenanceEntity = null;
    startDate = null;
    endDate = null;
  }

  public boolean isMaintenanceScheduled() {
    return maintenanceStartFuture != null && maintenanceEndFuture == null;
  }

  public boolean isMaintenanceRunning() {
    return maintenanceEndFuture != null && !maintenanceEndFuture.isDone();
  }

  public boolean isReadOnlyActive() {
    return readOnlyActive;
  }

  private void shutdownScheduler() {
    if (maintenanceScheduler != null && !maintenanceScheduler.isShutdown()) {
      try {
        maintenanceScheduler.shutdownNow();
        if (!maintenanceScheduler.awaitTermination(60, TimeUnit.SECONDS)) {
          System.err.println("Scheduler did not terminate");
        }
      } catch (InterruptedException ie) {
        logger.error("Interrupted while shutting down maintenance scheduler. This shouldn't happen.", ie);
        maintenanceScheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
    resetMaintenanceState();
    logger.info("Maintenance scheduler shut down");
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  // -------------------------------------------------------------------------------
  // OSGi
  // -------------------------------------------------------------------------------

  @Reference
  public void setMaintenanceDatabase(MaintenanceServiceDatabase maintenanceDatabase) {
    this.maintenanceDatabase = maintenanceDatabase;
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

}
