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

package org.opencastproject.ingest.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.opencastproject.util.JobUtil.waitForJob;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.ingest.impl.jmx.IngestStatistics;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.smil.api.util.SmilUtil;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.ProgressInputStream;
import org.opencastproject.util.XmlUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;

/**
 * Creates and augments Opencast MediaPackages. Stores media into the Working File Repository.
 */
public class IngestServiceImpl extends AbstractJobProducer implements IngestService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

  /** The source SMIL name */
  private static final String PARTIAL_SMIL_NAME = "source_partial.smil";

  /** The configuration key that defines the default workflow definition */
  protected static final String WORKFLOW_DEFINITION_DEFAULT = "org.opencastproject.workflow.default.definition";

  /** The workflow configuration property prefix **/
  protected static final String WORKFLOW_CONFIGURATION_PREFIX = "org.opencastproject.workflow.config.";

  /** The key for the legacy mediapackage identifier */
  public static final String LEGACY_MEDIAPACKAGE_ID_KEY = "org.opencastproject.ingest.legacy.mediapackage.id";

  public static final String JOB_TYPE = "org.opencastproject.ingest";

  /** Managed Property key to overwrite existing series */
  public static final String PROPKEY_OVERWRITE_SERIES = "org.opencastproject.series.overwrite";

  /** Methods that ingest zips create jobs with this operation type */
  public static final String INGEST_ZIP = "zip";

  /** Methods that ingest tracks directly create jobs with this operation type */
  public static final String INGEST_TRACK = "track";

  /** Methods that ingest tracks from a URI create jobs with this operation type */
  public static final String INGEST_TRACK_FROM_URI = "uri-track";

  /** Methods that ingest attachments directly create jobs with this operation type */
  public static final String INGEST_ATTACHMENT = "attachment";

  /** Methods that ingest attachments from a URI create jobs with this operation type */
  public static final String INGEST_ATTACHMENT_FROM_URI = "uri-attachment";

  /** Methods that ingest catalogs directly create jobs with this operation type */
  public static final String INGEST_CATALOG = "catalog";

  /** Methods that ingest catalogs from a URI create jobs with this operation type */
  public static final String INGEST_CATALOG_FROM_URI = "uri-catalog";

  /** The approximate load placed on the system by ingesting a file */
  public static final float DEFAULT_INGEST_FILE_JOB_LOAD = 0.2f;

  /** The approximate load placed on the system by ingesting a zip file */
  public static final float DEFAULT_INGEST_ZIP_JOB_LOAD = 0.2f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_INGEST_FILE_JOB_LOAD} */
  public static final String FILE_JOB_LOAD_KEY = "job.load.ingest.file";

  /** The key to look for in the service configuration file to override the {@link DEFAULT_INGEST_ZIP_JOB_LOAD} */
  public static final String ZIP_JOB_LOAD_KEY = "job.load.ingest.zip";

  /** The approximate load placed on the system by ingesting a file */
  private float ingestFileJobLoad = DEFAULT_INGEST_FILE_JOB_LOAD;

  /** The approximate load placed on the system by ingesting a zip file */
  private float ingestZipJobLoad = DEFAULT_INGEST_ZIP_JOB_LOAD;

  /** The JMX business object for ingest statistics */
  private IngestStatistics ingestStatistics = new IngestStatistics();

  /** The JMX bean object instance */
  private ObjectInstance registerMXBean;

  /** The workflow service */
  private WorkflowService workflowService;

  /** The working file repository */
  private WorkingFileRepository workingFileRepository;

  /** The http client */
  private TrustedHttpClient httpClient;

  /** The series service */
  private SeriesService seriesService;

  /** The dublin core service */
  private DublinCoreCatalogService dublinCoreService;

  /** The opencast service registry */
  private ServiceRegistry serviceRegistry;

  /** The authorization service */
  private AuthorizationService authorizationService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The scheduler service */
  private SchedulerService schedulerService = null;

  /** The media inspection service */
  private MediaInspectionService mediaInspectionService = null;

  /** The default workflow identifier, if one is configured */
  protected String defaultWorkflowDefinionId;

  /** The partial track start time map */
  private Cache<String, Long> partialTrackStartTimes = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
          .build();

  /** The default is to overwrite series catalog on ingest */
  protected boolean defaultIsOverWriteSeries = true;

  /** Option to overwrite series on ingest */
  protected boolean isOverwriteSeries = defaultIsOverWriteSeries;

  /**
   * Creates a new ingest service instance.
   */
  public IngestServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * OSGI callback for activating this component
   *
   * @param cc
   *          the osgi component context
   */
  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Ingest Service started.");
    defaultWorkflowDefinionId = StringUtils.trimToNull(cc.getBundleContext().getProperty(WORKFLOW_DEFINITION_DEFAULT));
    if (defaultWorkflowDefinionId == null) {
      defaultWorkflowDefinionId = "schedule-and-upload";
    }
    registerMXBean = JmxUtil.registerMXBean(ingestStatistics, "IngestStatistics");
  }

  /**
   * Callback from OSGi on service deactivation.
   */
  public void deactivate() {
    JmxUtil.unregisterMXBean(registerMXBean);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   *      Retrieve ManagedService configuration, including option to overwrite series
   */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    ingestFileJobLoad = LoadUtil.getConfiguredLoadValue(properties, FILE_JOB_LOAD_KEY, DEFAULT_INGEST_FILE_JOB_LOAD,
            serviceRegistry);
    ingestZipJobLoad = LoadUtil.getConfiguredLoadValue(properties, ZIP_JOB_LOAD_KEY, DEFAULT_INGEST_ZIP_JOB_LOAD,
            serviceRegistry);
    // try to get overwrite series option from config, use default if not configured
    try {
      isOverwriteSeries = Boolean.parseBoolean(((String) properties.get(PROPKEY_OVERWRITE_SERIES)).trim());
    } catch (Exception e) {
      isOverwriteSeries = defaultIsOverWriteSeries;
      logger.warn("Unable to update configuration. {}", e.getMessage());
    }
    logger.info("Configuration updated. It is {} that existing series will be overwritten during ingest.",
            isOverwriteSeries);
  }

  /**
   * Sets the trusted http client
   *
   * @param httpClient
   *          the http client
   */
  public void setHttpClient(TrustedHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Sets the service registry
   *
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the authorization service
   *
   * @param authorizationService
   *          the authorization service to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Sets the media inspection service
   *
   * @param mediaInspectionService
   *          the media inspection service to set
   */
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.mediaInspectionService = mediaInspectionService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream)
   */
  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream)
          throws IngestException, IOException, MediaPackageException {
    try {
      return addZippedMediaPackage(zipStream, null, null);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A not found exception was thrown without a lookup");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream, java.lang.String)
   */
  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream, String wd)
          throws MediaPackageException, IOException, IngestException, NotFoundException {
    return addZippedMediaPackage(zipStream, wd, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream, java.lang.String)
   */
  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream, String wd, Map<String, String> workflowConfig)
          throws MediaPackageException, IOException, IngestException, NotFoundException {
    try {
      return addZippedMediaPackage(zipStream, wd, workflowConfig, null);
    } catch (UnauthorizedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream, java.lang.String,
   *      java.util.Map, java.lang.Long)
   */
  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream, String workflowDefinitionId,
          Map<String, String> workflowConfig, Long workflowInstanceId)
          throws MediaPackageException, IOException, IngestException, NotFoundException, UnauthorizedException {
    // Start a job synchronously. We can't keep the open input stream waiting around.
    Job job = null;

    if (StringUtils.isNotBlank(workflowDefinitionId)) {
      try {
        workflowService.getWorkflowDefinitionById(workflowDefinitionId);
      } catch (WorkflowDatabaseException e) {
        throw new IngestException(e);
      } catch (NotFoundException nfe) {
        logger.warn("Workflow definition {} not found, using default workflow {} instead", workflowDefinitionId,
                defaultWorkflowDefinionId);
        workflowDefinitionId = defaultWorkflowDefinionId;
      }
    }

    if (workflowInstanceId != null) {
      logger.warn("Deprecated method! Ingesting zipped mediapackage with workflow {}", workflowInstanceId);
    } else {
      logger.info("Ingesting zipped mediapackage");
    }

    ZipArchiveInputStream zis = null;
    Set<String> collectionFilenames = new HashSet<>();
    try {
      // We don't need anybody to do the dispatching for us. Therefore we need to make sure that the job is never in
      // QUEUED state but set it to INSTANTIATED in the beginning and then manually switch it to RUNNING.
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_ZIP, null, null, false, ingestZipJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);

      // Create the working file target collection for this ingest operation
      String wfrCollectionId = Long.toString(job.getId());

      zis = new ZipArchiveInputStream(zipStream);
      ZipArchiveEntry entry;
      MediaPackage mp = null;
      Map<String, URI> uris = new HashMap<>();
      // Sequential number to append to file names so that, if two files have the same
      // name, one does not overwrite the other (see MH-9688)
      int seq = 1;
      // Folder name to compare with next one to figure out if there's a root folder
      String folderName = null;
      // Indicates if zip has a root folder or not, initialized as true
      boolean hasRootFolder = true;
      // While there are entries write them to a collection
      while ((entry = zis.getNextZipEntry()) != null) {
        try {
          if (entry.isDirectory() || entry.getName().contains("__MACOSX"))
            continue;

          if (entry.getName().endsWith("manifest.xml") || entry.getName().endsWith("index.xml")) {
            // Build the mediapackage
            mp = loadMediaPackageFromManifest(new ZipEntryInputStream(zis, entry.getSize()));
          } else {
            logger.info("Storing zip entry {}/{} in working file repository collection '{}'", job.getId(),
                    entry.getName(), wfrCollectionId);
            // Since the directory structure is not being mirrored, makes sure the file
            // name is different than the previous one(s) by adding a sequential number
            String fileName = FilenameUtils.getBaseName(entry.getName()) + "_" + seq++ + "."
                    + FilenameUtils.getExtension(entry.getName());
            URI contentUri = workingFileRepository.putInCollection(wfrCollectionId, fileName,
                    new ZipEntryInputStream(zis, entry.getSize()));
            collectionFilenames.add(fileName);
            // Key is the zip entry name as it is
            String key = entry.getName();
            uris.put(key, contentUri);
            ingestStatistics.add(entry.getSize());
            logger.info("Zip entry {}/{} stored at {}", job.getId(), entry.getName(), contentUri);
            // Figures out if there's a root folder. Does entry name starts with a folder?
            int pos = entry.getName().indexOf('/');
            if (pos == -1) {
              // No, we can conclude there's no root folder
              hasRootFolder = false;
            } else if (hasRootFolder && folderName != null && !folderName.equals(entry.getName().substring(0, pos))) {
              // Folder name different from previous so there's no root folder
              hasRootFolder = false;
            } else if (folderName == null) {
              // Just initialize folder name
              folderName = entry.getName().substring(0, pos);
            }
          }
        } catch (IOException e) {
          logger.warn("Unable to process zip entry {}: {}", entry.getName(), e);
          throw e;
        }
      }

      if (mp == null)
        throw new MediaPackageException("No manifest found in this zip");

      // Determine the mediapackage identifier
      if (mp.getIdentifier() == null || isBlank(mp.getIdentifier().toString()))
        mp.setIdentifier(new UUIDIdBuilderImpl().createNew());

      String mediaPackageId = mp.getIdentifier().toString();

      logger.info("Ingesting mediapackage {} is named '{}'", mediaPackageId, mp.getTitle());

      // Make sure there are tracks in the mediapackage
      if (mp.getTracks().length == 0) {
        logger.warn("Mediapackage {} has no media tracks", mediaPackageId);
      }

      // Update the element uris to point to their working file repository location
      for (MediaPackageElement element : mp.elements()) {
        // Key has root folder name if there is one
        URI uri = uris.get((hasRootFolder ? folderName + "/" : "") + element.getURI().toString());

        if (uri == null)
          throw new MediaPackageException("Unable to map element name '" + element.getURI() + "' to workspace uri");
        logger.info("Ingested mediapackage element {}/{} located at {}", mediaPackageId, element.getIdentifier(), uri);
        URI dest = workingFileRepository.moveTo(wfrCollectionId, FilenameUtils.getName(uri.toString()), mediaPackageId,
                element.getIdentifier(), FilenameUtils.getName(element.getURI().toString()));
        element.setURI(dest);

        // TODO: This should be triggered somehow instead of being handled here
        if (MediaPackageElements.SERIES.equals(element.getFlavor())) {
          logger.info("Ingested mediapackage {} contains updated series information", mediaPackageId);
          updateSeries(element.getURI());
        }
      }

      // Now that all elements are in place, start with ingest
      logger.info("Initiating processing of ingested mediapackage {}", mediaPackageId);
      WorkflowInstance workflowInstance = ingest(mp, workflowDefinitionId, workflowConfig, workflowInstanceId);
      logger.info("Ingest of mediapackage {} done", mediaPackageId);
      job.setStatus(Job.Status.FINISHED);
      return workflowInstance;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (MediaPackageException e) {
      job.setStatus(Job.Status.FAILED, Job.FailureReason.DATA);
      throw e;
    } catch (Exception e) {
      if (e instanceof IngestException)
        throw (IngestException) e;
      throw new IngestException(e);
    } finally {
      IOUtils.closeQuietly(zis);
      finallyUpdateJob(job);
      for (String filename : collectionFilenames) {
        workingFileRepository.deleteFromCollection(Long.toString(job.getId()), filename, true);
      }
    }
  }

  private MediaPackage loadMediaPackageFromManifest(InputStream manifest)
          throws IOException, MediaPackageException, IngestException {
    // TODO: Uncomment the following line and remove the patch when the compatibility with pre-1.4 MediaPackages is
    // discarded
    //
    // mp = builder.loadFromXml(manifestStream);
    //
    // =========================================================================================
    // =================================== PATCH BEGIN =========================================
    // =========================================================================================
    ByteArrayOutputStream baos = null;
    ByteArrayInputStream bais = null;
    try {
      Document domMP = new SAXBuilder().build(manifest);
      String mpNSUri = "http://mediapackage.opencastproject.org";

      Namespace oldNS = domMP.getRootElement().getNamespace();
      Namespace newNS = Namespace.getNamespace(oldNS.getPrefix(), mpNSUri);

      if (!newNS.equals(oldNS)) {
        @SuppressWarnings("rawtypes")
        Iterator it = domMP.getDescendants(new ElementFilter(oldNS));
        while (it.hasNext()) {
          Element elem = (Element) it.next();
          elem.setNamespace(newNS);
        }
      }

      baos = new ByteArrayOutputStream();
      new XMLOutputter().output(domMP, baos);
      bais = new ByteArrayInputStream(baos.toByteArray());
      return MediaPackageParser.getFromXml(IOUtils.toString(bais, "UTF-8"));
    } catch (JDOMException e) {
      throw new IngestException("Error unmarshalling mediapackage", e);
    } finally {
      IOUtils.closeQuietly(bais);
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(manifest);
    }
    // =========================================================================================
    // =================================== PATCH END ===========================================
    // =========================================================================================
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  @Override
  public MediaPackage createMediaPackage() throws MediaPackageException, ConfigurationException, HandleException {
    MediaPackage mediaPackage;
    try {
      mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (MediaPackageException e) {
      logger.error("INGEST:Failed to create media package " + e.getLocalizedMessage());
      throw e;
    }
    mediaPackage.setDate(new Date());
    logger.info("Created mediapackage {}", mediaPackage);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  @Override
  public MediaPackage createMediaPackage(String mediaPackageId)
          throws MediaPackageException, ConfigurationException, HandleException {
    MediaPackage mediaPackage;
    try {
      mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
              .createNew(new UUIDIdBuilderImpl().fromString(mediaPackageId));
    } catch (MediaPackageException e) {
      logger.error("INGEST:Failed to create media package " + e.getLocalizedMessage());
      throw e;
    }
    mediaPackage.setDate(new Date());
    logger.info("Created mediapackage {}", mediaPackage);
    return mediaPackage;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addTrack(java.net.URI,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws IOException, IngestException {
    String[] tags = null;
    return this.addTrack(uri, flavor, tags, mediaPackage);

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addTrack(java.net.URI,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, String[] ,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, String[] tags, MediaPackage mediaPackage)
          throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry
              .createJob(
                      JOB_TYPE, INGEST_TRACK_FROM_URI, Arrays.asList(uri.toString(),
                              flavor == null ? null : flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)),
                      null, false, ingestFileJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding track {} from URL {} on mediapackage {}", elementId, uri, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
              flavor);
      if (tags != null && tags.length > 0) {
        MediaPackageElement trackElement = mp.getTrack(elementId);
        for (String tag : tags) {
          logger.info("Adding Tag: " + tag + " to Element: " + elementId);
          trackElement.addTag(tag);
        }
      }

      job.setStatus(Job.Status.FINISHED);
      logger.info("Successful added track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (IOException e) {
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addTrack(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addTrack(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws IOException, IngestException {
    String[] tags = null;
    return this.addTrack(in, fileName, flavor, tags, mediaPackage);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addTrack(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addTrack(InputStream in, String fileName, MediaPackageElementFlavor flavor, String[] tags,
          MediaPackage mediaPackage) throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_TRACK, null, null, false, ingestFileJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding track {} from input stream on mediapackage {}", elementId, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
              flavor);
      if (tags != null && tags.length > 0) {
        MediaPackageElement trackElement = mp.getTrack(elementId);
        for (String tag : tags) {
          logger.info("Adding Tag: " + tag + " to Element: " + elementId);
          trackElement.addTag(tag);
        }
      }

      job.setStatus(Job.Status.FINISHED);
      logger.info("Successful added track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (IOException e) {
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  @Override
  public MediaPackage addPartialTrack(URI uri, MediaPackageElementFlavor flavor, long startTime,
          MediaPackage mediaPackage) throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(
              JOB_TYPE,
              INGEST_TRACK_FROM_URI,
              Arrays.asList(uri.toString(), flavor == null ? null : flavor.toString(),
                      MediaPackageParser.getAsXml(mediaPackage)), null, false);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding partial track {} from URL {} on mediapackage {}", elementId, uri, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      // store startTime
      partialTrackStartTimes.put(elementId, startTime);
      logger.debug("Added start time {} for track {}", startTime, elementId);
      logger.info("Successful added partial track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  @Override
  public MediaPackage addPartialTrack(InputStream in, String fileName, MediaPackageElementFlavor flavor, long startTime,
          MediaPackage mediaPackage) throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_TRACK, null, null, false);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding partial track {} from input stream on mediapackage {}", elementId, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      // store startTime
      partialTrackStartTimes.put(elementId, startTime);
      logger.debug("Added start time {} for track {}", startTime, elementId);
      logger.info("Successful added partial track {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addCatalog(java.net.URI,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addCatalog(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_CATALOG_FROM_URI,
              Arrays.asList(uri.toString(), flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)), null, false,
              ingestFileJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding catalog {} from URL {} on mediapackage {}", elementId, uri, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      if (MediaPackageElements.SERIES.equals(flavor)) {
        updateSeries(uri);
      }
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      logger.info("Successful added catalog {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * Updates the persistent representation of a series based on a potentially modified dublin core document.
   *
   * @param uri
   *          the URI to the dublin core document containing series metadata.
   * @return
   *         true, if the series is created or overwritten, false if the existing series remains intact.
   */
  protected boolean updateSeries(URI uri) throws IOException, IngestException {
    HttpResponse response = null;
    InputStream in = null;
    boolean isUpdated = false;
    try {
      HttpGet getDc = new HttpGet(uri);
      response = httpClient.execute(getDc);
      in = response.getEntity().getContent();
      DublinCoreCatalog dc = dublinCoreService.load(in);
      String id = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
      if (id == null) {
        logger.warn("Series dublin core document contains no identifier, rejecting ingested series cagtalog.");
      } else {
        try {
          try {
            seriesService.getSeries(id);
            if (isOverwriteSeries) {
              // Update existing series
              seriesService.updateSeries(dc);
              isUpdated = true;
              logger.debug("Ingest is overwriting the existing series {} with the ingested series", id);
            } else {
              logger.debug("Series {} already exists. Ignoring series catalog from ingest.", id);
            }
          } catch (NotFoundException e) {
            logger.info("Creating new series {} with default ACL", id);
            seriesService.updateSeries(dc);
            isUpdated = true;
            String anonymousRole = securityService.getOrganization().getAnonymousRole();
            AccessControlList acl = new AccessControlList(new AccessControlEntry(anonymousRole, "read", true));
            seriesService.updateAccessControl(id, acl);
          }

        } catch (Exception e) {
          throw new IngestException(e);
        }
      }
      in.close();
    } catch (IOException e) {
      logger.error("Error updating series from DublinCoreCatalog: {}", e.getMessage());
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
    return isUpdated;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addCatalog(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addCatalog(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws IOException, IngestException {
    return addCatalog(in, fileName, flavor, null, mediaPackage);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addCatalog(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addCatalog(InputStream in, String fileName, MediaPackageElementFlavor flavor, String[] tags,
          MediaPackage mediaPackage) throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_CATALOG, null, null, false, ingestFileJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding catalog {} from input stream on mediapackage {}", elementId, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      if (MediaPackageElements.SERIES.equals(flavor)) {
        updateSeries(newUrl);
      }
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog,
              flavor);
      if (tags != null && tags.length > 0) {
        MediaPackageElement trackElement = mp.getCatalog(elementId);
        for (String tag : tags) {
          logger.info("Adding Tag: " + tag + " to Element: " + elementId);
          trackElement.addTag(tag);
        }
      }

      job.setStatus(Job.Status.FINISHED);
      logger.info("Successful added catalog {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addAttachment(java.net.URI,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_ATTACHMENT_FROM_URI,
              Arrays.asList(uri.toString(), flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)), null, false,
              ingestFileJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding attachment {} from URL {} on mediapackage {}", elementId, uri, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      logger.info("Successful added attachment {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addAttachment(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addAttachment(InputStream in, String fileName, MediaPackageElementFlavor flavor, String[] tags,
          MediaPackage mediaPackage) throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_ATTACHMENT, null, null, false, ingestFileJobLoad);
      job.setStatus(Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      String elementId = UUID.randomUUID().toString();
      logger.info("Start adding attachment {} from input stream on mediapackage {}", elementId, mediaPackage);
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment,
              flavor);
      if (tags != null && tags.length > 0) {
        MediaPackageElement trackElement = mp.getAttachment(elementId);
        for (String tag : tags) {
          logger.info("Adding Tag: " + tag + " to Element: " + elementId);
          trackElement.addTag(tag);
        }
      }
      job.setStatus(Job.Status.FINISHED);
      logger.info("Successful added attachment {} on mediapackage {} at URL {}", elementId, mediaPackage, newUrl);
      return mp;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (NotFoundException e) {
      throw new IngestException("Unable to update ingest job", e);
    } finally {
      finallyUpdateJob(job);
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#addAttachment(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public MediaPackage addAttachment(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws IOException, IngestException {
    String[] tags = null;
    return addAttachment(in, fileName, flavor, tags, mediaPackage);
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#ingest(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance ingest(MediaPackage mp) throws IngestException {
    try {
      return ingest(mp, null, null, null);
    } catch (NotFoundException e) {
      throw new IngestException(e);
    } catch (UnauthorizedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#ingest(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  @Override
  public WorkflowInstance ingest(MediaPackage mp, String wd) throws IngestException, NotFoundException {
    try {
      return ingest(mp, wd, null, null);
    } catch (UnauthorizedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#ingest(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String, java.util.Map)
   */
  @Override
  public WorkflowInstance ingest(MediaPackage mp, String wd, Map<String, String> properties)
          throws IngestException, NotFoundException {
    try {
      return ingest(mp, wd, properties, null);
    } catch (UnauthorizedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#ingest(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String, java.util.Map, java.lang.Long)
   */
  @Override
  public WorkflowInstance ingest(MediaPackage mp, String workflowDefinitionId, Map<String, String> properties,
          Long workflowInstanceId) throws IngestException, NotFoundException, UnauthorizedException {
    // Check for legacy media package id
    mp = checkForLegacyMediaPackageId(mp, properties);

    try {
      mp = createSmil(mp);
    } catch (IOException e) {
      throw new IngestException("Unable to add SMIL Catalog", e);
    }

    // Done, update the job status and return the created workflow instance
    if (workflowInstanceId != null) {
      logger.warn(
              "Resuming workflow {} with ingested mediapackage {} is deprecated, skip resuming and start new workflow",
              workflowInstanceId, mp);
    }

    if (workflowDefinitionId == null) {
      logger.info("Starting a new workflow with ingested mediapackage {} based on the default workflow definition '{}'",
              mp, defaultWorkflowDefinionId);
    } else {
      logger.info("Starting a new workflow with ingested mediapackage {} based on workflow definition '{}'", mp,
              workflowDefinitionId);
    }

    try {
      // Determine the workflow definition
      WorkflowDefinition workflowDef = getWorkflowDefinition(workflowDefinitionId, mp);

      // Get the final set of workflow properties
      properties = mergeWorkflowConfiguration(properties, mp.getIdentifier().compact());

      // Remove potential workflow configuration prefixes from the workflow properties
      properties = removePrefixFromProperties(properties);

      // Merge scheduled mediapackage with ingested
      mp = mergeScheduledMediaPackage(mp);

      ingestStatistics.successful();
      if (workflowDef != null) {
        logger.info("Starting new workflow with ingested mediapackage '{}' using the specified template '{}'",
                mp.getIdentifier().toString(), workflowDefinitionId);
      } else {
        logger.info("Starting new workflow with ingested mediapackage '{}' using the default template '{}'",
                mp.getIdentifier().toString(), defaultWorkflowDefinionId);
      }
      return workflowService.start(workflowDef, mp, properties);
    } catch (WorkflowException e) {
      ingestStatistics.failed();
      throw new IngestException(e);
    }
  }

  @Override
  public void schedule(MediaPackage mediaPackage, String workflowDefinitionID, Map<String, String> properties)
          throws IllegalStateException, IngestException, NotFoundException, UnauthorizedException, SchedulerException {
    MediaPackageElement[] mediaPackageElements = mediaPackage.getElementsByFlavor(MediaPackageElements.EPISODE);
    if (mediaPackageElements.length != 1) {
      logger.debug("There can be only one (and exactly one) episode dublin core catalog: https://youtu.be/_J3VeogFUOs");
      throw new IngestException("There can be only one (and exactly one) episode dublin core catalog");
    }
    InputStream inputStream;
    DublinCoreCatalog dublinCoreCatalog;
    try {
      inputStream = workingFileRepository.get(mediaPackage.getIdentifier().toString(),
              mediaPackageElements[0].getIdentifier());
      dublinCoreCatalog = dublinCoreService.load(inputStream);
    } catch (IOException e) {
      throw new IngestException(e);
    }

    EName temporal = new EName(DublinCore.TERMS_NS_URI, "temporal");
    List<DublinCoreValue> periods = dublinCoreCatalog.get(temporal);
    if (periods.size() != 1) {
      logger.debug("There can be only one (and exactly one) period");
      throw new IngestException("There can be only one (and exactly one) period");
    }
    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(periods.get(0));
    if (!period.hasStart() || !period.hasEnd()) {
      logger.debug("A scheduled recording needs to have a start and end.");
      throw new IngestException("A scheduled recording needs to have a start and end.");
    }
    EName createdEName = new EName(DublinCore.TERMS_NS_URI, "created");
    List<DublinCoreValue> created = dublinCoreCatalog.get(createdEName);
    if (created.size() == 0) {
      logger.debug("Created not set");
    } else if (created.size() == 1) {
      Date date = EncodingSchemeUtils.decodeMandatoryDate(created.get(0));
      if (date.getTime() != period.getStart().getTime()) {
        logger.debug("start and created date differ ({} vs {})", date.getTime(), period.getStart().getTime());
        throw new IngestException("Temporal start and created date differ");
      }
    } else {
      logger.debug("There can be only one created date");
      throw new IngestException("There can be only one created date");
    }
    // spatial
    EName spatial = new EName(DublinCore.TERMS_NS_URI, "spatial");
    List<DublinCoreValue> captureAgents = dublinCoreCatalog.get(spatial);
    if (captureAgents.size() != 1) {
      logger.debug("Exactly one capture agent needs to be set");
      throw new IngestException("Exactly one capture agent needs to be set");
    }
    String captureAgent = captureAgents.get(0).getValue();

    // Go through properties
    Map<String, String> agentProperties = new HashMap<>();
    Map<String, String> workflowProperties = new HashMap<>();
    for (String key : properties.keySet()) {
      if (key.startsWith("org.opencastproject.workflow.config.")) {
        workflowProperties.put(key, properties.get(key));
      } else {
        agentProperties.put(key, properties.get(key));
      }
    }
    try {
      schedulerService.addEvent(period.getStart(), period.getEnd(), captureAgent, new HashSet<>(), mediaPackage,
              workflowProperties, agentProperties, Opt.none(), Opt.none(), "ingest-service");
    } finally {
      for (MediaPackageElement mediaPackageElement : mediaPackage.getElements()) {
        try {
          workingFileRepository.delete(mediaPackage.getIdentifier().toString(), mediaPackageElement.getIdentifier());
        } catch (IOException e) {
          logger.warn("Failed to delete media package element", e);
        }
      }
    }
  }

  /**
   * Check whether the mediapackage id is set via the legacy workflow identifier and change the id if existing.
   *
   * @param mp
   *          the mediapackage
   * @param properties
   *          the workflow properties
   * @return the mediapackage
   */
  private MediaPackage checkForLegacyMediaPackageId(MediaPackage mp, Map<String, String> properties)
          throws IngestException {
    if (properties == null || properties.isEmpty())
      return mp;

    try {
      String mediaPackageId = properties.get(LEGACY_MEDIAPACKAGE_ID_KEY);
      if (StringUtils.isNotBlank(mediaPackageId) && schedulerService != null) {
        logger.debug("Check ingested mediapackage {} for legacy mediapackage identifier {}",
                mp.getIdentifier().compact(), mediaPackageId);
        try {
          schedulerService.getMediaPackage(mp.getIdentifier().compact());
          return mp;
        } catch (NotFoundException e) {
          logger.info("No scheduler mediapackage found with ingested id {}, try legacy mediapackage id {}",
                  mp.getIdentifier().compact(), mediaPackageId);
          try {
            schedulerService.getMediaPackage(mediaPackageId);
            logger.info("Legacy mediapackage id {} exists, change ingested mediapackage id {} to legacy id",
                    mediaPackageId, mp.getIdentifier().compact());
            mp.setIdentifier(new IdImpl(mediaPackageId));
            return mp;
          } catch (NotFoundException e1) {
            logger.info("No scheduler mediapackage found with legacy mediapackage id {}, skip merging", mediaPackageId);
          } catch (Exception e1) {
            logger.error("Unable to get event mediapackage from scheduler event {}", mediaPackageId, e);
            throw new IngestException(e);
          }
        } catch (Exception e) {
          logger.error("Unable to get event mediapackage from scheduler event {}", mp.getIdentifier().compact(), e);
          throw new IngestException(e);
        }
      }
      return mp;
    } finally {
      properties.remove(LEGACY_MEDIAPACKAGE_ID_KEY);
    }
  }

  private Map<String, String> mergeWorkflowConfiguration(Map<String, String> properties, String mediaPackageId) {
    if (isBlank(mediaPackageId) || schedulerService == null)
      return properties;

    HashMap<String, String> mergedProperties = new HashMap<>();

    try {
      Map<String, String> recordingProperties = schedulerService.getCaptureAgentConfiguration(mediaPackageId);
      logger.debug("Restoring workflow properties from scheduler event {}", mediaPackageId);
      mergedProperties.putAll(recordingProperties);
    } catch (SchedulerException e) {
      logger.warn("Unable to get workflow properties from scheduler event {}: {}", mediaPackageId,
              ExceptionUtils.getMessage(e));
    } catch (NotFoundException e) {
      logger.info("No capture event found for id {}", mediaPackageId);
    } catch (UnauthorizedException e) {
      throw new IllegalStateException(e);
    }

    if (properties != null) {
      // Merge the properties, this must be after adding the recording properties
      logger.debug("Merge workflow properties with the one from the scheduler event {}", mediaPackageId);
      mergedProperties.putAll(properties);
    }

    return mergedProperties;
  }

  /**
   * Merges the ingested mediapackage with the scheduled mediapackage. The ingested mediapackage takes precedence over
   * the scheduled mediapackage.
   *
   * @param mp
   *          the ingested mediapackage
   * @return the merged mediapackage
   */
  private MediaPackage mergeScheduledMediaPackage(MediaPackage mp) throws IngestException {
    if (schedulerService == null) {
      logger.warn("No scheduler service available to merge mediapackage!");
      return mp;
    }

    try {
      MediaPackage scheduledMp = schedulerService.getMediaPackage(mp.getIdentifier().compact());
      logger.info("Found matching scheduled event for id '{}', merging mediapackage...", mp.getIdentifier().compact());
      mergeMediaPackageElements(mp, scheduledMp);
      mergeMediaPackageMetadata(mp, scheduledMp);
      return mp;
    } catch (NotFoundException e) {
      logger.debug("No scheduler mediapackage found with id {}, skip merging", mp.getIdentifier());
      return mp;
    } catch (Exception e) {
      throw new IngestException(String.format("Unable to get event media package from scheduler event %s",
              mp.getIdentifier()), e);
    }
  }

  private void mergeMediaPackageElements(MediaPackage mp, MediaPackage scheduledMp) {
    for (MediaPackageElement element : scheduledMp.getElements()) {
      // Asset manager media package may have a publication element (for live) if retract live has not run yet
      if (element.getFlavor() != null
              && !MediaPackageElement.Type.Publication.equals(element.getElementType())
              && mp.getElementsByFlavor(element.getFlavor()).length > 0) {
        logger.info("Ignore scheduled element '{}', there is already an ingested element with flavor '{}'", element,
                element.getFlavor());
        continue;
      }
      logger.info("Adding new scheduled element '{}' to ingested mediapackage", element);
      mp.add(element);
    }
  }

  private void mergeMediaPackageMetadata(MediaPackage mp, MediaPackage scheduledMp) {
    // Merge media package fields
    if (mp.getDate() == null)
      mp.setDate(scheduledMp.getDate());
    if (isBlank(mp.getLicense()))
      mp.setLicense(scheduledMp.getLicense());
    if (isBlank(mp.getSeries()))
      mp.setSeries(scheduledMp.getSeries());
    if (isBlank(mp.getSeriesTitle()))
      mp.setSeriesTitle(scheduledMp.getSeriesTitle());
    if (isBlank(mp.getTitle()))
      mp.setTitle(scheduledMp.getTitle());
    if (mp.getSubjects().length == 0) {
      for (String subject : scheduledMp.getSubjects()) {
        mp.addSubject(subject);
      }
    }
    if (mp.getContributors().length == 0) {
      for (String contributor : scheduledMp.getContributors()) {
        mp.addContributor(contributor);
      }
    }
    if (mp.getCreators().length == 0) {
      for (String creator : scheduledMp.getCreators()) {
        mp.addCreator(creator);
      }
    }
  }

  /**
   * Removes the workflow configuration file prefix from all properties in a map.
   *
   * @param properties
   *          The properties to remove the prefixes from
   * @return A Map with the same collection of properties without the prefix
   */
  private Map<String, String> removePrefixFromProperties(Map<String, String> properties) {
    Map<String, String> fixedProperties = new HashMap<>();
    if (properties != null) {
      for (Entry<String, String> entry : properties.entrySet()) {
        if (entry.getKey().startsWith(WORKFLOW_CONFIGURATION_PREFIX)) {
          logger.debug("Removing prefix from key '" + entry.getKey() + " with value '" + entry.getValue() + "'");
          fixedProperties.put(entry.getKey().replace(WORKFLOW_CONFIGURATION_PREFIX, ""), entry.getValue());
        } else {
          fixedProperties.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return fixedProperties;
  }

  private WorkflowDefinition getWorkflowDefinition(String workflowDefinitionID, MediaPackage mediapackage)
          throws NotFoundException, WorkflowDatabaseException, IngestException {
    // If the workflow definition and instance ID are null, use the default, or throw if there is none
    if (isBlank(workflowDefinitionID)) {
      String mediaPackageId = mediapackage.getIdentifier().compact();
      if (schedulerService != null) {
        logger.info("Determining workflow template for ingested mediapckage {} from capture event {}", mediapackage,
                mediaPackageId);
        try {
          Map<String, String> recordingProperties = schedulerService.getCaptureAgentConfiguration(mediaPackageId);
          workflowDefinitionID = recordingProperties.get(CaptureParameters.INGEST_WORKFLOW_DEFINITION);
          if (isBlank(workflowDefinitionID)) {
            workflowDefinitionID = defaultWorkflowDefinionId;
            logger.debug("No workflow set. Falling back to default.");
          }
          if (isBlank(workflowDefinitionID)) {
            throw new IngestException("No value found for key '" + CaptureParameters.INGEST_WORKFLOW_DEFINITION
                    + "' from capture event configuration of scheduler event '" + mediaPackageId + "'");
          }
          logger.info("Ingested event {} will be processed using workflow '{}'", mediapackage, workflowDefinitionID);
        } catch (NotFoundException e) {
          logger.warn("Specified capture event {} was not found", mediaPackageId);
        } catch (UnauthorizedException e) {
          throw new IllegalStateException(e);
        } catch (SchedulerException e) {
          logger.warn("Unable to get the workflow definition id from scheduler event {}: {}", mediaPackageId,
                  ExceptionUtils.getMessage(e));
          throw new IngestException(e);
        }
      } else {
        logger.warn(
                "Scheduler service not bound, unable to determine the workflow template to use for ingested mediapckage {}",
                mediapackage);
      }

    } else {
      logger.info("Ingested mediapackage {} is processed using workflow template '{}', specified during ingest",
              mediapackage, workflowDefinitionID);
    }

    // Use the default workflow definition if nothing was determined
    if (isBlank(workflowDefinitionID) && defaultWorkflowDefinionId != null) {
      logger.info("Using default workflow definition '{}' to process ingested mediapackage {}",
              defaultWorkflowDefinionId, mediapackage);
      workflowDefinitionID = defaultWorkflowDefinionId;
    }

    // Check if the workflow definition is valid
    if (StringUtils.isNotBlank(workflowDefinitionID) && StringUtils.isNotBlank(defaultWorkflowDefinionId)) {
      try {
        workflowService.getWorkflowDefinitionById(workflowDefinitionID);
      } catch (WorkflowDatabaseException e) {
        throw new IngestException(e);
      } catch (NotFoundException nfe) {
        logger.warn("Workflow definition {} not found, using default workflow {} instead", workflowDefinitionID,
                defaultWorkflowDefinionId);
        workflowDefinitionID = defaultWorkflowDefinionId;
      }
    }

    // Have we been able to find a workflow definition id?
    if (isBlank(workflowDefinitionID)) {
      ingestStatistics.failed();
      throw new IllegalStateException(
              "Can not ingest a workflow without a workflow definition or an existing instance. No default definition is specified");
    }

    // Let's make sure the workflow definition exists
    return workflowService.getWorkflowDefinitionById(workflowDefinitionID);
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.ingest.api.IngestService#discardMediaPackage(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public void discardMediaPackage(MediaPackage mp) throws IOException {
    String mediaPackageId = mp.getIdentifier().compact();
    for (MediaPackageElement element : mp.getElements()) {
      if (!workingFileRepository.delete(mediaPackageId, element.getIdentifier()))
        logger.warn("Unable to find (and hence, delete), this mediapackage element");
    }
    logger.info("Sucessful discarded mediapackage {}", mp);
  }

  protected URI addContentToRepo(MediaPackage mp, String elementId, URI uri) throws IOException {
    InputStream in = null;
    HttpResponse response = null;
    try {
      if (uri.toString().startsWith("http")) {
        HttpGet get = new HttpGet(uri);
        response = httpClient.execute(get);
        int httpStatusCode = response.getStatusLine().getStatusCode();
        if (httpStatusCode != 200) {
          throw new IOException(uri + " returns http " + httpStatusCode);
        }
        in = response.getEntity().getContent();
      } else {
        in = uri.toURL().openStream();
      }
      String fileName = FilenameUtils.getName(uri.getPath());
      if (isBlank(FilenameUtils.getExtension(fileName)))
        fileName = getContentDispositionFileName(response);

      if (isBlank(FilenameUtils.getExtension(fileName)))
        throw new IOException("No filename extension found: " + fileName);
      return addContentToRepo(mp, elementId, fileName, in);
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
  }

  private String getContentDispositionFileName(HttpResponse response) {
    if (response == null)
      return null;

    Header header = response.getFirstHeader("Content-Disposition");
    ContentDisposition contentDisposition = new ContentDisposition(header.getValue());
    return contentDisposition.getParameter("filename");
  }

  private URI addContentToRepo(MediaPackage mp, String elementId, String filename, InputStream file)
          throws IOException {
    ProgressInputStream progressInputStream = new ProgressInputStream(file);
    progressInputStream.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        long totalNumBytesRead = (Long) evt.getNewValue();
        long oldTotalNumBytesRead = (Long) evt.getOldValue();
        ingestStatistics.add(totalNumBytesRead - oldTotalNumBytesRead);
      }
    });
    return workingFileRepository.put(mp.getIdentifier().compact(), elementId, filename, progressInputStream);
  }

  private MediaPackage addContentToMediaPackage(MediaPackage mp, String elementId, URI uri,
          MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    logger.info("Adding element of type {} to mediapackage {}", type, mp);
    MediaPackageElement mpe = mp.add(uri, type, flavor);
    mpe.setIdentifier(elementId);
    return mp;
  }

  // ---------------------------------------------
  // --------- bind and unbind bundles ---------
  // ---------------------------------------------
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public void setWorkingFileRepository(WorkingFileRepository workingFileRepository) {
    this.workingFileRepository = workingFileRepository;
  }

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public void setDublinCoreService(DublinCoreCatalogService dublinCoreService) {
    this.dublinCoreService = dublinCoreService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    throw new IllegalStateException("Ingest jobs are not expected to be dispatched");
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Callback for setting the scheduler service.
   *
   * @param schedulerService
   *          the scheduler service to set
   */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  private MediaPackage createSmil(MediaPackage mediaPackage) throws IOException, IngestException {
    Stream<Track> partialTracks = Stream.empty();
    for (Track track : mediaPackage.getTracks()) {
      Long startTime = partialTrackStartTimes.getIfPresent(track.getIdentifier());
      if (startTime == null)
        continue;
      partialTracks = partialTracks.append(Opt.nul(track));
    }

    // No partial track available return without adding SMIL catalog
    if (partialTracks.isEmpty())
      return mediaPackage;

    // Inspect the partial tracks
    List<Track> tracks = partialTracks.map(newEnrichJob(mediaInspectionService).toFn())
            .map(payloadAsTrack(getServiceRegistry()).toFn())
            .each(MediaPackageSupport.updateElement(mediaPackage).toFn().toFx()).toList();

    // Create the SMIL document
    org.w3c.dom.Document smilDocument = SmilUtil.createSmil();
    for (Track track : tracks) {
      Long startTime = partialTrackStartTimes.getIfPresent(track.getIdentifier());
      if (startTime == null) {
        logger.error("No start time found for track {}", track);
        throw new IngestException("No start time found for track " + track.getIdentifier());
      }
      smilDocument = addSmilTrack(smilDocument, track, startTime);
      partialTrackStartTimes.invalidate(track.getIdentifier());
    }

    // Store the SMIL document in the mediapackage
    return addSmilCatalog(smilDocument, mediaPackage);
  }

  /**
   * Adds a SMIL catalog to a mediapackage if it's not already existing.
   *
   * @param smilDocument
   *          the smil document
   * @param mediaPackage
   *          the mediapackage to extend with the SMIL catalog
   * @return the augmented mediapcakge
   * @throws IOException
   *           if reading or writing of the SMIL catalog fails
   * @throws IngestException
   *           if the SMIL catalog already exists
   */
  private MediaPackage addSmilCatalog(org.w3c.dom.Document smilDocument, MediaPackage mediaPackage)
          throws IOException, IngestException {
    Option<org.w3c.dom.Document> optSmilDocument = loadSmilDocument(workingFileRepository, mediaPackage);
    if (optSmilDocument.isSome())
      throw new IngestException("SMIL already exists!");

    InputStream in = null;
    try {
      in = XmlUtil.serializeDocument(smilDocument);
      String elementId = UUID.randomUUID().toString();
      URI uri = workingFileRepository.put(mediaPackage.getIdentifier().compact(), elementId, PARTIAL_SMIL_NAME, in);
      MediaPackageElement mpe = mediaPackage.add(uri, MediaPackageElement.Type.Catalog, MediaPackageElements.SMIL);
      mpe.setIdentifier(elementId);
      // Reset the checksum since it changed
      mpe.setChecksum(null);
      mpe.setMimeType(MimeTypes.SMIL);
      return mediaPackage;
    } finally {
      IoSupport.closeQuietly(in);
    }
  }

  /**
   * Load a SMIL document of a media package.
   *
   * @return the document or none if no media package element found.
   */
  private Option<org.w3c.dom.Document> loadSmilDocument(final WorkingFileRepository workingFileRepository,
          MediaPackage mp) {
    return mlist(mp.getElements()).filter(MediaPackageSupport.Filters.isSmilCatalog).headOpt()
            .map(new Function<MediaPackageElement, org.w3c.dom.Document>() {
              @Override
              public org.w3c.dom.Document apply(MediaPackageElement mpe) {
                InputStream in = null;
                try {
                  in = workingFileRepository.get(mpe.getMediaPackage().getIdentifier().compact(), mpe.getIdentifier());
                  return SmilUtil.loadSmilDocument(in, mpe);
                } catch (Exception e) {
                  logger.warn("Unable to load smil document from catalog '{}'", mpe, e);
                  return Misc.chuck(e);
                } finally {
                  IOUtils.closeQuietly(in);
                }
              }
            });
  }

  /**
   * Adds a SMIL track by a mediapackage track to a SMIL document
   *
   * @param smilDocument
   *          the SMIL document to extend
   * @param track
   *          the mediapackage track
   * @param startTime
   *          the start time
   * @return the augmented SMIL document
   * @throws IngestException
   *           if the partial flavor type is not valid
   */
  private org.w3c.dom.Document addSmilTrack(org.w3c.dom.Document smilDocument, Track track, long startTime)
          throws IngestException {
    if (MediaPackageElements.PRESENTER_SOURCE.getType().equals(track.getFlavor().getType())) {
      return SmilUtil.addTrack(smilDocument, SmilUtil.TrackType.PRESENTER, track.hasVideo(), startTime,
              track.getDuration(), track.getURI(), track.getIdentifier());
    } else if (MediaPackageElements.PRESENTATION_SOURCE.getType().equals(track.getFlavor().getType())) {
      return SmilUtil.addTrack(smilDocument, SmilUtil.TrackType.PRESENTATION, track.hasVideo(), startTime,
              track.getDuration(), track.getURI(), track.getIdentifier());
    } else {
      logger.warn("Invalid partial flavor type {} of track {}", track.getFlavor(), track);
      throw new IngestException(
              "Invalid partial flavor type " + track.getFlavor().getType() + " of track " + track.getURI().toString());
    }
  }

  /** Create a media inspection job for a mediapackage element. */
  public static Function<MediaPackageElement, Job> newEnrichJob(final MediaInspectionService svc) {
    return new Function.X<MediaPackageElement, Job>() {
      @Override
      public Job xapply(MediaPackageElement e) throws Exception {
        return svc.enrich(e, true);
      }
    };
  }

  /**
   * Interpret the payload of a completed Job as a MediaPackageElement. Wait for the job to complete if necessary.
   */
  public static Function<Job, Track> payloadAsTrack(final ServiceRegistry reg) {
    return new Function.X<Job, Track>() {
      @Override
      public Track xapply(Job job) throws MediaPackageException {
        waitForJob(reg, none(0L), job);
        return (Track) MediaPackageElementParser.getFromXml(job.getPayload());
      }
    };
  }
}
