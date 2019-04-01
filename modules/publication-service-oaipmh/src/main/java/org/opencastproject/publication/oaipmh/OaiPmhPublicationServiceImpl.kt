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
package org.opencastproject.publication.oaipmh

import java.lang.String.format
import org.opencastproject.util.JobUtil.waitForJobs

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.distribution.api.StreamingDistributionService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.MediaPackageReference
import org.opencastproject.mediapackage.MediaPackageRuntimeException
import org.opencastproject.mediapackage.MediaPackageSupport
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException
import org.opencastproject.oaipmh.persistence.QueryBuilder
import org.opencastproject.oaipmh.persistence.SearchResult
import org.opencastproject.oaipmh.persistence.SearchResultItem
import org.opencastproject.oaipmh.server.OaiPmhServerInfo
import org.opencastproject.oaipmh.server.OaiPmhServerInfoUtil
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Collections

import com.entwinemedia.fn.data.Opt

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.URIUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.Hashtable
import java.util.Optional
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Publishes a recording to an OAI-PMH publication repository.
 */
class OaiPmhPublicationServiceImpl : AbstractJobProducer(JOB_TYPE), OaiPmhPublicationService {

    private var downloadDistributionService: DownloadDistributionService? = null
    private var oaiPmhServerInfo: OaiPmhServerInfo? = null
    private var oaiPmhDatabase: OaiPmhDatabase? = null
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getOrganizationDirectoryService
     */
    /** OSGI DI  */
    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getSecurityService
     */
    /** OSGI DI  */
    override var securityService: SecurityService? = null
        set
    /** OSGI DI  */
    protected override var serviceRegistry: ServiceRegistry? = null
        set
    private var streamingDistributionService: StreamingDistributionService? = null
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getUserDirectoryService
     */
    /** OSGI DI  */
    override var userDirectoryService: UserDirectoryService? = null
        set

    enum class Operation {
        Publish, Retract, UpdateMetadata, Replace
    }

    @Throws(Exception::class)
    override fun process(job: Job): String {
        if (!StringUtils.equalsIgnoreCase(JOB_TYPE, job.jobType))
            throw IllegalArgumentException("Can not handle job type " + job.jobType)

        var publication: Publication? = null
        val mediaPackage = MediaPackageParser.getFromXml(job.arguments[0])
        val repository = job.arguments[1]
        var checkAvailability = false
        when (Operation.valueOf(job.operation)) {
            OaiPmhPublicationServiceImpl.Operation.Publish -> {
                val downloadElementIds = StringUtils.split(job.arguments[2], SEPARATOR)
                val streamingElementIds = StringUtils.split(job.arguments[3], SEPARATOR)
                checkAvailability = BooleanUtils.toBoolean(job.arguments[4])
                publication = publish(job, mediaPackage, repository,
                        Collections.set(*downloadElementIds), Collections.set(*streamingElementIds), checkAvailability)
            }
            OaiPmhPublicationServiceImpl.Operation.Replace -> {
                val downloadElements = Collections.toSet(MediaPackageElementParser.getArrayFromXml(job.arguments[2]))
                val streamingElements = Collections.toSet(MediaPackageElementParser.getArrayFromXml(job.arguments[3]))
                val retractDownloadFlavors = Arrays.stream(
                        StringUtils.split(job.arguments[4], SEPARATOR))
                        .filter({ s -> !s.isEmpty() })
                        .map(???({ parseFlavor() }))
                .collect(Collectors.toSet<T>())
                val retractStreamingFlavors = Arrays.stream(
                        StringUtils.split(job.arguments[5], SEPARATOR))
                        .filter({ s -> !s.isEmpty() })
                        .map(???({ parseFlavor() }))
                .collect(Collectors.toSet<T>())
                val publications = Collections.toSet(MediaPackageElementParser.getArrayFromXml(job.arguments[6]))
                checkAvailability = BooleanUtils.toBoolean(job.arguments[7])
                publication = replace(job, mediaPackage, repository, downloadElements, streamingElements,
                        retractDownloadFlavors, retractStreamingFlavors, publications, checkAvailability)
            }
            OaiPmhPublicationServiceImpl.Operation.Retract -> publication = retract(job, mediaPackage, repository)
            OaiPmhPublicationServiceImpl.Operation.UpdateMetadata -> {
                checkAvailability = BooleanUtils.toBoolean(job.arguments[4])
                val flavors = StringUtils.split(job.arguments[2], SEPARATOR)
                val tags = StringUtils.split(job.arguments[3], SEPARATOR)
                publication = updateMetadata(job, mediaPackage, repository,
                        Collections.set(*flavors), Collections.set(*tags), checkAvailability)
            }
            else -> throw IllegalArgumentException("Can not handle this type of operation: " + job.operation)
        }
        return if (publication != null) MediaPackageElementParser.getAsXml(publication) else null
    }

