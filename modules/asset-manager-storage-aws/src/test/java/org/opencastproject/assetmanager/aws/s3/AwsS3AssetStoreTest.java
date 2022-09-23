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

import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetDatabaseImpl;
import org.opencastproject.assetmanager.aws.persistence.AwsAssetMapping;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.entwinemedia.fn.data.Opt;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public class AwsS3AssetStoreTest {
  private ComboPooledDataSource pooledDataSource;
  private ComponentContext cc;
  private AwsAssetDatabaseImpl database;

  private AmazonS3Client s3Client;
  private TransferManager s3Transfer;
  private S3Object s3Object;

  private ObjectListing s3ObjectListing;
  private ObjectMetadata objMetadata;
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
  private static final String OBJECT_KEY_1 = KEY_VERSION_1 + ASSET_ID + ".xml";
  private static final String OBJECT_KEY_2 = KEY_VERSION_2 + ASSET_ID + ".xml";

  private URI uri;
  private File sampleFile;

  private AwsS3AssetStore store;

  @Before
  public void setUp() throws Exception {
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc, cc);

    database = new AwsAssetDatabaseImpl();
    database.setEntityManagerFactory(
        PersistenceUtil.newTestEntityManagerFactory(AwsAssetDatabaseImpl.PERSISTENCE_UNIT));
    database.activate(cc);

    uri = getClass().getClassLoader().getResource(FILE_NAME).toURI();
    sampleFile = new File(uri);

    // Set up the service
    objMetadata = EasyMock.createStrictMock(ObjectMetadata.class);
    EasyMock.expect(objMetadata.getVersionId()).andReturn(AWS_VERSION_1).anyTimes();
    EasyMock.expect(objMetadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).anyTimes();
    EasyMock.replay(objMetadata);
    s3Object = EasyMock.createNiceMock(S3Object.class);
    EasyMock.expect(s3Object.getObjectMetadata()).andReturn(objMetadata).anyTimes();
    s3Client = EasyMock.createNiceMock(AmazonS3Client.class);

    s3Transfer = EasyMock.createStrictMock(TransferManager.class);
    s3ObjectListing = EasyMock.createNiceMock(ObjectListing.class);
    EasyMock.replay(s3ObjectListing);
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andReturn(s3ObjectListing);

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
  public void testBucketErrorSetup() throws Exception {
    AmazonServiceException exception = EasyMock.createNiceMock(AmazonServiceException.class);
    EasyMock.expect(exception.getStatusCode()).andReturn(999).anyTimes();
    //Clear the existing behaviour from setup
    EasyMock.resetToNice(s3Client);
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andThrow(exception);

    Upload upload = EasyMock.createStrictMock(Upload.class);

    EasyMock.replay(exception, upload, s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    try {
      store.put(path, Source.mk(uri));
      Assert.fail("Bucket should not be set up, if we get here then there's something wrong.");
    } catch (AssetStoreException e) {
      //Verify that the bucket exception code matches
      Assert.assertEquals(999, ((AmazonServiceException) e.getCause().getCause()).getStatusCode());
    }
  }

  @Test
  public void testBadBucketPerms() throws Exception {
    AmazonServiceException noperms = EasyMock.createNiceMock(AmazonServiceException.class);
    //NB: not sure what the code here is, but it's *not* 404
    EasyMock.expect(noperms.getStatusCode()).andReturn(403).once();
    EasyMock.expect(noperms.getMessage()).andReturn("Fake message simulating an bad bucket permissions").once();

    //Clear any existing behaviour from setup
    EasyMock.resetToStrict(s3Client);

    // Pretend bucket pre-existing, but no permissions.
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andThrow(noperms).once();
    EasyMock.replay(noperms, s3Client);

    try {
      store.createAWSBucket();
      Assert.fail("Exception should have been thrown!");
    } catch (IllegalStateException e) { }
    Assert.assertFalse(store.bucketCreated);
    EasyMock.verify();
  }

  @Test
  public void testBadBucketName() throws Exception {
    AmazonServiceException nobucket = EasyMock.createNiceMock(AmazonServiceException.class);
    EasyMock.expect(nobucket.getStatusCode()).andReturn(404).once();
    EasyMock.expect(nobucket.getMessage()).andReturn("Fake message simulating initial lack of bucket").once();

    AmazonServiceException badname = EasyMock.createNiceMock(AmazonServiceException.class);
    EasyMock.expect(badname.getStatusCode()).andReturn(404).once();
    EasyMock.expect(badname.getMessage()).andReturn("Fake message simulating an invalid bucket name").once();

    //Clear any existing behaviour from setup
    EasyMock.resetToStrict(s3Client);

    // no bucket exists, so let's create it, but we have a bad bucket name
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andThrow(nobucket).once();
    EasyMock.expect(s3Client.createBucket(BUCKET_NAME)).andThrow(badname).once();
    EasyMock.replay(nobucket, badname, s3Client);

    try {
      store.createAWSBucket();
      Assert.fail("Exception should have been thrown!");
    } catch (IllegalStateException e) { }
    Assert.assertFalse(store.bucketCreated);
    EasyMock.verify();
  }

  @Test
  public void testGoodBucketCreation() throws Exception {
    AmazonServiceException nobucket = EasyMock.createNiceMock(AmazonServiceException.class);
    EasyMock.expect(nobucket.getStatusCode()).andReturn(404).once();
    EasyMock.expect(nobucket.getMessage()).andReturn("Fake message simulating initial lack of bucket").once();

    //Clear any existing behaviour from setup
    EasyMock.resetToStrict(s3Client);

    // no bucket exists, but creation succeeds and it exists afterwards!
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andThrow(nobucket).once();
    EasyMock.expect(s3Client.createBucket(BUCKET_NAME)).andReturn(EasyMock.createNiceMock(Bucket.class)).once();
    s3Client.setBucketVersioningConfiguration(EasyMock.anyObject(SetBucketVersioningConfigurationRequest.class));
    EasyMock.expectLastCall().once();
    // Bucket created!
    EasyMock.expect(s3Client.listObjects(BUCKET_NAME)).andReturn(s3ObjectListing).once();
    EasyMock.replay(nobucket, s3Client);

    try {
      store.createAWSBucket();
    } catch (IllegalStateException e) {
      Assert.fail("Exception should NOT have been thrown!");
    }
    Assert.assertTrue(store.bucketCreated);
    EasyMock.verify();
  }

  private void setupUpload(String keyId) throws Exception {
    setupUpload(keyId, objMetadata);
  }

  private void setupUpload(String keyId, ObjectMetadata metadata) throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, keyId, sampleFile)).andReturn(upload).once();
    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, keyId)).andReturn(metadata).anyTimes();
  }

  @Test
  public void testPut() throws Exception {
    setupUpload(OBJECT_KEY_1);
    EasyMock.replay(s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    Source source = Source.mk(uri);
    store.put(path, source);

    // Check if mapping saved to db
    AwsAssetMapping mapping = database.findMapping(path);
    Assert.assertEquals(OBJECT_KEY_1, mapping.getObjectKey());
    Assert.assertNotNull(mapping);
    Assert.assertEquals(ORG_ID, mapping.getOrganizationId());
    Assert.assertEquals(MP_ID, mapping.getMediaPackageId());
    Assert.assertEquals(1L, mapping.getVersion().longValue());
    Assert.assertEquals(ASSET_ID, mapping.getMediaPackageElementId());

    Assert.assertEquals(OBJECT_KEY_1, store.getAssetObjectKey(path));
  }

  @Test
  public void testTagging() throws Exception {
    Capture<SetObjectTaggingRequest> tags = Capture.newInstance();
    EasyMock.expect(s3Client.setObjectTagging(EasyMock.capture(tags))).andReturn(null).anyTimes();
    //Fake mimetypes to trigger, or not trigger tagging
    String[] mimetypes = { "audio", "image", "video" };
    StoragePath path;
    String objectKey;
    Opt<MimeType> type;
    Source source;
    for (String mimetype : mimetypes) {
      path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), mimetype);
      type = Opt.some(MimeType.mimeType(mimetype, "fake"));
      objectKey = KEY_VERSION_1 + mimetype + ".xml";
      source = Source.mk(uri, null, type);
      //We need to do this every time we upload something.
      setupUpload(objectKey);
      EasyMock.replay(s3Client, s3Transfer);
      store.put(path, source);
      EasyMock.resetToNice(s3Client, s3Transfer);
      EasyMock.expect(s3Client.setObjectTagging(EasyMock.capture(tags))).andReturn(null).anyTimes();

      //Check with AWS that the tags are applied correctly
      Assert.assertTrue(tags.hasCaptured());
      SetObjectTaggingRequest req = tags.getValue();
      Assert.assertEquals(BUCKET_NAME, req.getBucketName());
      Assert.assertEquals(objectKey, req.getKey());
      Assert.assertEquals("Freezable", req.getTagging().getTagSet().get(0).getKey());
      Assert.assertEquals("true", req.getTagging().getTagSet().get(0).getValue());

      tags.reset();
    }
    path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), "fake");
    type = Opt.some(MimeType.mimeType("non-freezable", "fake"));
    objectKey = KEY_VERSION_1 + "fake.xml";
    source = Source.mk(uri, null, type);
    setupUpload(objectKey);
    EasyMock.replay(s3Client, s3Transfer);
    store.put(path, source);
    //Check with AWS that the tags are not applied since this isn't in the allowlist
    Assert.assertFalse(tags.hasCaptured());
  }

  @Test
  public void testStorageClasses() throws Exception {
    GetObjectTaggingResult gotr = EasyMock.createStrictMock(GetObjectTaggingResult.class);
    List<Tag> tags = List.of(new Tag("Freezable", "true"));
    EasyMock.expect(gotr.getTagSet()).andReturn(tags).once();
    ObjectMetadata metadata = EasyMock.createStrictMock(ObjectMetadata.class);
    EasyMock.expect(metadata.getVersionId()).andReturn(AWS_VERSION_1).anyTimes();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).times(4);
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Glacier.toString()).once();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).once();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.DeepArchive.toString()).once();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).once();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.ReducedRedundancy.toString()).once();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).once();
    EasyMock.expect(s3Client.getObjectTagging(EasyMock.anyObject(GetObjectTaggingRequest.class)))
        .andReturn(gotr).once();
    //FIXME: Mock appropriate results, not that they're checked.
    EasyMock.expect(s3Client.copyObject(EasyMock.anyObject(CopyObjectRequest.class))).andReturn(null).anyTimes();
    EasyMock.replay(gotr, metadata);

    setupUpload(OBJECT_KEY_1, metadata);
    EasyMock.replay(s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    Source source = Source.mk(uri);
    store.put(path, source);

    //This asset doesn't exist
    Assert.assertEquals("NONE",
        store.getAssetStorageClass(new StoragePath(ORG_ID, MP_ID, new VersionImpl(0L), ASSET_ID)));
    //This does, test the default
    Assert.assertEquals(OBJECT_KEY_1, store.getAssetObjectKey(path));
    Assert.assertEquals(StorageClass.Standard.toString(), store.getAssetStorageClass(path));

    //Set the storage class to the same thing -> should be a noop, and not change the class
    store.modifyAssetStorageClass(path, StorageClass.Standard.toString());
    Assert.assertEquals(StorageClass.Standard.toString(), store.getAssetStorageClass(path));
    //change the storage type to glacier or deep archive
    for (String sc : new String[] { StorageClass.Glacier.toString(), StorageClass.DeepArchive.toString() }) {
      store.modifyAssetStorageClass(path, sc);
      Assert.assertEquals(sc, store.getAssetStorageClass(path));
    }
    //change the type to some other type that does not have special handling
    store.modifyAssetStorageClass(path, StorageClass.ReducedRedundancy.toString());
    Assert.assertEquals(StorageClass.ReducedRedundancy.toString(), store.getAssetStorageClass(path));

    EasyMock.verify();
  }

  @Test
  public void testNonFreezableObject() throws Exception {
    ObjectMetadata metadata = EasyMock.createStrictMock(ObjectMetadata.class);
    EasyMock.expect(metadata.getVersionId()).andReturn("2").anyTimes();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).anyTimes();

    GetObjectTaggingResult gotr = EasyMock.createStrictMock(GetObjectTaggingResult.class);
    List<Tag> tags = List.of(new Tag("non-freezable", "fake"));
    EasyMock.expect(gotr.getTagSet()).andReturn(tags).once();

    EasyMock.expect(s3Client.getObjectTagging(EasyMock.anyObject(GetObjectTaggingRequest.class)))
        .andReturn(gotr).once();
    //FIXME: Mock appropriate results, not that they're checked.
    EasyMock.expect(s3Client.copyObject(EasyMock.anyObject(CopyObjectRequest.class))).andReturn(null).anyTimes();
    setupUpload(OBJECT_KEY_2, metadata);
    EasyMock.replay(metadata, gotr, s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(2L), ASSET_ID);
    Opt<MimeType> type = Opt.some(MimeType.mimeType("non-freezable", "fake"));
    Source source = Source.mk(uri, null, type);
    store.put(path, source);

    store.modifyAssetStorageClass(path, StorageClass.DeepArchive.toString());
    Assert.assertEquals(StorageClass.Standard.toString(), store.getAssetStorageClass(path));

    EasyMock.verify();
  }

  @Test
  public void testCopy() throws Exception {
    Upload upload = EasyMock.createStrictMock(Upload.class);
    upload.waitForCompletion();
    EasyMock.expectLastCall().once();
    EasyMock.replay(upload);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_1, sampleFile)).andReturn(upload);
    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_1)).andReturn(objMetadata);
    EasyMock.replay(s3Client, s3Transfer);

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

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_1, sampleFile)).andReturn(upload);
    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_1)).andReturn(objMetadata).anyTimes();
    EasyMock.expect(s3Client.generatePresignedUrl(EasyMock.anyObject(GeneratePresignedUrlRequest.class)))
            .andReturn(uri.toURL());
    EasyMock.replay(s3Object, s3Client, s3Transfer);

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

    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_1)).andReturn(objMetadata);
    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_2)).andReturn(objMetadata);
    s3Client.deleteObject(BUCKET_NAME, OBJECT_KEY_1);
    EasyMock.expectLastCall().once();
    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_1, sampleFile)).andReturn(upload)
            .once();
    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_2, sampleFile))
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

    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_1)).andReturn(objMetadata);
    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_2)).andReturn(objMetadata);
    s3Client.deleteObject(BUCKET_NAME, OBJECT_KEY_1);
    EasyMock.expectLastCall().once();
    s3Client.deleteObject(BUCKET_NAME, OBJECT_KEY_2);
    EasyMock.expectLastCall().once();

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_1, sampleFile)).andReturn(upload)
            .once();
    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_2, sampleFile))
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

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_1, sampleFile)).andReturn(upload)
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

    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_1)).andReturn(objMetadata);
    EasyMock.expect(s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY_2)).andReturn(objMetadata);

    EasyMock.expect(s3Transfer.upload(BUCKET_NAME, OBJECT_KEY_1, sampleFile)).andReturn(upload)
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

  @Test @Ignore
  public void testAssetRestore() throws Exception {
    //FIXME: Partially implemented, but the poll time is a constant...
    GetObjectTaggingResult gotr = EasyMock.createStrictMock(GetObjectTaggingResult.class);
    List<Tag> tags = List.of(new Tag("Freezable", "true"));
    EasyMock.expect(gotr.getTagSet()).andReturn(tags).once();
    ObjectMetadata metadata = EasyMock.createStrictMock(ObjectMetadata.class);
    EasyMock.expect(metadata.getVersionId()).andReturn(AWS_VERSION_1).anyTimes();
    EasyMock.expect(metadata.getStorageClass()).andReturn(StorageClass.Standard.toString()).times(1);
    EasyMock.expect(metadata.getOngoingRestore()).andReturn(false).once();
    EasyMock.expect(metadata.getRestoreExpirationTime()).andReturn(null).once();
    EasyMock.expect(metadata.getOngoingRestore()).andReturn(true).once();

    EasyMock.expect(s3Client.getObjectTagging(EasyMock.anyObject(GetObjectTaggingRequest.class)))
        .andReturn(gotr).once();
    //FIXME: Mock appropriate results, not that they're checked.
    EasyMock.expect(s3Client.copyObject(EasyMock.anyObject(CopyObjectRequest.class))).andReturn(null).anyTimes();
    //FIXME: Mock appropriate results, not that they're checked.
    EasyMock.expect(s3Client.restoreObjectV2(EasyMock.anyObject(RestoreObjectRequest.class))).andReturn(null).once();
    EasyMock.replay(gotr, metadata);

    setupUpload(OBJECT_KEY_1, metadata);
    EasyMock.replay(s3Client, s3Transfer);

    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    Source source = Source.mk(uri);
    store.put(path, source);

    store.modifyAssetStorageClass(path, StorageClass.Glacier.toString());

    //Picked by random dice roll, guaranteed to be random
    store.initiateRestoreAsset(path, 4);
    //RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, objectName, objectRestorePeriod);
    //s3.restoreObjectV2(requestRestore);
    //s3.getObjectMetadata(bucketName, objectName).getRestoreExpirationTime() -> null

    //Mid restore
    //String status = store.getAssetRestoreStatusString(path);
    //Restore is now finished
    //status = store.getAssetRestoreStatusString(path);

    EasyMock.verify(s3Client, metadata);
    Assert.fail("Not done");
  }

  @Test @Ignore
  public void testGlacierDirectAccess() throws Exception {
    setupUpload(OBJECT_KEY_1);
    EasyMock.replay(s3Client, s3Transfer);
    StoragePath path = new StoragePath(ORG_ID, MP_ID, new VersionImpl(1L), ASSET_ID);
    Source source = Source.mk(uri);
    store.put(path, source);
    store.modifyAssetStorageClass(path, StorageClass.Glacier.toString());
    Opt<InputStream> stream = store.get(path);
    Assert.fail("Not done yet");
  }
}
