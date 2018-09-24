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

package org.opencastproject.assetmanager.aws.persistence;

import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.persistence.PersistenceUtil;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeSet;

public class AwsAssetDatabaseImplTest {

  private ComboPooledDataSource pooledDataSource;
  private String storage;

  private ComponentContext cc;

  private AwsAssetDatabaseImpl database;

  private static final String ORG = "org";
  private static final String MP_ID = "abcd";
  private static final String ASSET1_ID = "efgh";
  private static final String ASSET2_ID = "ijkl";
  private static final String AWS_VERSION_1 = "mnop";
  private static final String AWS_VERSION_2 = "qrst";

  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc, cc);

    database = new AwsAssetDatabaseImpl();
    database.setEntityManagerFactory(PersistenceUtil.newTestEntityManagerFactory(AwsAssetDatabaseImpl.PERSISTENCE_UNIT));
    database.activate(cc);
  }

  @After
  public void tearDown() throws SQLException {
    database.deactivate(cc);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
  }

  @Test
  public void testStoreMapping() throws Exception {
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals("archive_path/" + ASSET1_ID, mapping.getObjectKey());
    Assert.assertEquals(AWS_VERSION_1, mapping.getObjectVersion());
    Assert.assertNull(mapping.getDeletionDate());
  }

  @Test
  public void testDeleteMapping() throws Exception {
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);

    database.deleteMapping(path);
    // Mapping row is not deleted, but findMapping should not find it
    mapping = database.findMapping(path);
    Assert.assertNull(mapping);
    // Mapping should have its deletion date set
    List<AwsAssetMapping> mappings = database.findAllByMediaPackage(MP_ID);
    Assert.assertEquals(1, mappings.size());
    Assert.assertNotNull(mappings.get(0).getDeletionDate());
  }

  @Test
  public void testFindMapping() throws Exception {
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals("archive_path/" + ASSET1_ID, mapping.getObjectKey());
    Assert.assertEquals(AWS_VERSION_1, mapping.getObjectVersion());
    Assert.assertNull(mapping.getDeletionDate());
  }

  @Test
  public void testFindMappingByKey() throws Exception {
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // Store another mapping with the same key (logic hard-link to existing file)
    path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET2_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    List<AwsAssetMapping> mappings = database.findMappingsByKey("archive_path/" + ASSET1_ID);
    Assert.assertEquals(2, mappings.size());
    AwsAssetMapping m1 = mappings.get(0);
    AwsAssetMapping m2 = mappings.get(1);
    Assert.assertTrue((ASSET1_ID.equals(m1.getMediaPackageElementId()) && ASSET2_ID.equals(m2.getMediaPackageElementId()))
            || (ASSET2_ID.equals(m1.getMediaPackageElementId()) && ASSET1_ID.equals(m2.getMediaPackageElementId())));
  }

  @Test
  public void testFindMappingByMediaPackageAndVersion() throws Exception {
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // Another version of the SAME asset (same checksum)
    path = new StoragePath(ORG, MP_ID, new VersionImpl(2L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // One more version
    path = new StoragePath(ORG, MP_ID, new VersionImpl(3L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    // Search by media package, version 2
    path = new StoragePath(ORG, MP_ID, new VersionImpl(2L), null);

    List<AwsAssetMapping> mappings = database.findMappingsByMediaPackageAndVersion(path);
    Assert.assertEquals(1, mappings.size());
    Assert.assertEquals(2L, mappings.get(0).getVersion().longValue());
  }

  @Test
  public void testFindMappingByMediaPackageNoVersion() throws Exception {
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // Another version
    path = new StoragePath(ORG, MP_ID, new VersionImpl(2L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // One more version
    path = new StoragePath(ORG, MP_ID, new VersionImpl(3L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    // Search by media package, any version
    path = new StoragePath(ORG, MP_ID, null, null);

    List<AwsAssetMapping> mappings = database.findMappingsByMediaPackageAndVersion(path);
    Assert.assertEquals(3, mappings.size());
    // Store versions found in order
    TreeSet<Long> versions = new TreeSet<Long>();
    versions.add(mappings.get(0).getVersion());
    versions.add(mappings.get(1).getVersion());
    versions.add(mappings.get(2).getVersion());
    Assert.assertEquals(3, versions.size());
    long count = 1;
    for (long v : versions) {
      Assert.assertEquals(count++, v);
    }
  }

  // public List<AwsAssetMapping> findAllByMediaPackage(String mpId) throws AwsAssetDatabaseException {
  @Test
  public void testFindAllByMediaPackage() throws Exception {
    // Store an asset
    StoragePath path1 = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET1_ID);
    database.storeMapping(path1, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // Store another asset
    StoragePath path2 = new StoragePath(ORG, MP_ID, new VersionImpl(1L), ASSET2_ID);
    database.storeMapping(path2, "archive_path/" + ASSET2_ID, AWS_VERSION_2);
    // Another version of the asset 1 (same checksum)
    StoragePath path = new StoragePath(ORG, MP_ID, new VersionImpl(2L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);
    // One more version
    path = new StoragePath(ORG, MP_ID, new VersionImpl(3L), ASSET1_ID);
    database.storeMapping(path, "archive_path/" + ASSET1_ID, AWS_VERSION_1);

    // Delete version 1
    database.deleteMapping(path1);
    database.deleteMapping(path2);

    // Search by media package
    List<AwsAssetMapping> mappings = database.findAllByMediaPackage(MP_ID);
    // Deleted assets should also be returned
    Assert.assertEquals(4, mappings.size());
  }
}
