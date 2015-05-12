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
package org.opencastproject.loadtest.impl;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.systems.MatterhornConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.Properties;

/**
 * A single load test that can be run concurrently with other load tests. This will ingest a configurable number of
 * mediapackages to a matterhorn server with different intervals set between them.
 **/
public class LoadTest implements Runnable {
  // The key for the storage location for use as a default location to find and process the source media package.
  public static final String BUNDLE_CONTEXT_STORAGE_DIR = "org.opencastproject.storage.dir";
  // The key to look for the default workflow to apply to the load testing.
  public static final String BUNDLE_CONTEXT_DEFAULT_WORKFLOW = "org.opencastproject.workflow.default.definition";
  /** Constants **/
  public static final int MILLISECONDS_IN_SECONDS = 1000;
  public static final int SECONDS_IN_MINUTES = 60;
  public static final String DEFAULT_WORKFLOW_ID = "full";
  // Key for the server that we will attempt to load test.
  public static final String CORE_ADDRESS_KEY = "org.opencastproject.loadtest.core.url";
  // Key for the workspace that we will generate the media packages in to ingest.
  public static final String WORKSPACE_KEY = "org.opencastproject.loadtest.workspace";
  // Key for the location to find a media package to ingest with.
  public static final String SOURCE_MEDIA_PACKAGE_KEY = "org.opencastproject.loadtest.source.media.package";
  // Key for the number of seconds between checking to see if the jobs are finished ingesting.
  public static final String JOB_CHECK_INTERVAL_KEY = "org.opencastproject.loadtest.job.check.interval";
  // Key for the series of ingests to do, how many in each group e.g. 2, 3, 4
  public static final String PACKAGE_DISTRIBUTION_KEY = "org.opencastproject.loadtest.package.distribution";
  // Key for the time to wait before starting a group of ingests.
  public static final String PACKAGE_DISTRIBUTION_TIMINGS_KEY = "org.opencastproject.loadtest.package.distribution.timings";
  // Key for the boolean value that determines whether to use curl or the built in java ingestion.
  public static final String USE_CURL_KEY = "org.opencastproject.loadtest.use.curl";
  // Key for the workflow id to use for the ingest.
  public static final String WORKFLOW_KEY = "org.opencastproject.loadtest.workflow";
  /** Default Values. **/
  public static final int DEFAULT_JOB_CHECK_INTERVAL = 5;
  private static final int DEFAULT_PACKAGE_DISTRIBUTION_VALUE = 1;
  private static final int DEFAULT_PACKAGE_DISTRIBUTION_TIMING_VALUE = 0;

  // The logger.
  private static final Logger logger = LoggerFactory.getLogger(LoadTest.class);
  // The location of the core to load test against.
  private String coreAddress = null;
  // The path where we can duplicate our media package.
  private String workspaceLocation = null;
  // The path to find the source media package that will be duplicated and ingested.
  private String sourceMediaPackageLocation = null;
  // The rate at which to check the ingest jobs to see if they have started executing on the core.
  private int jobCheckInterval = -1;

  // REST Endpoint Username
  public static final String USER_NAME = "matterhorn_system_account";
  // REST Endpoint Password
  public static final String PASSWORD = "CHANGE_ME";

  // The distribution of the number of packages to ingest
  private int[] packageDistribution = { 1 };
  // the amount of time in between each set of ingests in minutes
  private int[] packageDistributionTiming = { 0 };

  // The http client used to communicate with the core
  private TrustedHttpClient client = null;

  // Configuration for Load Testing.
  private Properties properties = null;

  // The component context this load testing is operating in.
  private ComponentContext componentContext = null;

  // The unique identifier for the particular workflow to use for load testing.
  private String workflowID = "full";

