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

package org.opencastproject.dataloader;

import static com.entwinemedia.fn.Prelude.chuck;
import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.OpencastDctermsDublinCore;
import org.opencastproject.metadata.dublincore.OpencastDctermsDublinCore.Series;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A data loader to populate events provider with sample data for testing scalability.
 */
public class EventsLoader {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(EventsLoader.class);

  /** The series service */
  protected SeriesService seriesService = null;

  /** The workflow service */
  protected WorkflowService workflowService = null;

  /** The scheduler service */
  protected SchedulerService schedulerService = null;

  protected AssetManager assetManager = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** The workspace */
  protected Workspace workspace = null;

  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

  private String systemUserName;

  private final AccessControlList defaultAcl = new AccessControlList(
          new AccessControlEntry("ROLE_ADMIN", Permissions.Action.WRITE.toString(), true),
          new AccessControlEntry("ROLE_ADMIN", Permissions.Action.READ.toString(), true),
          new AccessControlEntry("ROLE_USER", Permissions.Action.READ.toString(), true));

  /**
   * Callback on component activation.
   */
  protected void activate(ComponentContext cc) throws Exception {
    boolean loadTestData = BooleanUtils
            .toBoolean(cc.getBundleContext().getProperty("org.opencastproject.dataloader.testdata"));

    String csvPath = StringUtils.trimToNull(cc.getBundleContext().getProperty("org.opencastproject.dataloader.csv"));

    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);

    File csv = new File(csvPath);

