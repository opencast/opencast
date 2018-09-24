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

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.fns.Booleans.eq;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.Snapshots;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.PropertySchema;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.AssetStoreException;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistencefn.PersistenceEnv;
import org.opencastproject.util.persistencefn.PersistenceEnvs;
import org.opencastproject.util.persistencefn.Queries;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.FnX;
import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.P1Lazy;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;
import com.mysema.query.jpa.impl.JPAQuery;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

/**
 * Base class for {@link AssetManager} tests.
 * <p>
 * See {@link org.opencastproject.util.persistencefn.PersistenceUtil#mkTestEntityManagerFactoryFromSystemProperties(String)}
 * for command line configuration options.
 * <p>
 * Implementations of this class need to call {@link #setUp(AssetManager)} to setup the necessary variables prior to
 * running a test. You may implement a {@link org.junit.Before} annotated method like this:
 * <pre>
 *   |@Before
 *   |public void setUp() throws Exception {
 *   |  setUp(mkAbstractAssetManager());
 *   |}
 * </pre>
 */
// CHECKSTYLE:OFF
public abstract class AssetManagerTestBase<A extends AssetManager> {
  protected static final Logger logger = LoggerFactory.getLogger(AssetManagerTestBase.class);
  public static final String PERSISTENCE_UNIT = "org.opencastproject.assetmanager.impl";

  protected static final String OWNER = "test";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  /** The asset manager under test. */
  protected A am;
  protected AQueryBuilder q;
  protected Props p;
  protected Props p2;
  protected PersistenceEnv penv;

  /**
   * Return the underlying instance of {@link AbstractAssetManager}.
   * If the asset manager under test is of type AbstractAssetManager just return that instance.
   */
  public abstract AbstractAssetManager getAbstractAssetManager();

  public abstract String getCurrentOrgId();

  public final void setUp(A assetManager) {
    am = assetManager;
    q = am.createQuery();
    p = new Props(q, "org.opencastproject.service");
    p2 = new Props(q, "org.opencastproject.service.sub");
  }

  public static MediaPackage mkMediaPackage(MediaPackageElement... elements) throws Exception {
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    for (MediaPackageElement e : elements) {
      mp.add(e);
    }
    return mp;
  }

  public static Catalog mkCatalog() throws Exception {
    final Catalog mpe = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(Type.Catalog, MediaPackageElements.EPISODE);
    mpe.setURI(new URI("http://dummy.org"));
    mpe.setMimeType(MimeTypes.XML);
    return mpe;
  }

  static <A> int sizeOf(Stream<A> stream) {
    int count = 0;
    for (A ignore : stream) {
      count++;
    }
    return count;
  }

  static P1<Integer> inc() {
    return new P1Lazy<Integer>() {
      private int i = 0;

      @Override public Integer get1() {
        return i++;
      }
    };
  }

  @SafeVarargs
  public static  <A> A[] $a(A... as) {
    return as;
  }

  /**
   * Create a number of media packages with one catalog each and add it to the AssetManager. Return the media package IDs as an array.
   * <p>
   * Please note that each media package creates two assets in the store--the catalog and the manifest--but only one asset
   * in the database which is the catalog. The manifest is represented in the snapshot table, not the asset table.
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
  protected String[] createAndAddMediaPackagesSimple(int amount, final int minVersions, final int maxVersions, final Opt<String> seriesId) {
    return $(createAndAddMediaPackages(amount, minVersions, maxVersions, seriesId)).map(Snapshots.getMediaPackageId).toSet().toArray(new String[]{});
  }

  /**
   * Like {@link #createAndAddMediaPackagesSimple(int, int, int, Opt)} but without series ID.
   */
  protected String[] createAndAddMediaPackagesSimple(int amount, final int minVersions, final int maxVersions) {
    return createAndAddMediaPackagesSimple(amount, minVersions, maxVersions, Opt.<String>none());
  }

  /**
   * Continuous versions.
   *
   * @see #createAndAddMediaPackages(int, int, int, boolean, Opt)
   */
  protected Snapshot[] createAndAddMediaPackages(
          int amount, final int minVersions, final int maxVersions, final Opt<String> seriesId) {
    return createAndAddMediaPackages(amount, minVersions, maxVersions, true, seriesId);
  }