  /**
   * Creates a load test with a particular configuration.
   *
   * @param properties
   *          The incoming properties to use for load testing.
   * @param client
   *          The client to use to connect to the system to load test.
   * @param componentContext
   *          The context to use as a backup if some of the properties are not specified in the load testing
   *          configuration or if replacement variables are specified.
   */
  public LoadTest(Properties properties, TrustedHttpClient client, ComponentContext componentContext) {
    this.properties = properties;
    this.client = client;
    this.componentContext = componentContext;

    try {
      updateImportantProperties(properties);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * Begins the load testing as its own thread.
   */
  @Override
  public void run() {
    try {
      updateImportantProperties(properties);
    } catch (InvalidConfigurationException e1) {
      logger.error(e1.getMessage());
    } catch (URISyntaxException e1) {
      logger.error(e1.getMessage());
    }
    logger.info("Starting load test on core " + coreAddress + " with distribution " + getPrettyPackageDistribution()
            + "@" + getPrettyPackageDistributionTimings());
    logger.info("Creating Workspace");
    try {
      FileUtils.forceMkdir(new File(workspaceLocation));
    } catch (IOException e) {
      logger.error("Had trouble creating workspace at " + workspaceLocation + " because " + e.getMessage());
    }

    if (this.packageDistribution.length != this.packageDistributionTiming.length) {
      logger.warn("The length of the distribution must be 1 greater than the number of package distribution timings. ");
      return;
    }
    long delay = 0;
    LinkedList<IngestionGroup> ingestGroups = new LinkedList<IngestionGroup>();
    logger.info("Creating " + this.packageDistribution.length + " ingestion groups. " + getPrettyPackageDistribution());
    for (int i = 0; i < this.packageDistribution.length; i++) {
      delay += this.packageDistributionTiming[i];
      IngestionGroup ingestionGroup = new IngestionGroup(this.packageDistribution[i], delay, this, client);
      ingestGroups.add(ingestionGroup);
    }

    LinkedList<IngestJob> ingestJobs = new LinkedList<IngestJob>();

    for (IngestionGroup ingestionGroup : ingestGroups) {
      ingestJobs.addAll(ingestionGroup.getJobs());
    }

    while (!ThreadCounter.allDone()) {
      try {
        Thread.sleep(15 * MILLISECONDS_IN_SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      logger.info("There are still " + ThreadCounter.getCount() + " threads that are executing.");
    }

    logger.info("Load Testing script is finished now just to wait for processing.");
  }

  /**
   * Update the important properties for load testing.
   *
   * @param properties
   *          The new properties
   * @throws InvalidConfigurationException
   *           Thrown if configurations are not set properly.
   * @throws URISyntaxException
   */
  @SuppressWarnings("unchecked")
  private void updateImportantProperties(Dictionary properties) throws InvalidConfigurationException,
          URISyntaxException {
    updateCoreAddress(properties);
    updateWorkflowID(properties);
    updateWorkspaceLocation(properties);
    updateSourceMediaPackageLocation(properties);
    updateJobCheckInterval(properties);
    updatePackageDistribution(properties);
    updatePackageDistributionTimings(properties);
  }

  /**
   * Sets a new URI to hit the core with.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updateCoreAddress(Dictionary properties) throws InvalidConfigurationException {
    String newCoreAddress = StringUtils.trimToNull((String) properties.get(CORE_ADDRESS_KEY));
    if (newCoreAddress != null) {
      coreAddress = newCoreAddress;
    } else if (componentContext != null && componentContext.getBundleContext() != null) {
      // Use the core address as a default.
      coreAddress = componentContext.getBundleContext().getProperty(MatterhornConstants.SERVER_URL_PROPERTY);
      if (coreAddress == null) {
        throw new InvalidConfigurationException(
                "The core address must be set in the configuration file so that loadtesting will occur. It isn't set in {FELIX_HOME}/conf/config.properties or {FELIX_HOME}/conf/services/org.opencastproject.loadtest.impl.LoadTestFactory.properties");
      }
    }
  }

  /**
   * Sets a new workflow id to process the media package with.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updateWorkflowID(Dictionary properties) throws InvalidConfigurationException {
    String newWorkflowID = StringUtils.trimToNull((String) properties.get(WORKFLOW_KEY));
    if (newWorkflowID != null) {
      workflowID = newWorkflowID;
    } else if (componentContext != null && componentContext.getBundleContext() != null) {
      // Use the core address as a default.
      workflowID = componentContext.getBundleContext().getProperty(BUNDLE_CONTEXT_DEFAULT_WORKFLOW);
      if (workflowID == null) {
        workflowID = DEFAULT_WORKFLOW_ID;
        logger.warn("No workflow id set in {FELIX_HOME}/conf/config.properties or {FELIX_HOME}/conf/services/org.opencastproject.loadtest.impl.LoadTestFactory.properties so default of "
                + DEFAULT_WORKFLOW_ID + " will be used.");
      }
    }
  }

  /**
   * Sets a new workspace location.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updateWorkspaceLocation(Dictionary properties) throws InvalidConfigurationException, URISyntaxException {
    boolean foundWorkspaceLocation = false;
    if (properties != null) {
      String newWorkspaceLocation = StringUtils.trimToNull((String) properties.get(WORKSPACE_KEY));
      if (newWorkspaceLocation != null) {
        URI newWorkspace = new URI(newWorkspaceLocation);
        if (newWorkspace != null) {
          workspaceLocation = newWorkspace.getPath();
          logger.debug("Workspace Location: " + newWorkspace);
          foundWorkspaceLocation = true;
        }
      }
    }
    if (!foundWorkspaceLocation && componentContext != null && componentContext.getBundleContext() != null) {
      // Try to get the storage location/loadtest/ as a default.
      workspaceLocation = StringUtils.trimToNull(componentContext.getBundleContext().getProperty(
              BUNDLE_CONTEXT_STORAGE_DIR));
      if (workspaceLocation != null) {
        workspaceLocation = workspaceLocation + "/loadtest/workspace/";
        foundWorkspaceLocation = true;
      }
    }
    if (!foundWorkspaceLocation) {
      throw new InvalidConfigurationException(
              "The workspace must be set in the configuration file so that loadtesting will occur. It isn't set in {FELIX_HOME}/conf/config.properties or {FELIX_HOME}/conf/services/org.opencastproject.loadtest.impl.LoadTestFactory.properties");
    }
  }

  /**
   * Sets a new source media package location.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updateSourceMediaPackageLocation(Dictionary properties) throws InvalidConfigurationException,
          URISyntaxException {
    Boolean foundSourceMediaPackageLocation = false;
    String sourceMediaPackageLocation = StringUtils.trimToNull((String) properties.get(SOURCE_MEDIA_PACKAGE_KEY));
    if (sourceMediaPackageLocation != null) {
      URI newSourceMediaPackage = new URI(sourceMediaPackageLocation);
      if (newSourceMediaPackage != null) {
        this.sourceMediaPackageLocation = newSourceMediaPackage.toString();
        foundSourceMediaPackageLocation = true;
      }
    }

    if (!foundSourceMediaPackageLocation) {
      // Try to get the storage location as a default.
      this.sourceMediaPackageLocation = StringUtils.trimToNull(componentContext.getBundleContext().getProperty(
              BUNDLE_CONTEXT_STORAGE_DIR));
      if (this.sourceMediaPackageLocation != null) {
        this.sourceMediaPackageLocation += "/loadtest/source/media.zip";
        foundSourceMediaPackageLocation = true;
      }
    }

    if (!foundSourceMediaPackageLocation) {
      throw new InvalidConfigurationException(
              "The source media package must be set in the configuration file so that loadtesting will occur. It isn't set in {FELIX_HOME}/conf/config.properties or {FELIX_HOME}/conf/services/org.opencastproject.loadtest.impl.LoadTestFactory.properties");
    }
  }

  /**
   * Updates to a new interval in seconds to check the ingest jobs.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updateJobCheckInterval(Dictionary properties) {
    String jobCheckIntervalInputString = StringUtils.trimToNull((String) properties.get(JOB_CHECK_INTERVAL_KEY));
    if (jobCheckIntervalInputString != null) {
      try {
        jobCheckInterval = Integer.parseInt(jobCheckIntervalInputString);
        if (jobCheckInterval < 0) {
          jobCheckInterval *= -1;
        }
        logger.info("Set ingest job check interval to {}", jobCheckInterval);
      } catch (NumberFormatException e) {
        jobCheckInterval = DEFAULT_JOB_CHECK_INTERVAL;
        logger.warn("Can not set job check interval to \"" + jobCheckIntervalInputString + "\". "
                + JOB_CHECK_INTERVAL_KEY + " must be an integer. It is set to default " + DEFAULT_JOB_CHECK_INTERVAL);
      }
    } else {
      jobCheckInterval = DEFAULT_JOB_CHECK_INTERVAL;
      logger.info("Setting job check interval to default " + DEFAULT_JOB_CHECK_INTERVAL + " seconds.");
    }
  }

  /**
   * Gets the distribution of media packages to ingest.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updatePackageDistribution(Dictionary properties) throws InvalidConfigurationException {
    String packageDistributionsInputString = StringUtils.trimToNull((String) properties.get(PACKAGE_DISTRIBUTION_KEY));
    int packageDistributionValue = 0;
    if (packageDistributionsInputString != null) {
      logger.info("Setting package distribution to " + packageDistributionsInputString);
      String[] groups = packageDistributionsInputString.split(",");
      int[] distributions = new int[groups.length];
      String packageDistributionInputString = "";
      for (int i = 0; i < groups.length; i++) {
        packageDistributionInputString = groups[i];
        packageDistributionValue = 0;
        try {
          packageDistributionValue = Integer.parseInt(packageDistributionInputString);
          if (packageDistributionValue < 0) {
            packageDistributionValue *= -1;
          }
          logger.debug("Set this package distribution to {}", packageDistributionValue);
        } catch (NumberFormatException e) {
          packageDistributionValue = DEFAULT_PACKAGE_DISTRIBUTION_VALUE;
          logger.warn("Can not set current package distribution to {}. {} must be an integer. It is set to default "
                  + DEFAULT_PACKAGE_DISTRIBUTION_VALUE, packageDistributionsInputString, this.packageDistribution);
        }
        distributions[i] = packageDistributionValue;
      }
      this.packageDistribution = distributions;
    } else {
      logger.info("No package distribution set for load testing so default of 1 load test will be done. ");
      this.packageDistribution = new int[1];
      this.packageDistribution[0] = 1;
    }
  }

  /**
   * Gets the time between ingesting each group of ingests.
   *
   * @param properties
   *          The new properties to extract the property from.
   * @throws InvalidConfigurationException
   *           Thrown if property doesn't exist
   */
  @SuppressWarnings("unchecked")
  private void updatePackageDistributionTimings(Dictionary properties) throws InvalidConfigurationException {
    String packageDistributionTimingsInputString = StringUtils.trimToNull((String) properties
            .get(PACKAGE_DISTRIBUTION_TIMINGS_KEY));
    int packageDistributionTimingValue = 0;
    if (packageDistributionTimingsInputString != null) {
      logger.info("Setting package distribution timing to " + packageDistributionTimingsInputString);
      String[] groups = packageDistributionTimingsInputString.split(",");
      int[] distributions = new int[groups.length];
      String packageDistributionTimingInputString = "";
      for (int i = 0; i < groups.length; i++) {
        packageDistributionTimingInputString = groups[i];
        packageDistributionTimingValue = 0;
        try {
          packageDistributionTimingValue = Integer.parseInt(packageDistributionTimingInputString);
          if (packageDistributionTimingValue < 0) {
            packageDistributionTimingValue *= -1;
          }
          logger.debug("Set package distribution timing value to {}", packageDistributionTimingValue);
        } catch (NumberFormatException e) {
          packageDistributionTimingValue = DEFAULT_PACKAGE_DISTRIBUTION_TIMING_VALUE;
          logger.warn("Can not set current package distribution to {}. {} must be an integer. It is set to default "
                  + DEFAULT_PACKAGE_DISTRIBUTION_VALUE, packageDistributionTimingsInputString, packageDistribution);
        }
        distributions[i] = packageDistributionTimingValue;
      }
      packageDistributionTiming = distributions;
    } else {
      logger.info("No package distribution timings set for load testing so default of 0 delay will be done.");
      this.packageDistributionTiming = new int[1];
      this.packageDistributionTiming[0] = 0;
    }
  }

  /**
   * Start the thread to check if ingest jobs have finished.
   *
   * @param ingestJobs
   *          The list of ingests to check.
   */
  private void createJobChecker(LinkedList<IngestJob> ingestJobs) {
    JobChecker jobChecker = new JobChecker(ingestJobs, this, client);
    Thread thread = new Thread(jobChecker);
    thread.start();
  }

  /**
   * Shuts down the load testing.
   */
  public void stop() {
    while (ThreadCounter.getCount() > 0) {
      ThreadCounter.subtract();
    }
    logger.info("Deactivating Load Test.");
  }

  public String getCoreAddress() {
    return coreAddress;
  }

  public int getJobCheckInterval() {
    return jobCheckInterval;
  }

  public String getWorkspaceLocation() {
    return workspaceLocation;
  }

  public String getSourceMediaPackageLocation() {
    return sourceMediaPackageLocation;
  }

  public String getPrettyPackageDistribution() {
    return getPrettyIntArray(packageDistribution);
  }

  public String getPrettyPackageDistributionTimings() {
    return getPrettyIntArray(packageDistributionTiming);
  }

  /**
   * Creates a string that is a nice representation of an array of ints.
   *
   * @param array
   *          The array to create the string from.
   * @return The String representing the array.
   */
  public String getPrettyIntArray(int[] array) {
    String returnString = "{ ";
    for (int number : array) {
      returnString += number + " ";
    }
    returnString += "}";
    return returnString;
  }

  public String getWorkflowID() {
    return workflowID;
  }
}
