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
package org.opencastproject.assetmanager.impl;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.RemoteAssetStore;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.util.TestUser;
import org.opencastproject.db.DBTestEnv;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.message.broker.api.update.AssetManagerUpdateHandler;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for {@link org.opencastproject.assetmanager.api.AssetManager} tests.
 */
public abstract class AssetManagerTestBase {
  protected static final Logger logger = LoggerFactory.getLogger(AssetManagerTestBase.class);
  public static final String PERSISTENCE_UNIT = "org.opencastproject.assetmanager.impl";

  protected static final String OWNER = "test";

  public static final String LOCAL_STORE_ID = "local-test";
  public static final String REMOTE_STORE_1_ID = "remote-1-test";
  public static final String REMOTE_STORE_2_ID = "remote-2-test";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  /** The asset manager under test. */
  protected AssetManagerImpl am;
  protected Props p;
  protected Props p2;

  @Before
  public void setUp() throws Exception {
    this.am = makeAssetManager();
    p = new Props("org.opencastproject.service");
    p2 = new Props("org.opencastproject.service.sub");
  }

  protected AssetManagerImpl makeAssetManager() throws Exception {
    AssetManagerImpl am = makeAssetManagerWithoutHandlers();
    am.addEventHandler(EasyMock.createNiceMock(AssetManagerUpdateHandler.class));
    am.addEventHandler(EasyMock.createNiceMock(AssetManagerUpdateHandler.class));
    return am;
  }

