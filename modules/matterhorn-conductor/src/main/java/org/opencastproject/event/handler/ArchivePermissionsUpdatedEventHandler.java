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

package org.opencastproject.event.handler;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.base.QueryBuilder;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Responds to series events by re-distributing metadata and security policy files to episodes.
 */
public class ArchivePermissionsUpdatedEventHandler {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(ArchivePermissionsUpdatedEventHandler.class);

  /** The archive */
  protected Archive<?> archive = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** The organization directory */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** Dublin core catalog service */
  protected DublinCoreCatalogService dublinCoreService = null;

  /** HttpMediaPackagheElementProvider */
  protected HttpMediaPackageElementProvider httpMediaPackageElementProvider = null;

  /** The workspace */
  protected Workspace workspace = null;

  /** The system account to use for running asynchronous events */
  protected String systemAccount = null;

  /**
   * OSGI callback for component activation.
   *
   * @param bundleContext
   *          the OSGI bundle context
   */
  protected void activate(BundleContext bundleContext) {
    this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user");
  }

  /**
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * @param dublinCoreService
   *          the dublin core service to set
   */
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublinCoreService) {
    this.dublinCoreService = dublinCoreService;
  }

  /**
   * @param archive
   *          the archive to set
   */
  public void setArchive(Archive<?> archive) {
    this.archive = archive;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param authorizationService
   *          the authorizationService to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** OSGi DI callback. */
  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }

  public void handleEvent(final SeriesItem seriesItem) {
    // A series or its ACL has been updated. Find any mediapackages with that series, and update them.
    logger.debug("Handling {}", seriesItem);
    String seriesId = seriesItem.getSeriesId();

    // We must be an administrative user to make this query
    final User prevUser = securityService.getUser();
    final Organization prevOrg = securityService.getOrganization();
    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, prevOrg));

      final Query q = QueryBuilder.query().seriesId(seriesId).onlyLastVersion(true);
      final ResultSet result = archive.findForAdministrativeRead(q, httpMediaPackageElementProvider.getUriRewriter());

      for (ResultItem item : result.getItems()) {
        final String orgId = item.getOrganizationId();
        final Organization organization = organizationDirectoryService.getOrganization(orgId);
        if (organization == null) {
          logger.warn("Skipping update of episode {} since organization {} is unkown", item.getMediaPackageId(), orgId);
          continue;
        }
        securityService.setOrganization(organization);

        MediaPackage mp = item.getMediaPackage();

        // Update the series XACML file
        if (SeriesItem.Type.UpdateAcl.equals(seriesItem.getType())) {
          // Build a new XACML file for this mediapackage
          authorizationService.setAcl(mp, AclScope.Series, seriesItem.getAcl());
        }

        // Update the series dublin core
        if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())) {
          DublinCoreCatalog seriesDublinCore = seriesItem.getSeries();
          mp.setSeriesTitle(seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE));

          // Update the series dublin core
          Catalog[] seriesCatalogs = mp.getCatalogs(MediaPackageElements.SERIES);
          if (seriesCatalogs.length == 1) {
            Catalog c = seriesCatalogs[0];
            String filename = FilenameUtils.getName(c.getURI().toString());
            URI uri = workspace.put(mp.getIdentifier().toString(), c.getIdentifier(), filename,
                    dublinCoreService.serialize(seriesDublinCore));
            c.setURI(uri);
            // setting the URI to a new source so the checksum will most like be invalid
            c.setChecksum(null);
          }
        }

        // Remove the series catalog and isPartOf from episode catalog
        if (SeriesItem.Type.Delete.equals(seriesItem.getType())) {
          mp.setSeries(null);
          mp.setSeriesTitle(null);
          for (Catalog seriesCatalog : mp.getCatalogs(MediaPackageElements.SERIES)) {
            mp.remove(seriesCatalog);
          }
          for (Catalog episodeCatalog : mp.getCatalogs(MediaPackageElements.EPISODE)) {
            DublinCoreCatalog episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace, episodeCatalog);
            episodeDublinCore.remove(DublinCore.PROPERTY_IS_PART_OF);
            String filename = FilenameUtils.getName(episodeCatalog.getURI().toString());
            URI uri = workspace.put(mp.getIdentifier().toString(), episodeCatalog.getIdentifier(), filename,
                    dublinCoreService.serialize(episodeDublinCore));
            episodeCatalog.setURI(uri);
            // setting the URI to a new source so the checksum will most like be invalid
            episodeCatalog.setChecksum(null);
          }
        }

        try {
          // Update the search index with the modified mediapackage
          archive.add(mp);
        } catch (ArchiveException e) {
          logger.error("Error updating mediapackage {}: {}", mp, e.getMessage());
        }
      }
    } catch (NotFoundException e) {
      logger.warn(e.getMessage());
    } catch (IOException e) {
      logger.warn(e.getMessage());
    } finally {
      securityService.setOrganization(prevOrg);
      securityService.setUser(prevUser);
    }
  }
}
