/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.maintenance.impl.perstistance;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.maintenance.impl.MaintenanceServiceDatabase;
import org.opencastproject.maintenance.impl.MaintenanceServiceDatabaseException;
import org.opencastproject.security.api.SecurityService;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implements {@link MaintenanceServiceDatabase}. Defines permanent storage for maintenances.
 */
@Component(
    property = {
        "service.description=Maintenance Database Service"
    },
    immediate = true,
    service = { MaintenanceServiceDatabase.class }
)
public class MaintenanceServiceDatabaseImpl implements MaintenanceServiceDatabase {

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(MaintenanceServiceDatabaseImpl.class);

  //public static final String PERSISTENCE_UNIT = "org.opencastproject.series.impl.persistence";
  private static final long SINGLE_ENTRY_ID = 1;

  protected DBSession db;

  /**
   * TODO braucht man das?
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for maintenances");
  }

  @Override
  public MaintenanceEntity saveSchedule(boolean activateMaintenance, boolean activateReadOnly,
      LocalDateTime startDate, LocalDateTime endDate) throws MaintenanceServiceDatabaseException {

    MaintenanceEntity entity = new MaintenanceEntity();
    entity.setActivateMaintenance(activateMaintenance);
    entity.setActivateReadOnly(activateReadOnly);
    entity.setStartDate(startDate);
    entity.setEndDate(endDate);
    entity.setId(SINGLE_ENTRY_ID); // We want only one entry in the database

    try {
      db.execTx(em -> {
        em.persist(entity);
      });
    } catch (Exception e) {
      logger.error("Could not save maintenance schedule to database", e);
      throw new MaintenanceServiceDatabaseException(e);
    }
    return entity;
  }

  @Override
  public MaintenanceEntity loadSchedule() throws MaintenanceServiceDatabaseException {
    try {
      List<MaintenanceEntity> scheduledMaintenance = db.exec(
          namedQuery.findAll("Maintenance.getSingleEntry", MaintenanceEntity.class));
      if (scheduledMaintenance.size() > 1) {
        throw new IllegalStateException("There is only one scheduled maintenance allowed in the database.");
      }
      return scheduledMaintenance.stream().findFirst().orElse(null);
    } catch (Exception e) {
      logger.error("Could not retrieve scheduled maintenance", e);
      throw new MaintenanceServiceDatabaseException(e);
    }
  }

  @Override
  public void removeSchedule() {
    try {
      db.execTx(em -> {
        MaintenanceEntity entity = em.find(MaintenanceEntity.class, SINGLE_ENTRY_ID);
        if (entity != null) {
          em.remove(entity);
        }
      });
    } catch (Exception e) {
      throw new MaintenanceServiceDatabaseException("Error while deleting maintenance schedule from database", e);
    }
  }

}
