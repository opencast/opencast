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

import java.lang.String.format
import org.opencastproject.util.RequireUtil.notNull

import org.opencastproject.distribution.api.AbstractDistributionService
import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DistributionService
import org.opencastproject.distribution.aws.s3.api.AwsS3DistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.LoadUtil
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.OsgiUtil
import org.opencastproject.util.data.Option

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.policy.Policy
import com.amazonaws.auth.policy.Principal
import com.amazonaws.auth.policy.Statement
import com.amazonaws.auth.policy.Statement.Effect
import com.amazonaws.auth.policy.actions.S3Actions
import com.amazonaws.auth.policy.resources.S3ObjectResource
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteVersionRequest
import com.amazonaws.services.s3.model.GetObjectMetadataRequest
import com.amazonaws.services.s3.model.ListVersionsRequest
import com.amazonaws.services.s3.model.VersionListing
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.Upload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

import javax.servlet.http.HttpServletResponse

/**
 * Creates a new instance of the AWS S3 distribution service.
 */
class AwsS3DistributionServiceImpl : AbstractDistributionService(JOB_TYPE), AwsS3DistributionService, DistributionService {

    /** The load on the system introduced by creating a distribute job  */
    private var distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD

    /** The load on the system introduced by creating a retract job  */
    private var retractJobLoad = DEFAULT_RETRACT_JOB_LOAD

    /** The load on the system introduced by creating a restore job  */
    private var restoreJobLoad = DEFAULT_RESTORE_JOB_LOAD

    /** The AWS client and transfer manager  */
    private var s3: AmazonS3? = null
    private var s3TransferManager: TransferManager? = null

    /** The AWS S3 bucket name  */
    private var bucketName: String? = null

    /** The opencast download distribution url  */
    private var opencastDistributionUrl: String? = null

    private val gson = Gson()

    override val distributionType: String
        get() = distributionChannel

    /** List of available operations on jobs  */
    enum class Operation {
        Distribute, Retract, Restore
    }

    private fun getAWSConfigKey(cc: ComponentContext, key: String): String {
        try {
            return OsgiUtil.getComponentContextProperty(cc, key)
        } catch (e: RuntimeException) {
            throw ConfigurationException("$key is missing or invalid", e)
        }

    }

