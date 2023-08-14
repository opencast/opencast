/**
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


package org.opencastproject.ingest.scanner;

import static org.opencastproject.security.util.SecurityUtil.getUserAndOrganization;
import static org.opencastproject.util.data.Collections.dict;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The inbox scanner monitors a directory for incoming media packages.
 * <p>
 * There is one InboxScanner instance per inbox. Each instance is configured by a config file in
 * <code>.../etc/load</code> named <code>&lt;inbox-scanned-pid&gt;-&lt;name&gt;.cfg</code> where <code>name</code>
 * can be arbitrarily chosen and has no further meaning. <code>inbox-scanned-pid</code> must confirm to the PID given to
 * the InboxScanner in the declarative service (DS) configuration <code>OSGI-INF/inbox-scanner-service.xml</code>.
 *
 * <h2>Implementation notes</h2>
 * Monitoring leverages Apache FileInstall by implementing {@link ArtifactInstaller}.
 *
 * @see Ingestor
 */
@Component(
  immediate = true,
  service = {
    ArtifactInstaller.class,
    ManagedService.class
  },
  property = {
    "service.pid=org.opencastproject.ingest.scanner.InboxScannerService",
    "service.description=Inbox Scanner"
  }
)
public class InboxScannerService implements ArtifactInstaller, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(InboxScannerService.class);

  /** The configuration key to use for determining the user to run as for ingest */
  public static final String USER_NAME = "user.name";

  /** The configuration key to use for determining the user's organization */
  public static final String USER_ORG = "user.organization";

  /** The configuration key to use for determining the workflow definition to use for ingest */
  public static final String WORKFLOW_DEFINITION = "workflow.definition";

  /** The configuration key to use for determining the default media flavor */
  public static final String MEDIA_FLAVOR = "media.flavor";


  /** The configuration key to use for determining the workflow configuration to use for ingest */
  public static final String WORKFLOW_CONFIG = "workflow.config";

  /** The configuration key to use for determining the inbox path */
  public static final String INBOX_PATH = "inbox.path";

  /** The configuration key to use for determining the polling interval in ms. */
  public static final String INBOX_POLL = "inbox.poll";

  public static final String INBOX_THREADS = "inbox.threads";
  public static final String INBOX_TRIES = "inbox.tries";
  public static final String INBOX_TRIES_BETWEEN_SEC = "inbox.tries.between.sec";

  public static final String INBOX_METADATA_REGEX = "inbox.metadata.regex";
  public static final String INBOX_DATETIME_FORMAT = "inbox.datetime.format";
  public static final String INBOX_METADATA_FFPROBE = "inbox.metadata.ffprobe";
  public static final String INBOX_SCHEDULE_MATCH = "inbox.schedule.match";
  public static final String INBOX_SCHEDULE_MATCH_THRESHOLD = "inbox.schedule.match.threshold";

  public static final String FFPROBE_BINARY_CONFIG = "org.opencastproject.inspection.ffprobe.path";
  public static final String FFPROBE_BINARY_DEFAULT = "ffprobe";

  private IngestService ingestService;
  private SecurityService securityService;
  private UserDirectoryService userDir;
  private OrganizationDirectoryService orgDir;
  private SeriesService seriesService;
  private SchedulerService schedulerService;
  protected Workspace workspace;

  private ComponentContext cc;

  private volatile Ingestor ingestor = null;
  private volatile Configuration fileInstallCfg = null;

  /** OSGi callback. */
  // synchronized with updated(Dictionary)
  @Activate
  public synchronized void activate(ComponentContext cc) {
    this.cc = cc;
  }

  /** OSGi callback. */
  @Deactivate
  public void deactivate() {
    removeFileInstallCfg();
  }

  // synchronized with activate(ComponentContext)
  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    // build scanner configuration
    if (properties == null) {
      return;
    }
    final String orgId = getCfg(properties, USER_ORG);
    final String userId = getCfg(properties, USER_NAME);
    final String mediaFlavor = getCfg(properties, MEDIA_FLAVOR);
    final String workflowDefinition = Objects.toString(properties.get(WORKFLOW_DEFINITION), null);
    final Map<String, String> workflowConfig = getCfgAsMap(properties, WORKFLOW_CONFIG);
    final int interval = NumberUtils.toInt(Objects.toString(properties.get(INBOX_POLL), "5000"));
    final File inbox = new File(getCfg(properties, INBOX_PATH));
    if (!inbox.isDirectory()) {
      try {
        FileUtils.forceMkdir(inbox);
      } catch (IOException e) {
        throw new ConfigurationException(INBOX_PATH,
            String.format("%s does not exists and could not be created", inbox.getAbsolutePath()));
      }
    }
    /* We need to be able to read from the inbox to get files from there */
    if (!inbox.canRead()) {
      throw new ConfigurationException(INBOX_PATH, String.format("Cannot read from %s", inbox.getAbsolutePath()));
    }
    /* We need to be able to write to the inbox to remove files after they have been ingested */
    if (!inbox.canWrite()) {
      throw new ConfigurationException(INBOX_PATH, String.format("Cannot write to %s", inbox.getAbsolutePath()));
    }
    final int maxThreads = NumberUtils.toInt(Objects.toString(properties.get(INBOX_THREADS), "1"));
    final int maxTries = NumberUtils.toInt(Objects.toString(properties.get(INBOX_TRIES), "3"));
    final int secondsBetweenTries = NumberUtils.toInt(Objects.toString(properties.get(INBOX_TRIES_BETWEEN_SEC), "300"));

    // Metadata parsing configuration
    var metadataPattern = Optional.ofNullable(properties.get(INBOX_METADATA_REGEX))
            .map(Objects::toString)
            .map(Pattern::compile);
    var dateFormatter = Optional.ofNullable(properties.get(INBOX_DATETIME_FORMAT))
            .map(Objects::toString)
            .map(s -> new DateTimeFormatterBuilder().appendPattern(s)
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .toFormatter())
            .orElse(DateTimeFormatter.ISO_DATE_TIME);
    var ffprobe = BooleanUtils.toBoolean((String) properties.get(INBOX_METADATA_FFPROBE))
            ? Objects.toString(cc.getBundleContext().getProperty(FFPROBE_BINARY_CONFIG), FFPROBE_BINARY_DEFAULT)
            : null;
    var matchSchedule = BooleanUtils.toBoolean((String) properties.get(INBOX_SCHEDULE_MATCH));
    var matchThreshold = NumberUtils.toFloat((String) properties.get(INBOX_SCHEDULE_MATCH_THRESHOLD), -1F);

    var securityContext = getUserAndOrganization(securityService, orgDir, orgId, userDir, userId)
            .map(a -> new SecurityContext(securityService, a.getB(), a.getA()));

    if (securityContext.isEmpty()) {
      logger.warn("Could not create security context for user {}, organization {}. "
          + "Either the organization or the user does not exist (yet).", userId, orgId);
    }
    for (int attempts = 0; attempts < 25 && securityContext.isEmpty(); attempts++) {
      logger.info("Waiting for security context...");
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for security context");
      }
      securityContext = getUserAndOrganization(securityService, orgDir, orgId, userDir, userId)
          .map(a -> new SecurityContext(securityService, a.getB(), a.getA()));
    }
    if (securityContext.isEmpty()) {
      logger.warn("Security context for user {} and organization {} is still empty. Giving up.", userId, orgId);
      return;
    }

    // remove old file install configuration
    removeFileInstallCfg();
    // set up new file install config
    fileInstallCfg = configureFileInstall(cc.getBundleContext(), inbox, interval);
    // create new scanner
    this.ingestor = new Ingestor(ingestService, securityContext.get(), workflowDefinition,
            workflowConfig, mediaFlavor, inbox, maxThreads, seriesService, maxTries, secondsBetweenTries,
            metadataPattern, dateFormatter, schedulerService, ffprobe, matchSchedule, matchThreshold,
            workspace);
    new Thread(ingestor).start();
    logger.info("Now watching inbox {}", inbox.getAbsolutePath());
  }

  private void removeFileInstallCfg() {
    if (fileInstallCfg != null) {
      try {
        fileInstallCfg.delete();
      } catch (IOException e) {
        logger.error("Failed to delete file install configuration", e);
      }
      fileInstallCfg = null;
    }
  };

  /**
   * Setup an Apache FileInstall configuration for the inbox folder this scanner is responsible for.
   *
   * see section 104.4.1 Location Binding, paragraph 4, of the OSGi Spec 4.2 The correct permissions are needed in order
   * to set configuration data for a bundle other than the calling bundle itself.
   */
  private static Configuration configureFileInstall(BundleContext bc, File inbox, int interval) {
    final ServiceReference caRef = bc.getServiceReference(ConfigurationAdmin.class.getName());
    if (caRef == null) {
      throw new Error("Cannot obtain a reference to the ConfigurationAdmin service");
    }
    final Dictionary<String, String> fileInstallConfig = dict(tuple("felix.fileinstall.dir", inbox.getAbsolutePath()),
            tuple("felix.fileinstall.poll", Integer.toString(interval)),
            tuple("felix.fileinstall.subdir.mode", "recurse"));

    // update file install config with the new directory
    try {
      final String fileInstallBundleLocation = bc.getServiceReferences("org.osgi.service.cm.ManagedServiceFactory",
              "(service.pid=org.apache.felix.fileinstall)")[0].getBundle().getLocation();
      final Configuration conf = ((ConfigurationAdmin) bc.getService(caRef)).createFactoryConfiguration(
              "org.apache.felix.fileinstall", fileInstallBundleLocation);
      conf.update(fileInstallConfig);
      return conf;
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  // --

  // FileInstall callback, called on a different thread
  // Attention: This method may be called _before_ the updated(Dictionary) which means that config parameters
  // are not set yet.
  @Override
  public boolean canHandle(final File artifact) {
    return ingestor != null && ingestor.canHandle(artifact);
  }

  @Override
  public void install(final File artifact) throws Exception {
    if (ingestor != null) {
      logger.trace("install(): {}", artifact.getName());
      ingestor.ingest(artifact);
    }
  }

  @Override
  public void update(File artifact) {
    logger.trace("update(): {}", artifact.getName());
  }

  @Override
  public void uninstall(File artifact) {
    if (ingestor != null) {
      logger.trace("uninstall(): {}", artifact.getName());
      ingestor.cleanup(artifact);
    }
  }

  // --

  /** OSGi callback to set the ingest service. */
  @Reference
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi callback to set the security service. */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to set the user directory. */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDir = userDirectoryService;
  }

  /** OSGi callback to set the organization directory server. */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.orgDir = organizationDirectoryService;
  }

  /**
   * Get a mandatory, non-blank value from a dictionary.
   *
   * @throws ConfigurationException
   *           key does not exist or its value is blank
   */
  private static String getCfg(Dictionary d, String key) throws ConfigurationException {
    Object p = d.get(key);
    if (p == null)
      throw new ConfigurationException(key, "does not exist");
    String ps = p.toString();
    if (StringUtils.isBlank(ps))
      throw new ConfigurationException(key, "is blank");
    return ps;
  }

  private static Map<String, String> getCfgAsMap(final Dictionary d, final String key) {

    HashMap<String, String> config = new HashMap<>();
    if (d == null) return config;
    for (Enumeration e = d.keys(); e.hasMoreElements();) {
      final String dKey = Objects.toString(e.nextElement());
      if (dKey.startsWith(key)) {
        config.put(dKey.substring(key.length() + 1), Objects.toString(d.get(dKey), null));
      }
    }
    return config;
  }

  @Reference
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Reference
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