  /**
   * @param continuousVersions true if version numbers should be increased continuously, false if there should be
   *          discontinuities
   * @see #createAndAddMediaPackagesSimple(int, int, int, Opt)
   */
  protected Snapshot[] createAndAddMediaPackages(
          int amount,
          final int minVersions, final int maxVersions,
          final boolean continuousVersions,
          final Opt<String> seriesId) {
    logger.info(format("Create %s media packages with %d to %d snapshots each", amount, minVersions, maxVersions));
    final Stream<Snapshot> inserts = Stream.cont(inc()).take(amount).bind(new FnX<Integer, Iterable<Snapshot>>() {
      @Override public Iterable<Snapshot> applyX(final Integer mpCount) throws Exception {
        final MediaPackage mp = mkMediaPackage(mkCatalog());
        for (String sid : seriesId) {
          mp.setSeries(sid);
        }
        final int versions = (int) (Math.random() * ((double) maxVersions - minVersions) + minVersions);
        final String mpId = mp.getIdentifier().toString();
        logger.debug(format("Going to take %d snapshot/s of media package %s", versions, mpId));
        return Stream.cont(inc()).take(versions).map(new Fn<Integer, Snapshot>() {
          @Override public Snapshot apply(Integer versionCount) {
            if (!continuousVersions) {
              // insert a gap into the version claim
              getAbstractAssetManager().getDb().claimVersion(mp.getIdentifier().toString());
            }
            logger.debug(format("Taking snapshot %d of media package %s", versionCount + 1, mpId));
            return am.takeSnapshot(OWNER, mp);
          }
        });
      }
    });
    return Collections.toArray(Snapshot.class, inserts.toList());
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * A property schema definition.
   */
  public static class Props extends PropertySchema {
    public Props(AQueryBuilder q, String namespace) {
      super(q, namespace);
    }

    // define your properties here below

    // CHECKSTYLE:OFF
    public final PropertyField<Long> count = longProp("count");

    public final PropertyField<Boolean> approved = booleanProp("approved");

    public final PropertyField<Date> start = dateProp("start");

    public final PropertyField<Date> end = dateProp("end");

    public final PropertyField<String> legacyId = stringProp("legacyId");

    public final PropertyField<String> agent = stringProp("agent");

    public final PropertyField<String> seriesId = stringProp("series");

    public final PropertyField<Version> versionId = versionProp("version");
    // CHECKSTYLE:ON
  }

  /**
   * Create a new test asset manager.
   */
  protected AbstractAssetManager mkAbstractAssetManager() throws Exception {
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
    final Database db = new Database(penv);
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
    final AssetStore assetStore = mkAssetStore("test-store-type");
    //
    return new AbstractAssetManager() {
      @Override public Database getDb() {
        return db;
      }

      @Override public AssetStore getLocalAssetStore() {
        return assetStore;
      }

      @Override public HttpAssetProvider getHttpAssetProvider() {
        // identity provider
        return new HttpAssetProvider() {
          @Override public Snapshot prepareForDelivery(Snapshot snapshot) {
            return snapshot;
          }
        };
      }

      @Override protected Workspace getWorkspace() {
        return workspace;
      }

      @Override protected String getCurrentOrgId() {
        return AssetManagerTestBase.this.getCurrentOrgId();
      }
    };
  }

  /**
   * Create a test asset store.
   */
  protected AssetStore mkAssetStore(String storeType) {
    return new AssetStore() {
      private Set<StoragePath> store = new HashSet<>();

      private void logSize() {
        logger.debug(format("Store contains %d asset(s)", store.size()));
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

      @Override public String getStoreType() { return storeType; }
    };
  }

  long runCount(final JPAQuery q) {
    return penv.tx(new Fn<EntityManager, Long>() {
      @Override public Long apply(EntityManager em) {
        return q.clone(em, Database.TEMPLATES).count();
      }
    });
  }

  void assertStoreSize(long size) {
    assertEquals("Assets in store", size, (long) getAbstractAssetManager().getLocalAssetStore().getUsedSpace().get());
  }

  String getStoreType() {
    return "test";
  }
}
// CHECKSTYLE:ON