  /**
   * Create a new test asset manager.
   */
  protected AssetManagerImpl makeAssetManagerWithoutHandlers() throws Exception {
    HttpAssetProvider httpAssetProvider =  new HttpAssetProvider() {
      @Override public Snapshot prepareForDelivery(Snapshot snapshot) {
        return snapshot;
      }
    };

    final Database db = new Database(DBTestEnv.newDBSession(PERSISTENCE_UNIT));
    db.setHttpAssetProvider(httpAssetProvider);

    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class)))
            .andReturn(IoSupport.classPathResourceAsFile("/dublincore-a.xml").get()).anyTimes();
    EasyMock.expect(workspace.read(EasyMock.anyObject(URI.class)))
            .andAnswer(() -> getClass().getResourceAsStream("/dublincore-a.xml")).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class), EasyMock.anyBoolean())).andAnswer(() -> {
      File tmp = tempFolder.newFile();
      FileUtils.copyFile(new File(getClass().getResource("/dublincore-a.xml").toURI()), tmp);
      return tmp;
    }).anyTimes();
    EasyMock.replay(workspace);

    AssetStore localAssetStore = mkAssetStore(LOCAL_STORE_ID);
    RemoteAssetStore remoteAssetStore1 = mkRemoteAssetStore(REMOTE_STORE_1_ID);
    RemoteAssetStore remoteAssetStore2 = mkRemoteAssetStore(REMOTE_STORE_2_ID);

    Organization org = new DefaultOrganization();
    User currentUser = TestUser.mk(org, org.getAdminRole());

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(() -> currentUser).anyTimes();
    EasyMock.replay(securityService);

    final AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl(EasyMock.<MediaPackage>anyObject()))
            .andReturn(tuple(new AccessControlList(), AclScope.Episode))
            .anyTimes();
    EasyMock.replay(authorizationService);

    ElasticsearchIndex esIndex = EasyMock.createNiceMock(ElasticsearchIndex.class);
    EasyMock.expect(esIndex.addOrUpdateEvent(EasyMock.anyString(), EasyMock.anyObject(Function.class),
            EasyMock.anyString(), EasyMock.anyObject(User.class))).andReturn(Optional.empty()).atLeastOnce();
    EasyMock.replay(esIndex);

    AssetManagerImpl am = new AssetManagerImpl();
    am.setAssetStore(localAssetStore);
    am.addRemoteAssetStore(remoteAssetStore1);
    am.addRemoteAssetStore(remoteAssetStore2);
    am.setHttpAssetProvider(httpAssetProvider);
    am.setWorkspace(workspace);
    am.setSecurityService(securityService);
    am.setDatabase(db);
    am.setAuthorizationService(authorizationService);
    am.setIndex(esIndex);
    return am;
  }

  public static MediaPackage mkMediaPackage(MediaPackageElement... elements) throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    for (MediaPackageElement e : elements) {
      mp.add(e);
    }
    return mp;
  }

  public static Catalog mkCatalog() throws Exception {
    final Catalog mpe = (Catalog) MediaPackageElementBuilderFactory
        .newInstance()
        .newElementBuilder()
        .newElement(Type.Catalog, MediaPackageElements.EPISODE);
    mpe.setURI(new URI("http://dummy.org"));
    mpe.setMimeType(MimeTypes.XML);
    return mpe;
  }

  /**
   * Create a number of media packages with one catalog each and add it to the
   * AssetManager. Return the media package IDs as an array.
   * <p>
   * Please note that each media package creates two assets in the store--the
   * catalog and the manifest--but only one asset in the database which is the
   * catalog. The manifest is represented in the snapshot table, not the asset
   * table.
   *
   * @param amount
   *         the amount of media packages to create
   * @param minVersions
   *         the minimum amount of versions to create per media package
   * @param maxVersions
   *         the maximum amount of versions to create per media package
   * @param seriesId
   *         an optional series ID
   */
  protected String[] createAndAddMediaPackagesSimple(
      int amount,
      final int minVersions,
      final int maxVersions,
      final Optional<String> seriesId
  ) {
    return Arrays.stream(createAndAddMediaPackages(amount, minVersions, maxVersions, seriesId))
        .map(s -> s.getMediaPackage().getIdentifier().toString())
        .collect(Collectors.toSet())
        .toArray(new String[]{});
  }

  /**
   * Like {@link #createAndAddMediaPackagesSimple(int, int, int, Optional)} but without series ID.
   */
  protected String[] createAndAddMediaPackagesSimple(int amount, final int minVersions, final int maxVersions) {
    return createAndAddMediaPackagesSimple(amount, minVersions, maxVersions, Optional.empty());
  }

  /**
   * Continuous versions.
   *
   * @see #createAndAddMediaPackages(int, int, int, boolean, Optional)
   */
  protected Snapshot[] createAndAddMediaPackages(
          int amount, final int minVersions, final int maxVersions, final Optional<String> seriesId) {
    return createAndAddMediaPackages(amount, minVersions, maxVersions, true, seriesId);
  }

  /**
   * @param continuousVersions true if version numbers should be increased continuously, false if there should be
   *          discontinuities
   * @see #createAndAddMediaPackagesSimple(int, int, int, Optional)
   */
  protected Snapshot[] createAndAddMediaPackages(
      int amount,
      final int minVersions, final int maxVersions,
      final boolean continuousVersions,
      final Optional<String> seriesId) {
    logger.info("Create {} media packages with {} to {} snapshots each", amount, minVersions, maxVersions);

    List<Snapshot> snapshots = new ArrayList<>();

    for (int mpCount = 0; mpCount < amount; mpCount++) {
      try {
        MediaPackage mp = mkMediaPackage(mkCatalog());
        if (seriesId.isPresent()) {
          mp.setSeries(seriesId.get());
        }
        int versions = (int) (Math.random() * ((double) maxVersions - minVersions) + minVersions);
        String mpId = mp.getIdentifier().toString();
        logger.debug("Going to take {} snapshot/s of media package {}", versions, mpId);

        for (int versionCount = 0; versionCount < versions; versionCount++) {
          if (!continuousVersions) {
            am.getDatabase().claimVersion(mpId);
          }

          logger.debug("Taking snapshot {} of media package {}", versionCount + 1, mpId);
          snapshots.add(am.takeSnapshot(OWNER, mp));
        }
      } catch (Exception e) {
        logger.error("Failed to create media package or snapshot", e);
        throw new RuntimeException("Media package generation failed", e);
      }
    }

    return snapshots.toArray(new Snapshot[0]);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * A property schema definition.
   */
  public static class Props {
    private final String namespace;

    public Props(String namespace) {
      this.namespace = namespace;
    }

    public String getNamespace() {
      return namespace;
    }

    private PropertyName name(String localName) {
      return PropertyName.mk(namespace, localName);
    }

    public Property count(String mpId, long value) {
      return Property.mk(PropertyId.mk(mpId, name("count")), Value.LONG.mk(value));
    }

    public Property approved(String mpId, boolean value) {
      return Property.mk(PropertyId.mk(mpId, name("approved")), Value.BOOLEAN.mk(value));
    }

    public Property start(String mpId, Date value) {
      return Property.mk(PropertyId.mk(mpId, name("start")), Value.DATE.mk(value));
    }

    public Property end(String mpId, Date value) {
      return Property.mk(PropertyId.mk(mpId, name("end")), Value.DATE.mk(value));
    }

    public Property legacyId(String mpId, String value) {
      return Property.mk(PropertyId.mk(mpId, name("legacyId")), Value.STRING.mk(value));
    }

    public Property agent(String mpId, String value) {
      return Property.mk(PropertyId.mk(mpId, name("agent")), Value.STRING.mk(value));
    }

    public Property seriesId(String mpId, String value) {
      return Property.mk(PropertyId.mk(mpId, name("series")), Value.STRING.mk(value));
    }

    public Property versionId(String mpId, Version value) {
      return Property.mk(PropertyId.mk(mpId, name("version")), Value.VERSION.mk(value));
    }
  }

  /**
   * Create a test asset store.
   */
  protected AssetStore mkAssetStore(String storeType) {
    return new AssetStore() {
      private Set<StoragePath> store = new HashSet<>();

      private void logSize() {
        logger.debug("Store contains {} asset(s)", store.size());
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

      @Override public Optional<InputStream> get(StoragePath path) throws AssetStoreException {
        return IoSupport.openClassPathResource("/dublincore-a.xml").isPresent()
            ? Optional.of(IoSupport.openClassPathResource("/dublincore-a.xml").get())
            : Optional.empty();
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
              && (sel.getVersion().isPresent() ? sel.getVersion().get().equals(s.getVersion()) : true))) {
            newStore.add(s);
          } else {
            deleted = true;
          }
        }
        store = newStore;
        logSize();
        return deleted;
      }

      @Override public Optional<Long> getTotalSpace() {
        return Optional.empty();
      }

      @Override public Optional<Long> getUsableSpace() {
        return Optional.empty();
      }

      @Override public Optional<Long> getUsedSpace() {
        return Optional.of((long) store.size());
      }

      @Override public String getStoreType() {
        return storeType;
      }

      @Override public String getStorageName() {
        return "Test Store";
      }
    };
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

      @Override public Optional<InputStream> get(StoragePath path) throws AssetStoreException {
        return IoSupport.openClassPathResource("/dublincore-a.xml").isPresent()
            ? Optional.of(IoSupport.openClassPathResource("/dublincore-a.xml").get())
            : Optional.empty();
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
                  && (sel.getVersion().isPresent() ? sel.getVersion().get().equals(s.getVersion()) : true))) {
            newStore.add(s);
          } else {
            deleted = true;
          }
        }
        store = newStore;
        logSize();
        return deleted;
      }

      @Override public Optional<Long> getTotalSpace() {
        return Optional.empty();
      }

      @Override public Optional<Long> getUsableSpace() {
        return Optional.empty();
      }

      @Override public Optional<Long> getUsedSpace() {
        return Optional.of((long) store.size());
      }

      @Override public String getStoreType() {
        return storeType;
      }

      @Override public String getStorageName() {
        return "Test Store";
      }
    };
  }

  void assertStoreSize(long size) {
    assertEquals("Assets in store", size, (long) am.getLocalAssetStore().getUsedSpace().get());
  }

  String getStoreType() {
    return "test";
  }
}
