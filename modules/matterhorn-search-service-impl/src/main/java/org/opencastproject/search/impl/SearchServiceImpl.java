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

package org.opencastproject.search.impl;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
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
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.osgi.framework.ServiceException;
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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * A Solr-based {@link SearchService} implementation.
 */
public final class SearchServiceImpl implements SearchService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.search.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.search.solr.dir";

  /** The job type */
  public static final String JOB_TYPE = "org.opencastproject.search";

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

  /** Persistent storage */
  private SearchServiceDatabase persistence;

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
          String solrRoot = PathSupport.concat(storageDir, "searchindex");
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

    populateIndex();
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
  public void add(MediaPackage mediaPackage) throws SearchException, MediaPackageException, IllegalArgumentException,
          UnauthorizedException, ServiceRegistryException {
    User currentUser = securityService.getUser();
    String orgAdminRole = securityService.getOrganization().getAdminRole();
    if (!currentUser.hasRole(orgAdminRole) && !currentUser.hasRole(GLOBAL_ADMIN_ROLE)
            && !authorizationService.hasPermission(mediaPackage, WRITE_PERMISSION)) {
      throw new UnauthorizedException(currentUser, SearchService.WRITE_PERMISSION);
    }
    if (mediaPackage == null) {
      throw new IllegalArgumentException("Unable to add a null mediapackage");
    }
    logger.debug("Attempting to add mediapackage {} to search index", mediaPackage.getIdentifier());
    AccessControlList acl = authorizationService.getAccessControlList(mediaPackage);

    Date now = new Date();

    try {
      persistence.storeMediaPackage(mediaPackage, acl, now);
    } catch (SearchServiceDatabaseException e) {
      logger.error("Could not store media package to search database {}: {}", mediaPackage.getIdentifier(), e);
      throw new SearchException(e);
    }

    try {
      List<String> args = new ArrayList<String>();
      args.add(securityService.getOrganization().getId());
      Job job = serviceRegistry.createJob(JOB_TYPE, OPERATION_ADD, args, MediaPackageParser.getAsXml(mediaPackage),
              false);
      job.setStatus(Status.FINISHED);
      try {
        serviceRegistry.updateJob(job);
      } catch (NotFoundException e) {
        throw new IllegalStateException(e); // should not be possible
      }
      if (indexManager.add(mediaPackage, acl, now)) {
        logger.info("Added mediapackage {} to the search index", mediaPackage.getIdentifier());
      } else {
        logger.warn("Failed to add mediapackage {} to the search index", mediaPackage.getIdentifier());
      }
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  public boolean delete(String mediaPackageId) throws SearchException, UnauthorizedException, NotFoundException {
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
      } catch (SearchServiceDatabaseException e) {
        logger.error("Could not delete media package with id {} from persistence storage", mediaPackageId);
        throw new SearchException(e);
      }

      return indexManager.delete(mediaPackageId, now);
    } catch (SolrServerException e) {
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
    if (!user.hasRole(GLOBAL_ADMIN_ROLE)) {
      throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");
    }
    try {
      return solrRequester.getForAdministrativeRead(q);
    } catch (SolrServerException e) {
      throw new SearchException(e);
    }
  }

  protected void populateIndex() {
    long instancesInSolr = 0L;
    try {
      instancesInSolr = indexManager.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    if (instancesInSolr == 0L) {
      try {
        Iterator<Tuple<MediaPackage, String>> mediaPackages = persistence.getAllMediaPackages();
        while (mediaPackages.hasNext()) {
          Tuple<MediaPackage, String> mediaPackage = mediaPackages.next();

          String mediaPackageId = mediaPackage.getA().getIdentifier().toString();

          Organization organization = orgDirectory.getOrganization(mediaPackage.getB());
          securityService.setOrganization(organization);
          securityService.setUser(new User(organization.getName(), organization.getId(), new String[] { organization
                  .getAdminRole() }));

          AccessControlList acl = persistence.getAccessControlList(mediaPackageId);
          Date modificationDate = persistence.getModificationDate(mediaPackageId);
          Date deletionDate = persistence.getDeletionDate(mediaPackageId);

          indexManager.add(mediaPackage.getA(), acl, deletionDate, modificationDate);
        }
        logger.info("Finished populating search index");
      } catch (Exception e) {
        logger.warn("Unable to index search instances: {}", e);
        throw new ServiceException(e.getMessage());
      } finally {
        securityService.setOrganization(null);
        securityService.setUser(null);
      }
    }
  }
}
