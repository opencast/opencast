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
package org.opencastproject.ingest.impl;

import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.ingest.impl.jmx.IngestStatistics;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.ZipUtil;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import javax.management.ObjectInstance;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Creates and augments Matterhorn MediaPackages. Stores media into the Working File Repository.
 */
public class IngestServiceImpl extends AbstractJobProducer implements IngestService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

  /** The configuration key that defines the default workflow definition */
  protected static final String WORKFLOW_DEFINITION_DEFAULT = "org.opencastproject.workflow.default.definition";

  public static final String JOB_TYPE = "org.opencastproject.ingest";

  /** Methods that ingest streams create jobs with this operation type */
  public static final String INGEST_STREAM = "zip";

  /** Methods that ingest tracks from a URI create jobs with this operation type */
  public static final String INGEST_TRACK_FROM_URI = "track";

  /** Methods that ingest attachments from a URI create jobs with this operation type */
  public static final String INGEST_ATTACHMENT_FROM_URI = "attachment";

  /** Methods that ingest catalogs from a URI create jobs with this operation type */
  public static final String INGEST_CATALOG_FROM_URI = "catalog";

  /** Ingest can only occur for a workflow currently in one of these operations. */
  public static final String[] PRE_PROCESSING_OPERATIONS = new String[] { "schedule", "capture", "ingest" };

  /** The JMX business object for ingest statistics */
  private IngestStatistics ingestStatistics = new IngestStatistics();

  /** The JMX bean object instance */
  private ObjectInstance registerMXBean;

  /** The workflow service */
  private WorkflowService workflowService;

  /** The workspace */
  private Workspace workspace;

  /** The http client */
  private TrustedHttpClient httpClient;

  /** The series service */
  private SeriesService seriesService;

  /** The dublin core service */
  private DublinCoreCatalogService dublinCoreService;

  /** The opencast service registry */
  private ServiceRegistry serviceRegistry;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The default workflow identifier, if one is configured */
  protected String defaultWorkflowDefinionId;

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
  protected void activate(ComponentContext cc) {
    logger.info("Ingest Service started.");
    defaultWorkflowDefinionId = StringUtils.trimToNull(cc.getBundleContext().getProperty(WORKFLOW_DEFINITION_DEFAULT));
    if (defaultWorkflowDefinionId == null) {
      logger.info("No default workflow definition specified. Ingest operations without a specified workflow "
              + "definition will fail");
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream)
   */
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream) throws IngestException, IOException,
          MediaPackageException {
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
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream, String wd) throws MediaPackageException,
          IOException, IngestException, NotFoundException {
    return addZippedMediaPackage(zipStream, wd, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addZippedMediaPackage(java.io.InputStream, java.lang.String)
   */
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
  public WorkflowInstance addZippedMediaPackage(InputStream zipStream, String wd, Map<String, String> workflowConfig,
          Long workflowId) throws MediaPackageException, IOException, IngestException, NotFoundException,
          UnauthorizedException {
    // Start a job synchronously. We can't keep the open input stream waiting around.
    Job job = null;

    // Keep track of the zip file we use to store the zip stream
    File zipFile = null;

    try {

      // We don't need anybody to do the dispatching for us. Therefore we need to make sure that the job is never in
      // QUEUED state but set it to INSTANTIATED in the beginning and then manually switch it to RUNNING.
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_STREAM, null, null, false);
      job.setStatus(Status.RUNNING);
      serviceRegistry.updateJob(job);

      // locally unpack the mediaPackage
      // save inputStream to file
      URI uri = workspace.putInCollection("ingest-temp" + job.getId(), job.getId() + ".zip", zipStream);
      zipFile = workspace.get(uri);
      logger.info("Ingesting zipped media package to {}", zipFile);

      // unpack, cleanup will happen in the finally block
      ZipUtil.unzip(zipFile, zipFile.getParentFile());

      // check media package and write data to file repo
      File manifest = getManifest(zipFile.getParentFile());
      if (manifest == null) {
        // try to find the manifest in a subdirectory, since the zip may
        // have been constructed this way
        File[] subDirs = zipFile.getParentFile().listFiles(new FileFilter() {
          public boolean accept(File pathname) {
            return pathname.isDirectory();
          }
        });
        for (File subdir : subDirs) {
          manifest = getManifest(subdir);
          if (manifest != null)
            break;
        }
        if (manifest == null)
          throw new MediaPackageException("no manifest found in this zip");
      }

      // Build the mediapackage
      MediaPackage mp = null;
      MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      builder.setSerializer(new DefaultMediaPackageSerializerImpl(manifest.getParentFile()));
      InputStream manifestStream = null;
      try {
        manifestStream = manifest.toURI().toURL().openStream();
        mp = builder.loadFromXml(manifestStream);
        // TODO: Remove the following patch when the compatibility with pre-1.4 MediaPackages is discarded
        // =========================================================================================
        // =================================== PATCH BEGIN =========================================
        // =========================================================================================
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        String nameSpace = "http://mediapackage.opencastproject.org";

        try {
          Document domMP = MediaPackageParser.getAsXml(mp,
                  new DefaultMediaPackageSerializerImpl(manifest.getParentFile()));
          if (!nameSpace.equals(domMP.getDocumentElement().getAttribute("xmlns"))) {
            domMP.getDocumentElement().setAttribute("xmlns", nameSpace);
            baos = new ByteArrayOutputStream();
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(domMP), new StreamResult(baos));
            bais = new ByteArrayInputStream(baos.toByteArray());
            mp = builder.loadFromXml(bais);
          }
        } catch (TransformerException e) {
          throw new MediaPackageException("Error serializing a MediaPackage", e);
        } catch (TransformerFactoryConfigurationError e) {
          throw new MediaPackageException("Error serializing a MediaPackage", e);
        } finally {
          IOUtils.closeQuietly(bais);
          IOUtils.closeQuietly(baos);
        }
        // =========================================================================================
        // =================================== PATCH END ===========================================
        // =========================================================================================
      } finally {
        IOUtils.closeQuietly(manifestStream);
      }
      for (MediaPackageElement element : mp.elements()) {
        String elId = element.getIdentifier();
        if (elId == null) {
          elId = UUID.randomUUID().toString();
          element.setIdentifier(elId);
        }
        String filename = element.getURI().toURL().getFile();
        filename = filename.substring(filename.lastIndexOf("/"));
        InputStream elementStream = null;
        URI newUrl = null;
        try {
          elementStream = element.getURI().toURL().openStream();
          newUrl = addContentToRepo(mp, elId, filename, elementStream);
          elementStream.close();
        } finally {
          IOUtils.closeQuietly(elementStream);
        }
        element.setURI(newUrl);

        // if this is a series, update the series service
        // TODO: This should be triggered somehow instead of being handled here
        if (MediaPackageElements.SERIES.equals(element.getFlavor())) {
          updateSeries(element.getURI());
        }
      }

      // Done, update the job status and return the created workflow instance
      WorkflowInstance workflowInstance = null;
      if (wd == null) {
        workflowInstance = ingest(mp);
      } else {
        workflowInstance = ingest(mp, wd, workflowConfig, workflowId);
      }
      job.setStatus(Job.Status.FINISHED);
      return workflowInstance;

    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } catch (IOException e) {
      job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (MediaPackageException e) {
      job.setStatus(Job.Status.FAILED);
      throw e;
    } finally {
      workspace.deleteFromCollection("ingest-temp" + job.getId(), job.getId() + ".zip");
      FileUtils.deleteQuietly(zipFile.getParentFile());
      try {
        serviceRegistry.updateJob(job);
      } catch (Exception e) {
        throw new IngestException("Unable to update job", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#createMediaPackage()
   */
  public MediaPackage createMediaPackage() throws MediaPackageException,
          org.opencastproject.util.ConfigurationException, HandleException {
    MediaPackage mediaPackage;
    try {
      mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    } catch (MediaPackageException e) {
      logger.error("INGEST:Failed to create media package " + e.getLocalizedMessage());
      throw e;
    }
    mediaPackage.setDate(new Date());
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
    Job job = null;
    try {
      job = serviceRegistry.createJob(
              JOB_TYPE,
              INGEST_TRACK_FROM_URI,
              Arrays.asList(uri.toString(), flavor == null ? null : flavor.toString(),
                      MediaPackageParser.getAsXml(mediaPackage)), null, false);
      String elementId = UUID.randomUUID().toString();
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      return mp;
    } catch (IOException e) {
      if (job != null)
        job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } finally {
      try {
        if (job != null) {
          serviceRegistry.updateJob(job);
        }
      } catch (Exception e) {
        throw new IngestException("Unable to update ingest job", e);
      }
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
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_STREAM, null, null, false);
      String elementId = UUID.randomUUID().toString();
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Track,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      return mp;
    } catch (IOException e) {
      if (job != null)
        job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } finally {
      try {
        serviceRegistry.updateJob(job);
      } catch (Exception e) {
        throw new IngestException("Unable to update ingest job", e);
      }
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
              Arrays.asList(uri.toString(), flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)), null, false);
      String elementId = UUID.randomUUID().toString();
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      if (MediaPackageElements.SERIES.equals(flavor)) {
        updateSeries(uri);
      }
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      return mp;
    } catch (IOException e) {
      if (job != null)
        job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } finally {
      try {
        serviceRegistry.updateJob(job);
      } catch (Exception e) {
        throw new IngestException("Unable to update ingest job", e);
      }
    }
  }

  /**
   * Updates the persistent representation of a series based on a potentially modified dublin core document.
   * 
   * @param uri
   *          the URI to the dublin core document containing series metadata.
   */
  protected void updateSeries(URI uri) throws IOException, IngestException {
    HttpResponse response = null;
    InputStream in = null;
    try {
      HttpGet getDc = new HttpGet(uri);
      response = httpClient.execute(getDc);
      in = response.getEntity().getContent();
      DublinCoreCatalog dc = dublinCoreService.load(in);
      String id = dc.getFirst(DublinCore.PROPERTY_IDENTIFIER);
      if (id == null) {
        logger.warn("Series dublin core document contains no identifier");
      } else {
        try {
          seriesService.updateSeries(dc);
        } catch (Exception e) {
          throw new IngestException(e);
        }
      }
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
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
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_STREAM, null, null, false);
      String elementId = UUID.randomUUID().toString();
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      if (MediaPackageElements.SERIES.equals(flavor)) {
        updateSeries(newUrl);
      }
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Catalog,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      return mp;
    } catch (IOException e) {
      if (job != null)
        job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } finally {
      try {
        serviceRegistry.updateJob(job);
      } catch (Exception e) {
        throw new IngestException("Unable to update ingest job", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addAttachment(java.net.URI,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  public MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_ATTACHMENT_FROM_URI,
              Arrays.asList(uri.toString(), flavor.toString(), MediaPackageParser.getAsXml(mediaPackage)), null, false);
      String elementId = UUID.randomUUID().toString();
      URI newUrl = addContentToRepo(mediaPackage, elementId, uri);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      return mp;
    } catch (IOException e) {
      if (job != null)
        job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } finally {
      try {
        serviceRegistry.updateJob(job);
      } catch (Exception e) {
        throw new IngestException("Unable to update ingest job", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.ingest.api.IngestService#addAttachment(java.io.InputStream, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, org.opencastproject.mediapackage.MediaPackage)
   */
  public MediaPackage addAttachment(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws IOException, IngestException {
    Job job = null;
    try {
      job = serviceRegistry.createJob(JOB_TYPE, INGEST_STREAM, null, null, false);
      String elementId = UUID.randomUUID().toString();
      URI newUrl = addContentToRepo(mediaPackage, elementId, fileName, in);
      MediaPackage mp = addContentToMediaPackage(mediaPackage, elementId, newUrl, MediaPackageElement.Type.Attachment,
              flavor);
      job.setStatus(Job.Status.FINISHED);
      return mp;
    } catch (IOException e) {
      if (job != null)
        job.setStatus(Job.Status.FAILED);
      throw e;
    } catch (ServiceRegistryException e) {
      throw new IngestException(e);
    } finally {
      try {
        serviceRegistry.updateJob(job);
      } catch (Exception e) {
        throw new IngestException("Unable to update ingest job", e);
      }
    }

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
  public WorkflowInstance ingest(MediaPackage mp, String wd, Map<String, String> properties) throws IngestException,
          NotFoundException {
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
  public WorkflowInstance ingest(MediaPackage mp, String workflowDefinitionID, Map<String, String> properties,
          Long workflowId) throws IngestException, NotFoundException, UnauthorizedException {
    // If the workflow definition and instance ID are null, use the default, or throw if there is none
    if (workflowDefinitionID == null && workflowId == null) {
      if (this.defaultWorkflowDefinionId == null) {
        ingestStatistics.failed();
        throw new IllegalStateException(
                "Can not ingest a workflow without a workflow definition or an existing instance. No default definition is specified");
      } else {
        workflowDefinitionID = this.defaultWorkflowDefinionId;
      }
    }

    // Look for the workflow instance (if provided)
    WorkflowInstance workflow = null;
    if (workflowId != null) {
      try {
        workflow = workflowService.getWorkflowById(workflowId.longValue());
        if (workflow.getState().equals(WorkflowState.FAILED)) {
          logger.warn("The workflow with id '{}' is failed, starting a new workflow for this recording",
                  workflow.getId());
          workflow = null;
        } else if (workflow.getState().equals(WorkflowState.SUCCEEDED)) {
          logger.warn("The workflow with id '{}' already succeeded, starting a new workflow for this recording",
                  workflow.getId());
          workflow = null;
        }

      } catch (NotFoundException e) {
        logger.warn("Failed to find a workflow with id '{}'", workflowId);
      } catch (WorkflowDatabaseException e) {
        ingestStatistics.failed();
        throw new IngestException(e);
      }
    }

    try {
      if (workflow == null) {
        WorkflowDefinition workflowDef = workflowService.getWorkflowDefinitionById(workflowDefinitionID);
        ingestStatistics.successful();
        return workflowService.start(workflowDef, mp, properties);
      } else {
        // Ensure that we're in one of the pre-processing operations
        WorkflowDefinition workflowDef = workflowService.getWorkflowDefinitionById(workflowDefinitionID);

        // The pre-processing workflow contains three operations: schedule, capture, and ingest. If we are not in the
        // last operation of the preprocessing workflow (due to the capture agent not reporting on its recording
        // status), we need to advance the workflow.
        WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
        if (currentOperation == null) {
          ingestStatistics.failed();
          throw new IllegalStateException(workflow
                  + " has no current operation, so can not be resumed with a new mediapackage");
        }
        String currentOperationTemplate = currentOperation.getTemplate();
        if (!Arrays.asList(PRE_PROCESSING_OPERATIONS).contains(currentOperationTemplate)) {
          ingestStatistics.failed();
          throw new IllegalStateException(workflow + " is already in operation " + currentOperationTemplate
                  + ", so we can not ingest");
        }

        int preProcessingOperations = workflow.getOperations().size();

        // Merge the current mediapackage with the new one
        MediaPackage existingMediaPackage = workflow.getMediaPackage();
        for (MediaPackageElement element : mp.getElements()) {
          if (element instanceof Catalog) {
            // if the existing mediapackage contains a catalog of the same flavor, keep the server-side catalog, since
            // it is more likely to be up-to-date
            MediaPackageElementFlavor catalogFlavor = element.getFlavor();
            MediaPackageElement[] existingCatalogs = existingMediaPackage.getCatalogs(catalogFlavor);
            if (existingCatalogs != null && existingCatalogs.length > 0) {
              logger.info(
                      "Mediapackage {} already contains a catalog with flavor {}.  Skipping the conflicting ingested catalog",
                      existingMediaPackage, catalogFlavor);
              continue;
            }
          }
          existingMediaPackage.add(element);
        }

        // Extend the workflow operations
        workflow.extend(workflowDef);

        // Advance the workflow
        int currentPosition = workflow.getOperations().indexOf(currentOperation);
        while (currentPosition < preProcessingOperations - 1) {
          currentOperation = workflow.getCurrentOperation();
          logger.debug("Advancing workflow (skipping {})", currentOperation);
          if (currentOperation.getId() != null) {
            try {
              Job job = serviceRegistry.getJob(currentOperation.getId());
              job.setStatus(Status.FINISHED);
              serviceRegistry.updateJob(job);
            } catch (ServiceRegistryException e) {
              ingestStatistics.failed();
              throw new IllegalStateException("Error updating job associated with skipped operation "
                      + currentOperation, e);
            }
          }
          currentOperation = workflow.next();
          currentPosition++;
        }

        // Ingest succeeded
        currentOperation.setState(OperationState.SUCCEEDED);

        // Update
        workflowService.update(workflow);

        // resume the workflow
        workflowService.resume(workflowId.longValue(), properties);

        ingestStatistics.successful();

        // Return the updated workflow instance
        return workflowService.getWorkflowById(workflowId.longValue());
      }
    } catch (WorkflowException e) {
      ingestStatistics.failed();
      throw new IngestException(e);
    }
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
      try {
        workspace.delete(mediaPackageId, element.getIdentifier());
      } catch (NotFoundException e) {
        logger.warn("Unable to find (and hence, delete), this mediapackage element", e);
      }
    }
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
      return addContentToRepo(mp, elementId, FilenameUtils.getName(uri.toURL().toString()), in);
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
  }

  private URI addContentToRepo(MediaPackage mp, String elementId, String filename, InputStream file) throws IOException {
    ProgressInputStream progressInputStream = new ProgressInputStream(file);
    progressInputStream.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        long totalNumBytesRead = (Long) evt.getNewValue();
        long oldTotalNumBytesRead = (Long) evt.getOldValue();
        ingestStatistics.add(totalNumBytesRead - oldTotalNumBytesRead);
      }
    });
    return workspace.put(mp.getIdentifier().compact(), elementId, filename, progressInputStream);
  }

  private MediaPackage addContentToMediaPackage(MediaPackage mp, String elementId, URI uri,
          MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    logger.info("Adding element of type {} to mediapackage {}", type, mp);
    MediaPackageElement mpe = mp.add(uri, type, flavor);
    mpe.setIdentifier(elementId);
    return mp;
  }

  /**
   * Returns the manifest from a media package directory or <code>null</code> if no manifest was found. Manifests are
   * expected to be named <code>index.xml</code> or <code>manifest.xml</code>.
   * 
   * @param mediapackageDir
   *          the potential mediapackage directory
   * @return the manifest file
   */
  private File getManifest(File mediapackageDir) {
    Stack<File> stack = new Stack<File>();
    stack.push(mediapackageDir);
    for (File f : mediapackageDir.listFiles()) {
      if (f.isDirectory())
        stack.push(f);
    }
    while (!stack.empty()) {
      File dir = stack.pop();
      File[] files = dir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.getName().endsWith(".xml");
        }
      });
      for (File f : files) {
        if ("index.xml".equals(f.getName()) || "manifest.xml".equals(f.getName()))
          return f;
      }
    }
    return null;
  }

  private File createDirectory(String dir) throws IOException {
    File f = new File(dir);
    if (!f.exists()) {
      FileUtils.forceMkdir(f);
    }
    return f;
  }

  // ---------------------------------------------
  // --------- bind and unbind bundles ---------
  // ---------------------------------------------
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
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
   * Sets a reference to the organization directory service.
   * 
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
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

}