    override fun activate(cc: ComponentContext) {

        // Get the configuration
        if (cc != null) {

            if (!java.lang.Boolean.valueOf(getAWSConfigKey(cc, AWS_S3_DISTRIBUTION_ENABLE))) {
                logger.info("AWS S3 distribution disabled")
                return
            }

            // AWS S3 bucket name
            bucketName = getAWSConfigKey(cc, AWS_S3_BUCKET_CONFIG)
            logger.info("AWS S3 bucket name is {}", bucketName)

            // AWS region
            val regionStr = getAWSConfigKey(cc, AWS_S3_REGION_CONFIG)
            logger.info("AWS region is {}", regionStr)

            opencastDistributionUrl = getAWSConfigKey(cc, AWS_S3_DISTRIBUTION_BASE_CONFIG)
            if (!opencastDistributionUrl!!.endsWith("/")) {
                opencastDistributionUrl = opencastDistributionUrl!! + "/"
            }
            logger.info("AWS distribution url is {}", opencastDistributionUrl)

            distributeJobLoad = LoadUtil.getConfiguredLoadValue(cc.properties, DISTRIBUTE_JOB_LOAD_KEY,
                    DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry!!)
            retractJobLoad = LoadUtil.getConfiguredLoadValue(cc.properties, RETRACT_JOB_LOAD_KEY,
                    DEFAULT_RETRACT_JOB_LOAD, serviceRegistry!!)
            restoreJobLoad = LoadUtil.getConfiguredLoadValue(cc.properties, RESTORE_JOB_LOAD_KEY,
                    DEFAULT_RESTORE_JOB_LOAD, serviceRegistry!!)

            // Explicit credentials are optional.
            var provider: AWSCredentialsProvider? = null
            val accessKeyIdOpt = OsgiUtil.getOptCfg(cc.properties, AWS_S3_ACCESS_KEY_ID_CONFIG)
            val accessKeySecretOpt = OsgiUtil.getOptCfg(cc.properties, AWS_S3_SECRET_ACCESS_KEY_CONFIG)

            // Keys not informed so use default credentials provider chain, which
            // will look at the environment variables, java system props, credential files, and instance
            // profile credentials
            if (accessKeyIdOpt.isNone && accessKeySecretOpt.isNone)
                provider = DefaultAWSCredentialsProviderChain()
            else
                provider = AWSStaticCredentialsProvider(
                        BasicAWSCredentials(accessKeyIdOpt.get(), accessKeySecretOpt.get()))

            // Create AWS client.
            s3 = AmazonS3ClientBuilder.standard().withRegion(regionStr).withCredentials(provider).build()

            s3TransferManager = TransferManager(s3)

            // Create AWS S3 bucket if not there yet
            createAWSBucket()
            distributionChannel = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE)

            logger.info("AwsS3DistributionService activated!")
        }
    }

    fun deactivate() {
        // Transfer manager is null if service disabled
        if (s3TransferManager != null)
            s3TransferManager!!.shutdownNow()

        logger.info("AwsS3DistributionService deactivated!")
    }

    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(pubChannelId: String, mediaPackage: MediaPackage, downloadIds: Set<String>,
                            checkAvailability: Boolean, preserveReference: Boolean): Job {
        throw UnsupportedOperationException("Not supported yet.")
        //stub function
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.DownloadDistributionService.distribute
     */
    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>, checkAvailability: Boolean): Job {
        notNull(mediaPackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Distribute.toString(), Arrays.asList(channelId,
                    MediaPackageParser.getAsXml(mediaPackage), gson.toJson(elementIds), java.lang.Boolean.toString(checkAvailability)),
                    distributeJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.DistributionService.distribute
     */
    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediapackage: MediaPackage, elementId: String): Job {
        return distribute(channelId, mediapackage, elementId, true)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.distribution.api.DownloadDistributionService.distribute
     */
    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediaPackage: MediaPackage, elementId: String, checkAvailability: Boolean): Job {
        val elementIds = HashSet<String>()
        elementIds.add(elementId)
        return distribute(channelId, mediaPackage, elementIds, checkAvailability)
    }

    /**
     * Distribute Mediapackage elements to the download distribution service.
     *
     * @param channelId
     * # The id of the publication channel to be distributed to.
     * @param mediapackage
     * The media package that contains the elements to be distributed.
     * @param elementIds
     * The ids of the elements that should be distributed contained within the media package.
     * @param checkAvailability
     * Check the availability of the distributed element via http.
     * @return A reference to the MediaPackageElements that have been distributed.
     * @throws DistributionException
     * Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
     * cannot be copied or another unexpected exception occurs.
     */
    @Throws(DistributionException::class)
    fun distributeElements(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>?,
                           checkAvailability: Boolean): Array<MediaPackageElement> {
        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")

        val elements = getElements(mediapackage, elementIds!!)
        val distributedElements = ArrayList<MediaPackageElement>()

        for (element in elements) {
            val distributedElement = distributeElement(channelId, mediapackage, element, checkAvailability)
            distributedElements.add(distributedElement)
        }
        return distributedElements.toTypedArray()
    }

    @Throws(IllegalStateException::class)
    private fun getElements(mediapackage: MediaPackage, elementIds: Set<String>): Set<MediaPackageElement> {
        val elements = HashSet<MediaPackageElement>()
        for (elementId in elementIds) {
            val element = mediapackage.getElementById(elementId)
            if (element != null) {
                elements.add(element)
            } else {
                throw IllegalStateException(
                        format("No element %s found in mediapackage %s", elementId, mediapackage.identifier))
            }
        }
        return elements
    }

    /**
     * Distribute a media package element to AWS S3.
     *
     * @param mediaPackage
     * The media package that contains the element to distribute.
     * @param element
     * The element that should be distributed contained within the media package.
     * @param checkAvailability
     * Checks if the distributed element is available
     * @return A reference to the MediaPackageElement that has been distributed.
     * @throws DistributionException
     */
    @Throws(DistributionException::class)
    fun distributeElement(channelId: String, mediaPackage: MediaPackage,
                          element: MediaPackageElement, checkAvailability: Boolean): MediaPackageElement {
        notNull(channelId, "channelId")
        notNull(mediaPackage, "mediapackage")
        notNull(element, "element")

        try {
            val source: File
            try {
                source = workspace!!.get(element.getURI())
            } catch (e: NotFoundException) {
                throw DistributionException("Unable to find " + element.getURI() + " in the workspace", e)
            } catch (e: IOException) {
                throw DistributionException("Error loading " + element.getURI() + " from the workspace", e)
            }

            // Use TransferManager to take advantage of multipart upload.
            // TransferManager processes all transfers asynchronously, so this call will return immediately.
            val objectName = buildObjectName(channelId, mediaPackage.identifier.toString(), element)
            logger.info("Uploading {} to bucket {}...", objectName, bucketName)
            val upload = s3TransferManager!!.upload(bucketName, objectName, source)
            val start = System.currentTimeMillis()

            try {
                // Block and wait for the upload to finish
                upload.waitForCompletion()
                logger.info("Upload of {} to bucket {} completed in {} seconds", objectName, bucketName,
                        (System.currentTimeMillis() - start) / 1000)
            } catch (e: AmazonClientException) {
                throw DistributionException("AWS error: " + e.message, e)
            }

            // Create a representation of the distributed file in the media package
            val distributedElement = element.clone() as MediaPackageElement
            try {
                distributedElement.setURI(getDistributionUri(objectName))
            } catch (e: URISyntaxException) {
                throw DistributionException("Distributed element produces an invalid URI", e)
            }

            logger.info("Distributed element {}, object {}", element.identifier, objectName)

            if (checkAvailability) {
                val uri = distributedElement.getURI()
                var tries = 0
                var response: CloseableHttpResponse? = null
                var success = false
                while (tries < MAX_TRIES) {
                    try {
                        val httpClient = HttpClients.createDefault()
                        logger.trace("Trying to access {}", uri)
                        response = httpClient.execute(HttpHead(uri))
                        if (response!!.statusLine.statusCode == HttpServletResponse.SC_OK) {
                            logger.trace("Successfully got {}", uri)
                            success = true
                            break // Exit the loop, response is closed
                        } else {
                            logger.debug("Http status code when checking distributed element {} is {}", objectName,
                                    response.statusLine.statusCode)
                        }
                    } catch (e: Exception) {
                        logger.info("Checking availability of {} threw exception {}. Trying again.", objectName, e.message)
                        // Just try again
                    } finally {
                        response?.close()
                    }
                    tries++
                    logger.trace("Sleeping for {} seconds...", SLEEP_INTERVAL / 1000)
                    Thread.sleep(SLEEP_INTERVAL)
                }
                if (!success) {
                    logger.warn("Could not check availability of distributed file {}", uri)
                    // throw new DistributionException("Unable to load distributed file " + uri.toString());
                }
            }

            return distributedElement
        } catch (e: Exception) {
            logger.warn("Error distributing element " + element.identifier + " of media package " + mediaPackage, e)
            if (e is DistributionException) {
                throw e
            } else {
                throw DistributionException(e)
            }
        }

    }

    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediapackage: MediaPackage, elementId: String): Job {
        val elementIds = HashSet<String>()
        elementIds.add(elementId)
        return retract(channelId, mediapackage, elementIds)
    }

    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>): Job {
        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Retract.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds)),
                    retractJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    @Throws(DistributionException::class)
    override fun distributeSync(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>,
                                checkAvailability: Boolean): List<MediaPackageElement> {
        val distributedElements = distributeElements(channelId, mediapackage, elementIds, checkAvailability)
                ?: return null
        return Arrays.asList(*distributedElements)
    }

    @Throws(DistributionException::class)
    override fun retractSync(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement> {
        val retractedElements = retractElements(channelId, mediaPackage, elementIds) ?: return null
        return Arrays.asList(*retractedElements)
    }

    /**
     * Retracts the media package element with the given identifier from the distribution channel.
     *
     * @param channelId
     * the channel id
     * @param mediaPackage
     * the media package
     * @param element
     * the element
     * @return the retracted element or `null` if the element was not retracted
     */
    @Throws(DistributionException::class)
    protected fun retractElement(channelId: String, mediaPackage: MediaPackage, element: MediaPackageElement): MediaPackageElement {
        notNull(mediaPackage, "mediaPackage")
        notNull(element, "element")

        try {
            val objectName = getDistributedObjectName(element)
            if (objectName != null) {
                s3!!.deleteObject(bucketName, objectName)
                logger.info("Retracted element {}, object {}", element.identifier, objectName)
            }
            return element
        } catch (e: AmazonClientException) {
            throw DistributionException("AWS error: " + e.message, e)
        }

    }

    /**
     * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
     * one given as parameter `elementId`. Instead, the element's distribution URI will be calculated. This way
     * you are able to retract elements by providing the "original" element here.
     *
     * @param channelId
     * the channel id
     * @param mediapackage
     * the mediapackage
     * @param elementIds
     * the element identifiers
     * @return the retracted element or `null` if the element was not retracted
     * @throws org.opencastproject.distribution.api.DistributionException
     * in case of an error
     */
    @Throws(DistributionException::class)
    fun retractElements(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>?): Array<MediaPackageElement> {
        notNull(mediapackage, "mediapackage")
        notNull(elementIds, "elementIds")
        notNull(channelId, "channelId")

        val elements = getElements(mediapackage, elementIds!!)
        val retractedElements = ArrayList<MediaPackageElement>()

        for (element in elements) {
            val retractedElement = retractElement(channelId, mediapackage, element)
            retractedElements.add(retractedElement)
        }
        return retractedElements.toTypedArray()
    }

    @Throws(DistributionException::class)
    override fun restore(channelId: String?, mediaPackage: MediaPackage?, elementId: String?): Job {
        if (mediaPackage == null)
            throw IllegalArgumentException("Media package must be specified")
        if (elementId == null)
            throw IllegalArgumentException("Element ID must be specified")
        if (channelId == null)
            throw IllegalArgumentException("Channel ID must be specified")

        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Restore.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), elementId), restoreJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    @Throws(DistributionException::class)
    override fun restore(channelId: String?, mediaPackage: MediaPackage?, elementId: String?, fileName: String?): Job {
        if (mediaPackage == null)
            throw IllegalArgumentException("Media package must be specified")
        if (elementId == null)
            throw IllegalArgumentException("Element ID must be specified")
        if (channelId == null)
            throw IllegalArgumentException("Channel ID must be specified")
        if (fileName == null)
            throw IllegalArgumentException("Filename must be specified")

        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Restore.toString(),
                    Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), elementId, fileName), restoreJobLoad)
        } catch (e: ServiceRegistryException) {
            throw DistributionException("Unable to create a job", e)
        }

    }

    @Throws(DistributionException::class)
    protected fun restoreElement(channelId: String, mediaPackage: MediaPackage, elementId: String,
                                 fileName: String): MediaPackageElement {
        var objectName: String? = null
        if (StringUtils.isNotBlank(fileName)) {
            objectName = buildObjectName(channelId, mediaPackage.identifier.toString(), elementId, fileName)
        } else {
            objectName = buildObjectName(channelId, mediaPackage.identifier.toString(),
                    mediaPackage.getElementById(elementId))
        }
        // Get the latest version of the file
        // Note that this should be the delete marker for the file. We'll check, but if there is more than one delete marker
        // we'll have probs
        val lv = ListVersionsRequest().withBucketName(bucketName).withPrefix(objectName)
                .withMaxResults(1)
        val listing = s3!!.listVersions(lv)
        if (listing.versionSummaries.size < 1) {
            throw DistributionException("Object not found: $objectName")
        }
        val versionId = listing.versionSummaries[0].versionId
        // Verify that this is in fact a delete marker
        val metadata = GetObjectMetadataRequest(bucketName, objectName, versionId)
        // Ok, so there's no way of asking AWS directly if the object is deleted in this version of the SDK
        // So instead, we ask for its metadata
        // If it's deleted, then there *isn't* any metadata and we get a 404, which throws the exception
        // This, imo, is an incredibly boneheaded omission from the AWS SDK, and implies we should look for something which
        // sucks less
        // FIXME: This section should be refactored with a simple s3.doesObjectExist(bucketName, objectName) once we update
        // the AWS SDK
        var isDeleted = false
        try {
            s3!!.getObjectMetadata(metadata)
        } catch (e: AmazonServiceException) {
            // Note: This exception is actually a 405, not a 404.
            // This is expected, but very confusing if you're thinking it should be a 'file not found', rather than a 'method
            // not allowed on stuff that's deleted'
            // It's unclear what the expected behaviour is for things which have never existed...
            isDeleted = true
        }

        if (isDeleted) {
            // Delete the delete marker
            val delete = DeleteVersionRequest(bucketName, objectName, versionId)
            s3!!.deleteVersion(delete)
        }
        return mediaPackage.getElementById(elementId)
    }

    /**
     * Builds the aws s3 object name.
     *
     * @param channelId
     * @param mpId
     * @param element
     * @return
     */
    fun buildObjectName(channelId: String, mpId: String, element: MediaPackageElement): String {
        // Something like CHANNEL_ID/MP_ID/ELEMENT_ID/FILE_NAME.EXTENSION
        val uriString = element.getURI().toString()
        val fileName = FilenameUtils.getName(uriString)
        return buildObjectName(channelId, mpId, element.identifier, fileName)
    }

    /**
     * Builds the aws s3 object name using the raw elementID and filename
     *
     * @param channelId
     * @param mpId
     * @param elementId
     * @param fileName
     * @return
     */
    protected fun buildObjectName(channelId: String, mpId: String, elementId: String, fileName: String): String {
        return StringUtils.join(arrayOf(channelId, mpId, elementId, fileName), "/")
    }

    /**
     * Gets the URI for the element to be distributed.
     *
     * @return The resulting URI after distribution
     * @throws URISyntaxException
     * if the concrete implementation tries to create a malformed uri
     */
    @Throws(URISyntaxException::class)
    fun getDistributionUri(objectName: String): URI {
        // Something like https://OPENCAST_DOWNLOAD_URL/CHANNEL_ID/MP_ID/ELEMENT_ID/FILE_NAME.EXTENSION
        return URI(opencastDistributionUrl!! + objectName)
    }

    /**
     * Gets the distributed object's name.
     *
     * @return The distributed object name
     */
    fun getDistributedObjectName(element: MediaPackageElement): String? {
        // Something like https://OPENCAST_DOWNLOAD_URL/CHANNEL_ID/MP_ID/ORIGINAL_ELEMENT_ID/FILE_NAME.EXTENSION
        val uriString = element.getURI().toString()

        // String directoryName = distributionDirectory.getAbsolutePath();
        if (uriString.startsWith(opencastDistributionUrl!!) && uriString.length > opencastDistributionUrl!!.length) {
            return uriString.substring(opencastDistributionUrl!!.length)
        } else {
            // Cannot retract
            logger.warn(
                    "Cannot retract {}. Uri must be in the format https://host/bucketName/channelId/mpId/originalElementId/fileName.extension",
                    uriString)
            return null
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.process
     */
    @Throws(Exception::class)
    override fun process(job: Job): String {
        var op: Operation? = null
        val operation = job.operation
        val arguments = job.arguments
        try {
            op = Operation.valueOf(operation)
            val channelId = arguments[0]
            val mediaPackage = MediaPackageParser.getFromXml(arguments[1])
            val elementIds = gson.fromJson<Set<String>>(arguments[2], object : TypeToken<Set<String>>() {

            }.type)
            when (op) {
                AwsS3DistributionServiceImpl.Operation.Distribute -> {
                    val checkAvailability = java.lang.Boolean.parseBoolean(arguments[3])
                    val distributedElements = distributeElements(channelId, mediaPackage, elementIds,
                            checkAvailability)
                    return if (distributedElements != null)
                        MediaPackageElementParser.getArrayAsXml(Arrays.asList(*distributedElements))
                    else
                        null
                }
                AwsS3DistributionServiceImpl.Operation.Retract -> {
                    val retractedElements = retractElements(channelId, mediaPackage, elementIds)
                    return if (retractedElements != null)
                        MediaPackageElementParser.getArrayAsXml(Arrays.asList(*retractedElements))
                    else
                        null
                }
                /*
         * TODO
         * Commented out due to changes in the way the element IDs are passed (ie, a list rather than individual ones
         * per job). This code is still useful long term, but I don't have time to write the necessary wrapper code
         * around it right now.
         * case Restore:
         * String fileName = arguments.get(3);
         * MediaPackageElement restoredElement = null;
         * if (StringUtils.isNotBlank(fileName)) {
         * restoredElement = restoreElement(channelId, mediaPackage, elementIds, fileName);
         * } else {
         * restoredElement = restoreElement(channelId, mediaPackage, elementIds, null);
         * }
         * return (restoredElement != null) ? MediaPackageElementParser.getAsXml(restoredElement) : null;
         */
                else -> throw IllegalStateException("Don't know how to handle operation '$operation'")
            }
        } catch (e: IllegalArgumentException) {
            throw ServiceRegistryException("This service can't handle operations of type '$op'", e)
        } catch (e: IndexOutOfBoundsException) {
            throw ServiceRegistryException("This argument list for operation '$op' does not meet expectations", e)
        } catch (e: Exception) {
            throw ServiceRegistryException("Error handling operation '$op'", e)
        }

    }

    /**
     * Creates the AWS S3 bucket if it doesn't exist yet.
     */
    fun createAWSBucket() {
        // Does bucket exist?
        try {
            s3!!.listObjects(bucketName)
        } catch (e: AmazonServiceException) {
            if (e.statusCode == 404) {
                // Create the bucket
                try {
                    s3!!.createBucket(bucketName)
                    // Allow public read
                    val allowPublicReadStatement = Statement(Effect.Allow).withPrincipals(Principal.AllUsers)
                            .withActions(S3Actions.GetObject).withResources(S3ObjectResource(bucketName, "*"))
                    val policy = Policy().withStatements(allowPublicReadStatement)
                    s3!!.setBucketPolicy(bucketName, policy.toJson())
                    logger.info("AWS S3 bucket {} created", bucketName)
                } catch (e2: Exception) {
                    throw ConfigurationException("Bucket " + bucketName + " cannot be created: " + e2.message, e2)
                }

            } else {
                throw ConfigurationException("Bucket " + bucketName + " exists, but we can't access it: " + e.message,
                        e)
            }
        }

    }

    /** The methods below are used by the test class  */

    fun setS3(s3: AmazonS3) {
        this.s3 = s3
    }

    fun setS3TransferManager(s3TransferManager: TransferManager) {
        this.s3TransferManager = s3TransferManager
    }

    fun setBucketName(bucketName: String) {
        this.bucketName = bucketName
    }

    fun setOpencastDistributionUrl(distributionUrl: String) {
        opencastDistributionUrl = distributionUrl
    }

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(AwsS3DistributionServiceImpl::class.java)

        /** Job type  */
        val JOB_TYPE = "org.opencastproject.distribution.aws.s3"

        // Service configuration
        val AWS_S3_DISTRIBUTION_ENABLE = "org.opencastproject.distribution.aws.s3.distribution.enable"
        val AWS_S3_DISTRIBUTION_BASE_CONFIG = "org.opencastproject.distribution.aws.s3.distribution.base"
        val AWS_S3_ACCESS_KEY_ID_CONFIG = "org.opencastproject.distribution.aws.s3.access.id"
        val AWS_S3_SECRET_ACCESS_KEY_CONFIG = "org.opencastproject.distribution.aws.s3.secret.key"
        val AWS_S3_REGION_CONFIG = "org.opencastproject.distribution.aws.s3.region"
        val AWS_S3_BUCKET_CONFIG = "org.opencastproject.distribution.aws.s3.bucket"
        // config.properties
        val OPENCAST_DOWNLOAD_URL = "org.opencastproject.download.url"

        /** The load on the system introduced by creating a distribute job  */
        val DEFAULT_DISTRIBUTE_JOB_LOAD = 0.1f

        /** The load on the system introduced by creating a retract job  */
        val DEFAULT_RETRACT_JOB_LOAD = 0.1f

        /** The load on the system introduced by creating a restore job  */
        val DEFAULT_RESTORE_JOB_LOAD = 0.1f

        /** The keys to look for in the service configuration file to override the defaults  */
        val DISTRIBUTE_JOB_LOAD_KEY = "job.load.aws.s3.distribute"
        val RETRACT_JOB_LOAD_KEY = "job.load.aws.s3.retract"
        val RESTORE_JOB_LOAD_KEY = "job.load.aws.s3.restore"

        /** Maximum number of tries for checking availability of distributed file  */
        private val MAX_TRIES = 10

        /** Interval time in millis to sleep between checks of availability  */
        private val SLEEP_INTERVAL = 30000L
    }

}
