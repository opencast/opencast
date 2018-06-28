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

package org.opencastproject.assetmanager.aws.s3;

import org.opencastproject.assetmanager.aws.persistence.AwsAssetDatabaseImpl;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetMapping;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.entwinemedia.fn.data.Opt;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

public class AwsS3AssetStoreTest {
  private ComboPooledDataSource pooledDataSource;
  private ComponentContext cc;
  private AwsAssetDatabaseImpl database;

  private AmazonS3Client s3Client;
  private TransferManager s3Transfer;
  private S3Object s3Object;
  private Workspace workspace;

  private static final String BUCKET_NAME = "aws-archive-bucket";

  private static final String ORG_ID = "org";
  private static final String MP_ID = "abcd";
  private static final String MP_ID2 = "mnop";
  private static final String ASSET_ID = "efgh";
  private static final String ASSET_ID2 = "ijkl";
  private static final String FILE_NAME = "dublincore.xml";
  private static final String KEY_VERSION_1 = ORG_ID + "/" + MP_ID + "/1/";
  private static final String KEY_VERSION_2 = ORG_ID + "/" + MP_ID + "/2/";
  private static final String AWS_VERSION_1 = "1234";

  private URI uri;
  private File sampleFile;

  private AwsS3AssetStore store;

  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc, cc);

    database = new AwsAssetDatabaseImpl();
    database.setEntityManagerFactory(PersistenceUtil.newTestEntityManagerFactory(AwsAssetDatabaseImpl.PERSISTENCE_UNIT));
    database.activate(cc);

    uri = getClass().getClassLoader().getResource(FILE_NAME).toURI();
    sampleFile = new File(uri);

    // Set up the service
    ObjectMetadata objMetadata = EasyMock.createStrictMock(ObjectMetadata.class);
    EasyMock.expect(objMetadata.getVersionId()).andReturn(AWS_VERSION_1).anyTimes();
    EasyMock.replay(objMetadata);
    s3Object = EasyMock.createNiceMock(S3Object.class);
    EasyMock.expect(s3Object.getObjectMetadata()).andReturn(objMetadata).anyTimes();
    s3Client = EasyMock.createStrictMock(AmazonS3Client.class);
    s3Transfer = EasyMock.createStrictMock(TransferManager.class);
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andReturn(null);
    EasyMock.expect(s3Client.getObject(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml")).andReturn(s3Object);
    // Replay will be called in each test

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(uri)).andReturn(sampleFile).anyTimes();
    EasyMock.replay(workspace);

    store = new AwsS3AssetStore();
    store.setBucketName(BUCKET_NAME);
    store.setS3(s3Client);
    store.setS3TransferManager(s3Transfer);
    store.setWorkspace(workspace);
    store.setDatabase(database);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testPut() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload);
    EasyMock.expect(s3Client.getObject(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml")).andReturn(s3Object);
    EasyMock.replay(s3Object, s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));

    // Check if mapping saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());
    Assert.assertEquals(ASSET_ID, mapping.getMediaPackageElementId());
  }

  @Test
  public void testCopy() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload);
    EasyMock.replay(s3Object, s3Client, s3Transfer);

    // Store first asset
    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));

    // Copy asset, this should not generate any aws call, just a new mapping in the db
    StoragePath path2 = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID2);
    Assert.assertTrue(store.copy(path, path2));

    // Check if both mappings saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());
    Assert.assertEquals(ASSET_ID, mapping.getMediaPackageElementId());

    AwsAssetMapping mapping2 = database.findMapping(path2);
    Assert.assertNotNull(mapping2);
    Assert.assertEquals(ORG_ID, mapping2.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping2.getMediaPackageId());
    Assert.assertEquals(1L, mapping2.getVersion().longValue());
    Assert.assertEquals(ASSET_ID2, mapping2.getMediaPackageElementId());

    // Check if both have the same AWS object key
    Assert.assertTrue(mapping.getObjectKey().equals(mapping2.getObjectKey()));
  }

  @Test
  public void testBadCopy() throws Exception {
    EasyMock.replay(s3Client, s3Transfer);

    // Create first asset path, but do not store it
    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);

    // Try to copy asset, this should return false
    StoragePath path2 = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID2);
    Assert.assertFalse(store.copy(path, path2));
  }

  @Test
  public void testGet() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload);
    S3ObjectInputStream s3Stream = EasyMock.createNiceMock(S3ObjectInputStream.class);
    EasyMock.expect(s3Object.getObjectContent()).andReturn(s3Stream);
    EasyMock.expect(s3Client.getObject(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml")).andReturn(s3Object);
    EasyMock.replay(s3Object, s3Stream, s3Client, s3Transfer);

    // Store asset
    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));

    Opt<InputStream> streamOpt = store.get(path);
    Assert.assertTrue(streamOpt.isSome());
  }

  @Test
  public void testBadGet() throws Exception {
    EasyMock.replay(s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);

    Opt<InputStream> streamOpt = store.get(path);
    Assert.assertFalse(streamOpt.isSome());
  }

  @Test
  public void testDeleteWithVersion() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().times(2);
    EasyMock.replay(upload);

    EasyMock.expect(s3Client.getObject(BUCKET_NAME, KEY_VERSION_2 + ASSET_ID + ".xml")).andReturn(s3Object);
    s3Client.deleteObject(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml");
    EasyMock.expectLastCall().once();
    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload)
            .once();
    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_2 + ASSET_ID + ".xml", sampleFile))
            .andReturn(upload).once();
    EasyMock.replay(s3Object, s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));
    // Store another version of same asset
    StoragePath path2 = new StoragePath(ORG_ID, MP_ID, new VersionImpl(2L), ASSET_ID);
    store.put(path2, Source.mk(uri));

    // Check if mappings were saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());

    AwsAssetMapping mapping2 = database.findMapping(path2);
    Assert.assertNotNull(mapping2);
    Assert.assertEquals(ORG_ID, mapping2.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping2.getMediaPackageId());
    Assert.assertEquals(2L, mapping2.getVersion().longValue());

    // Delete version 1 only
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, new VersionImpl(1L));
    Assert.assertTrue(store.delete(versionSelector));

    // Check if version1 was deleted but version 2 is still there
    Assert.assertNull(database.findMapping(path));

    mapping2 = database.findMapping(path2);
    Assert.assertNotNull(mapping2);
  }

  @Test
  public void testDeleteNoneVersion() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().times(2);
    EasyMock.replay(upload);

    EasyMock.expect(s3Client.getObject(BUCKET_NAME, KEY_VERSION_2 + ASSET_ID + ".xml")).andReturn(s3Object);
    s3Client.deleteObject(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml");
    EasyMock.expectLastCall().once();
    s3Client.deleteObject(BUCKET_NAME, KEY_VERSION_2 + ASSET_ID + ".xml");
    EasyMock.expectLastCall().once();

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload)
            .once();
    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_2 + ASSET_ID + ".xml", sampleFile))
            .andReturn(upload).once();
    EasyMock.replay(s3Object, s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));
    // Store another version of same asset
    StoragePath path2 = new StoragePath(ORG_ID, MP_ID, new VersionImpl(2L), ASSET_ID);
    store.put(path2, Source.mk(uri));

    // Check if mappings were saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());

    AwsAssetMapping mapping2 = database.findMapping(path2);
    Assert.assertNotNull(mapping2);
    Assert.assertEquals(ORG_ID, mapping2.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping2.getMediaPackageId());
    Assert.assertEquals(2L, mapping2.getVersion().longValue());

    // Delete without specifying version, both should be deleted
    DeletionSelector mpSelector = DeletionSelector.deleteAll(ORG_ID, MP_ID);
    Assert.assertTrue(store.delete(mpSelector));

    // Check if both versions were deleted
    Assert.assertNull(database.findMapping(path));
    Assert.assertNull(database.findMapping(path2));
  }

  // @Test
  public void testDeleteNonExistentVersion() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    s3Client.deleteObject(BUCKET_NAME, KEY_VERSION_1 + sampleFile.getName());
    EasyMock.expectLastCall().times(2);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload)
            .once();
    EasyMock.replay(s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));

    // Check if mappings were saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());

    // Delete wrong version
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, new VersionImpl(2L));
    store.delete(versionSelector);

    // Check if version 1 is still there
    Assert.assertNotNull(database.findMapping(path));
  }

  @Test
  public void testDeleteLinkedAsset() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, KEY_VERSION_1 + ASSET_ID + ".xml", sampleFile)).andReturn(upload)
            .once();
    EasyMock.replay(s3Object, s3Client, s3Transfer);

    // Store first asset
    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    store.put(path, Source.mk(uri));

    // Copy asset, this should not generate any aws call, just a new mapping in the db
    StoragePath path2 = new StoragePath(ORG_ID, MP_ID2, new VersionImpl(1L), ASSET_ID2);
    Assert.assertTrue(store.copy(path, path2));

    // Check if both mappings saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());
    Assert.assertEquals(ASSET_ID, mapping.getMediaPackageElementId());

    AwsAssetMapping mapping2 = database.findMapping(path2);
    Assert.assertNotNull(mapping2);
    Assert.assertEquals(ORG_ID, mapping2.getOrganizationId());
    Assert.assertEquals(MP_ID2, mapping2.getMediaPackageId());
    Assert.assertEquals(1L, mapping2.getVersion().longValue());
    Assert.assertEquals(ASSET_ID2, mapping2.getMediaPackageElementId());

    // Check if both have the same AWS object key
    Assert.assertTrue(mapping.getObjectKey().equals(mapping2.getObjectKey()));

    // Delete version 1 only, no call to s3Client.deleteObject is expected: the
    // object should not be deleted from s3 because it's linked to by ASSET_ID2
    DeletionSelector versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, new VersionImpl(1L));
    Assert.assertTrue(store.delete(versionSelector));

    // Check if mp 1, asset 1 was deleted
    Assert.assertNull(database.findMapping(path));
    // Check if mp2, asset 2 still there
    mapping2 = database.findMapping(path2);
    Assert.assertNotNull(mapping2);
  }

}
