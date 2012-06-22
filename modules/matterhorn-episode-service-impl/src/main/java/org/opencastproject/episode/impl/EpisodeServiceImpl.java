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

package org.opencastproject.episode.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.impl.solr.SolrIndexManager;
import org.opencastproject.episode.impl.solr.SolrRequester;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/**
 * A Solr-based {@link EpisodeService} implementation.
 * todo use archive component
 */
public final class EpisodeServiceImpl implements EpisodeService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(EpisodeServiceImpl.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.episode.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.episode.solr.dir";

  /** The job type */
  public static final String JOB_TYPE = "org.opencastproject.episode";

  /** The add operation */
  public static final String OPERATION_ADD = "add";

  /** The delete operation */
  public static final String OPERATION_DELETE = "delete";

  /** Solr server */
  private SolrServer solrServer;

  private SolrRequester solrRequester;

  private SolrIndexManager indexManager;

  private List<StaticMetadataService> mdServices = new ArrayList<StaticMetadataService>();

  private Mpeg7CatalogService mpeg7CatalogService;

  private SeriesService seriesService;

  /** The local workspace */
  private Workspace workspace;

  /** The security service */
  private SecurityService securityService;

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** The organization directory */
  private OrganizationDirectoryService orgDirectory;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  /** The workflow service */
  private WorkflowService workflowService;

  /** Dynamic reference. */
  public void setStaticMetadataService(StaticMetadataService mdService) {
    this.mdServices.add(mdService);
    if (indexManager != null)
      indexManager.setStaticMetadataServices(mdServices);
  }

  public void unsetStaticMetadataService(StaticMetadataService mdService) {
    this.mdServices.remove(mdService);
    if (indexManager != null)
      indexManager.setStaticMetadataServices(mdServices);
  }

  public void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
  }

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setOrgDirectory(OrganizationDirectoryService orgDirectory) {
    this.orgDirectory = orgDirectory;
  }

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** For testing purposes only! */
  void testSetup(SolrServer server, SolrRequester requester, SolrIndexManager manager) {
    this.solrServer = server;
    this.solrRequester = requester;
    this.indexManager = manager;
  }

  /**
   * Service activator, called via declarative services configuration. If the solr server url is configured, we try to
   * connect to it. If not, the solr data directory with an embedded Solr server is used.
   * 
   * @param cc
   *          the component context
   */
  public void activate(final ComponentContext cc) throws IllegalStateException {
    final String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));

    solrServer = new Object() {
      SolrServer create() {
        if (solrServerUrlConfig != null) {
          try {
            URL solrServerUrl = new URL(solrServerUrlConfig);
            return setupSolr(solrServerUrl);
          } catch (MalformedURLException e) {
            throw connectError(solrServerUrlConfig, e);
          } catch (SolrServerException e) {
            throw connectError(solrServerUrlConfig, e);
          } catch (IOException e) {
            throw connectError(solrServerUrlConfig, e);
          }
        } else if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
          String solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
          try {
            return setupSolr(new File(solrRoot));
          } catch (IOException e) {
            throw connectError(solrServerUrlConfig, e);
          } catch (SolrServerException e) {
            throw connectError(solrServerUrlConfig, e);
          }
        } else {
          String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
          if (storageDir == null)
            throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
          String solrRoot = PathSupport.concat(storageDir, "archive");
          try {
            return setupSolr(new File(solrRoot));
          } catch (IOException e) {
            throw connectError(solrServerUrlConfig, e);
          } catch (SolrServerException e) {
            throw connectError(solrServerUrlConfig, e);
          }
        }
      }

      IllegalStateException connectError(String target, Exception e) {
        return new IllegalStateException("Unable to connect to solr at " + target, e);
      }
      // CHECKSTYLE:OFF
    }.create();
    // CHECKSTYLE:ON

    solrRequester = new SolrRequester(solrServer, securityService);
    indexManager = new SolrIndexManager(solrServer, workspace, mdServices, seriesService, mpeg7CatalogService,
            securityService);

    // Populate the search index if it is empty
    try {
      if (solrRequester.getForAdministrativeRead(new EpisodeQuery()).getItems().length == 0
              && serviceRegistry.count(JOB_TYPE, Status.FINISHED) > 0) {
        // todo populateIndex();
      }
    } catch (EpisodeServiceException e) {
      throw new IllegalStateException("Can not read the solr index", e);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Can not read jobs from the service registry", e);
//    } catch (MediaPackageException e) {
//      throw new IllegalStateException("Can not read the mediapackages from jobs in the service registry", e);
    } catch (SolrServerException e) {
      throw new IllegalStateException("Can not read the solr index", e);
//    } catch (UnauthorizedException e) {
//      throw new IllegalStateException("Operation not permitted", e);
    }
  }

  /**
   * Service deactivator, called via declarative services configuration.
   */
  public void deactivate() {
    SolrServerFactory.shutdown(solrServer);
  }

  /**
   * Prepares the embedded solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  static SolrServer setupSolr(File solrRoot) throws IOException, SolrServerException {
    logger.info("Setting up solr search index at {}", solrRoot);
    File solrConfigDir = new File(solrRoot, "conf");

    // Create the config directory
    if (solrConfigDir.exists()) {
      logger.info("solr search index found at {}", solrConfigDir);
    } else {
      logger.info("solr config directory doesn't exist.  Creating {}", solrConfigDir);
      FileUtils.forceMkdir(solrConfigDir);
    }

    // Make sure there is a configuration in place
    copyClasspathResourceToFile("/solr/conf/protwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/schema.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/scripts.conf", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/solrconfig.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/stopwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/synonyms.txt", solrConfigDir);

    // Test for the existence of a data directory
    File solrDataDir = new File(solrRoot, "data");
    if (!solrDataDir.exists()) {
      FileUtils.forceMkdir(solrDataDir);
    }

    // Test for the existence of the index. Note that an empty index directory will prevent solr from
    // completing normal setup.
    File solrIndexDir = new File(solrDataDir, "index");
    if (solrIndexDir.exists() && solrIndexDir.list().length == 0) {
      FileUtils.deleteDirectory(solrIndexDir);
    }

    return SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
  }

  /**
   * Prepares the embedded solr environment.
   * 
   * @param url
   *          the url of the remote solr server
   */
  static SolrServer setupSolr(URL url) throws IOException, SolrServerException {
    logger.info("Connecting to solr search index at {}", url);
    return SolrServerFactory.newRemoteInstance(url);
  }

  // TODO: generalize this method
  static void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = EpisodeServiceImpl.class.getResourceAsStream(classpath);
      File file = new File(dir, FilenameUtils.getName(classpath));
      logger.debug("copying " + classpath + " to " + file);
      fos = new FileOutputStream(file);
      IOUtils.copy(in, fos);
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fos);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.episode.api.EpisodeService#getByQuery(String, int, int)
   */
  public SearchResult getByQuery(String query, int limit, int offset) throws EpisodeServiceException {
    try {
      logger.debug("Searching index using custom query '" + query + "'");
      return solrRequester.getByQuery(query, limit, offset);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.episode.api.EpisodeService#add(org.opencastproject.mediapackage.MediaPackage)
   */
  public void add(MediaPackage mediaPackage) throws EpisodeServiceException, MediaPackageException, IllegalArgumentException,
          UnauthorizedException, ServiceRegistryException {
    User currentUser = securityService.getUser();
    String orgAdminRole = securityService.getOrganization().getAdminRole();
    if (!currentUser.hasRole(orgAdminRole) && !currentUser.hasRole(GLOBAL_ADMIN_ROLE)
            && !authorizationService.hasPermission(mediaPackage, WRITE_PERMISSION)) {
      throw new UnauthorizedException(currentUser, EpisodeService.WRITE_PERMISSION);
    }
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Unable to add a null mediapackage");
    }
    try {
      logger.debug("Attempting to add mediapackage {} to archive", mediaPackage.getIdentifier());
      AccessControlList acl = authorizationService.getAccessControlList(mediaPackage);
      List<String> args = new ArrayList<String>();
      args.add(securityService.getOrganization().getId());
      // create a job to make the operation visible to the admin ui
      Job job = serviceRegistry.createJob(JOB_TYPE, OPERATION_ADD, args, MediaPackageParser.getAsXml(mediaPackage),
              false);
      job.setStatus(Status.FINISHED);
      try {
        serviceRegistry.updateJob(job);
      } catch (NotFoundException e) {
        throw new IllegalStateException(e); // should not be possible
      }
      // todo save to archive
      if (indexManager.add(mediaPackage, acl)) {
        logger.info("Added mediapackage {} to the archive", mediaPackage.getIdentifier());
      } else {
        logger.warn("Failed to add mediapackage {} to the archive", mediaPackage.getIdentifier());
      }
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.episode.api.EpisodeService#delete(String)
   */
  public boolean delete(String mediaPackageId) throws EpisodeServiceException, UnauthorizedException {
    SearchResult result;
    try {
      result = solrRequester.getForWrite(new EpisodeQuery().withId(mediaPackageId));
      if (result.getItems().length == 0) {
        logger.warn(
                "Can not delete mediapackage {}, which is not available for the current user to delete from the archive",
                mediaPackageId);
        return false;
      }
      logger.info("Removing mediapackage {} from the archive", mediaPackageId);
      // todo delete from the archive component
      return indexManager.delete(mediaPackageId);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  @Override
  public boolean lock(String mediaPackageId, boolean lock) throws EpisodeServiceException, UnauthorizedException {
    SearchResult result;
    try {
      result = solrRequester.getForWrite(new EpisodeQuery().withId(mediaPackageId));
      if (result.getItems().length == 0) {
        logger.warn(
            "Can not (un)lock mediapackage {}, which is not available for the current user",
            mediaPackageId);
        return false;
      }
      logger.info("(Un)locking mediapackage {}", mediaPackageId);
      return indexManager.setLocked(mediaPackageId, lock);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  // -- applyWorkflow

  @Override
  public WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, EpisodeQuery q) 
          throws EpisodeServiceException, UnauthorizedException {
    // never include locked packages todo it's bad to manipulate the query object... immutability!
    q.includeLocked(false);
    SearchResult result = null;
    try {
      result = solrRequester.getForRead(q);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
    List<WorkflowInstance> workflows = applyWorkflow(workflowDefinition, result.getItems(), NO_WORKFLOW_PROPS);
    return workflows.toArray(new WorkflowInstance[workflows.size()]);
  }    

  @Override
  public WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, List<String> mediaPackageIds) 
          throws EpisodeServiceException, UnauthorizedException {
    return applyWorkflow(workflowDefinition, mediaPackageIds, NO_WORKFLOW_PROPS);
  }

  @Override
  public WorkflowInstance[] applyWorkflow(String workflowDefinitionId, List<String> mediaPackageIds) 
          throws EpisodeServiceException, UnauthorizedException {
    return applyWorkflow(getWorkflowDefinition(workflowDefinitionId), mediaPackageIds, NO_WORKFLOW_PROPS);
  }

  @Override
  public WorkflowInstance[] applyWorkflow(String workflowDefinitionId, List<String> mediaPackageIds, 
                                          Map<String, String> properties) 
          throws EpisodeServiceException, UnauthorizedException {
    return applyWorkflow(getWorkflowDefinition(workflowDefinitionId),
                         mediaPackageIds,
                         some(properties));
  }

  @Override
  public WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, EpisodeQuery q,
          Map<String, String> properties) throws EpisodeServiceException, UnauthorizedException {
    return applyWorkflow(workflowDefinition, q, some(properties));
  }

  @Override
  public WorkflowInstance[] applyWorkflow(WorkflowDefinition workflowDefinition, List<String> mediaPackageIds,
          Map<String, String> properties) throws EpisodeServiceException, UnauthorizedException {
    return applyWorkflow(workflowDefinition, mediaPackageIds, some(properties));
  }

  // -- applyWorkflow helper

  private static final Option<Map<String, String>> NO_WORKFLOW_PROPS = none();
  
  private WorkflowDefinition getWorkflowDefinition(String workflowDefinitionId) {
    try {
      return workflowService.getWorkflowDefinitionById(workflowDefinitionId);
    } catch (WorkflowDatabaseException e) {
      throw new EpisodeServiceException(e);
    } catch (NotFoundException e) {
      throw new EpisodeServiceException(e);
    }
  }

  private WorkflowInstance[] applyWorkflow(
          WorkflowDefinition workflowDefinition,
          EpisodeQuery q,
          Option<Map<String, String>> properties)
          throws EpisodeServiceException, UnauthorizedException {
    // never include locked packages todo it's bad to manipulate the query object... immutability!
    q.includeLocked(false);
    SearchResult result = null;
    try {
      result = solrRequester.getForRead(q);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
    List<WorkflowInstance> workflows = applyWorkflow(workflowDefinition, result.getItems(), properties);
    return workflows.toArray(new WorkflowInstance[workflows.size()]);
  }

  private WorkflowInstance[] applyWorkflow(
          final WorkflowDefinition workflowDefinition,
          List<String> mediaPackageIds,
          Option<Map<String, String>> properties)
          throws EpisodeServiceException, UnauthorizedException {
    List<WorkflowInstance> workflows = new ArrayList<WorkflowInstance>();
    for (String id : mediaPackageIds) {
      SearchResultItem[] items = new SearchResultItem[0];
      try {
        items = solrRequester.getForWrite(new EpisodeQuery().withId(id)).getItems();
      } catch (SolrServerException e) {
        logger.error("Error accessing solr index", e);
      }
      workflows.addAll(applyWorkflow(workflowDefinition, items, properties));
    }
    return workflows.toArray(new WorkflowInstance[workflows.size()]);
  }

  private List<WorkflowInstance> applyWorkflow(
          final WorkflowDefinition workflowDefinition,
          SearchResultItem[] items,
          Option<Map<String, String>> properties) {
    final List<WorkflowInstance> workflows = new ArrayList<WorkflowInstance>(items.length);
    for (final SearchResultItem item : items) {
      properties.fold(new Option.Match<Map<String, String>, Void>() {
        @Override
        public Void some(Map<String, String> p) {
          try {
            workflows.add(
                    workflowService.start(workflowDefinition, item.getMediaPackage(), p));
          } catch (WorkflowException e) {
            logger.error("Error starting workflow", e);
          }
          return null;
        }

        @Override
        public Void none() {
          try {
            workflows.add(
                    workflowService.start(workflowDefinition, item.getMediaPackage()));
          } catch (WorkflowException e) {
            logger.error("Error starting workflow", e);
          }
          return null;
        }
      });
    }
    return workflows;
  }

  // --

  /**
   * Clears the complete solr index.
   * 
   * @throws EpisodeServiceException
   *           if clearing the index fails
   */
  public void clear() throws EpisodeServiceException {
    try {
      logger.info("Clearing the search index");
      indexManager.clear();
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.episode.api.EpisodeService#getByQuery(EpisodeQuery)
   */
  public SearchResult getByQuery(EpisodeQuery q) throws EpisodeServiceException {
    try {
      logger.debug("Searching index using query object '" + q + "'");
      return solrRequester.getForRead(q);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.episode.api.EpisodeService#getForAdministrativeRead(EpisodeQuery)
   */
  @Override
  public SearchResult getForAdministrativeRead(EpisodeQuery q) throws EpisodeServiceException, UnauthorizedException {
    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE)) {
      throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");
    }
    try {
      return solrRequester.getForAdministrativeRead(q);
    } catch (SolrServerException e) {
      throw new EpisodeServiceException(e);
    }
  }

  // FIXME: this should use paging. It is a work in progress...
  protected void populateIndex() throws ServiceRegistryException, MediaPackageException, SolrServerException,
          UnauthorizedException {
    List<Job> jobs = serviceRegistry.getJobs(JOB_TYPE, Status.FINISHED);
    Organization originalOrg = securityService.getOrganization();
    for (Job job : jobs) {
      MediaPackage mediaPackage = MediaPackageParser.getFromXml(job.getPayload());
      String orgId = job.getArguments().get(0);
      try {
        Organization org = orgDirectory.getOrganization(orgId);
        securityService.setOrganization(org);
        AccessControlList acl = authorizationService.getAccessControlList(mediaPackage);
        indexManager.add(mediaPackage, acl);
      } catch (NotFoundException e) {
        logger.warn("{} is not a registered organization", orgId);
      } finally {
        securityService.setOrganization(originalOrg);
      }
    }
  }
}
