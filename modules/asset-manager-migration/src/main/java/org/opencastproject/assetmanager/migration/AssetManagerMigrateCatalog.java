/*
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
package org.opencastproject.assetmanager.migration;


import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.impl.HttpAssetProvider;
import org.opencastproject.assetmanager.impl.RuntimeTypes;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

@Component(
    property = {
      "service.description=Opencast Asset Manager Catalog Migrator"
    },
    immediate = true,
    service = { AssetManagerMigrateCatalog.class }
)
public class AssetManagerMigrateCatalog {
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerMigrateCatalog.class);

  private static final int PAGE_SIZE = 1000;

  /* OSGI Service References */
  private HttpAssetProvider httpAssetProvider;
  private DBSessionFactory dbSessionFactory;
  private EntityManagerFactory emf;
  private AssetManager assetManager;
  private SecurityService securityService;
  private OrganizationDirectoryService orgDir;
  private Workspace workspace;

  private String systemUserName;
  private Database db;

  @Activate
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activating AssetManager Migration");

    db = new Database(dbSessionFactory.createSession(emf));
    db.setHttpAssetProvider(httpAssetProvider);
    systemUserName = SecurityUtil.getSystemUserName(cc);

    populateSnapshotCatalogs();
  }

  private void populateSnapshotCatalogs() throws AssetManagerException {
    final Organization originalOrg = securityService.getOrganization();
    final User originalUser = (originalOrg != null ? securityService.getUser() : null);

    try {
      final Organization defaultOrg = new DefaultOrganization();
      final User defaultSystemUser = SecurityUtil.createSystemUser(systemUserName, defaultOrg);
      securityService.setOrganization(defaultOrg);
      securityService.setUser(defaultSystemUser);
      int offset = 0;
      int total = (int) assetManager.countEvents(null);
      int errors = 0;
      int rewritten = 0;
      int unchanged = 0;
      int current = 0;

      // Not an index rebuild, just using logging mechanism
      logger.info("Starting Asset snapshot catalog population for {} events",
          total);
      do {
        List<Snapshot> snapshots = db.getSnapshotsForIndexRebuild(offset, PAGE_SIZE);
        offset += PAGE_SIZE;
        int n = 20;

        final Map<String, List<Snapshot>> byOrg = snapshots.stream()
            .collect(Collectors.groupingBy(Snapshot::getOrganizationId));
        for (String orgId : byOrg.keySet()) {
          final Organization snapshotOrg;
          try {
            snapshotOrg = orgDir.getOrganization(orgId);
            User snapshotSystemUser = SecurityUtil.createSystemUser(systemUserName, snapshotOrg);
            securityService.setOrganization(snapshotOrg);
            securityService.setUser(snapshotSystemUser);
            for (Snapshot snapshot : byOrg.get(orgId)) {
              try {
                current++;

                // Check that the snapshot doesn't already have a valid catalog
                if (snapshot.getEpisodeCatalog().isEmpty()) {
                  // mediapackage URIs need to be rewritten to concrete URLs
                  Snapshot snapshotWithUris = httpAssetProvider.prepareForDelivery(snapshot);
                  MediaPackage mediapackage = snapshotWithUris.getMediaPackage();
                  Optional<DublinCoreCatalog> episodeCatalog =
                      DublinCoreUtil.loadEpisodeDublinCore(workspace, mediapackage);

                  if (episodeCatalog.isPresent()) {
                    db.setDublinCoreXml(RuntimeTypes.convert(snapshot.getVersion()),
                        mediapackage.getIdentifier().toString(), episodeCatalog.get().toXmlString());
                    rewritten++;
                  } else {
                    logger.warn("Snapshot {} for mediapackage {} has no episode catalog", snapshot.getVersion(),
                        mediapackage.getIdentifier().toString());
                    errors++;
                  }
                } else {
                  unchanged++;
                }

                logPopulateProgress(total, current);
              } catch (Throwable t) {
                logger.error("Unable to populate snapshot {} with episode catalogs, skipping.",
                    snapshot.getMediaPackage().getIdentifier().toString());
              }
            }
          } catch (Throwable t) {
            logger.error("Error updating populating snapshot catalogs for organization {}.", originalOrg, t);
            throw new AssetManagerException("Could update missing catalogs", t);
          } finally {
            securityService.setOrganization(defaultOrg);
            securityService.setUser(defaultSystemUser);
          }
        }
      } while (offset < total);

      logger.info("FINISHED Population of snapshots with catalogs, {} episodes, {} unchanged, {} updated,"
          + " {} failed.", total, unchanged, rewritten, errors);
      if (errors != 0) {
        throw new AssetManagerException("Population of snapshots with episode catalogs finished with " + errors
            + " errors");
      }
    } finally {
      securityService.setOrganization(originalOrg);
      securityService.setUser(originalUser);
    }
  }

  private void logPopulateProgress(int total, int current) {
    final int responseInterval = (total < 100) ? 1 : (total / 100);
    if (responseInterval == 1 || PAGE_SIZE > responseInterval || current == total
            || current % responseInterval < PAGE_SIZE) {
      int progress = total > 0 ? (current * 100 / total) : 100;
      logger.info("Snapshot catalog population: {}/{} finished, {}% complete.",
          current, total, progress);
    }
  }

  /**
   * OSGi dependencies
   */

  @Reference(target = "(osgi.unit.name=org.opencastproject.assetmanager.impl)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Reference
  public void setHttpAssetProvider(HttpAssetProvider httpAssetProvider) {
    this.httpAssetProvider = httpAssetProvider;
  }

  @Reference
  void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  @Reference
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setOrgDir(OrganizationDirectoryService orgDir) {
    this.orgDir = orgDir;
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
