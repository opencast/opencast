/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.ws.rs.core.Response;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Duration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.api.ScheduledEvent;
import org.opencastproject.capture.api.ScheduledEventImpl;
import org.opencastproject.capture.impl.jobs.CleanCaptureJob;
import org.opencastproject.capture.impl.jobs.IngestJob;
import org.opencastproject.capture.impl.jobs.JobCreator;
import org.opencastproject.capture.impl.jobs.JobDetailTriggerPair;
import org.opencastproject.capture.impl.jobs.JobParameters;
import org.opencastproject.capture.impl.jobs.PollCalendarJob;
import org.opencastproject.capture.impl.jobs.SerializeJob;
import org.opencastproject.capture.impl.jobs.StartCaptureJob;
import org.opencastproject.capture.impl.jobs.StopCaptureJob;
import org.opencastproject.capture.pipeline.GStreamerPipelineTools;
import org.opencastproject.capture.pipeline.InvalidCaptureDevicesSpecifiedException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.QueryStringBuilder;
import org.opencastproject.util.XProperties;
import org.osgi.service.cm.ConfigurationException;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler implementation class. This class is responsible for retrieving iCal and then scheduling captures from the
 * resulting calendaring data. It expects the calendaring data in RFC 2445 form, and makes use of the following fields:
 * VEVENT UID DTSTART DTEND DURATION SUMMARY ATTACHMENT
 * 
 * @see {@link http://www.ietf.org/rfc/rfc2445.txt}
 */
public class SchedulerImpl {

  /** Log facility */
  private static final Logger log = LoggerFactory.getLogger(SchedulerImpl.class);

  protected static final Dur ZERO_DURATION = new Dur(0, 0, 0, 0);

  protected static final Dur ONE_MINUTE_DURATION = new Dur(0, 0, 1, 0);

  /** The name of the job that polls the remote calendar for new captures. **/
  private static final String calendarPollingJobName = "calendarPolling";

  /** The scheduler used for all of the scheduling */
  private Scheduler scheduler = null;

  /** The scheduler's properties */
  private Properties schedProps = null;

  /** The stored URL for the remote calendar source */
  private URL remoteCalendarURL = null;

  /** The URL of the cached copy of the recording schedule */
  private URL localCalendarCacheURL = null;

  /** The time in milliseconds between attempts to fetch the calendar data */
  private long calendarPollTime = 0;

  /** The last etag, if any, returned by a calendar polling request. */
  private String lastCalendarEtag = null;

  /** A stored copy of the Calendar */
  private Calendar calendar = null;

  /** The configuration for this service */
  private ConfigurationManager configService = null;

  /** The capture agent this scheduler is scheduling for */
  private CaptureAgentImpl captureAgent = null;

  /** The trusted HttpClient used to talk to the core */
  private TrustedHttpClient trustedClient = null;

  /** 
   * A variable that is set when the calendar polling process is running, and unset when it is not.
   * The purpose of this flag is to keep the calendar polling thread from crushing the server if the
   * endpoint which returns the calendar takes a long time to return (say, with a huge number of events).
   */
  private Semaphore locked = new Semaphore(1);

  @SuppressWarnings("unchecked")
  public SchedulerImpl(Dictionary dictionary, ConfigurationManager configurationManager,
          CaptureAgentImpl captureAgentImpl) throws ConfigurationException {
    configService = configurationManager;
    this.captureAgent = captureAgentImpl;
    this.updated(dictionary);
  }
  
  /**
   * Set the current ConfigurationManager and register this class as a listener for property updates.
   * 
   * @param ConfigurationManager
   *          The ConfigurationManager service that is activated
   **/
  public void setConfigService(ConfigurationManager svc) {
    configService = svc;
  }

  /** Remove the reference to the ConfigurationManager service. **/
  public void unsetConfigService() {
    configService = null;
  }

  /**
   * Called when the bundle is deactivated. This function shuts down all of the schedulers.
   */
  public void deactivate() {
    shutdown(scheduler);
  }

  /**
   * Gets a {@code Scheduler} object which can be used to schedule events
   * 
   * @param schedProps
   *          The {@code Properties} object containing the scheduler's properties
   * @return A {@Scheduler} object
   */
  protected Scheduler getScheduler(Properties schedProps) {
    return getScheduler(schedProps, UUID.randomUUID().toString());
  }

  /**
   * Gets a {@code Scheduler} object which can be used to schedule events
   * 
   * @param schedProps
   *          The {@code Properties} object containing the scheduler's properties
   * @param schedName
   *          The name for the scheduler
   * @return A {@Scheduler} object
   */
  protected Scheduler getScheduler(Properties schedProps, String schedName) {
    schedProps.setProperty("org.quartz.scheduler.instanceName", schedName);
    try {
      return new StdSchedulerFactory(schedProps).getScheduler();
    } catch (SchedulerException e) {
      log.error("Unable to create scheduler!", e);
      return null;
    }
  }

  /**
   * Updates the scheduler with new configuration data. {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(Dictionary)
   */
  //@Override
  @SuppressWarnings("unchecked")
  private void updated(Dictionary properties) throws ConfigurationException {
    log.debug("Scheduler updated.");

    if (properties == null) {
      log.debug("Null properties in updated!");
      throw new ConfigurationException("Null properties in updated!", "null");
    } else if (properties.size() == 0) {
      log.debug("0 size properties in updated!");
      throw new ConfigurationException("Properties object empty in updated, this should be a scheduler configuration!",
              "empty");
    }

    // Clone the properties. Note that we can't use serialization to do this because the Dictionary above is actually a
    // org.apache.felix.cm.impl.CaseInsensitiveDictionary
    schedProps = new Properties();
    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      schedProps.put(key, properties.get(key));
    }

    try {
      localCalendarCacheURL = new File(configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL)).toURI()
              .toURL();
    } catch (NullPointerException e) {
      log.warn("Invalid location specified for {} unable to cache scheduling data.",
              CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL);
    } catch (MalformedURLException e) {
      log.warn("Invalid location specified for {} unable to cache scheduling data.",
              CaptureParameters.CAPTURE_SCHEDULE_CACHE_URL);
    }
    updateCalendar();
  }

  /**
   * Convenience function which generates a new scheduler object with which one can schedule things.
   * 
   * @param schedProps
   *          The parameters for the scheduler
   * @param immediate
   *          True to start the schedule update polling immediately, false to wait one polling period before firing
   * @return A {@code Scheduler} which can be used for scheduling
   */
  protected Scheduler generateNewScheduler(Properties schedProps, boolean immediate) {
    // Create and start the scheduler
    Scheduler newScheduler = getScheduler(schedProps);
    setupPolling(newScheduler, immediate);
    scheduleCleanJob(newScheduler);
    return newScheduler;
  }

  /**
   * Creates the schedule polling task which checks with the core to see if there is any new scheduling data
   * 
   * @param sched
   *          The scheduler in which to create the polling tasks
   * @param immediate
   *          True to start the polling immediately, false to wait one polling period before starting
   */
  private void setupPolling(Scheduler sched, boolean immediate) {
    if (sched == null) {
      log.warn("Unable to setup polling because internal scheduler is null.");
      return;
    }

    removeFormerPollingTasks(sched);
    addCalendarPolling(sched, immediate);
  }

  /**
   * Removes any of the former polling tasks that are recurring.
   * 
   * @param sched
   *          The scheduler to remove the polling tasks from.
   */
  private void removeFormerPollingTasks(Scheduler sched) {
    if (sched == null) {
      log.warn("Unable to remove polling tasks from null scheduler!");
      return;
    }

    try {
      // Nuke any existing polling tasks
      for (String name : sched.getJobNames(JobParameters.RECURRING_TYPE)) {
        sched.deleteJob(name, JobParameters.RECURRING_TYPE);
      }
    } catch (SchedulerException e) {
      log.warn("Scheduler exception when attempting to remove former polling tasks: {}", e.toString());
    }
  }

  /**
   * Adds to the schedule polling the remote calendar for new jobs to add to the scheduler
   * 
   * @param sched
   *          The scheduler add the polling tasks to.
   * @param immediate
   *          True to start the polling immediately, false to wait one polling period before starting
   */
  private void addCalendarPolling(Scheduler sched, boolean immediate) {
    if (sched == null) {
      log.warn("Unable to add polling tasks to null scheduler!");
      return;
    }

    try {
      // Find the remote endpoint for the scheduler and add the agent's name to it
      String remoteBase = StringUtils.trimToNull(configService
              .getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL));
      if (remoteBase != null && remoteBase.charAt(remoteBase.length() - 1) != '/') {
        remoteBase = remoteBase + "/";
      } else if (remoteBase == null) {
        log.error("Key {} is missing from the config file or invalid, unable to start polling. Result was {}",
                CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL,
                StringUtils.trimToNull(configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL)));
        return;
      }
      remoteCalendarURL = new URL(remoteBase);

      // Times are in minutes in the config file
      calendarPollTime = Long.parseLong(configService
              .getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL));
      calendarPollTime = calendarPollTime * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;

      if (calendarPollTime >= (1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS)) {
        // Setup the polling
        JobDetail job = new JobDetail("calendarUpdate", JobParameters.RECURRING_TYPE, PollCalendarJob.class);

        Date start = null;
        // Possibly add a delay to prevent an infinite recursion:
        // A new calendar creating a new update which creates a new calendar which...
        if (immediate) {
          start = new Date();
        } else {
          start = new Date(System.currentTimeMillis() + calendarPollTime);
        }

        // We are hardcoding the update to fire at least 10 seconds away from a possible capture start to prevent a
        // race condition between firing a capture and updating the internal calendar. The problem lies in the non-
        // deterministic order of execution when the calendar update fires: We first dump the existing schedule then
        // reschedule with the new data, so if somehow we have a capture which should start within that fraction of a
        // second it would be lost. Changing the update time to 10 seconds off of the full minute mark prevents the
        // issue without resorting to keeping two schedulers around (which doesn't work very well, tried it) or having
        // weird logic around either the start capture or poll calendar jobs to delay one or the other if there will
        // be a conflict.
        // If we're within the first 10 seconds push an extra 10 seconds onto the time
        if (start.getSeconds() < 10) {
          start.setTime(start.getTime() + (10 * CaptureParameters.MILLISECONDS));
          // If we're within the last 10 seconds push an extra 20 seconds onto the time
          // (10 for the last 10 seconds, 10 for the first 10 of the next minute)
        } else if (start.getSeconds() > 50) {
          start.setTime(start.getTime() + (20 * CaptureParameters.MILLISECONDS));
        }

        // Create a new trigger Name Group name Start End # of times to repeat Repeat interval
        SimpleTrigger trigger = new SimpleTrigger(calendarPollingJobName, JobParameters.RECURRING_TYPE, start, null,
                SimpleTrigger.REPEAT_INDEFINITELY, calendarPollTime);

        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

        trigger.getJobDataMap().put(JobParameters.SCHEDULER, this);

        // Schedule the update
        sched.scheduleJob(job, trigger);
      } else {
        log.info("{} has been set to less than 1 minute, calendar updates disabled.",
                CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL);
      }
    } catch (StringIndexOutOfBoundsException e) {
      log.warn("Unable to build valid scheduling data endpoint from key {}: {}.  Value must end in a / character.",
              CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL,
              configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL));
    } catch (MalformedURLException e) {
      log.warn("Invalid location specified for {} unable to retrieve new scheduling data: {}.",
              CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL,
              configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL));
    } catch (NumberFormatException e) {
      log.warn("Invalid polling interval for {} unable to retrieve new scheduling data: {}.",
              CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL,
              configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL));
    } catch (SchedulerException e) {
      log.warn("Scheduler exception when attempting to start polling tasks: {}", e);
    }
  }

  /**
   * Returns the value of the lock.
   * @return  True if the lock is closed, false otherwise.
   */
  public boolean isLocked() {
    return locked.availablePermits() != 1;
  }

  /**
   * Locks the scheduler from updating its calendar
   * @return True if the lock is available, false otherwise
   */
  private boolean lock()  {
    return locked.tryAcquire();
  }

  /**
   * Unlocks the scheduler from updating its calendar
   * @return True if the lock is available, false otherwise
   */
  private void unlock() {
    locked.release();
  }

  /**
   * Sets the capture agent which this scheduler should be scheduling for.
   * 
   * @param agent
   *          The agent.
   */
  public void setCaptureAgent(CaptureAgentImpl agent) {
    captureAgent = agent;
  }

  /**
   * Sets the trusted client this service uses to communicate with the outside work.
   * 
   * @param client
   *          The {@code TrustedHttpClient} which is setup to communicate with the outside world.
   */
  public void setTrustedClient(TrustedHttpClient client) {
    trustedClient = client;
  }

  /**
   * Overridden finalize function to shutdown all Quartz schedulers.
   * 
   * @see java.lang.Object#finalize()
   */
  @Override
  public void finalize() {
    shutdown(scheduler);
    try {
      super.finalize();
    } catch (Throwable e) {
      log.error("Scheduler finalization failed", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#updateCalendar()
   */
  public void updateCalendar() {
    if (isLocked() || !lock()) {
      log.info("Scheduler is currently locked, update is already in progress...");
      return;
    }
    // Fetch the calendar data and build a iCal4j representation of it
    // General logic:
    // * If there is a remote calendar URL, check for updates
    // * If the local calendar is null (ie, it hasn't been created) and there is a locally cached copy of the calendar
    // load it
    // * If the local calendar is null and there is no locally cached copy throw a warning but continue operating (aka
    // manual mode)
    // * If the local calendar is not null then we've already obtained scheduling data and can't update, so just return
    if (remoteCalendarURL != null) {
      calendar = parseCalendar(remoteCalendarURL);
    } else if (calendar == null && localCalendarCacheURL != null) {
      calendar = parseCalendar(localCalendarCacheURL);
    } else if (calendar == null && localCalendarCacheURL == null) {
      log.warn("Unable to update calendar from either local or remote sources.");
    } else {
      log.debug("Calendar already exists, and {} is invalid, skipping update.",
              CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL);
      return;
    }

    // Create the scheduler if it doesn't exist, otherwise just update the polling settings.
    if (scheduler == null) {
      scheduler = generateNewScheduler(schedProps, false);
      startScheduler();
    } else {
      setupPolling(scheduler, false);
    }

    if (calendar != null) {
      try {
        // Nuke the existing jobs
        for (String name : scheduler.getJobNames(JobParameters.CAPTURE_TYPE)) {
          scheduler.deleteJob(name, JobParameters.CAPTURE_TYPE);
        }
      } catch (SchedulerException e) {
        log.error("Unable to remove previously scheduled jobs: {}.", e);
      }

      // Schedule the new ones
      setCaptureSchedule(scheduler, calendar);
    }

    logSchedules(scheduler);
    unlock();
  }

  /**
   * Reads in a calendar from either an HTTP or local source and turns it into a iCal4j Calendar object.
   * 
   * @param url
   *          The {@code URL} to read the calendar data from.
   * @return A {@code Calendar} object, or null in the case of an error or to indicate that no update should be
   *         performed.
   */
  Calendar parseCalendar(URL url) {
    URI uri;
    try {
      uri = url.toURI();
    } catch (URISyntaxException e1) {
      log.warn("{} is not a valid uri", url);
      return null;
    }
    String calendarString = null;
    try {
      if ("file".equals(url.getProtocol())) {
        if (new File(uri).exists()) {
          calendarString = IOUtils.toString(url.openStream(), "UTF-8");
        } else {
          log.debug("File {} does not exist", url);
          return null;
        }
      } else {
        HttpResponse response = null;
        try {
          QueryStringBuilder qsb = new QueryStringBuilder(url.toString());
          qsb.add("agentid", configService.getItem(CaptureParameters.AGENT_NAME));
          HttpGet get = new HttpGet(qsb.toString());
          Header[] requestHeaders = lastCalendarEtag == null ? null : new Header[] { new BasicHeader("If-None-Match",
                  lastCalendarEtag) };
          get.setHeaders(requestHeaders);
          response = trustedClient.execute(get);
          if (Response.Status.NOT_MODIFIED.equals(response.getStatusLine().getStatusCode())) {
            log.debug("Calendar has not changed");
            return null;
          } else if (response.getStatusLine().getStatusCode() == 200) {
            calendarString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            Header etagHeader = response.getFirstHeader("ETag");
            if (etagHeader != null && StringUtils.isNotBlank(etagHeader.getValue())) {
              lastCalendarEtag = etagHeader.getValue();
            }
            log.debug("Calendar updated from {}", url);
          }
        } catch (TrustedHttpClientException e) {
          log.warn("Unable to fetch file from {}.", url, e);
          return null;
        } finally {
          trustedClient.close(response);
        }
      }
    } catch (IOException e) {
      log.warn("Error parsing calendar", e);
      return null;
    }

    if (calendarString == null) {
      // If the calendar is null, which only happens the first time through
      // This case handles not having a network connection by just reading from the cached copy of the calendar
    	if (calendar == null) {
    		if (localCalendarCacheURL != null) {
    			//The file might contain Unice characters so be sure to read in UTF8
    			File file = new File(localCalendarCacheURL.getFile()); 
    			try {
    				calendarString = FileUtils.readFileToString(file, "UTF-8");
    			} catch (IOException e) {
    				log.warn("Error reading schedule file from disc", e);

    			} 

    		} else {
          log.warn("Unable to read calendar from local calendar cache because location was null.");
          return null;
        }
      } else {
        log.debug("Calendar already exists, and {} is invalid, skipping update.",
                CaptureParameters.CAPTURE_SCHEDULE_REMOTE_ENDPOINT_URL);
        return null;
      }
    }

    if (calendarString == null) {
      log.warn("Invalid calendar data, skipping parse attempt.");
      return null;
    }

    Calendar cal = null;
    try {
      // Build the calendar instance
      CalendarBuilder cb = new CalendarBuilder();
      cal = cb.build(new StringReader(calendarString));
    } catch (ParserException e) {
      log.error("Parsing error for {}: {}.", url, e);
      return null;
    } catch (IOException e) {
      log.error("I/O error for {}: {}.", url, e);
      return null;
    } catch (NullPointerException e) {
      log.error("NullPointerException for {}: {}.", url, e);
      return null;
    }

    if (cal != null && localCalendarCacheURL != null) {
      try {
        IoSupport.writeUTF8File(localCalendarCacheURL, calendarString);
      } catch (IOException e) {
        log.error("Unable to write to {}, IOException occurred", localCalendarCacheURL);
      }
    }

    return cal;
  }

  /**
   * Returns the name for every scheduled job. Job titles are their {@code UUID}s assigned from the scheduler, or
   * Unscheduled-$agent_name-$timestamp.
   * 
   * @return An array of {@code String}s containing the name of every scheduled job, or null if there is an error.
   */
  public String[] getCaptureSchedule() {
    if (scheduler != null) {
      try {
        return scheduler.getJobNames(JobParameters.CAPTURE_TYPE);
      } catch (SchedulerException e) {
        log.error("Scheduler exception: {}.", e.toString());
      }
    }
    log.warn("Internal scheduler was null, returning null!");
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#getSchedule()
   */
  public List<ScheduledEvent> getSchedule() {
    List<ScheduledEvent> events = new LinkedList<ScheduledEvent>();
    String[] jobnames = getCaptureSchedule();
    if (jobnames != null) {
      for (String jobname : jobnames) {
        try {
          JobDetail job = scheduler.getJobDetail(jobname, JobParameters.CAPTURE_TYPE);
          Trigger[] triggers = scheduler.getTriggersOfJob(jobname, JobParameters.CAPTURE_TYPE);
          for (Trigger t : triggers) {
            events.add(new ScheduledEventImpl(job.getName(), t.getFireTimeAfter(new Date()).getTime(), (Long) job
                    .getJobDataMap().get(CaptureParameters.RECORDING_DURATION)));
          }
        } catch (SchedulerException e) {
          log.warn("Scheduler exception while generating capture schedule: {}.", e);
        }
      }
    }
    return events;
  }

  /**
   * Logs a complete list of all of the scheduled events this scheduler knows about. The job count is logged at DEBUG,
   * the job details are logged at TRACE.
   */
  void logSchedules(Scheduler sched) {
    try {
      if (sched != null) {
        String[] jobnames = sched.getJobNames(JobParameters.CAPTURE_TYPE);
        log.debug("Currently scheduled jobs for capture schedule: {}", jobnames.length);
        if (log.isTraceEnabled()) {
          for (String name : jobnames) {
            log.trace("{}.", name);
          }
        }

        jobnames = sched.getJobNames(JobParameters.SUPPORT_TYPE);
        log.debug("Currently scheduled jobs for capture related schedule: {}", jobnames.length);
        if (log.isTraceEnabled()) {
          for (String name : jobnames) {
            log.trace("{}.", name);
          }
        }

        jobnames = sched.getJobNames(JobParameters.RECURRING_TYPE);
        log.debug("Currently scheduled jobs for poll schedule: {}", jobnames.length);
        if (log.isTraceEnabled()) {
          for (String name : jobnames) {
            log.trace("{}.", name);
          }
        }

        jobnames = sched.getJobNames(JobParameters.OTHER_TYPE);
        log.debug("Currently scheduled jobs for other schedule: {}", jobnames.length);
        if (log.isTraceEnabled()) {
          for (String name : jobnames) {
            log.trace("{}.", name);
          }
        }
      }
    } catch (SchedulerException e) {
      log.debug("SchedulerException in logSchedules(): {}.", e);
    }
  }

  /**
   * Gets the maximum duration for a capture as a {@code Dur} object. If the capture length is not set correctly in the
   * capture parameters then this method will default to {@code DEFAULT_MAX_CAPTURE_LENGTH}
   * 
   * @return The maximum duration of a capture.
   */
  public Dur getMaxDuration() {
    // Get the maximum duration from the config service
    long length = CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH * CaptureParameters.MILLISECONDS;
    try {
      length = Integer.parseInt(configService.getItem(CaptureParameters.CAPTURE_MAX_LENGTH))
              * CaptureParameters.MILLISECONDS;
    } catch (NumberFormatException ex) {
      log.error("NumberFormatException:  Unable to set maximum duration because value set incorrectly");
    }
    return new Dur(new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + length));
  }

  /**
   * Sets this machine's schedule based on the iCal data passed in as a parameter. Note that this call wipes all
   * currently scheduled captures and then schedules based on the new data. Also note that any files which are in the
   * way when this call tries to save the iCal attachments are overwritten without prompting.
   * 
   * @param sched
   *          The scheduler to schedule the new events on
   * @param newCal
   *          The new {@code Calendar} data
   */
  private synchronized void setCaptureSchedule(Scheduler sched, Calendar newCal) {
    log.debug("setCaptureSchedule(sched, newCal)");

    try {
      Map<Long, String> scheduledEventStarts = new Hashtable<Long, String>();
      Map<String, Date> scheduledEventEnds = new Hashtable<String, Date>();
      // Sort the events into chronological starting order
      TreeSet<VEvent> list = new TreeSet<VEvent>(new VEventStartTimeComparator());
      list.addAll(newCal.getComponents(Component.VEVENT));
      VEvent[] startAry = list.toArray(new VEvent[list.size()]);

      for (int i = 0; i < startAry.length; i++) {
        Event event = new Event(startAry[i], captureAgent, this);
        if (!event.isValidEvent()) {
          continue;
        }

        boolean skipOnError = Boolean.valueOf(configService
                .getItem(CaptureParameters.CAPTURE_SCHEDULE_DROP_EVENT_IF_CONFLICT));
        int bufferMinutes = 1;
        if (configService.getItem(CaptureParameters.CAPTURE_SCHEDULE_INTEREVENT_BUFFERTIME) != null) {
          try {
            bufferMinutes = Integer.valueOf(configService
                    .getItem(CaptureParameters.CAPTURE_SCHEDULE_INTEREVENT_BUFFERTIME));
          } catch (NumberFormatException e) {
            log.info("Unable to parse value for {}, defaulting to 1 minute",
                    CaptureParameters.CAPTURE_SCHEDULE_INTEREVENT_BUFFERTIME);
          }
        }
        long bufferTime = bufferMinutes * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS;

        // If there could be an event scheduled before this one
        if (i > 0 && startAry[i - 1] != null && scheduledEventEnds.size() > 0) {
          int j = i - 1;
          String otherUID = null;
          // Search through the list of captures which could possibly have been scheduled
          // checking to see which one is closest to us
          while (j > 0) {
            String testUID = startAry[j].getUid().getValue();
            if (scheduledEventEnds.containsKey(testUID)) {
              otherUID = testUID;
              break;
            }
            j--;
          }
          // If we found something
          if (otherUID != null) {
            Date lastEndDate = scheduledEventEnds.get(otherUID);
            if (event.getStart().before(lastEndDate)) {
              if (skipOnError) {
                log.warn("Start time for event {} is before end time of event {}!  Skipping...", event.getUID(),
                        otherUID);
                continue;
              } else {
                log.warn("Start time for event {} is before end time of event {}!  Shortening to fit...",
                        event.getUID(), otherUID);
                event.setStart(new Date(lastEndDate.getTime() + bufferTime));
              }
            } else if (ONE_MINUTE_DURATION.compareTo(new Dur(lastEndDate, event.getStart())) >= 0) {
              if (skipOnError) {
                log.warn("Start time for event {} is within one minute of event {}!  Skipping...", event.getUID(),
                        otherUID);
                continue;
              } else {
                log.warn("Start time for event {} is within one minute of event {}!  Shortening to fit...",
                        event.getUID(), otherUID);
                event.setStart(new Date(lastEndDate.getTime() + bufferTime));
              }
            }
          }
        }

        if (!event.isValidEvent()) {
          continue;
        }

        // Get the cron expression and make sure it doesn't conflict with any existing captures
        // Note that this means the order in which the scheduled events appear in the source iCal makes a functional
        // difference!
        String conflict = scheduledEventStarts.get(event.getStart().getTime());
        if (conflict != null) {
          // This case should have disappeared with MH-1253, but I'm leaving it here anyway just in case
          log.warn("Unable to schedule event {} because its starting time coinsides with event {}!", event.getUID(),
                  conflict);
          continue;
        }

        PropertyList attachments = event.getProperties(Property.ATTACH);
        scheduleEvent(sched, event, attachments);
        scheduledEventStarts.put(event.getStart().getTime(), event.getUID());
        scheduledEventEnds.put(event.getUID(), event.getEnd());
      }
    } catch (NullPointerException e) {
      log.error("Invalid calendar data, one of the start or end times is incorrect: {}.", e);
    } catch (ParseException e) {
      log.error("Parsing error: {}.", e);
    } catch (org.opencastproject.util.ConfigurationException e) {
      log.error("Configuration exception: {}.", e);
    } catch (MediaPackageException e) {
      log.error("MediaPackageException exception: {}.", e);
    } catch (MalformedURLException e) {
      log.error("MalformedURLException: {}.", e);
    }
  }

  /**
   * Schedules an event in the internal scheduler
   * 
   * @param sched
   *          The scheduler to use
   * @param event
   *          The {@code Event} to schedule
   * @param attachments
   *          The list of attachments for the VEvent
   * @throws ParseException
   * @throws org.opencastproject.util.ConfigurationException
   * @throws MalformedURLException
   * @throws MediaPackageException
   */
  private void scheduleEvent(Scheduler sched, Event event, PropertyList attachments) throws ParseException,
          org.opencastproject.util.ConfigurationException, MalformedURLException, MediaPackageException {
    CronExpression startCronExpression = getCronString(event.getStart());
    // Create the trigger
    CronTrigger trig = new CronTrigger();
    trig.setCronExpression(startCronExpression);
    trig.setName(startCronExpression.toString());
    trig.setGroup(JobParameters.CAPTURE_TYPE);
    trig.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);

    JobDetail job = new JobDetail(event.getUID(), JobParameters.CAPTURE_TYPE, StartCaptureJob.class);
    // Setup the basic job properties
    Properties props = new Properties();
    props.put(CaptureParameters.RECORDING_ID, event.getUID());
    props.put(JobParameters.JOB_POSTFIX, event.getUID());
    String endCronString = getCronString(event.getEnd()).toString();
    props.put(CaptureParameters.RECORDING_END, endCronString);

    job.getJobDataMap()
            .put(CaptureParameters.RECORDING_DURATION, event.getEnd().getTime() - event.getStart().getTime());

    setupEvent(attachments, props, job);
    job.getJobDataMap().put(JobParameters.SCHEDULER, sched);
    try {
      sched.scheduleJob(job, trig);
    } catch (SchedulerException e) {
      if (event.getStart().after(new Date())) {
        log.error("Invalid scheduling data: {}.", e);
      }
    }
  }

  /**
   * A helper function to wrap the configuration of an event. This function writes the attached files to disk and
   * generates the initial {@code Mediapackage}.
   * 
   * @param attachments
   *          The {@code PropertyList} of the capture. This contains all of the attachments and such needed to setup the
   *          directory structure.
   * @param props
   *          The system {@code Properties} for the job. This can be overridden by the properties attached to the event.
   * @param job
   *          The {@code JobDetail} instance itself. This is what everything gets attached to so that Quartz can run
   *          properly.
   * @throws org.opencastproject.util.ConfigurationException
   * @throws MediaPackageException
   * @throws MalformedURLException
   * @throws ParseException
   */
  @SuppressWarnings("unchecked")
  private void setupEvent(PropertyList attachments, Properties props, JobDetail job)
          throws org.opencastproject.util.ConfigurationException, MediaPackageException, MalformedURLException,
          ParseException {
    MediaPackage pack = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    pack.setIdentifier(new IdImpl(props.getProperty(CaptureParameters.RECORDING_ID)));

    // Create the directory we'll be capturing into
    File captureDir = new File(configService.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL),
            props.getProperty(CaptureParameters.RECORDING_ID));
    if (!captureDir.exists()) {
      try {
        FileUtils.forceMkdir(captureDir);
      } catch (IOException e) {
        log.error("IOException creating required directory {}, skipping capture.", captureDir.toString());
        return;
      }
    }

    ListIterator<Property> iter = (ListIterator<Property>) attachments.listIterator();
    // For each attachment
    while (iter.hasNext()) {
      Property p = iter.next();
      // TODO: Make this not hardcoded? Make this not depend on Apple's idea of rfc 2445?
      String filename = p.getParameter("X-APPLE-FILENAME").getValue();
      if (filename == null) {
        log.warn("No filename given for attachment, skipping.");
        continue;
      }
      // Get the contents of the attachment. Note that this assumes the attachment is a string of some sorts
      // This breaks with binary files (probably)
      String contents = propertyToString(p);

      // Handle any attachments
      try {
        // If the event has properties
        if (filename.equals(CaptureParameters.RECORDING_PROPERTIES)) {
          // Load the properties
          Properties jobProps = new Properties();
          StringReader s = new StringReader(contents);
          jobProps.load(s);
          s.close();
          // Merge them overtop of the system properties
          jobProps.putAll(props);
          // Attach the properties to the job itself
          job.getJobDataMap().put(JobParameters.CAPTURE_PROPS, jobProps);
          // And attach the properties file to the mediapackage
          pack.add(new URI(filename));
          // Handle the episode metadata
        } else if ("episode.xml".equals(filename)) {
          pack.add(new URI(filename), MediaPackageElement.Type.Catalog, MediaPackageElements.EPISODE);
          // Handle the series metadata
        } else if ("series.xml".equals(filename)) {
          pack.add(new URI(filename), MediaPackageElement.Type.Catalog, MediaPackageElements.SERIES);
        } else {
          pack.add(new URI(filename));
        }
      } catch (IOException e) {
        log.error("Unable to read properties file attached to event {}!",
                props.getProperty(CaptureParameters.RECORDING_ID));
      } catch (URISyntaxException e) {
        log.error("Unable to add file {} to mediapackage: {}.", filename, e);
      }
      job.getJobDataMap().put(JobParameters.MEDIA_PACKAGE, pack);
      // Note that we overwrite any pre-existing files with this. In other words, if there is a file called foo.txt in
      // the
      // captureDir directory and there is an attachment called foo.txt then we will overwrite the one on disk with the
      // one from the ical
      URL u = new File(captureDir, filename).toURI().toURL();
      try {
        IoSupport.writeUTF8File(u, contents);
      } catch (IOException e) {
        log.error("Unable to write to {}, IOException occurred", u.toString());
      }
    }

    job.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
    job.getJobDataMap().put(JobParameters.JOB_SCHEDULER, scheduler);
  }

  /**
   * Decodes and returns a Base64 encoded attachment as a String. Note that this function does not attempt to guess what
   * the file might actually be.
   * 
   * @param property
   *          The attachment to decode
   * @return A {@code String} representation of the attachment
   * @throws ParseException
   */
  String propertyToString(Property property) throws ParseException {
    byte[] bytes = Base64.decodeBase64(property.getValue());
    String attach = new String(bytes);
    return attach;
  }

  /**
   * Parses an date to build a cron-like time string.
   * 
   * @param date
   *          The {@code Date} you want returned in a cronstring.
   * @return A cron-like scheduling string in a {@code CronExpression} object.
   * @throws ParseException
   */
  public static CronExpression getCronString(Date date) throws ParseException {
    // TODO: Remove the deprecated calls here.
    StringBuilder sb = new StringBuilder();
    sb.append(date.getSeconds() + " ");
    sb.append(date.getMinutes() + " ");
    sb.append(date.getHours() + " ");
    sb.append(date.getDate() + " ");
    sb.append(date.getMonth() + 1 + " "); // Note: Java numbers months from 0-11, Quartz uses 1-12. Sigh.
    sb.append("? ");
    sb.append(date.getYear() + 1900); // Yay java.
    return new CronExpression(sb.toString());
  }

  /**
   * Returns the maximum length a scheduled capture can run, in milliseconds.
   * 
   * @return The maximum length in milliseconds.
   */
  public static long getMaxScheduledCaptureLength() {
    return CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH * CaptureParameters.MILLISECONDS;
  }

  /**
   * Schedules a {@code StopCaptureJob} to stop a capture at a given time.
   * 
   * @param recordingID
   *          The recordingID of the recording you wish to stop.
   * @param stop
   *          The time (in seconds since 1970) in a {@code Date} at which to stop the capture.
   * @return True if the job was scheduled, false otherwise.
   */
  public boolean scheduleUnscheduledStopCapture(String recordingID, Date stop) {
    SimpleTrigger trig = new SimpleTrigger(StopCaptureJob.TRIGGER_PREFIX + recordingID, JobParameters.SUPPORT_TYPE,
            stop);
    trig.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
    JobDetail job = new JobDetail(StopCaptureJob.JOB_PREFIX + recordingID, JobParameters.SUPPORT_TYPE,
            StopCaptureJob.class);

    job.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
    job.getJobDataMap().put(JobParameters.JOB_POSTFIX, recordingID);
    job.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);
    job.getJobDataMap().put(JobParameters.SCHEDULER, this.scheduler);

    try {
      scheduler.scheduleJob(job, trig);
    } catch (SchedulerException e) {
      log.error("Unable to schedule stop capture, failing capture attempt.");
      return false;
    }
    return true;
  }

  /**
   * Schedules a {@code StopCaptureJob} to stop a capture at a given time.
   * 
   * @param recordingID
   *          The recordingID of the recording you wish to stop.
   * @param atTime
   *          The time (in seconds since 1970) at which to stop the capture.
   * @return True if the job was scheduled, false otherwise.
   */
  public boolean scheduleUnscheduledStopCapture(String recordingID, long atTime) {
    return scheduleUnscheduledStopCapture(recordingID, new Date(atTime));
  }

  /**
   * Schedules an immediate {@code SerializeJob} for the recording. This method will manifest and zip the recording
   * before ingesting it.
   * 
   * @param recordingID
   *          The ID of the recording to it ingest.
   * @return True if the job was scheduled correctly, false otherwise.
   */
  public boolean scheduleSerializationAndIngest(String recordingID) {
    try {
      String[] jobs = scheduler.getJobNames(JobParameters.SUPPORT_TYPE);
      for (String jobname : jobs) {
        if (jobname.equals("StopCapture-" + recordingID)) {
          scheduler.deleteJob(jobname, JobParameters.SUPPORT_TYPE);
        } else if (jobname.equals("SerializeJob-" + recordingID)) {
          scheduler.deleteJob(jobname, JobParameters.SUPPORT_TYPE);
        }
      }
    } catch (SchedulerException e) {
      log.warn("Unable to remove scheduled stopCapture for recording {}.", recordingID);
    }

    // Create job and trigger
    JobDetail job = new JobDetail(SerializeJob.JOB_PREFIX + recordingID, JobParameters.SUPPORT_TYPE, SerializeJob.class);

    /*
     * Setup the trigger. The serialization job will automatically refire if it fails, so we don't need to worry about
     * it
     */
    SimpleTrigger trigger = new SimpleTrigger(SerializeJob.TRIGGER_PREFIX + recordingID, JobParameters.SUPPORT_TYPE,
            new Date());
    trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

    trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);
    trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, this.captureAgent);
    trigger.getJobDataMap().put(JobParameters.JOB_POSTFIX, recordingID);
    trigger.getJobDataMap().put(JobParameters.SCHEDULER, this.scheduler);

    try {
      scheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      log.error("Unable to schedule ingest of recording {}!", recordingID);
      return false;
    }
    return true;
  }

  /**
   * Schedules an immediate {@code IngestJob} for the recording. This method does not create a manifest or zip the
   * recording.
   * 
   * @param recordingID
   *          The ID of the recording to it ingest.
   * @return True if the job was scheduled correctly, false otherwise.
   */
  public boolean scheduleIngest(String recordingID) {
    try {
      long retryInterval;
      try {
        retryInterval = Long.parseLong(this.configService.getItem(CaptureParameters.INGEST_RETRY_INTERVAL));
      } catch (NullPointerException e) {
        log.warn(CaptureParameters.INGEST_RETRY_INTERVAL + " was null so the default "
                + IngestJob.DEFAULT_RETRY_INTERVAL + " will be used.", e);
        retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
      } catch (NumberFormatException e) {
        log.warn(
                CaptureParameters.INGEST_RETRY_INTERVAL + " was an invalid number "
                        + this.configService.getItem(CaptureParameters.INGEST_RETRY_INTERVAL) + "so the default "
                        + IngestJob.DEFAULT_RETRY_INTERVAL + " will be used.", e);
        retryInterval = IngestJob.DEFAULT_RETRY_INTERVAL;
      }
      JobDetailTriggerPair jobAndTrigger = JobCreator.createInjestJob(retryInterval, recordingID, recordingID,
              this.captureAgent, this.scheduler, this.configService);
      scheduler.scheduleJob(jobAndTrigger.getJob(), jobAndTrigger.getTrigger());
    } catch (ParseException e) {
      log.error("Invalid argument for CronTrigger: {}", e);
      return false;
    } catch (SchedulerException e) {
      log.error("Couldn't schedule task: {}", e);
      return false;
    }
    return true;
  }

  /**
   * Schedules the job in charge of deleting the old captures archived on disk once they age out or we run low on free
   * disk space.
   * 
   * @param sched
   *          The scheduler in which to schedule the cleanup job.
   */
  private void scheduleCleanJob(Scheduler sched) {

    try {
      long cleanInterval = Long.parseLong(configService.getItem(CaptureParameters.CAPTURE_CLEANER_INTERVAL))
              * CaptureParameters.MILLISECONDS;

      // Setup the polling
      JobDetail cleanJob = new JobDetail("cleanCaptures", JobParameters.OTHER_TYPE, CleanCaptureJob.class);

      cleanJob.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgent);
      cleanJob.getJobDataMap().put(JobParameters.CONFIG_SERVICE, configService);

      // Create a new trigger Name Group name Start End # of times to repeat Repeat interval
      SimpleTrigger cleanTrigger = new SimpleTrigger("cleanCapture", JobParameters.RECURRING_TYPE, new Date(), null,
              SimpleTrigger.REPEAT_INDEFINITELY, cleanInterval);

      // Schedule the update
      sched.scheduleJob(cleanJob, cleanTrigger);
    } catch (NumberFormatException e) {
      log.warn("Invalid time specified in the {} value. Job for cleaning captures not scheduled!",
              CaptureParameters.CAPTURE_CLEANER_INTERVAL);
    } catch (SchedulerException e) {
      log.error("SchedulerException while trying to schedule a cleaning job.", e);
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#stopScheduler()
   */
  public boolean stopScheduler() {
    return stopScheduler(scheduler);
  }

  protected boolean stopScheduler(Scheduler sched) {
    try {
      if (sched != null) {
        sched.pauseAll();
        return true;
      } else {
        log.error("Unable to stop scheduler, the scheduler is null!");
        return false;
      }
    } catch (SchedulerException e) {
      log.error("Unable to stop scheduler.", e);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#startScheduler()
   */
  public boolean startScheduler() {
    return startScheduler(scheduler);
  }

  /**
   * A wrapper function which starts a specific scheduler.
   * 
   * @param sched
   *          The scheduler to start
   */
  protected boolean startScheduler(Scheduler sched) {
    try {
      if (sched != null) {
        sched.start();
        return true;
      } else {
        log.error("Unable to start scheduler, the scheduler is null!");
        return false;
      }
    } catch (SchedulerException e) {
      log.error("Unable to start scheduler.", e);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#isSchedulerEnabled()
   */
  /*
   * @Override public boolean isSchedulerEnabled() { try { if (scheduler != null && scheduler.isStarted()) { return
   * true; } } catch (SchedulerException e) { log.warn("Unable to get scheduler state!"); }
   * 
   * return false; }
   */

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#enablePolling(boolean)
   */
  public void enablePolling(boolean enable) {
    log.debug("enablePolling(enable): {}", enable);
    try {
      if (enable) {
        if (scheduler != null) {
          scheduler.resumeJobGroup(JobParameters.RECURRING_TYPE);
        } else {
          log.error("Unable to start polling, the scheduler is null!");
        }
      } else {
        if (scheduler != null) {
          scheduler.pauseJobGroup(JobParameters.RECURRING_TYPE);
        }
      }
    } catch (SchedulerException e) {
      log.error("Unable to disable polling scheduler: {}.", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#getPollingTime()
   */
  public int getPollingTime() {
    return (int) (calendarPollTime / CaptureParameters.MILLISECONDS);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#getScheduleEndpoint()
   */
  public URL getScheduleEndpoint() {
    return remoteCalendarURL;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#setScheduleEndpoint(URL)
   */
  public void setScheduleEndpoint(URL url) {
    if (url == null) {
      log.warn("Invalid URL specified.");
      return;
    }
    remoteCalendarURL = url;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.Scheduler#isCalendarPollingEnabled()
   */
  public boolean isCalendarPollingEnabled() {
    return isCalendarPollingEnabled(scheduler);
  }

  /**
   * Checks to see if the is set to automatically poll for new scheduling data.
   * 
   * @param sched
   *          The scheduler to check if polling is enabled
   * @see org.opencastproject.capture.api.Scheduler#isCalendarPollingEnabled()
   */
  public boolean isCalendarPollingEnabled(Scheduler sched) {
    if (sched != null) {
      try {
        return sched.getTrigger(calendarPollingJobName, JobParameters.RECURRING_TYPE) != null;
      } catch (SchedulerException e) {
        log.warn("Scheduler exception: {}.", e);
        return false;
      }
    }
    return false;
  }

  public void shutdown() {
    shutdown(scheduler);
  }

  /**
   * Shuts down a scheduler, and waits until the schedulers have all terminated.
   * 
   * @param sched
   *          The scheduler to shut down
   */
  public void shutdown(Scheduler sched) {
    try {
      if (sched != null && !sched.isShutdown()) {
        for (String groupname : sched.getJobGroupNames()) {
          for (String jobname : sched.getJobNames(groupname)) {
            sched.deleteJob(jobname, groupname);
          }
        }
        sched.shutdown();
      }
    } catch (SchedulerException e) {
      log.warn("Finalize for scheduler {} did not execute cleanly: {}.", e);
    }
  }
}

class Event {
  private static final Logger log = LoggerFactory.getLogger(Event.class);
  private String uid;
  private Date start;
  private Date end;
  private Duration duration;
  private CaptureAgentImpl captureAgent;
  private SchedulerImpl scheduler;
  private boolean isValid = true;
  private VEvent sourceEvent = null;

  /** Simple Date Format used for start and end times of events to capture. **/
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

  public Event(VEvent event, CaptureAgentImpl agent, SchedulerImpl sched) {

    captureAgent = agent;
    scheduler = sched;

    sourceEvent = event;
    if (event == null) {
      log.error("Unable to create Event object around null VEvent.");
      isValid = false;
      return;
    }

    // Get the ID, or generate one
    uid = null;
    if (event.getUid() != null) {
      uid = event.getUid().getValue();
    } else {
      log.info("Event has no UID field, autogenerating name...");
      uid = "UnknownUID-" + event.hashCode();
    }

    // Get the start time
    start = null;
    if (event.getStartDate() != null) {
      start = event.getStartDate().getDate();
    } else {
      log.warn("Event {} has no start time, unable to schedule!", uid);
      isValid = false;
      return;
    }

    // Get the end time
    end = null;
    if (event.getEndDate() != null) {
      end = event.getEndDate().getDate();
    } else {
      log.debug("Event {} has no end time specified but may have a duration, checking...", uid);
    }

    // Determine the duration
    duration = event.getDuration();
    if (duration == null) {
      if (end != null) {
        log.debug("Event {} has start and end, calculating duration.", uid);
        duration = new Duration(start, end);
      } else {
        log.warn("Event {} has no duration and no end time, using max duration.", uid);
        end = new Date(start.getTime() + SchedulerImpl.getMaxScheduledCaptureLength());
        duration = new Duration(start, end);
      }
    } else if (end == null) {
      log.debug("Event {} has start and duration, calculating end time.", uid);
      // In this case we have a start and a duration, so calculate the stop end value
      end = duration.getDuration().getTime(start);
    } else {
      if (new Duration(start, end).getDuration().compareTo(duration.getDuration()) != 0) {
        log.error("Invalid ical data, an end and a duration were specified for event {} but do not match.", uid);
        isValid = false;
        return;
      }
    }

    doSanityChecks();
  }

  public PropertyList getProperties(String type) {
    return sourceEvent.getProperties(type);
  }

  public void setUID(String newUID) {
    uid = newUID;
  }

  public String getUID() {
    return uid;
  }

  public void setStart(Date newStart) {
    if (newStart == null) {
      log.error("Unable to set start date to null");
      return;
    }
    start = newStart;
    updateDuration();
    doSanityChecks();
  }

  public Date getStart() {
    return start;
  }

  public void setEnd(Date newEnd) {
    if (newEnd == null) {
      log.error("Unable to set end date to null");
      return;
    }
    end = newEnd;
    updateDuration();
    doSanityChecks();
  }

  public Date getEnd() {
    return end;
  }

  private void updateDuration() {
    duration = new Duration(new Dur(start, end));
  }

  public Dur getDuration() {
    return duration.getDuration();
  }

  /**
   * Checks to see if a capture should be happening now.
   * 
   * @param start
   *          The start time of the event
   * @param end
   *          The end time of the event
   * @return True if the current time is between start and end, false otherwise
   */
  private boolean captureIsHappeningNow(Date start, Date end) {
    return start.before(new Date()) && end.after(new Date());
  }

  /**
   * Checks to see if the capture agent is current capturing.
   * 
   * @return True if the agent exists, and its state is not idle
   */
  private boolean captureAgentIsCapturing() {
    return captureAgent != null && captureAgent.getAgentState() != null
            && !captureAgent.getAgentState().equals(AgentState.IDLE);
  }

  /**
   * Runs sanity checks on the the start and duration of this event. This method may change the start and/or duration
   * values if they are invalid. Note that if the sanity checks fail then isValidEvent() will also return false;
   * 
   * @return True if the sanity checks pass, false otherwise
   */
  private boolean doSanityChecks() {
    // Sanity checks on the duration
    if (this.getDuration().isNegative()) {
      log.warn("Event {} has a negative duration, skipping.", this.getUID());
      isValid = false;
      return false;
    } else if (captureIsHappeningNow(this.getStart(), this.getEnd())) {
      if (!captureAgentIsCapturing() && !mediaFilesExist()) {
        // Try to handle a capture we have just missed.
        log.warn("Event {} is scheduled for a time that has already passed,"
                + " but should be capturing.  Starting capture.", this.getUID());
        this.setStart(new Date(System.currentTimeMillis() + (1 * CaptureParameters.MILLISECONDS)));
        // Sanity check on the duration
        if (this.getDuration().isNegative()) {
          log.warn("Event {} has a negative duration, skipping.", uid);
          isValid = false;
          return false;
        }
      }
    } else if (this.getEnd().before(new Date())) {
      log.debug("Event {} is scheduled for a time that has already passed, skipping.", this.getUID());
      isValid = false;
      return false;
    } else if (this.getDuration().compareTo(new Dur(0, 0, 0, 0)) == 0) {
      log.warn("Event {} has a duration of 0, skipping.", this.getUID());
      isValid = false;
      return false;
    } else if (this.getDuration().compareTo(SchedulerImpl.ONE_MINUTE_DURATION) < 0) {
      log.warn("Event {} has a duration of less than one minute, skipping.", this.getUID());
      isValid = false;
      return false;
    } else if (this.getDuration().compareTo(scheduler.getMaxDuration()) > 0) {
      log.warn("Event {} set to longer than maximum allowed capture duration, cutting off capture at {} seconds.",
              this.getUID(), CaptureAgentImpl.DEFAULT_MAX_CAPTURE_LENGTH);
      duration.setDuration(scheduler.getMaxDuration());
      end = duration.getDuration().getTime(start);
    }
    isValid = true;
    return true;
  }

  /**
   * Tests to see if there are media files on the hard drive that are evidence of an earlier capturing. This way we can
   * check to see if the there was an earlier recording and not overwrite it.
   * 
   * @return True if there is evidence that a recording has already occurred with this UID.
   */
  private boolean mediaFilesExist() {
    try {
      XProperties properties = captureAgent.getConfigService().getAllProperties();
      String[] deviceNames = GStreamerPipelineTools.getDeviceNames(properties);
      if (deviceNames != null && deviceNames.length != 0 && deviceNames[0].contains("=")) {
        deviceNames[0] = deviceNames[0].split("=")[1];
      }
      File captureLocation = determineRootURL(uid, properties);
      for (String name : deviceNames) {
        String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_DEST;
        if (!properties.containsKey(outputProperty)) {
          // We can't check to see if this media has been created because we don't have an output location.
          return true;
        } 
        File captureDirectory = new File(captureLocation.getAbsolutePath(), uid);
        File outputFile = new File(captureDirectory.getAbsolutePath(), properties.get(outputProperty).toString());
        if (outputFile.exists()) {
          // At least one of the media files exist so return true.
          return true;
        }
      }
    } catch (InvalidCaptureDevicesSpecifiedException e) {
      log.warn("There were no capture devices specified in the properties so we can't check to see if the media files exist. ");
      return true;
    } catch (IOException e) {
      log.warn("Couldn't determine whether media files exists because " + e);
      return true;
    }
    return false;
    
   
  }

  /**
   * Determines the root URL and ID from the recording's properties //TODO: What if the properties object contains a
   * character in the recording id or root url fields that is invalid for the filesystem?
   * 
   * @throws IOException
   */
  private File determineRootURL(String uid, XProperties props) throws IOException {
    File baseDir;
    if (props == null) {
      log.info("Properties are null for recording, guessing that the root capture dir is java.io.tmpdir...");
      props = new XProperties();
      props.setProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, System.getProperty("java.io.tmpdir"));
    }

    // Figures out where captureDir lives
    if (props.containsKey(CaptureParameters.RECORDING_ROOT_URL)) {
      baseDir = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL));
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        // In this case they've set both the root URL and the recording ID, so we're done.
        uid = props.getProperty(CaptureParameters.RECORDING_ID);
      } else {
        // In this case they've set the root URL, but not the recording ID. Get the id from that url instead then.
        log.debug("{} was set, but not {}.", CaptureParameters.RECORDING_ROOT_URL, CaptureParameters.RECORDING_ID);
        uid = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL)).getName();
        props.put(CaptureParameters.RECORDING_ID, uid);
      }
    } else {
      File cacheDir = new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
      // If there is a recording ID use it, otherwise it's unscheduled so just grab a timestamp
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        uid = props.getProperty(CaptureParameters.RECORDING_ID);
        baseDir = new File(cacheDir, uid);
      } else {
        // Unscheduled capture, use a timestamp value instead
        uid = "Unscheduled-" + props.getProperty(CaptureParameters.AGENT_NAME) + "-" + System.currentTimeMillis();
        props.setProperty(CaptureParameters.RECORDING_ID, uid);
        baseDir = new File(cacheDir, uid);
      }
      props.put(CaptureParameters.RECORDING_ROOT_URL, baseDir.getCanonicalPath());
    }
    return baseDir;
  }
  
  /**
   * Returns true if this event is valid (ie, scheduleable)
   * 
   * @return True if the event can be scheduled, false otherwise.
   */
  public boolean isValidEvent() {
    return isValid;
  }
}

class VEventStartTimeComparator implements Comparator<VEvent>, Serializable {

  private static final long serialVersionUID = 4590767229806913896L;

  @Override
  public int compare(VEvent o1, VEvent o2) {
    if (o1.getStartDate() != null && o2.getStartDate() != null) {
      return o1.getStartDate().getDate().compareTo(o2.getStartDate().getDate());
    } else if (o1.getStartDate() == null && o2.getStartDate() != null) {
      return 1;
    } else if (o1.getStartDate() != null && o2.getStartDate() == null) {
      return -1;
    } else {
      return 0;
    }
  }

}
