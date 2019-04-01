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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.distribution.aws.s3

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workspace.api.Workspace

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.Upload
import com.google.gson.Gson

import org.easymock.EasyMock
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.net.URI
import java.util.LinkedHashSet
import java.util.LinkedList

class AwsS3DistributionServiceImplTest {
    private var s3: AmazonS3Client? = null
    private var tm: TransferManager? = null
    private var workspace: Workspace? = null
    private var serviceRegistry: ServiceRegistry? = null
    private val gson = Gson()

    private var service: AwsS3DistributionServiceImpl? = null

    private var mp: MediaPackage? = null
    private var distributedMp: MediaPackage? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Set up the service
        s3 = EasyMock.createNiceMock<AmazonS3Client>(AmazonS3Client::class.java)
        tm = EasyMock.createNiceMock<TransferManager>(TransferManager::class.java)
        // Replay will be called in each test

        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace!!.get(EasyMock.anyObject(URI::class.java))).andReturn(File("test"))
        EasyMock.replay(workspace!!)

        serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)

        service = AwsS3DistributionServiceImpl()
        service!!.serviceRegistry = serviceRegistry
        service!!.setBucketName(BUCKET_NAME)
        service!!.setOpencastDistributionUrl(DOWNLOAD_URL)
        service!!.setS3(s3)
        service!!.setS3TransferManager(tm)
        service!!.setWorkspace(workspace!!)

        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        var mpURI = AwsS3DistributionServiceImpl::class.java.getResource("/media_package.xml").toURI()
        mp = builder.loadFromXml(mpURI.toURL().openStream())

        mpURI = AwsS3DistributionServiceImpl::class.java.getResource("/media_package_distributed.xml").toURI()
        distributedMp = builder.loadFromXml(mpURI.toURL().openStream())
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    @Throws(Exception::class)
    fun testDistributeJob() {
        val mpeIds = LinkedHashSet<String>()
        mpeIds.add("presenter-delivery")

        val checkAvailability = true

        val args = LinkedList<String>()
        args.add("channelId")
        args.add(MediaPackageParser.getAsXml(mp))
        args.add(gson.toJson(mpeIds))
        args.add(java.lang.Boolean.toString(checkAvailability))

        EasyMock.expect<Job>(
                serviceRegistry!!.createJob(
                        AwsS3DistributionServiceImpl.JOB_TYPE,
                        AwsS3DistributionServiceImpl.Operation.Distribute.toString(),
                        args, AwsS3DistributionServiceImpl.DEFAULT_DISTRIBUTE_JOB_LOAD
                )).andReturn(null).once()
        EasyMock.replay(serviceRegistry!!)

        service!!.distribute("channelId", mp!!, "presenter-delivery")

        EasyMock.verify(serviceRegistry!!)
    }

    @Test
    @Throws(Exception::class)
    fun testDistributeMultipleJob() {
        val mpeIds = LinkedHashSet<String>()
        mpeIds.add("presenter-delivery")

        val checkAvailability = false

        val args = LinkedList<String>()
        args.add("channelId")
        args.add(MediaPackageParser.getAsXml(mp))
        args.add(gson.toJson(mpeIds))
        args.add(java.lang.Boolean.toString(checkAvailability))

        EasyMock.expect<Job>(
                serviceRegistry!!.createJob(
                        AwsS3DistributionServiceImpl.JOB_TYPE,
                        AwsS3DistributionServiceImpl.Operation.Distribute.toString(),
                        args, AwsS3DistributionServiceImpl.DEFAULT_DISTRIBUTE_JOB_LOAD
                )).andReturn(null).once()
        EasyMock.replay(serviceRegistry!!)

        service!!.distribute("channelId", mp!!, mpeIds, checkAvailability)

        EasyMock.verify(serviceRegistry!!)
    }

    @Test
    @Throws(Exception::class)
    fun testRetractMultipleJob() {

        val mpeIds = LinkedHashSet<String>()
        mpeIds.add("presenter-delivery")

        val checkAvailability = false

        val args = LinkedList<String>()
        args.add("channelId")
        args.add(MediaPackageParser.getAsXml(mp))
        args.add(gson.toJson(mpeIds))

        EasyMock.expect<Job>(
                serviceRegistry!!.createJob(
                        AwsS3DistributionServiceImpl.JOB_TYPE,
                        AwsS3DistributionServiceImpl.Operation.Retract.toString(),
                        args, AwsS3DistributionServiceImpl.DEFAULT_RETRACT_JOB_LOAD
                )).andReturn(null).once()
        EasyMock.replay(serviceRegistry!!)

        service!!.retract("channelId", mp!!, mpeIds)

        EasyMock.verify(serviceRegistry!!)
    }

    @Test
    @Throws(Exception::class)
    fun testRetractJob() {

        val mpeIds = LinkedHashSet<String>()
        mpeIds.add("presenter-delivery")

        val args = LinkedList<String>()
        args.add("channelId")
        args.add(MediaPackageParser.getAsXml(mp))
        args.add(gson.toJson(mpeIds))

        EasyMock.expect<Job>(
                serviceRegistry!!.createJob(
                        AwsS3DistributionServiceImpl.JOB_TYPE,
                        AwsS3DistributionServiceImpl.Operation.Retract.toString(),
                        args, AwsS3DistributionServiceImpl.DEFAULT_RETRACT_JOB_LOAD
                )).andReturn(null).once()
        EasyMock.replay(serviceRegistry!!)

        service!!.retract("channelId", mp!!, "presenter-delivery")

        EasyMock.verify(serviceRegistry!!)
    }

    @Test
    @Throws(Exception::class)
    fun testDistributeElement() {
        val upload = EasyMock.createNiceMock<Upload>(Upload::class.java)
        EasyMock.expect(
                tm!!.upload(EasyMock.anyObject(String::class.java), EasyMock.anyObject(String::class.java),
                        EasyMock.anyObject(File::class.java))).andReturn(
                upload)
        EasyMock.replay(upload, tm)

        val mpeIds = LinkedHashSet<String>()
        mpeIds.add("presenter-delivery")

        val mpes = service!!.distributeElements("channelId", mp, mpeIds, false)
        val mpe = mpes[0]

        Assert.assertEquals(URI(
                "http://XYZ.cloudfront.net/channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4"),
                mpe.getURI())
    }

    @Test
    @Throws(Exception::class)
    fun testRetractElements() {
        s3!!.deleteObject(BUCKET_NAME,
                "channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4")
        EasyMock.expectLastCall<Any>().once()
        EasyMock.replay(s3!!)

        val mpeIds = LinkedHashSet<String>()
        mpeIds.add("presenter-delivery-distributed")

        service!!.retractElements("channelId", distributedMp, mpeIds)
        EasyMock.verify(s3!!)
    }

    @Test
    fun testBuildObjectName() {
        val element = mp!!.getElementById("presenter-delivery")
        Assert.assertEquals(
                "channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4",
                service!!.buildObjectName("channelId", mp!!.identifier.toString(), element))
    }

    @Test
    @Throws(Exception::class)
    fun testGetDistributionUri() {
        Assert.assertEquals(URI(
                "http://XYZ.cloudfront.net/channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4"),
                service!!.getDistributionUri("channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4"))
    }

    @Test
    fun testGetDistributedObjectName() {
        val element = distributedMp!!.getElementById("presenter-delivery-distributed")
        Assert.assertEquals(
                "channelId/efd6e4df-63b6-49af-be5f-15f598778877/presenter-delivery/video-presenter-delivery.mp4",
                service!!.getDistributedObjectName(element))
    }

    @Test
    fun testCreateAWSBucket() {
        // Bucket does not exist
        val e = EasyMock.createNiceMock<AmazonServiceException>(AmazonServiceException::class.java)
        EasyMock.expect(e.statusCode).andReturn(404)
        EasyMock.replay(e)

        EasyMock.expect(s3!!.listObjects(BUCKET_NAME)).andThrow(e).once()
        EasyMock.expect(s3!!.createBucket(BUCKET_NAME)).andReturn(EasyMock.anyObject(Bucket::class.java)).once()
        s3!!.setBucketPolicy(BUCKET_NAME, EasyMock.anyObject(String::class.java))
        EasyMock.expectLastCall<Any>().once()
        EasyMock.replay(s3!!)

        service!!.createAWSBucket()
        // Verify that all expected methods were called
        EasyMock.verify(s3!!)
    }

    @Test
    fun testCreateAWSBucketWhenAlreadyExists() {
        // Bucket exists
        val list = EasyMock.createNiceMock<ObjectListing>(ObjectListing::class.java)
        EasyMock.replay(list)
        EasyMock.expect(s3!!.listObjects(BUCKET_NAME)).andReturn(list).once()
        EasyMock.replay(s3!!)

        service!!.createAWSBucket()
        // Verify that all expected methods were called
        EasyMock.verify(s3!!)
    }

    companion object {

        private val BUCKET_NAME = "aws-bucket"
        private val DOWNLOAD_URL = "http://XYZ.cloudfront.net/"
    }

}
