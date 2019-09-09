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

import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
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
public class AssetManagerUpdatedEventHandler {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(AssetManagerUpdatedEventHandler.class);

  /** The archive */
  protected AssetManager assetManager = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The authorization service */
  protected AuthorizationService authorizationService = null;

  /** The organization directory */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** Dublin core catalog service */
  protected DublinCoreCatalogService dublinCoreService = null;

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

  public void handleEvent(final SeriesItem seriesItem) {
    // A series or its ACL has been updated. Find any mediapackages with that series, and update them.
    logger.debug("Handling {}", seriesItem);
    String seriesId = seriesItem.getSeriesId();

    // We must be an administrative user to make this query
    final User prevUser = securityService.getUser();
    final Organization prevOrg = securityService.getOrganization();
    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, prevOrg));

      final AQueryBuilder q = assetManager.createQuery();
      final AResult result = q.select(q.snapshot()).where(q.seriesId().eq(seriesId).and(q.version().isLatest())).run();
      for (Snapshot snapshot : enrich(result).getSnapshots()) {
        final String orgId = snapshot.getOrganizationId();
        final Organization organization = organizationDirectoryService.getOrganization(orgId);
        if (organization == null) {
          logger.warn("Skipping update of episode {} since organization {} is unknown",
                  snapshot.getMediaPackage().getIdentifier().compact(), orgId);
          continue;
        }
        securityService.setOrganization(organization);

        MediaPackage mp = snapshot.getMediaPackage();

        // Update the series XACML file
        if (SeriesItem.Type.UpdateAcl.equals(seriesItem.getType())) {
          // Build a new XACML file for this mediapackage
          try {
            if (seriesItem.getOverrideEpisodeAcl()) {
              authorizationService.removeAcl(mp, AclScope.Episode);
            }
            authorizationService.setAcl(mp, AclScope.Series, seriesItem.getAcl());
          } catch (MediaPackageException e) {
            logger.error("Error setting ACL for media package {}", mp.getIdentifier(), e);
          }
        }

        // Update the series dublin core or extended metadata
        if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())
                || SeriesItem.Type.UpdateElement.equals(seriesItem.getType())) {
          DublinCoreCatalog seriesDublinCore = null;
          MediaPackageElementFlavor catalogType = null;
          if (SeriesItem.Type.UpdateCatalog.equals(seriesItem.getType())) {
            seriesDublinCore = seriesItem.getMetadata();
            mp.setSeriesTitle(seriesDublinCore.getFirst(DublinCore.PROPERTY_TITLE));
            catalogType = MediaPackageElements.SERIES;
          } else {
            seriesDublinCore = seriesItem.getExtendedMetadata();
            catalogType = MediaPackageElementFlavor.flavor(seriesItem.getElementType(), "series");
          }

          // Update the series dublin core
          Catalog[] seriesCatalogs = mp.getCatalogs(catalogType);
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

        // Remove the series catalogs and isPartOf from episode catalog
        if (SeriesItem.Type.Delete.equals(seriesItem.getType())) {
          mp.setSeries(null);
          mp.setSeriesTitle(null);
          for (Catalog seriesCatalog : mp.getCatalogs(MediaPackageElements.SERIES)) {
            mp.remove(seriesCatalog);
          }
          authorizationService.removeAcl(mp, AclScope.Series);
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
          // here we don't know the series extended metadata types,
          // we assume that all series catalog flavors have a fixed subtype: series
          MediaPackageElementFlavor seriesFlavor = MediaPackageElementFlavor.flavor("*", "series");
          for (Catalog catalog : mp.getCatalogs()) {
            if (catalog.getFlavor().matches(seriesFlavor))
              mp.remove(catalog);
          }
        }

        try {
          // Update the asset manager with the modified mediapackage
          assetManager.takeSnapshot(snapshot.getOwner(), mp);
        } catch (AssetManagerException e) {
          logger.error("Error updating mediapackage {}", mp.getIdentifier().compact(), e);
        }
      }
    } catch (IOException | NotFoundException e) {
      logger.warn("Unable to handle update event for series {} for user {}: {}",
                  seriesItem, prevUser.getUsername(), e.getMessage());
    } finally {
      securityService.setOrganization(prevOrg);
      securityService.setUser(prevUser);
    }
  }
}
