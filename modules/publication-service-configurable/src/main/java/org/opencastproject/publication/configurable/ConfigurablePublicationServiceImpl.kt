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
package org.opencastproject.publication.configurable

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.publication.api.ConfigurablePublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.JobUtil

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Arrays
import java.util.HashSet
import java.util.Optional
import java.util.UUID

class ConfigurablePublicationServiceImpl : AbstractJobProducer(JOB_TYPE), ConfigurablePublicationService {

    /* Gson is thread-safe so we use a single instance */
    private val gson = Gson()

    override val jobType: String?
        get() = super.jobType

    private var distributionService: DownloadDistributionService? = null

    override var securityService: SecurityService? = null
        set

    override var userDirectoryService: UserDirectoryService? = null
        set

    override var organizationDirectoryService: OrganizationDirectoryService? = null
        set

    protected override var serviceRegistry: ServiceRegistry? = null
        set

    override fun activate(cc: ComponentContext) {
        super.activate(cc)
    }

    enum class Operation {
        Replace
    }

    fun setDownloadDistributionService(distributionService: DownloadDistributionService) {
        this.distributionService = distributionService
    }

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun replace(mediaPackage: MediaPackage, channelId: String,
                         addElements: Collection<MediaPackageElement>, retractElementIds: Set<String>): Job {
        try {
            return serviceRegistry!!.createJob(JOB_TYPE, Operation.Replace.toString(),
                    Arrays.asList<T>(MediaPackageParser.getAsXml(mediaPackage), channelId,
                            MediaPackageElementParser.getArrayAsXml(addElements), gson.toJson(retractElementIds)))
        } catch (e: ServiceRegistryException) {
            throw PublicationException("Unable to create job", e)
        }

    }

    @Throws(PublicationException::class, MediaPackageException::class)
    override fun replaceSync(
            mediaPackage: MediaPackage, channelId: String, addElements: Collection<MediaPackageElement>,
            retractElementIds: Set<String>): Publication {
        try {
            return doReplaceSync(mediaPackage, channelId, addElements, retractElementIds)
        } catch (e: DistributionException) {
            throw PublicationException(e)
        }

    }

    @Throws(Exception::class)
    override fun process(job: Job): String {
        val arguments = job.arguments
        val mediaPackage = MediaPackageParser.getFromXml(arguments[0])
        val channelId = arguments[1]
        val addElements = MediaPackageElementParser
                .getArrayFromXml(arguments[2])
        val retractElementIds = gson.fromJson<Set<String>>(arguments[3], object : TypeToken<Set<String>>() {

        }.type)

        var result: Publication? = null
        when (Operation.valueOf(job.operation)) {
            ConfigurablePublicationServiceImpl.Operation.Replace -> result = doReplace(mediaPackage, channelId, addElements, retractElementIds)
            else -> {
            }
        }
        return if (result != null) {
            MediaPackageElementParser.getAsXml(result)
        } else {
            null
        }
    }

    @Throws(DistributionException::class, MediaPackageException::class)
    private fun distributeMany(mp: MediaPackage, channelId: String,
                               elements: Collection<MediaPackageElement>) {

        val publicationOpt = getPublication(mp, channelId)

        if (publicationOpt.isPresent) {

            val publication = publicationOpt.get()

            // Add all the elements top-level so the distribution service knows what to do
            elements.forEach(Consumer<out MediaPackageElement> { mp.add(it) })

            val elementIds = HashSet<String>()
            for (mpe in elements) {
                elementIds.add(mpe.identifier)
            }

            try {
                val job = distributionService!!.distribute(channelId, mp, elementIds, false)

                if (!JobUtil.waitForJob(serviceRegistry!!, job)!!.isSuccess) {
                    throw DistributionException("At least one of the publication jobs did not complete successfully")
                }
                val distributedElements = MediaPackageElementParser.getArrayFromXml(job.payload)
                for (mpe in distributedElements) {
                    PublicationImpl.addElementToPublication(publication, mpe)
                }
            } finally {
                // Remove our changes
                elements.stream().map<String>(Function<out MediaPackageElement, String> { it.getIdentifier() }).forEach(Consumer<String> { mp.removeElementById(it) })
            }
        }
    }

    @Throws(DistributionException::class)
    private fun distributeManySync(mp: MediaPackage, channelId: String,
                                   elements: Collection<MediaPackageElement>) {

        val publicationOpt = getPublication(mp, channelId)

        if (publicationOpt.isPresent) {

            val publication = publicationOpt.get()

            // Add all the elements top-level so the distribution service knows what to do
            elements.forEach(Consumer<out MediaPackageElement> { mp.add(it) })

            val elementIds = HashSet<String>()
            for (mpe in elements) {
                elementIds.add(mpe.identifier)
            }

            try {
                val distributedElements = distributionService!!.distributeSync(channelId, mp,
                        elementIds, false)
                for (mpe in distributedElements) {
                    PublicationImpl.addElementToPublication(publication, mpe)
                }
            } finally {
                // Remove our changes
                elements.stream().map<String>(Function<out MediaPackageElement, String> { it.getIdentifier() }).forEach(Consumer<String> { mp.removeElementById(it) })
            }
        }
    }

    @Throws(DistributionException::class, MediaPackageException::class)
    private fun doReplace(mp: MediaPackage, channelId: String,
                          addElementIds: Collection<MediaPackageElement>, retractElementIds: Set<String>?): Publication {
        // Retract old elements
        val retractJob = distributionService!!.retract(channelId, mp, retractElementIds!!)

        if (!JobUtil.waitForJobs(serviceRegistry!!, retractJob)!!.isSuccess) {
            throw DistributionException("At least one of the retraction jobs did not complete successfully")
        }

        val priorPublication = getPublication(mp, channelId)

        val publication: Publication

        if (priorPublication.isPresent) {
            publication = priorPublication.get()
        } else {
            val publicationUUID = UUID.randomUUID().toString()
            publication = PublicationImpl.publication(publicationUUID, channelId, null, null)
            mp.add(publication)
        }

        retractElementIds.forEach(Consumer<String> { publication.removeAttachmentById(it) })

        distributeMany(mp, channelId, addElementIds)

        return publication
    }

    @Throws(DistributionException::class)
    private fun doReplaceSync(mp: MediaPackage, channelId: String,
                              addElementIds: Collection<MediaPackageElement>, retractElementIds: Set<String>): Publication {
        // Retract old elements
        distributionService!!.retractSync(channelId, mp, retractElementIds)

        val priorPublication = getPublication(mp, channelId)

        val publication: Publication

        if (priorPublication.isPresent) {
            publication = priorPublication.get()
        } else {
            val publicationUUID = UUID.randomUUID().toString()
            publication = PublicationImpl.publication(publicationUUID, channelId, null, null)
            mp.add(publication)
        }

        retractElementIds.forEach(Consumer<String> { publication.removeAttachmentById(it) })

        distributeManySync(mp, channelId, addElementIds)

        return publication
    }

    private fun getPublication(mp: MediaPackage, channelId: String): Optional<Publication> {
        return Arrays.stream(mp.publications).filter { p -> p.channel.equals(channelId, ignoreCase = true) }.findAny()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConfigurablePublicationServiceImpl::class.java)
    }
}
