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
package org.opencastproject.distribution.aws.s3;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.FileSupport;
import org.opencastproject.workspace.api.Workspace;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AwsS3DistributionServiceImplTest {
  private AmazonS3Client s3;
  private TransferManager tm;
  private Workspace workspace;
  private ServiceRegistry serviceRegistry;
  private Gson gson = new Gson();

  private static final String BUCKET_NAME = "aws-bucket";
  private static final String DOWNLOAD_URL = "http://XYZ.cloudfront.net/";

  private AwsS3DistributionServiceImpl service = null;

  private MediaPackage mp = null;
  private MediaPackage distributedMp = null;
  private File storageDir = null;

  @Before
  public void setUp() throws Exception {
    // Set up the service
    s3 = EasyMock.createNiceMock(AmazonS3Client.class);
    tm = EasyMock.createNiceMock(TransferManager.class);
    // Replay will be called in each test
    File baseDir = FileSupport.getTempDirectory("s3distribution");
    File srcFile = new File(baseDir, "presenter-m3u8/video-presenter-delivery.m3u8");
    FileUtils.copyURLToFile(this.getClass().getResource("/video-presenter-delivery.m3u8"), srcFile);
    srcFile = new File(baseDir, "presenter-mp4/video-presenter-delivery.mp4");
    FileUtils.copyURLToFile(this.getClass().getResource("/video-presenter-delivery.mp4"), srcFile);
    srcFile = new File(baseDir, "video-presenter-delivery.mp4");
    FileUtils.copyURLToFile(this.getClass().getResource("/video-presenter-delivery.mp4"), srcFile);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        String name;
        URI uri = (URI) EasyMock.getCurrentArguments()[0];
        name = uri.getPath();
        return new File(baseDir, name);
      }
    }).anyTimes();
    EasyMock.replay(workspace);

    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);

    service = new AwsS3DistributionServiceImpl();
    service.setServiceRegistry(serviceRegistry);
    service.setBucketName(BUCKET_NAME);
    service.setOpencastDistributionUrl(DOWNLOAD_URL);
    service.setS3(s3);
    service.setS3TransferManager(tm);
    service.setStorageTmp(baseDir.getAbsolutePath());
    service.setWorkspace(workspace);

    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    storageDir = new File(baseDir.getAbsolutePath() + AwsS3DistributionServiceImpl.DEFAULT_TEMP_DIR);

    URI mpURI = AwsS3DistributionServiceImpl.class.getResource("/mediapackage.xml").toURI();
    mp = builder.loadFromXml(mpURI.toURL().openStream());

    mpURI = AwsS3DistributionServiceImpl.class.getResource("/mediapackage_distributed.xml").toURI();
    distributedMp = builder.loadFromXml(mpURI.toURL().openStream());
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testDistributeJob() throws Exception {
    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-delivery");

    boolean checkAvailability = true;

    List<String> args = new LinkedList<String>();
    args.add("channelId");
    args.add(MediaPackageParser.getAsXml(mp));
    args.add(gson.toJson(mpeIds));
    args.add(Boolean.toString(checkAvailability));

    EasyMock.expect(
            serviceRegistry.createJob(
                    AwsS3DistributionServiceImpl.JOB_TYPE,
                    AwsS3DistributionServiceImpl.Operation.Distribute.toString(),
                    args, AwsS3DistributionServiceImpl.DEFAULT_DISTRIBUTE_JOB_LOAD
            )).andReturn(null).once();
    EasyMock.replay(serviceRegistry);

    service.distribute("channelId", mp, "presenter-delivery");

    EasyMock.verify(serviceRegistry);
  }

  @Test
  public void testDistributeMultipleJob() throws Exception {
    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-delivery");

    boolean checkAvailability = false;

    List<String> args = new LinkedList<String>();
    args.add("channelId");
    args.add(MediaPackageParser.getAsXml(mp));
    args.add(gson.toJson(mpeIds));
    args.add(Boolean.toString(checkAvailability));

    EasyMock.expect(
            serviceRegistry.createJob(
                    AwsS3DistributionServiceImpl.JOB_TYPE,
                    AwsS3DistributionServiceImpl.Operation.Distribute.toString(),
                    args, AwsS3DistributionServiceImpl.DEFAULT_DISTRIBUTE_JOB_LOAD
                    )).andReturn(null).once();
    EasyMock.replay(serviceRegistry);

    service.distribute("channelId", mp, mpeIds, checkAvailability);

    EasyMock.verify(serviceRegistry);
  }

  @Test
  public void testRetractMultipleJob() throws Exception {

    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-delivery");

    boolean checkAvailability = false;

    List<String> args = new LinkedList<String>();
    args.add("channelId");
    args.add(MediaPackageParser.getAsXml(mp));
    args.add(gson.toJson(mpeIds));

    EasyMock.expect(
            serviceRegistry.createJob(
                    AwsS3DistributionServiceImpl.JOB_TYPE,
                    AwsS3DistributionServiceImpl.Operation.Retract.toString(),
                    args, AwsS3DistributionServiceImpl.DEFAULT_RETRACT_JOB_LOAD
            )).andReturn(null).once();
    EasyMock.replay(serviceRegistry);

    service.retract("channelId", mp, mpeIds);

    EasyMock.verify(serviceRegistry);
  }

  @Test
  public void testRetractJob() throws Exception {

    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-delivery");

    List<String> args = new LinkedList<String>();
    args.add("channelId");
    args.add(MediaPackageParser.getAsXml(mp));
    args.add(gson.toJson(mpeIds));

    EasyMock.expect(
            serviceRegistry.createJob(
                    AwsS3DistributionServiceImpl.JOB_TYPE,
                    AwsS3DistributionServiceImpl.Operation.Retract.toString(),
                    args, AwsS3DistributionServiceImpl.DEFAULT_RETRACT_JOB_LOAD
            )).andReturn(null).once();
    EasyMock.replay(serviceRegistry);

    service.retract("channelId", mp, "presenter-delivery");

    EasyMock.verify(serviceRegistry);
  }

  @Test
  public void testDistributeElement() throws Exception {
    Upload upload = EasyMock.createNiceMock(Upload.class);
    EasyMock.expect(
            tm.upload(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
                    EasyMock.anyObject(File.class))).andReturn(
            upload);
    EasyMock.replay(upload, tm);

    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-delivery");

    MediaPackageElement[] mpes = service.distributeElements("channelId", mp, mpeIds, false);
    MediaPackageElement mpe = mpes[0];

    Assert.assertEquals(new URI(
                    "http://XYZ.cloudfront.net/channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4"),
            mpe.getURI());
  }

  @Test
  public void testRetractElements() throws Exception {
    s3.deleteObject(BUCKET_NAME,
            "channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4");
    EasyMock.expectLastCall().once();
    EasyMock.replay(s3);

    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-delivery-distributed");

    service.retractElements("channelId", distributedMp, mpeIds);
    EasyMock.verify(s3);
  }

  @Test
  public void testDistributeHLSElement() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    URI mpURI = AwsS3DistributionServiceImpl.class.getResource("/hls_mediapackage.xml").toURI();
    mp = builder.loadFromXml(mpURI.toURL().openStream());

    mpURI = AwsS3DistributionServiceImpl.class.getResource("/distributed_hls_mediapackage.xml").toURI();
    distributedMp = builder.loadFromXml(mpURI.toURL().openStream());
    Upload upload = EasyMock.createNiceMock(Upload.class);
    EasyMock.expect(tm.upload(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
            EasyMock.anyObject(File.class))).andReturn(upload).anyTimes();
    EasyMock.replay(upload, tm);

    Set<String> mpeIds = new LinkedHashSet<String>();
    mpeIds.add("presenter-mp4");
    mpeIds.add("presenter-m3u8");

    MediaPackageElement[] mpes = service.distributeElements("channelId", mp, mpeIds, false);
    MediaPackageElement mpe = mpes[0];

    Assert.assertEquals(new URI(
            "http://XYZ.cloudfront.net/channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-mp4/video-presenter-delivery.mp4"),
            mpe.getURI());
    // Test that temp directory is removed
    Path tempfile = storageDir.toPath().resolve(mp.getIdentifier().toString());
    Assert.assertFalse(Files.exists(tempfile));
  }

  @Test
  public void testBuildObjectName() {
    MediaPackageElement element = mp.getElementById("presenter-delivery");
    Assert.assertEquals(
            "channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4",
            service.buildObjectName("channelId", mp.getIdentifier().toString(), element));
  }

  @Test
  public void testGetDistributionUri() throws Exception {
    Assert.assertEquals(new URI(
                    "http://XYZ.cloudfront.net/channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4"),
            service.getDistributionUri("channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4"));
  }

  @Test
  public void testGetDistributedObjectName() {
    MediaPackageElement element = distributedMp.getElementById("presenter-delivery-distributed");
    Assert.assertEquals(
            "channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4",
            service.getDistributedObjectName(element));
  }

  @Test
  public void testCreateAWSBucket() {
    // Bucket does not exist
    AmazonServiceException e = EasyMock.createNiceMock(AmazonServiceException.class);
    EasyMock.expect(e.getStatusCode()).andReturn(404);
    EasyMock.replay(e);

    EasyMock.expect(s3.listObjects(BUCKET_NAME)).andThrow(e).once();
    EasyMock.expect(s3.createBucket(BUCKET_NAME)).andReturn(EasyMock.anyObject(Bucket.class)).once();
    s3.setBucketPolicy(BUCKET_NAME, EasyMock.anyObject(String.class));
    EasyMock.expectLastCall().once();
    EasyMock.replay(s3);

    service.createAWSBucket();
    // Verify that all expected methods were called
    EasyMock.verify(s3);
  }

  @Test
  public void testCreateAWSBucketWhenAlreadyExists() {
    // Bucket exists
    ObjectListing list = EasyMock.createNiceMock(ObjectListing.class);
    EasyMock.replay(list);
    EasyMock.expect(s3.listObjects(BUCKET_NAME)).andReturn(list).once();
    EasyMock.replay(s3);

    service.createAWSBucket();
    // Verify that all expected methods were called
    EasyMock.verify(s3);
  }

}
