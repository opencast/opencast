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

package org.opencastproject.search.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

/**
 * A Solr-based {@link SearchService} implementation.
 */
public final class SearchServiceImpl extends AbstractJobProducer implements SearchService, ManagedService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.search.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.search.solr.dir";

  /** The job type */
  public static final String JOB_TYPE = "org.opencastproject.search";

  /** The load introduced on the system by creating an add job */
  public static final float DEFAULT_ADD_JOB_LOAD = 0.1f;

  /** The load introduced on the system by creating a delete job */
  public static final float DEFAULT_DELETE_JOB_LOAD = 0.1f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_ADD_JOB_LOAD} */
  public static final String ADD_JOB_LOAD_KEY = "job.load.add";

  /** The key to look for in the service configuration file to override the {@link DEFAULT_DELETE_JOB_LOAD} */
  public static final String DELETE_JOB_LOAD_KEY = "job.load.delete";

  /** The load introduced on the system by creating an add job */
  private float addJobLoad = DEFAULT_ADD_JOB_LOAD;

  /** The load introduced on the system by creating a delete job */
  private float deleteJobLoad = DEFAULT_DELETE_JOB_LOAD;

  /** counter how often the index has already been tried to populate */
  private int retriesToPopulateIndex = 0;

  /** List of available operations on jobs */
  private enum Operation {
    Add, Delete
  };

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

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  /** Persistent storage */
  private SearchServiceDatabase persistence;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectory = null;

  /** The optional Mediapackage serializer */
  protected MediaPackageSerializer serializer = null;

  /**
   * Creates a new instance of the search service.
   */
  public SearchServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Return the solr index manager
   *
   * @return indexManager
   */
  public SolrIndexManager getSolrIndexManager() {
    return indexManager;
  }

  /**
   * Service activator, called via declarative services configuration. If the solr server url is configured, we try to
   * connect to it. If not, the solr data directory with an embedded Solr server is used.
   *
   * @param cc
   *          the component context
   */
  @Override
  public void activate(final ComponentContext cc) throws IllegalStateException {
    super.activate(cc);
    final String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));

    logger.info("Setting up solr server");

    solrServer = new Object() {
      SolrServer create() {
        if (solrServerUrlConfig != null) {
          /* Use external SOLR server */
          try {
            logger.info("Setting up solr server at {}", solrServerUrlConfig);
            URL solrServerUrl = new URL(solrServerUrlConfig);
            return setupSolr(solrServerUrl);
          } catch (MalformedURLException e) {
            throw connectError(solrServerUrlConfig, e);
          }
        } else {
          /* Set-up embedded SOLR */
          String solrRoot = SolrServerFactory.getEmbeddedDir(cc, CONFIG_SOLR_ROOT, "search");

          try {
            logger.debug("Setting up solr server at {}", solrRoot);
            return setupSolr(new File(solrRoot));
          } catch (IOException e) {
            throw connectError(solrServerUrlConfig, e);
          } catch (SolrServerException e) {
            throw connectError(solrServerUrlConfig, e);
          }
        }
      }

      IllegalStateException connectError(String target, Exception e) {
        logger.error("Unable to connect to solr at {}: {}", target, e.getMessage());
        return new IllegalStateException("Unable to connect to solr at " + target, e);
      }
      // CHECKSTYLE:OFF
    }.create();
    // CHECKSTYLE:ON

    solrRequester = new SolrRequester(solrServer, securityService, serializer);
    indexManager = new SolrIndexManager(solrServer, workspace, mdServices, seriesService, mpeg7CatalogService,
            securityService);

    String systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    populateIndex(systemUserName);
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
    if (solrIndexDir.isDirectory() && solrIndexDir.list().length == 0) {
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
  static SolrServer setupSolr(URL url) {
    logger.info("Connecting to solr search index at {}", url);
    return SolrServerFactory.newRemoteInstance(url);
  }

  // TODO: generalize this method
  static void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = SearchServiceImpl.class.getResourceAsStream(classpath);
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
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  public SearchResult getByQuery(String query, int limit, int offset) throws SearchException {
    try {
      logger.debug("Searching index using custom query '" + query + "'");
      return solrRequester.getByQuery(query, limit, offset);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.mediapackage.MediaPackage)
   */
  public Job add(MediaPackage mediaPackage) throws SearchException, MediaPackageException, IllegalArgumentException,
          UnauthorizedException, ServiceRegistryException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Add.toString(),
              Arrays.asList(MediaPackageParser.getAsXml(mediaPackage)), addJobLoad);
    } catch (ServiceRegistryException e) {
      throw new SearchException(e);
    }
  }

  /**
   * Immediately adds the mediapackage to the search index.
   *
   * @param mediaPackage
   *          the media package
   * @throws SearchException
   *           if the media package cannot be added to the search index
   * @throws MediaPackageException
   *           if the mediapckage is invalid
   * @throws IllegalArgumentException
   *           if the mediapackage is <code>null</code>
   * @throws UnauthorizedException
   *           if the user does not have the rights to add the mediapackage
   */
  public void addSynchronously(MediaPackage mediaPackage) throws SearchException, MediaPackageException,
          IllegalArgumentException, UnauthorizedException {
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Unable to add a null mediapackage");
    }
    logger.debug("Attempting to add mediapackage {} to search index", mediaPackage.getIdentifier());
    AccessControlList acl = authorizationService.getActiveAcl(mediaPackage).getA();

    Date now = new Date();

    try {
      if (indexManager.add(mediaPackage, acl, now)) {
        logger.info("Added mediapackage `{}` to the search index, using ACL `{}`", mediaPackage, acl);
      } else {
        logger.warn("Failed to add mediapackage {} to the search index", mediaPackage.getIdentifier());
      }
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }

    try {
      persistence.storeMediaPackage(mediaPackage, acl, now);
    } catch (SearchServiceDatabaseException e) {
      logger.error("Could not store media package to search database {}: {}", mediaPackage.getIdentifier(), e);
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  public Job delete(String mediaPackageId) throws SearchException, UnauthorizedException, NotFoundException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Delete.toString(), Arrays.asList(mediaPackageId), deleteJobLoad);
    } catch (ServiceRegistryException e) {
      throw new SearchException(e);
    }
  }

  /**
   * Immediately removes the given mediapackage from the search service.
   *
   * @param mediaPackageId
   *          the mediapackage
   * @return <code>true</code> if the mediapackage was deleted
   * @throws SearchException
   *           if deletion failed
   * @throws UnauthorizedException
   *           if the user did not have access to the media package
   * @throws NotFoundException
   *           if the mediapackage did not exist
   */
  public boolean deleteSynchronously(String mediaPackageId) throws SearchException, UnauthorizedException,
          NotFoundException {
    SearchResult result;
    try {
      result = solrRequester.getForWrite(new SearchQuery().withId(mediaPackageId));
      if (result.getItems().length == 0) {
        logger.warn(
                "Can not delete mediapackage {}, which is not available for the current user to delete from the search index.",
                mediaPackageId);
        return false;
      }
      logger.info("Removing mediapackage {} from search index", mediaPackageId);

      Date now = new Date();
      try {
        persistence.deleteMediaPackage(mediaPackageId, now);
        logger.info("Removed mediapackage {} from search persistence", mediaPackageId);
      } catch (NotFoundException e) {
        // even if mp not found in persistence, it might still exist in search index.
        logger.info("Could not find mediapackage with id {} in persistence, but will try remove it from index, anyway.",
                mediaPackageId);
      } catch (SearchServiceDatabaseException e) {
        logger.error("Could not delete media package with id {} from persistence storage", mediaPackageId);
        throw new SearchException(e);
      }

      return indexManager.delete(mediaPackageId, now);
    } catch (SolrServerException e) {
      logger.info("Could not delete media package with id {} from search index", mediaPackageId);
      throw new SearchException(e);
    }
  }

  /**
   * Clears the complete solr index.
   *
   * @throws SearchException
   *           if clearing the index fails
   */
  public void clear() throws SearchException {
    try {
      logger.info("Clearing the search index");
      indexManager.clear();
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    try {
      logger.debug("Searching index using query object '" + q + "'");
      return solrRequester.getForRead(q);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.SearchService#getForAdministrativeRead(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getForAdministrativeRead(SearchQuery q) throws SearchException, UnauthorizedException {
    User user = securityService.getUser();
    if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole()))
      throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");

    try {
      return solrRequester.getForAdministrativeRead(q);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  protected void populateIndex(String systemUserName) {
    long instancesInSolr = 0L;

    try {
      instancesInSolr = indexManager.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    if (instancesInSolr > 0) {
      logger.debug("Search index found");
      return;
    }

    if (instancesInSolr == 0L) {
      logger.info("No search index found");
      Iterator<Tuple<MediaPackage, String>> mediaPackages;
      int total = 0;
      try {
        total = persistence.countMediaPackages();
        logger.info("Starting population of search index from {} items in database", total);
        mediaPackages = persistence.getAllMediaPackages();
      } catch (SearchServiceDatabaseException e) {
        logger.error("Unable to load the search entries: {}", e.getMessage());
        throw new ServiceException(e.getMessage());
      }
      int errors = 0;
      int current = 0;
      while (mediaPackages.hasNext()) {
        current++;
        try {
          Tuple<MediaPackage, String> mediaPackage = mediaPackages.next();

          String mediaPackageId = mediaPackage.getA().getIdentifier().toString();

          Organization organization = organizationDirectory.getOrganization(mediaPackage.getB());
          securityService.setOrganization(organization);
          securityService.setUser(SecurityUtil.createSystemUser(systemUserName, organization));

          AccessControlList acl = persistence.getAccessControlList(mediaPackageId);
          Date modificationDate = persistence.getModificationDate(mediaPackageId);
          Date deletionDate = persistence.getDeletionDate(mediaPackageId);

          indexManager.add(mediaPackage.getA(), acl, deletionDate, modificationDate);
        } catch (Exception e) {
          logger.error("Unable to index search instances:", e);
          if (retryToPopulateIndex(systemUserName)) {
            logger.warn("Trying to re-index search index later. Aborting for now.");
            return;
          }
          errors++;
        } finally {
          securityService.setOrganization(null);
          securityService.setUser(null);
        }

        // log progress
        if (current % 100 == 0) {
          logger.info("Indexing search {}/{} ({} percent done)", current, total, current * 100 / total);
        }
      }
      if (errors > 0)
        logger.error("Skipped {} erroneous search entries while populating the search index", errors);
      logger.info("Finished populating search index");
    }
  }

  private boolean retryToPopulateIndex(final String systemUserName) {
    if (retriesToPopulateIndex > 0) {
      return false;
    }

    long instancesInSolr = 0L;

    try {
      instancesInSolr = indexManager.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    if (instancesInSolr > 0) {
      logger.debug("Search index found, other files could be indexed. No retry needed.");
      return false;
    }

    retriesToPopulateIndex++;

      new Thread() {
        public void run() {
          try {
            Thread.sleep(30000);
          } catch (InterruptedException ex) {
          }
          populateIndex(systemUserName);
        }
      }.start();
    return true;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      switch (op) {
        case Add:
          MediaPackage mediaPackage = MediaPackageParser.getFromXml(arguments.get(0));
          addSynchronously(mediaPackage);
          return null;
        case Delete:
          String mediapackageId = arguments.get(0);
          boolean deleted = deleteSynchronously(mediapackageId);
          return Boolean.toString(deleted);
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /** For testing purposes only! */
  void testSetup(SolrServer server, SolrRequester requester, SolrIndexManager manager) {
    this.solrServer = server;
    this.solrRequester = requester;
    this.indexManager = manager;
  }

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

  public void setPersistence(SearchServiceDatabase persistence) {
    this.persistence = persistence;
  }

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
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
    this.organizationDirectory = organizationDirectory;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectory;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * Sets the optional MediaPackage serializer.
   *
   * @param serializer
   *          the serializer
   */
  protected void setMediaPackageSerializer(MediaPackageSerializer serializer) {
    this.serializer = serializer;
    if (solrRequester != null)
      solrRequester.setMediaPackageSerializer(serializer);
  }

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    addJobLoad = LoadUtil.getConfiguredLoadValue(properties, ADD_JOB_LOAD_KEY, DEFAULT_ADD_JOB_LOAD, serviceRegistry);
    deleteJobLoad = LoadUtil.getConfiguredLoadValue(properties, DELETE_JOB_LOAD_KEY, DEFAULT_DELETE_JOB_LOAD, serviceRegistry);
  }
}