    @Throws(PublicationException::class)
    override fun replace(mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
                         streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
                         retractStreamingFlavors: Set<MediaPackageElementFlavor>, publications: Set<Publication>,
                         checkAvailability: Boolean): Job {
        checkInputArguments(mediaPackage, repository)
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Replace.name,
                    Arrays.asList<T>(MediaPackageParser.getAsXml(mediaPackage), // 0
                            repository, // 1
                            MediaPackageElementParser.getArrayAsXml(Collections.toList(downloadElements)), // 2
                            MediaPackageElementParser.getArrayAsXml(Collections.toList(streamingElements)), // 3
                            StringUtils.join<Set<MediaPackageElementFlavor>>(retractDownloadFlavors, SEPARATOR), // 4
                            StringUtils.join<Set<MediaPackageElementFlavor>>(retractStreamingFlavors, SEPARATOR), // 5
                            MediaPackageElementParser.getArrayAsXml(Collections.toList(publications)), // 6
                            java.lang.Boolean.toString(checkAvailability))) // 7
        } catch (e: ServiceRegistryException) {
            throw PublicationException("Unable to create job", e)
        } catch (e: MediaPackageException) {
            throw PublicationException("Unable to serialize media package elements", e)
        }

    }

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun replaceSync(
            mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
            streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
            retractStreamingFlavors: Set<MediaPackageElementFlavor>, publications: Set<Publication>,
            checkAvailability: Boolean): Publication {
        return replace(null, mediaPackage, repository, downloadElements, streamingElements, retractDownloadFlavors,
                retractStreamingFlavors, publications, checkAvailability)
    }

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun publish(mediaPackage: MediaPackage, repository: String, downloadElementIds: Set<String>,
                         streamingElementIds: Set<String>, checkAvailability: Boolean): Job {
        checkInputArguments(mediaPackage, repository)

        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Publish.toString(),
                    Arrays.asList<T>(MediaPackageParser.getAsXml(mediaPackage), // 0
                            repository, // 1
                            StringUtils.join<Set<String>>(downloadElementIds, SEPARATOR), // 2
                            StringUtils.join<Set<String>>(streamingElementIds, SEPARATOR), // 3
                            java.lang.Boolean.toString(checkAvailability))) // 4
        } catch (e: ServiceRegistryException) {
            throw PublicationException("Unable to create job", e)
        }

    }

    @Throws(PublicationException::class, NotFoundException::class)
    override fun retract(mediaPackage: MediaPackage, repository: String): Job {
        checkInputArguments(mediaPackage, repository)

        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Retract.toString(),
                    Arrays.asList<T>(MediaPackageParser.getAsXml(mediaPackage), repository))
        } catch (e: ServiceRegistryException) {
            throw PublicationException("Unable to create job", e)
        }

    }

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun updateMetadata(mediaPackage: MediaPackage, repository: String, flavors: Set<String>, tags: Set<String>,
                                checkAvailability: Boolean): Job {
        checkInputArguments(mediaPackage, repository)
        if ((flavors == null || flavors.isEmpty()) && (tags == null || tags.isEmpty()))
            throw IllegalArgumentException("Flavors or tags must be set")

        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.UpdateMetadata.toString(),
                    Arrays.asList<T>(MediaPackageParser.getAsXml(mediaPackage), // 0
                            repository, // 1
                            StringUtils.join<Set<String>>(flavors, SEPARATOR), // 2
                            StringUtils.join<Set<String>>(tags, SEPARATOR), // 3
                            java.lang.Boolean.toString(checkAvailability))) // 4
        } catch (e: ServiceRegistryException) {
            throw PublicationException("Unable to create job", e)
        }

    }

    @Throws(PublicationException::class, MediaPackageException::class)
    fun publish(job: Job, mediaPackage: MediaPackage, repository: String, downloadElementIds: Set<String>?,
                streamingElementIds: Set<String>?, checkAvailability: Boolean): Publication {
        val mpId = mediaPackage.identifier.compact()
        val searchResult = oaiPmhDatabase!!.search(QueryBuilder.queryRepo(repository).mediaPackageId(mpId)
                .isDeleted(false).build())
        // retract oai-pmh if published
        if (searchResult.size() > 0) {
            try {
                val p = retract(job, mediaPackage, repository)
                if (p != null && mediaPackage.contains(p))
                    mediaPackage.remove(p)
            } catch (e: NotFoundException) {
                logger.debug("No OAI-PMH publication found for media package {}.", mpId, e)
                // this is ok
            }

        }
        val distributionJobs = ArrayList<Job>(2)
        if (downloadElementIds != null && !downloadElementIds.isEmpty()) {
            // select elements for download distribution
            val mpDownloadDist = mediaPackage.clone() as MediaPackage
            for (mpe in mpDownloadDist.elements) {
                if (downloadElementIds.contains(mpe.identifier))
                    continue
                mpDownloadDist.remove(mpe)
            }
            // publish to download
            if (mpDownloadDist.elements.size > 0) {
                try {
                    val downloadDistributionJob = downloadDistributionService!!
                            .distribute(getPublicationChannelName(repository), mpDownloadDist, downloadElementIds,
                                    checkAvailability)
                    if (downloadDistributionJob != null) {
                        distributionJobs.add(downloadDistributionJob)
                    }
                } catch (e: DistributionException) {
                    throw PublicationException(format("Unable to distribute media package %s to download distribution.", mpId),
                            e)
                }

            }
        }
        if (streamingElementIds != null && !streamingElementIds.isEmpty()) {
            // select elements for streaming distribution
            val mpStreamingDist = mediaPackage.clone() as MediaPackage
            for (mpe in mpStreamingDist.elements) {
                if (streamingElementIds.contains(mpe.identifier))
                    continue
                mpStreamingDist.remove(mpe)
            }
            // publish to streaming
            if (mpStreamingDist.elements.size > 0) {
                try {
                    val streamingDistributionJob = streamingDistributionService!!
                            .distribute(getPublicationChannelName(repository), mpStreamingDist, streamingElementIds)
                    if (streamingDistributionJob != null) {
                        distributionJobs.add(streamingDistributionJob)
                    }
                } catch (e: DistributionException) {
                    throw PublicationException(format("Unable to distribute media package %s to streaming distribution.", mpId),
                            e)
                }

            }
        }
        if (distributionJobs.isEmpty()) {
            throw IllegalStateException(format(
                    "The media package %s does not contain any elements for publishing to OAI-PMH", mpId))
        }
        // wait for distribution jobs
        if (!waitForJobs(job, serviceRegistry, distributionJobs).isSuccess()) {
            throw PublicationException(format(
                    "Unable to distribute elements of media package %s to distribution channels.", mpId))
        }

        val distributedElements = ArrayList<MediaPackageElement>()
        for (distributionJob in distributionJobs) {
            val distributedElementsXml = distributionJob.payload
            if (StringUtils.isNotBlank(distributedElementsXml)) {
                for (distributedElement in MediaPackageElementParser.getArrayFromXml(distributedElementsXml)) {
                    distributedElements.add(distributedElement)
                }
            }
        }
        val oaiPmhDistMp = mediaPackage.clone() as MediaPackage
        // cleanup media package elements
        for (mpe in oaiPmhDistMp.elements) {
            // keep publications
            if (MediaPackageElement.Type.Publication === mpe.elementType)
                continue
            oaiPmhDistMp.remove(mpe)
        }
        // ...add the distributed elements
        for (mpe in distributedElements) {
            oaiPmhDistMp.add(mpe)
        }

        // publish to oai-pmh
        try {
            oaiPmhDatabase!!.store(oaiPmhDistMp, repository)
        } catch (e: OaiPmhDatabaseException) {
            // todo: should we retract the elements from download and streaming here?
            throw PublicationException(format("Unable to distribute media package %s to OAI-PMH repository %s", mpId, repository), e)
        }

        return createPublicationElement(mpId, repository)
    }

    @Throws(MediaPackageException::class, PublicationException::class)
    private fun replace(job: Job?, mediaPackage: MediaPackage, repository: String,
                        downloadElements: Set<MediaPackageElement>, streamingElements: Set<MediaPackageElement>,
                        retractDownloadFlavors: Set<MediaPackageElementFlavor>, retractStreamingFlavors: Set<MediaPackageElementFlavor>,
                        publications: Set<MediaPackageElement>, checkAvailable: Boolean): Publication {
        val mpId = mediaPackage.identifier.compact()
        val channel = getPublicationChannelName(repository)

        try {
            val search = oaiPmhDatabase!!.search(QueryBuilder.queryRepo(repository).mediaPackageId(mpId)
                    .isDeleted(false).build())
            if (search.size() > 1) throw PublicationException("Found multiple OAI-PMH records for id $mpId")
            val existingMp = search.items.stream().findFirst().map<MediaPackage>(
                    Function<SearchResultItem, MediaPackage> { it.getMediaPackage() })

            // Collect Ids of elements to distribute
            val addDownloadElementIds = downloadElements.stream()
                    .map<String>(Function<out MediaPackageElement, String> { it.getIdentifier() })
                    .collect<Set<String>, Any>(Collectors.toSet())
            val addStreamingElementIds = streamingElements.stream()
                    .map<String>(Function<out MediaPackageElement, String> { it.getIdentifier() })
                    .collect<Set<String>, Any>(Collectors.toSet())

            // Use retractFlavors to search for existing elements to retract
            val removeDownloadElements = existingMp.map { mp ->
                Arrays.stream(mp.elements)
                        .filter { e -> retractDownloadFlavors.stream().anyMatch { f -> f.matches(e.flavor) } }
                        .collect<Set<MediaPackageElement>, Any>(Collectors.toSet())
            }.orElse(emptySet())
            val removeStreamingElements = existingMp.map { mp ->
                Arrays.stream(mp.elements)
                        .filter { e -> retractStreamingFlavors.stream().anyMatch { f -> f.matches(e.flavor) } }
                        .collect<Set<MediaPackageElement>, Any>(Collectors.toSet())
            }.orElse(emptySet())

            // Element IDs to retract. Elements identified by flavor and elements to re-distribute
            val removeDownloadElementIds = Stream
                    .concat(removeDownloadElements.stream(), downloadElements.stream())
                    .map<String>(Function<MediaPackageElement, String> { it.getIdentifier() })
                    .collect<Set<String>, Any>(Collectors.toSet())
            val removeStreamingElementIds = Stream
                    .concat(removeStreamingElements.stream(), streamingElements.stream())
                    .map<String>(Function<MediaPackageElement, String> { it.getIdentifier() })
                    .collect<Set<String>, Any>(Collectors.toSet())

            if (removeDownloadElementIds.isEmpty() && removeStreamingElementIds.isEmpty()
                    && addDownloadElementIds.isEmpty() && addStreamingElementIds.isEmpty()) {
                // Nothing to do
                return Arrays.stream(mediaPackage.publications)
                        .filter { p -> channel == p.channel }
                        .findFirst()
                        .orElse(null)
            }

            val temporaryMediaPackage = mediaPackage.clone() as MediaPackage
            downloadElements.forEach(Consumer<out MediaPackageElement> { temporaryMediaPackage.add(it) })
            streamingElements.forEach(Consumer<out MediaPackageElement> { temporaryMediaPackage.add(it) })
            removeDownloadElements.forEach(Consumer<MediaPackageElement> { temporaryMediaPackage.add(it) })
            removeStreamingElements.forEach(Consumer<MediaPackageElement> { temporaryMediaPackage.add(it) })

            val retractedElements = ArrayList<MediaPackageElement>()
            val distributedElements = ArrayList<MediaPackageElement>()
            if (job != null) {
                retractedElements
                        .addAll(retract(job, channel, temporaryMediaPackage, removeDownloadElementIds, removeStreamingElementIds))
                distributedElements
                        .addAll(distribute(job, channel, temporaryMediaPackage, addDownloadElementIds, addStreamingElementIds,
                                checkAvailable))
            } else {
                retractedElements
                        .addAll(retractSync(channel, temporaryMediaPackage, removeDownloadElementIds, removeStreamingElementIds))
                distributedElements
                        .addAll(distributeSync(channel, temporaryMediaPackage, addDownloadElementIds, addStreamingElementIds,
                                checkAvailable))
            }

            val oaiPmhDistMp = existingMp.orElse(mediaPackage).clone() as MediaPackage

            // Remove OAI-PMH publication
            Arrays.stream(oaiPmhDistMp.publications)
                    .filter { p -> channel == p.channel }
                    .forEach(Consumer<Publication> { oaiPmhDistMp.remove(it) })

            // Remove retracted elements
            retractedElements.stream()
                    .map<String>(Function<MediaPackageElement, String> { it.getIdentifier() })
                    .forEach(Consumer<String> { oaiPmhDistMp.removeElementById(it) })
            // Add new distributed elements
            distributedElements.forEach(Consumer<MediaPackageElement> { oaiPmhDistMp.add(it) })

            // Remove old publications
            publications.stream()
                    .map { p -> (p as Publication).channel }
                    .forEach { c ->
                        Arrays.stream(oaiPmhDistMp.publications)
                                .filter { p -> c == p.channel }
                                .forEach(Consumer<Publication> { oaiPmhDistMp.remove(it) })
                    }

            // Add updated publications
            publications.forEach(Consumer<out MediaPackageElement> { oaiPmhDistMp.add(it) })

            // publish to oai-pmh
            oaiPmhDatabase!!.store(oaiPmhDistMp, repository)

            return Arrays.stream(mediaPackage.publications)
                    .filter { p -> channel == p.channel }
                    .findFirst()
                    .orElse(createPublicationElement(mpId, repository))
        } catch (e: OaiPmhDatabaseException) {
            throw PublicationException(format("Unable to update media package %s in OAI-PMH repository %s", mpId,
                    repository), e)
        } catch (e: DistributionException) {
            throw PublicationException(format("Unable to update OAI-PMH distributions of media package %s.", mpId), e)
        } catch (e: MediaPackageRuntimeException) {
            throw e.wrappedException
        }

    }

    @Throws(PublicationException::class, DistributionException::class)
    private fun retract(
            job: Job, channel: String, mp: MediaPackage, removeDownloadElementIds: Set<String>,
            removeStreamingElementIds: Set<String>): List<MediaPackageElement> {
        val retractJobs = ArrayList<Job>(2)
        if (!removeDownloadElementIds.isEmpty()) {
            retractJobs.add(downloadDistributionService!!.retract(channel, mp, removeDownloadElementIds))
        }
        if (!removeStreamingElementIds.isEmpty()) {
            retractJobs.add(streamingDistributionService!!.retract(channel, mp, removeStreamingElementIds))
        }

        // wait for retract jobs
        if (!waitForJobs(job, serviceRegistry, retractJobs).isSuccess()) {
            throw PublicationException(format("Unable to retract OAI-PMH distributions of media package %s",
                    mp.identifier.toString()))
        }

        return retractJobs.stream()
                .filter { j -> StringUtils.isNotBlank(j.payload) }
                .map<String>(Function<Job, String> { it.getPayload() })
                .flatMap { p -> MediaPackageElementParser.getArrayFromXmlUnchecked(p).stream() }
                .collect<List<MediaPackageElement>, Any>(Collectors.toList())
    }

    @Throws(DistributionException::class)
    private fun retractSync(channel: String, mp: MediaPackage, removeDownloadElementIds: Set<String>,
                            removeStreamingElementIds: Set<String>): List<MediaPackageElement> {
        val retracted = ArrayList<MediaPackageElement>()
        if (!removeDownloadElementIds.isEmpty()) {
            retracted.addAll(downloadDistributionService!!.retractSync(channel, mp, removeDownloadElementIds))
        }
        if (!removeStreamingElementIds.isEmpty()) {
            retracted.addAll(streamingDistributionService!!.retractSync(channel, mp, removeStreamingElementIds))
        }
        return retracted
    }

    @Throws(PublicationException::class, MediaPackageException::class, DistributionException::class)
    private fun distribute(
            job: Job, channel: String, mp: MediaPackage, addDownloadElementIds: Set<String>,
            addStreamingElementIds: Set<String>, checkAvailable: Boolean): List<MediaPackageElement> {
        val distributeJobs = ArrayList<Job>(2)
        if (!addDownloadElementIds.isEmpty()) {
            distributeJobs.add(downloadDistributionService!!.distribute(channel, mp, addDownloadElementIds,
                    checkAvailable))
        }
        if (!addStreamingElementIds.isEmpty()) {
            distributeJobs.add(streamingDistributionService!!.distribute(channel, mp, addStreamingElementIds))
        }

        // wait for distribute jobs
        if (!waitForJobs(job, serviceRegistry, distributeJobs).isSuccess()) {
            throw PublicationException(format("Unable to distribute OAI-PMH distributions of media package %s",
                    mp.identifier.toString()))
        }

        return distributeJobs.stream()
                .filter { j -> StringUtils.isNotBlank(j.payload) }
                .map<String>(Function<Job, String> { it.getPayload() })
                .flatMap { p -> MediaPackageElementParser.getArrayFromXmlUnchecked(p).stream() }
                .collect<List<MediaPackageElement>, Any>(Collectors.toList())
    }

    @Throws(DistributionException::class)
    private fun distributeSync(
            channel: String, mp: MediaPackage, addDownloadElementIds: Set<String>, addStreamingElementIds: Set<String>,
            checkAvailable: Boolean): List<MediaPackageElement> {
        val distributed = ArrayList<MediaPackageElement>()
        if (!addDownloadElementIds.isEmpty()) {
            distributed.addAll(downloadDistributionService!!.distributeSync(channel, mp, addDownloadElementIds,
                    checkAvailable))
        }
        if (!addStreamingElementIds.isEmpty()) {
            distributed.addAll(streamingDistributionService!!.distributeSync(channel, mp, addStreamingElementIds))
        }
        return distributed
    }


    @Throws(PublicationException::class, NotFoundException::class)
    protected fun retract(job: Job, mediaPackage: MediaPackage, repository: String): Publication? {
        val mpId = mediaPackage.identifier.compact()

        // track elements for retraction
        var oaiPmhMp: MediaPackage? = null
        val searchResult = oaiPmhDatabase!!.search(QueryBuilder.queryRepo(repository).mediaPackageId(mpId)
                .isDeleted(false).build())
        for (searchResultItem in searchResult.items) {
            if (oaiPmhMp == null) {
                oaiPmhMp = searchResultItem.mediaPackage
            } else {
                for (mpe in searchResultItem.mediaPackage.elements) {
                    oaiPmhMp.add(mpe)
                }
            }
        }

        // retract oai-pmh
        try {
            oaiPmhDatabase!!.delete(mpId, repository)
        } catch (e: OaiPmhDatabaseException) {
            throw PublicationException(format("Unable to retract media package %s from OAI-PMH repository %s",
                    mpId, repository), e)
        } catch (e: NotFoundException) {
            logger.debug(format("Skip retracting media package %s from OIA-PMH repository %s as it isn't published.",
                    mpId, repository), e)
        }

        if (oaiPmhMp != null && oaiPmhMp.elements.size > 0) {
            // retract files from distribution channels
            val mpeIds = HashSet<String>()
            for (mpe in oaiPmhMp.elements()) {
                if (MediaPackageElement.Type.Publication === mpe.elementType)
                    continue

                mpeIds.add(mpe.identifier)
            }
            if (!mpeIds.isEmpty()) {
                val retractionJobs = ArrayList<Job>()
                // retract download
                try {
                    val retractDownloadJob = downloadDistributionService!!
                            .retract(getPublicationChannelName(repository), oaiPmhMp, mpeIds)
                    if (retractDownloadJob != null) {
                        retractionJobs.add(retractDownloadJob)
                    }
                } catch (e: DistributionException) {
                    throw PublicationException(format("Unable to create retraction job from distribution channel download for the media package %s ",
                            mpId), e)
                }

                // retract streaming
                try {
                    val retractDownloadJob = streamingDistributionService!!
                            .retract(getPublicationChannelName(repository), oaiPmhMp, mpeIds)
                    if (retractDownloadJob != null) {
                        retractionJobs.add(retractDownloadJob)
                    }
                } catch (e: DistributionException) {
                    throw PublicationException(format("Unable to create retraction job from distribution channel streaming for the media package %s ",
                            mpId), e)
                }

                if (retractionJobs.size > 0) {
                    // wait for distribution jobs
                    if (!waitForJobs(job, serviceRegistry, retractionJobs).isSuccess())
                        throw PublicationException(
                                format("Unable to retract elements of media package %s from distribution channels.", mpId))
                }
            }
        }

        val publicationChannel = getPublicationChannelName(repository)
        for (p in mediaPackage.publications) {
            if (StringUtils.equals(publicationChannel, p.channel))
                return p
        }
        return null
    }

    @Throws(PublicationException::class)
    protected fun updateMetadata(job: Job?, mediaPackage: MediaPackage, repository: String, flavors: Set<String>, tags: Set<String>,
                                 checkAvailability: Boolean): Publication? {
        val parsedFlavors = HashSet<MediaPackageElementFlavor>()
        for (flavor in flavors) {
            parsedFlavors.add(MediaPackageElementFlavor.parseFlavor(flavor))
        }

        val filteredMp: MediaPackage
        val result = oaiPmhDatabase!!.search(QueryBuilder.queryRepo(repository).mediaPackageId(mediaPackage)
                .isDeleted(false).build())
        if (result.size() == 1L) {
            // apply tags and flavors to the current media package
            try {
                logger.debug("filter elements with flavors {} and tags {} on media package {}",
                        StringUtils.join(flavors, ", "), StringUtils.join(tags, ", "),
                        MediaPackageParser.getAsXml(mediaPackage))

                filteredMp = filterMediaPackage(mediaPackage, parsedFlavors, tags)
            } catch (e: MediaPackageException) {
                throw PublicationException("Error filtering media package", e)
            }

        } else if (result.size() == 0L) {
            logger.info(format("Skipping update of media package %s since it is not currently published to %s",
                    mediaPackage, repository))
            return null
        } else {
            val msg = format("More than one media package with id %s found", mediaPackage.identifier.compact())
            logger.warn(msg)
            throw PublicationException(msg)
        }
        // re-distribute elements to download
        val elementIdsToDistribute = HashSet<String>()
        for (mpe in filteredMp.elements) {
            // do not distribute publications
            if (MediaPackageElement.Type.Publication === mpe.elementType)
                continue
            elementIdsToDistribute.add(mpe.identifier)
        }
        if (elementIdsToDistribute.isEmpty()) {
            logger.debug("The media package {} does not contain any elements to update. " + "Skip OAI-PMH metadata update operation for repository {}",
                    mediaPackage.identifier.compact(), repository)
            return null
        }
        logger.debug("distribute elements {}", StringUtils.join(elementIdsToDistribute, ", "))
        val distributedElements = ArrayList<MediaPackageElement>()
        try {
            val distJob = downloadDistributionService!!
                    .distribute(getPublicationChannelName(repository), filteredMp, elementIdsToDistribute, checkAvailability)
            if (job == null)
                throw PublicationException("The distribution service can not handle this type of media package elements.")
            if (!waitForJobs(job, serviceRegistry, distJob).isSuccess()) {
                throw PublicationException(format(
                        "Unable to distribute updated elements from media package %s to the download distribution service",
                        mediaPackage.identifier.compact()))
            }
            if (distJob.payload != null) {
                for (mpe in MediaPackageElementParser.getArrayFromXml(distJob.payload)) {
                    distributedElements.add(mpe)
                }
            }
        } catch (e: DistributionException) {
            throw PublicationException(format(
                    "Unable to distribute updated elements from media package %s to the download distribution service",
                    mediaPackage.identifier.compact()), e)
        } catch (e: MediaPackageException) {
            throw PublicationException(format("Unable to distribute updated elements from media package %s to the download distribution service", mediaPackage.identifier.compact()), e)
        }

        // update elements (URLs)
        for (e in filteredMp.elements) {
            if (MediaPackageElement.Type.Publication == e.elementType)
                continue
            filteredMp.remove(e)
        }
        for (e in distributedElements) {
            filteredMp.add(e)
        }
        val publishedMp = merge(filteredMp, removeMatchingNonExistantElements(filteredMp,
                result.items[0].mediaPackage.clone() as MediaPackage, parsedFlavors, tags))
        // Does the media package have a title and track?
        if (!MediaPackageSupport.isPublishable(publishedMp)) {
            throw PublicationException("Media package does not meet criteria for publication")
        }
        // Publish the media package to OAI-PMH
        try {
            logger.debug(format("Updating metadata of media package %s in %s",
                    publishedMp.identifier.compact(), repository))
            oaiPmhDatabase!!.store(publishedMp, repository)
        } catch (e: OaiPmhDatabaseException) {
            throw PublicationException(format("Media package %s could not be updated",
                    publishedMp.identifier.compact()))
        }

        // retract orphaned elements from download distribution
        // orphaned elements are all those elements to which the updated media package no longer refers (in terms of element uri)
        val elementUriMap = Hashtable<URI, MediaPackageElement>()
        for (oaiPmhSearchResultItem in result.items) {
            for (mpe in oaiPmhSearchResultItem.mediaPackage.elements) {
                if (MediaPackageElement.Type.Publication === mpe.elementType || null == mpe.getURI())
                    continue
                elementUriMap[mpe.getURI()] = mpe
            }
        }
        for (publishedMpe in publishedMp.elements) {
            if (MediaPackageElement.Type.Publication === publishedMpe.elementType)
                continue
            if (elementUriMap.containsKey(publishedMpe.getURI()))
                elementUriMap.remove(publishedMpe.getURI())
        }
        val orphanedElementIds = HashSet<String>()
        for (orphanedMpe in elementUriMap.values) {
            orphanedElementIds.add(orphanedMpe.identifier)
        }
        if (!orphanedElementIds.isEmpty()) {
            for (oaiPmhSearchResultItem in result.items) {
                try {
                    val retractJob = downloadDistributionService!!.retract(getPublicationChannelName(repository),
                            oaiPmhSearchResultItem.mediaPackage, orphanedElementIds)
                    if (retractJob != null) {
                        if (!waitForJobs(job, serviceRegistry, retractJob).isSuccess())
                            logger.warn("The download distribution retract job for the orphaned elements from media package {} does not end successfully",
                                    oaiPmhSearchResultItem.mediaPackage.identifier.compact())
                    }
                } catch (e: DistributionException) {
                    logger.warn("Unable to retract orphaned elements from download distribution service for the media package {} channel {}",
                            oaiPmhSearchResultItem.mediaPackage.identifier.compact(), getPublicationChannelName(repository), e)
                }

            }
        }

        // return the publication
        val publicationChannel = getPublicationChannelName(repository)
        for (p in mediaPackage.publications) {
            if (StringUtils.equals(publicationChannel, p.channel))
                return p
        }
        return null
    }

    protected fun checkInputArguments(mediaPackage: MediaPackage?, repository: String) {
        if (mediaPackage == null)
            throw IllegalArgumentException("Media package must be specified")
        if (StringUtils.isEmpty(repository))
            throw IllegalArgumentException("Repository must be specified")
        if (!oaiPmhServerInfo!!.hasRepo(repository))
            throw IllegalArgumentException("OAI-PMH repository '$repository' does not exist")
    }

    protected fun getPublicationChannelName(repository: String): String {
        return PUBLICATION_CHANNEL_PREFIX.concat(repository)
    }

    /** Create a new publication element.  */
    @Throws(PublicationException::class)
    protected fun createPublicationElement(mpId: String, repository: String): Publication {
        for (hostUrl in OaiPmhServerInfoUtil.oaiPmhServerUrlOfCurrentOrganization(securityService!!)) {
            val engageUri = URIUtils.resolve(
                    URI.create(UrlSupport.concat(hostUrl, oaiPmhServerInfo!!.mountPoint, repository)),
                    "?verb=ListMetadataFormats&identifier=$mpId")
            return PublicationImpl.publication(UUID.randomUUID().toString(), getPublicationChannelName(repository), engageUri,
                    MimeTypes.parseMimeType(MimeTypes.XML.toString()))
        }
        // no host URL
        val msg = format("No host url for oai-pmh server configured for organization %s " + "("
                + OaiPmhServerInfoUtil.ORG_CFG_OAIPMH_SERVER_HOSTURL + ")", securityService!!.organization.id)
        throw PublicationException(msg)
    }

    /**
     * Creates a clone of the media package and removes those elements that do not match the flavor and tags filter
     * criteria.
     *
     * @param mediaPackage
     * the media package
     * @param flavors
     * the flavors
     * @param tags
     * the tags
     * @return the filtered media package
     */
    @Throws(MediaPackageException::class)
    private fun filterMediaPackage(mediaPackage: MediaPackage, flavors: Set<MediaPackageElementFlavor>,
                                   tags: Set<String>): MediaPackage {
        if (flavors.isEmpty() && tags.isEmpty())
            throw IllegalArgumentException("Flavors or tags parameter must be set")

        val filteredMediaPackage = mediaPackage.clone() as MediaPackage

        // The list of elements to keep
        val keep = ArrayList<MediaPackageElement>()

        val selector = SimpleElementSelector()
        // Filter elements
        for (flavor in flavors) {
            selector.addFlavor(flavor)
        }
        for (tag in tags) {
            selector.addTag(tag)
        }
        keep.addAll(selector.select(mediaPackage, true))

        // Keep publications
        for (p in filteredMediaPackage.publications)
            keep.add(p)

        // Fix references and flavors
        for (element in filteredMediaPackage.elements) {

            if (!keep.contains(element)) {
                logger.debug("Removing {} '{}' from media package '{}'", element.elementType.toString().toLowerCase(),
                        element.identifier, filteredMediaPackage.identifier.toString())
                filteredMediaPackage.remove(element)
                continue
            }

            // Is the element referencing anything?
            var reference: MediaPackageReference? = element.reference
            if (reference != null) {
                val referenceProperties = reference.properties
                val referencedElement = mediaPackage.getElementByReference(reference)

                // if we are distributing the referenced element, everything is fine. Otherwise...
                if (referencedElement != null && !keep.contains(referencedElement)) {

                    // Follow the references until we find a flavor
                    var parent: MediaPackageElement? = null
                    while ((parent = mediaPackage.getElementByReference(reference!!)) != null) {
                        if (parent!!.flavor != null && element.flavor == null) {
                            element.flavor = parent.flavor
                        }
                        if (parent.reference == null)
                            break
                        reference = parent.reference
                    }

                    // Done. Let's cut the path but keep references to the mediapackage itself
                    if (reference != null && reference.type == MediaPackageReference.TYPE_MEDIAPACKAGE)
                        element.reference = reference
                    else if (reference != null && (referenceProperties == null || referenceProperties.size == 0))
                        element.clearReference()
                    else {
                        // Ok, there is more to that reference than just pointing at an element. Let's keep the original,
                        // you never know.
                        referencedElement.setURI(null)
                        referencedElement.checksum = null
                    }
                }
            }
        }

        return filteredMediaPackage
    }

    /** OSGI DI  */
    fun setDownloadDistributionService(downloadDistributionService: DownloadDistributionService) {
        this.downloadDistributionService = downloadDistributionService
    }

    /** OSGI DI  */
    fun setStreamingDistributionService(streamingDistributionService: StreamingDistributionService) {
        this.streamingDistributionService = streamingDistributionService
    }

    /** OSGI DI  */
    fun setOaiPmhServerInfo(oaiPmhServerInfo: OaiPmhServerInfo) {
        this.oaiPmhServerInfo = oaiPmhServerInfo
    }

    /** OSGI DI  */
    fun setOaiPmhDatabase(oaiPmhDatabase: OaiPmhDatabase) {
        this.oaiPmhDatabase = oaiPmhDatabase
    }

    companion object {

        /** Logging facility  */
        private val logger = LoggerFactory.getLogger(OaiPmhPublicationServiceImpl::class.java)

        private fun updateMediaPackageFields(oaiPmhMp: MediaPackage, mediaPackage: MediaPackage): MediaPackage {
            oaiPmhMp.title = mediaPackage.title
            oaiPmhMp.date = mediaPackage.date
            oaiPmhMp.language = mediaPackage.language
            oaiPmhMp.license = mediaPackage.license
            oaiPmhMp.series = mediaPackage.series
            oaiPmhMp.seriesTitle = mediaPackage.seriesTitle
            for (contributor in oaiPmhMp.contributors)
                oaiPmhMp.removeContributor(contributor)
            for (contributor in mediaPackage.contributors)
                oaiPmhMp.addContributor(contributor)
            for (creator in oaiPmhMp.creators)
                oaiPmhMp.removeCreator(creator)
            for (creator in mediaPackage.creators)
                oaiPmhMp.addCreator(creator)
            for (subject in oaiPmhMp.subjects)
                oaiPmhMp.removeSubject(subject)
            for (subject in mediaPackage.subjects)
                oaiPmhMp.addSubject(subject)
            return oaiPmhMp
        }

        /**
         * Remove all these elements from `publishedMp`, that matches the given flavors and tags
         * but are not in the `updatedMp`.
         *
         * @param updatedMp the updated media package
         * @param publishedMp the media package that is currently published
         * @param flavors flavors of elements to update
         * @param tags tags of elements to update
         * @return published media package without elements, that matches the flavors and tags
         * but are not in the updated media package
         */
        fun removeMatchingNonExistantElements(updatedMp: MediaPackage, publishedMp: MediaPackage,
                                              flavors: Set<MediaPackageElementFlavor>, tags: Set<String>): MediaPackage {
            val selector = SimpleElementSelector()
            // Filter elements
            for (flavor in flavors) {
                selector.addFlavor(flavor)
            }
            for (tag in tags) {
                selector.addTag(tag)
            }
            for (publishedMpe in selector.select(publishedMp, true)) {
                var foundInUpdatedMp = false
                for (updatedMpe in updatedMp.getElementsByFlavor(publishedMpe.flavor)) {
                    if (!updatedMpe.containsTag(tags)) {
                        // todo: this case shouldn't happen!
                    }
                    foundInUpdatedMp = true
                    break
                }

                if (!foundInUpdatedMp) {
                    publishedMp.remove(publishedMpe)
                }
            }
            return publishedMp
        }

        /**
         * Merges the updated media package with the one that is currently published in a way where the updated elements
         * replace existing ones in the published media package based on their flavor.
         *
         *
         * If `publishedMp` is `null`, this method returns the updated media package without any
         * modifications.
         *
         * @param updatedMp
         * the updated media package
         * @param publishedMp
         * the media package that is currently published
         * @return the merged media package
         */
        fun merge(updatedMp: MediaPackage, publishedMp: MediaPackage?): MediaPackage {
            if (publishedMp == null)
                return updatedMp

            val mergedMp = MediaPackageSupport.copy(publishedMp)

            // Merge the elements
            for (updatedElement in updatedMp.elements()) {
                for (flavor in Opt.nul(updatedElement.flavor)) {
                    for (outdated in mergedMp.getElementsByFlavor(flavor)) {
                        mergedMp.remove(outdated)
                    }
                    logger.debug(format("Update %s %s of type %s", updatedElement.elementType.toString().toLowerCase(),
                            updatedElement.identifier, updatedElement.elementType))
                    mergedMp.add(updatedElement)
                }
            }

            // Remove publications
            for (p in mergedMp.publications)
                mergedMp.remove(p)

            // Add updated publications
            for (updatedPublication in updatedMp.publications)
                mergedMp.add(updatedPublication)

            // Merge media package fields
            updateMediaPackageFields(mergedMp, updatedMp)
            return mergedMp
        }
    }
}
