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
package org.opencastproject.migration;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.impl.SearchServiceImpl;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

/**
 * This class provides migration index and DB migrations to Matterhorn.
 */
public class SearchMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(SearchMigrationService.class);

  /** The search service */
  private SearchServiceImpl searchService = null;

  /** The search database service */
  private SearchServiceDatabase searchServiceDatabase = null;

  /** The authorization service */
  private AuthorizationService authorizationService = null;

  /** The security service */
  private SecurityService securityService = null;

  /** The workspace */
  private Workspace workspace = null;

  /** Flag describing if download directory migration has been made */
  private boolean validateDownload = false;

  /**
   * Callback for setting the search service.
   * 
   * @param searchService
   *          the search service to set
   */
  public void setSearchService(SearchServiceImpl searchService) {
    this.searchService = searchService;
  }

  /**
   * Callback for setting the search service database.
   * 
   * @param searchServiceDatabase
   *          the search service database to set
   */
  public void setSearchServiceDatabase(SearchServiceDatabase searchServiceDatabase) {
    this.searchServiceDatabase = searchServiceDatabase;
  }

  /**
   * Callback for setting the authorization service.
   * 
   * @param authorizationService
   *          the authorization service to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for setting the security service.
   * 
   * @param securityService
   *          the security service to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the workspace
   * 
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Migrates Matterhorn 1.3 search index to a 1.4 index and DB
   */
  public void activate(ComponentContext cc) throws IOException {
    logger.info("Start migration 1.3 search index to 1.4 index and DB");
    try {
      String systemAccount = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
      String downloadUrl = cc.getBundleContext().getProperty("org.opencastproject.download.url");
      String streamingUrl = cc.getBundleContext().getProperty("org.opencastproject.streaming.url");
      String wfrUrl = UrlSupport.concat(cc.getBundleContext().getProperty("org.opencastproject.server.url"), "files",
              "mediapackage");

      DefaultOrganization defaultOrg = new DefaultOrganization();
      securityService.setOrganization(defaultOrg);
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, defaultOrg));

      SearchResult result = searchService.getForAdministrativeRead(new SearchQuery().withLimit(Integer.MAX_VALUE));

      // to keep track of the number of items migrated
      int totalMigrated = 0;
      int failed = 0;
      long totalRecords = result.getTotalSize();

      logger.info("Found {} total search service items to migrate", totalRecords);

      for (SearchResultItem item : result.getItems()) {
        MediaPackage mediaPackage;
        if (item.getMediaPackage() != null && item.getMediaPackage().getTitle() != null) {
          mediaPackage = item.getMediaPackage();
        } else {
          mediaPackage = parseOldMediaPackage(item.getOcMediapackage());
        }

        boolean mediapackageChanged = false;

        // migration distribution URL's!
        for (MediaPackageElement e : mediaPackage.getElements()) {
          URI newUri = null;
          String uri = e.getURI().toString();

          logger.debug("Looking to migrate '{}'", uri);

          if (uri.indexOf("engage-player") > 0) {
            logger.debug("Mediapackage element {}/{} has already been migrated");
            continue;
          }

          if (StringUtils.isNotBlank(downloadUrl) && uri.startsWith(downloadUrl)) {
            String path = uri.substring(downloadUrl.length());
            newUri = URI.create(UrlSupport.concat(downloadUrl, "engage-player", path));
            mediapackageChanged = true;

            // Validate if download directory has been migrated
            if (!validateDownload) {
              workspace.get(newUri);
              validateDownload = true;
            }
          } else if (StringUtils.isNotBlank(streamingUrl) && uri.startsWith(streamingUrl)) {
            String path = uri.substring(streamingUrl.length());
            newUri = URI.create(UrlSupport.concat(streamingUrl, "engage-player", path));
            mediapackageChanged = true;
          } else if (StringUtils.isNotBlank(downloadUrl) && uri.startsWith(wfrUrl)
                  && uri.endsWith("security-policy/xacml.xml")) {
            String path = uri.substring(wfrUrl.length());
            newUri = URI.create(UrlSupport.concat(downloadUrl, "engage-player", path));
            mediapackageChanged = true;
          } else {
            newUri = e.getURI();
            logger.error("Unable to migrate URI '{}' from mediapackage '{}'! DO IT MANUALLY", uri, mediaPackage);
          }
          e.setURI(newUri);
        }

        String mediaPackageId = mediaPackage.getIdentifier().toString();

        if (!mediapackageChanged) {
          logger.info("Mediapackage {} has already been migrated", mediaPackageId);
          continue;
        }

        // Write mediapackage to DB and update the index
        try {
          AccessControlList acl = authorizationService.getAccessControlList(mediaPackage);
          searchService.getSolrIndexManager().add(mediaPackage, acl, item.getModified());
          searchServiceDatabase.storeMediaPackage(mediaPackage, acl, item.getModified());
          // increment the number of items migrated
          totalMigrated++;
          logger.info("Successfully migrated {} ({}/{})", new Object[] { mediaPackage.getIdentifier(), totalMigrated,
                  totalRecords });
        } catch (SolrServerException e) {
          logger.error("Mediapackage {} could not be migrated: {}", mediaPackageId, e.getMessage());
          failed++;
        } catch (SearchServiceDatabaseException e) {
          logger.error("Mediapackage {} could not be migrated: {}", mediaPackageId, e.getMessage());
          failed++;
        }
      }
      logger.info(
              "Finished migration 1.3 search index to 1.4 index and DB. {} entries migrated. {} items couldn't be migrated. Check logs for errors",
              new Object[] { totalMigrated, failed });
    } catch (SearchException e) {
      logger.error(e.getMessage());
    } catch (UnauthorizedException e) {
      logger.error(e.getMessage());
    } catch (MediaPackageException e) {
      logger.error(e.getMessage());
    } catch (IOException e) {
      logger.error(e.getMessage());
    } catch (NotFoundException e) {
      logger.error("Download directory hasn't been migrated to 'engage-player' channel, aborting migration!");
    } finally {
      securityService.setOrganization(null);
      securityService.setUser(null);
    }
  }

  /**
   * Parse an old 1.3 mediapackage
   * 
   * @param mediapackage
   *          the 1.3 mediapackage
   * @return the 1.4 mediapacakge
   * 
   */
  @SuppressWarnings("unchecked")
  private MediaPackage parseOldMediaPackage(String mediapackage) throws MediaPackageException, IOException {
    ByteArrayOutputStream baos = null;
    ByteArrayInputStream bais = null;
    InputStream manifest = null;
    try {
      manifest = IOUtils.toInputStream(mediapackage, "UTF-8");
      Document domMP = new SAXBuilder().build(manifest);

      Namespace newNS = Namespace.getNamespace("http://mediapackage.opencastproject.org");

      Iterator<Element> it = domMP.getDescendants(new ElementFilter());
      while (it.hasNext()) {
        Element elem = it.next();
        elem.setNamespace(newNS);
      }

      baos = new ByteArrayOutputStream();
      new XMLOutputter().output(domMP, baos);
      bais = new ByteArrayInputStream(baos.toByteArray());
      return MediaPackageParser.getFromXml(IOUtils.toString(bais, "UTF-8"));
    } catch (JDOMException e) {
      throw new MediaPackageException("Error unmarshalling mediapackage", e);
    } finally {
      IOUtils.closeQuietly(bais);
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(manifest);
    }
  }

}