    // Load the demo users, if necessary
    if (loadTestData && csv.exists() && serviceRegistry.count(null, null) == 0) {
      // Load events by CSV file
      new Loader(csv).start();
    }
  }

  private void addArchiveEntry(final MediaPackage mediaPackage) {
    final User user = securityService.getUser();
    final Organization organization = securityService.getOrganization();
    singleThreadExecutor.execute(new Runnable() {
      @Override
      public void run() {
        SecurityUtil.runAs(securityService, organization, user, new Effect0() {
          @Override
          protected void run() {
            assetManager.takeSnapshot(DEFAULT_OWNER, mediaPackage);
          }
        });
      }
    });
  }

  private void addWorkflowEntry(MediaPackage mediaPackage) throws Exception {
    WorkflowDefinition def = workflowService.getWorkflowDefinitionById("full");
    WorkflowInstance workflowInstance = new WorkflowInstanceImpl(def, mediaPackage, null, securityService.getUser(),
            securityService.getOrganization(), new HashMap<String, String>());
    workflowInstance.setState(WorkflowState.SUCCEEDED);

    String xml = WorkflowParser.toXml(workflowInstance);

    // create job
    Job job = serviceRegistry.createJob(WorkflowService.JOB_TYPE, "START_WORKFLOW", null, null, false);
    job.setStatus(Status.FINISHED);
    job.setPayload(xml);
    job = serviceRegistry.updateJob(job);

    workflowInstance.setId(job.getId());
    workflowService.update(workflowInstance);
  }

  private void addSchedulerEntry(EventEntry event, MediaPackage mediaPackage) throws Exception {
    Date endDate = new DateTime(event.getRecordingDate().getTime()).plusMinutes(event.getDuration()).toDate();
    schedulerService.addEvent(event.getRecordingDate(), endDate, event.getCaptureAgent(),
            Collections.<String> emptySet(), mediaPackage, Collections.<String, String> emptyMap(),
            Collections.<String, String> emptyMap(), Opt.<Boolean> none(), Opt.some("org.opencastproject.dataloader"),
            SchedulerService.ORIGIN);
    cleanUpMediaPackage(mediaPackage);
  }

  private void execute(CSVParser csv) throws Exception {
    List<EventEntry> events = parseCSV(csv);
    logger.info("Found {} events to populate", events.size());

    int i = 1;
    final Date now = new Date();
    for (EventEntry event : events) {
      logger.info("Populating event {}", i);

      createSeries(event);

      MediaPackage mediaPackage = getBasicMediaPackage(event);

      if (now.after(event.getRecordingDate())) {
        addWorkflowEntry(mediaPackage);
        if (event.isArchive())
          addArchiveEntry(mediaPackage);
      } else {
        addSchedulerEntry(event, mediaPackage);
      }
      logger.info("Finished populating event {}", i++);
    }
  }

  private void cleanUpMediaPackage(MediaPackage mp) {
    for (MediaPackageElement element : mp.getElements()) {
      try {
        workspace.delete(element.getURI());
      } catch (NotFoundException e) {
        logger.warn("Unable to find (and hence, delete), this mediapackage '{}' element '{}'", mp.getIdentifier(),
                element.getIdentifier());
      } catch (IOException e) {
        chuck(e);
      }
    }
  }

  private MediaPackage getBasicMediaPackage(EventEntry event) throws Exception {
    URL baseMediapackageUrl = EventsLoader.class.getResource("/base_mediapackage.xml");
    MediaPackage mediaPackage = MediaPackageParser.getFromXml(IOUtils.toString(baseMediapackageUrl));
    DublinCoreCatalog episodeDublinCore = getBasicEpisodeDublinCore(event);
    mediaPackage.setDate(event.getRecordingDate());
    mediaPackage.setIdentifier(new IdImpl(episodeDublinCore.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER)));
    mediaPackage.setTitle(event.getTitle());
    addDublinCoreCatalog(IOUtils.toInputStream(episodeDublinCore.toXmlString(), "UTF-8"), MediaPackageElements.EPISODE,
            mediaPackage);

    // assign to a series
    if (event.getSeries().isSome()) {
      DublinCoreCatalog seriesCatalog = seriesService.getSeries(event.getSeries().get());
      mediaPackage.setSeries(event.getSeries().get());
      mediaPackage.setSeriesTitle(seriesCatalog.getFirst(DublinCoreCatalog.PROPERTY_TITLE));
      addDublinCoreCatalog(IOUtils.toInputStream(seriesCatalog.toXmlString(), "UTF-8"), MediaPackageElements.SERIES,
              mediaPackage);

      AccessControlList acl = seriesService.getSeriesAccessControl(event.getSeries().get());
      if (acl != null) {
        authorizationService.setAcl(mediaPackage, AclScope.Series, acl);
      }
    }

    // Set track URI's to demo file
    for (Track track : mediaPackage.getTracks()) {
      InputStream in = null;
      try {
        in = getClass().getResourceAsStream("/av.mov");
        URI uri = workspace.put(mediaPackage.getIdentifier().compact(), track.getIdentifier(),
                FilenameUtils.getName(track.toString()), in);
        track.setURI(uri);
        track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, getClass().getResourceAsStream("/av.mov")));
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
    return mediaPackage;
  }

  private void createSeries(EventEntry event) throws SeriesException, UnauthorizedException, NotFoundException {
    if (event.getSeries().isNone())
      return;

    try {
      // Test if the series already exist, it does not create it.
      seriesService.getSeries(event.getSeries().get());
    } catch (NotFoundException e) {
      Series catalog = DublinCores.mkOpencastSeries(event.getSeries().get());
      catalog.updateTitle(event.getSeriesName().toOpt());

      // If the series does not exist, we create it.
      seriesService.updateSeries(catalog.getCatalog());
      seriesService.updateAccessControl(event.getSeries().get(), defaultAcl);
    }
  }

  private DublinCoreCatalog getBasicEpisodeDublinCore(EventEntry event) throws IOException {
    OpencastDctermsDublinCore.Episode catalog = DublinCores.mkOpencastEpisode(Opt.<String> none());
    catalog.setTitle(event.getTitle());
    catalog.setSpatial(event.getCaptureAgent());
    catalog.setSource(event.getSource());
    catalog.setCreated(event.getRecordingDate());
    catalog.setContributor(event.getContributor());
    catalog.setCreators(event.getPresenters());
    catalog.updateDescription(event.getDescription().toOpt());
    catalog.setTemporal(event.getRecordingDate(),
            new DateTime(event.getRecordingDate().getTime()).plusMinutes(event.getDuration()).toDate());
    catalog.updateIsPartOf(event.getSeries().toOpt());
    return catalog.getCatalog();
  }

  private List<EventEntry> parseCSV(CSVParser csv) {
    List<EventEntry> arrayList = new ArrayList<>();
    for (CSVRecord record : csv) {
      String title = record.get(0);
      String description = StringUtils.trimToNull(record.get(1));
      String series = StringUtils.trimToNull(record.get(2));
      String seriesName = StringUtils.trimToNull(record.get(3));

      Integer days = Integer.parseInt(record.get(4));
      float signum = Math.signum(days);
      DateTime now = DateTime.now();
      if (signum > 0) {
        now = now.plusDays(days);
      } else if (signum < 0) {
        now = now.minusDays(days * -1);
      }

      Integer duration = Integer.parseInt(record.get(5));
      boolean archive = BooleanUtils.toBoolean(record.get(6));
      String agent = StringUtils.trimToNull(record.get(7));
      String source = StringUtils.trimToNull(record.get(8));
      String contributor = StringUtils.trimToNull(record.get(9));
      List<String> presenters = Arrays.asList(StringUtils.split(StringUtils.trimToEmpty(record.get(10)), ","));
      EventEntry eventEntry = new EventEntry(title, now.toDate(), duration, archive, series, agent, source,
              contributor, description, seriesName, presenters);
      arrayList.add(eventEntry);
    }
    return arrayList;
  }

  protected class Loader extends Thread {

    private CSVParser csvParser;

    public Loader(File csvData) throws IOException {
      try {
        logger.info("Reading event test data from csv {}...", csvData);
        csvParser = CSVParser.parse(csvData, Charset.forName("UTF-8"), CSVFormat.RFC4180);
      } catch (IOException e) {
        logger.error("Unable to parse CSV data from {}", csvData);
        throw e;
      }
    }

    @Override
    public void run() {
      Organization org = new DefaultOrganization();
      User createSystemUser = SecurityUtil.createSystemUser(systemUserName, org);
      SecurityUtil.runAs(securityService, org, createSystemUser, new Effect0() {
        @Override
        protected void run() {
          try {
            logger.info("Start populating event test data...");
            execute(csvParser);
            logger.info("Finished populating event test data");
          } catch (Exception e) {
            logger.error("Unable to populate event test data: {}", ExceptionUtils.getStackTrace(e));
          }
        }
      });
    }
  }

  private MediaPackage addDublinCoreCatalog(InputStream in, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws IOException {
    try {
      String elementId = UUID.randomUUID().toString();
      URI catalogUrl = workspace.put(mediaPackage.getIdentifier().compact(), elementId, "dublincore.xml", in);
      logger.info("Adding catalog with flavor {} to mediapackage {}", flavor, mediaPackage);
      MediaPackageElement mpe = mediaPackage.add(catalogUrl, MediaPackageElement.Type.Catalog, flavor);
      mpe.setIdentifier(elementId);
      mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, workspace.get(catalogUrl)));
      return mediaPackage;
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param seriesService
   *          the seriesService to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * @param schedulerService
   *          the schedulerService to set
   */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /**
   * @param assetManager
   *          the asset manager to set
   */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param workflowService
   *          the workflowService to set
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * @param authorizationService
   *          the authorizationService to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
