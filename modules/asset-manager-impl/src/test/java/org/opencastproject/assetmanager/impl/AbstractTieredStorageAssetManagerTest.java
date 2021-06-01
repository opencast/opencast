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
package org.opencastproject.assetmanager.impl;

import static com.entwinemedia.fn.fns.Booleans.eq;

import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.RemoteAssetStore;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistencefn.PersistenceEnvs;
import org.opencastproject.util.persistencefn.PersistenceUtil;
import org.opencastproject.util.persistencefn.Queries;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;

public class AbstractTieredStorageAssetManagerTest
    extends AssetManagerTestBase {
  public static final String LOCAL_STORE_ID = "local-test";
  public static final String REMOTE_STORE_1_ID = "remote-1-test";
  public static final String REMOTE_STORE_2_ID = "remote-2-test";
  private static final String ORG_ID = "fake_org_id";

  private AssetStore localAssetStore;
  //We're using local asset stores here to make things easier, RemoteAssetStore just adds a cache directory config key
  protected RemoteAssetStore remoteAssetStore1;
  protected RemoteAssetStore remoteAssetStore2;

  public AssetManagerImpl mkTieredStorageAM() throws Exception {
    penv = PersistenceEnvs.mkTestEnvFromSystemProperties(PERSISTENCE_UNIT);
    // empty database
    penv.tx(new Fn<EntityManager, Object>() {
      @Override public Object apply(EntityManager entityManager) {
        Queries.sql.update(entityManager, "delete from oc_assets_asset");
        Queries.sql.update(entityManager, "delete from oc_assets_properties");
        Queries.sql.update(entityManager, "delete from oc_assets_snapshot");
        Queries.sql.update(entityManager, "delete from oc_assets_version_claim");
        return null;
      }
    });

    final Database db = new Database(
            PersistenceUtil.mkTestEntityManagerFactoryFromSystemProperties(PERSISTENCE_UNIT));
    //
    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class)))
            .andReturn(IoSupport.classPathResourceAsFile("/dublincore-a.xml").get()).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class), EasyMock.anyBoolean())).andAnswer(() -> {
      File tmp = tempFolder.newFile();
      FileUtils.copyFile(new File(getClass().getResource("/dublincore-a.xml").toURI()), tmp);
      return tmp;
    }).anyTimes();
    EasyMock.replay(workspace);
    //
    localAssetStore = mkAssetStore(LOCAL_STORE_ID);
    remoteAssetStore1 = mkRemoteAssetStore(REMOTE_STORE_1_ID);
    remoteAssetStore2 = mkRemoteAssetStore(REMOTE_STORE_2_ID);

    HttpAssetProvider httpAssetProvider =  new HttpAssetProvider() {
      @Override public Snapshot prepareForDelivery(Snapshot snapshot) {
        return snapshot;
      }
    };

    Organization org = EasyMock.niceMock(Organization.class);
    EasyMock.expect(org.getId()).andReturn(getCurrentOrgId()).anyTimes();

    SecurityService securityService = EasyMock.niceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.replay(org, securityService);

    AssetManagerImpl am = new AssetManagerImpl();
    am.setDatabase(db);
    am.setHttpAssetProvider(httpAssetProvider);
    am.setWorkspace(workspace);
    am.setSecurityService(securityService);
    am.setAssetStore(localAssetStore);
    return am;
  }

  @Override public AssetManagerImpl getAssetManager() {
    return am;
  }

  public String getCurrentOrgId() {
    return ORG_ID;
  }

  /**
   * Create a test asset store.
   */
  protected RemoteAssetStore mkRemoteAssetStore(String storeType) {
    return new RemoteAssetStore() {
      private Set<StoragePath> store = new HashSet<>();

      private void logSize() {
        logger.debug("Store contains {} asset/s", store.size());
      }

      @Override public void put(StoragePath path, Source source) throws AssetStoreException {
        store.add(path);
        logSize();
      }

      @Override public boolean copy(StoragePath from, StoragePath to) throws AssetStoreException {
        if (store.contains(from)) {
          store.add(to);
          logSize();
          return true;
        } else {
          return false;
        }
      }

      @Override public Opt<InputStream> get(StoragePath path) throws AssetStoreException {
        return IoSupport.openClassPathResource("/dublincore-a.xml").toOpt();
      }

      @Override public boolean contains(StoragePath path) throws AssetStoreException {
        return store.contains(path);
      }

      @Override public boolean delete(DeletionSelector sel) throws AssetStoreException {
        logger.info("Delete from asset store " + sel);
        final Set<StoragePath> newStore = new HashSet<>();
        boolean deleted = false;
        for (StoragePath s : store) {
          if (!(sel.getOrganizationId().equals(s.getOrganizationId())
                  && sel.getMediaPackageId().equals(s.getMediaPackageId())
                  && sel.getVersion().map(eq(s.getVersion())).getOr(true))) {
            newStore.add(s);
          } else {
            deleted = true;
          }
        }
        store = newStore;
        logSize();
        return deleted;
      }

      @Override public Option<Long> getTotalSpace() {
        return Option.none();
      }

      @Override public Option<Long> getUsableSpace() {
        return Option.none();
      }

      @Override public Option<Long> getUsedSpace() {
        return Option.some((long) store.size());
      }

      @Override public String getStoreType() {
        return storeType;
      }
    };
  }

  /**
   * Create a test asset store.
   */
  protected RemoteAssetStore mkFaultyRemoteAssetStore(String storeType, Float faultyness) {
    return new RemoteAssetStore() {
      private Set<StoragePath> store = new HashSet<>();
      private Random r = new Random(System.nanoTime());

      private void logSize() {
        logger.debug("Store contains {} asset/s", store.size());
      }

      private void logFault() throws AssetStoreException {
        logger.debug("Store Fault!");
        throw new AssetStoreException();
      }

      private boolean shouldFault() {
        return r.nextFloat() > faultyness;
      }

      @Override public void put(StoragePath path, Source source) throws AssetStoreException {
        if (shouldFault()) {
          store.add(path);
          logSize();
        } else {
          logFault();
        }
      }

      @Override public boolean copy(StoragePath from, StoragePath to) throws AssetStoreException {
        if (shouldFault()) {
          if (store.contains(from)) {
            store.add(to);
            logSize();
            return true;
          } else {
            return false;
          }
        } else {
          logFault();
          return false;
        }
      }

      @Override public Opt<InputStream> get(StoragePath path) throws AssetStoreException {
        if (shouldFault()) {
          return IoSupport.openClassPathResource("/dublincore-a.xml").toOpt();
        } else {
          logFault();
          return null;
        }
      }

      @Override public boolean contains(StoragePath path) throws AssetStoreException {
        if (shouldFault()) {
          return store.contains(path);
        } else {
          logFault();
          return false;
        }
      }

      @Override public boolean delete(DeletionSelector sel) throws AssetStoreException {
        if (shouldFault()) {
          logger.info("Delete from asset store " + sel);
          final Set<StoragePath> newStore = new HashSet<>();
          boolean deleted = false;
          for (StoragePath s : store) {
            if (!(sel.getOrganizationId().equals(s.getOrganizationId())
                    && sel.getMediaPackageId().equals(s.getMediaPackageId())
                    && sel.getVersion().map(eq(s.getVersion())).getOr(true))) {
              newStore.add(s);
            } else {
              deleted = true;
            }
          }
          store = newStore;
          logSize();
          return deleted;
        } else {
          logFault();
          return false;
        }
      }

      @Override public Option<Long> getTotalSpace() {
        return Option.none();
      }

      @Override public Option<Long> getUsableSpace() {
        return Option.none();
      }

      @Override public Option<Long> getUsedSpace() {
        return Option.some((long) store.size());
      }

      @Override public String getStoreType() {
        return storeType;
      }
    };
  }
}
