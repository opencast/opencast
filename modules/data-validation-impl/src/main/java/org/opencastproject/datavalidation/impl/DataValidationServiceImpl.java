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

package org.opencastproject.datavalidation.impl;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.datavalidation.api.DataValidationService;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManagerFactory;

/**
 * TODO: This class implements...
 */

@Component(
    property = {
        "service.description=Data Validation Service"
    },
    immediate = true,
    service = DataValidationService.class
)
public class DataValidationServiceImpl implements DataValidationService {
  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(DataValidationServiceImpl.class);

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.datavalidation";

  /** The asset manager */
  private AssetManager assetManager;

  /** The authorization service */
  private AuthorizationService authorizationService;

  // /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  private ElasticsearchIndex elasticsearchIndex;

  protected DBSession db;

  /** Opencast's security service */
  protected SecurityService securityService;

  /** Provides access to search information */
  private SearchService searchService;

  /** OSGi DI */
  // @Reference(target = "(osgi.unit.name=org.opencastproject.datavalidation)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /** Configuration key for the default Opencast storage directory. A value is optional. */
  public static final String CFG_OPT_STORAGE_DIR = "org.opencastproject.storage.dir";

  /**
 * The default store directory name.
 * Will be used in conjunction with {@link #CFG_OPT_STORAGE_DIR} if {@link #CFG_OPT_STORAGE_DIR} is not set.
 */
  private static final String DEFAULT_STORE_DIRECTORY = "archive";

  /** The OSGI bundle context */
  protected BundleContext bundleContext;

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Reference
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * OSGi callback to set the authorization service.
   *
   * @param authorizationService
   */
  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Activate
  @Modified
  public void activate(BundleContext bundleContext) throws Exception {
    logger.info("Activating Data Validation Service");
    // this.db = dbSessionFactory.createSession(this.emf);
    this.bundleContext = bundleContext;
  }

  @Deactivate
  public void deactivate() {
    this.db.close();
  }

  /*
   * ENDPOINT METHODS
   */
  // Endpoint method to check all assets for corrupted data
  @Override
  public String checkAssetsForCorruptedData(int offset, int limit) {
    logger.info("Checking all assets for corrupted data");
    // Level of assets
    ArrayList<String> corruptedAssets = new ArrayList<>();
    List<Optional<MediaPackage>> mediaPackages = this.assetManager.getAllMediaPackages(offset, limit);
    for (Optional<MediaPackage> mp : mediaPackages) {
      boolean hasVideoFile = false;
      MediaPackageElement[] elements = mp.get().getElements();
      RichAResult snapshots = this.assetManager.getSnapshotsByIdOrderedByVersion(
            mp.get().getIdentifier().toString(), false);
      Version v = snapshots.getVersions().get(0);
      for (MediaPackageElement element : elements) {
        Optional<Asset> asset = this.assetManager.getAsset(v, mp.get().getIdentifier().toString(),
              element.getIdentifier());
        String mimeType = asset.isPresent() ? asset.get().getMimeType().get().getType() : "";
        if ("video".equals(mimeType)) {
          hasVideoFile = true;
          break;
        }
      }
      if (!hasVideoFile) {
        corruptedAssets.add(mp.get().getIdentifier().toString());
      }
    }

    if (corruptedAssets.isEmpty()) {
      return "{\"Success\": \"No corrupted assets found.\"}";
    } else {
      JSONObject json = new JSONObject();
      json.put("corruptedAssets", corruptedAssets);
      StringBuilder sb = new StringBuilder();
      sb.append("Corrupted assets found:\n");
      for (String uid : corruptedAssets) {
        sb.append("\t").append(uid).append("\n");
      }
      logger.info("Corrupted assets: {}", sb.toString());
      return json.toJSONString();
    }
  }

  // Endpoint method to output a detailed report on a single asset
  public String checkAclMatching(int offset, int limit) {
    List<Optional<MediaPackage>> mediaPackages = this.assetManager.getAllMediaPackages(offset, limit);
    ArrayList<String> aclMismatchedMediaPackages = new ArrayList<>();
    if (mediaPackages != null && !mediaPackages.isEmpty()) {
      for (Optional<MediaPackage> archivedMp : mediaPackages) {
        // Check if media package can be found in publications
        try {
          MediaPackage publishedMp = this.searchService.get(archivedMp.get().getIdentifier().toString());
          if (publishedMp != null) {
            AccessControlList archivedAcl = this.authorizationService.getActiveAcl(archivedMp.get()).getA();
            AccessControlList publishedAcl = this.authorizationService.getActiveAcl(publishedMp).getA();
            if (archivedAcl == null || publishedAcl == null) {
              logger.warn("Published or archived ACL could not be found for media package with UID: {}",
                    archivedMp.get().getIdentifier());
            } else {
              if (!archivedAcl.equals(publishedAcl)) {
                logger.info("Published and archived ACLs differ for UID: {}", archivedMp.get().getIdentifier());
                aclMismatchedMediaPackages.add(archivedMp.get().getIdentifier().toString());
              }
            }
          }
        } catch (NotFoundException e) {
          logger.warn("The search service couldn't find a media package with ID: {}", archivedMp.get().getIdentifier());
        } catch (UnauthorizedException e) {
          logger.warn("Not allowed to access media package with ID: {}", archivedMp.get().getIdentifier());
        }
      }
    }

    if (aclMismatchedMediaPackages.isEmpty()) {
      return "{\"Success\": \"No ACL mismatches found.\"}";
    } else {
      JSONObject json = new JSONObject();
      json.put("aclMismatches", aclMismatchedMediaPackages);
      return json.toJSONString();
    }
  }
}
