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
package org.opencastproject.workflow.handler.workflow

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.serviceregistry.api.ServiceRegistration
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.UrlSupport
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workingfilerepository.api.WorkingFileRepository
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.StatusLine
import org.apache.http.client.methods.HttpUriRequest
import org.easymock.EasyMock
import org.easymock.IArgumentMatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Hashtable
import java.util.UUID

/**
 * Test class for [CleanupWorkflowOperationHandler]
 */
class CleanupWorkflowOperationHandlerTest {

    private var cleanupWOH: CleanupWorkflowOperationHandler? = null
    private val deletedFilesURIs = ArrayList<URI>()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        cleanupWOH = CleanupWorkflowOperationHandler()

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.baseUri).andReturn(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX)).anyTimes()
        EasyMock.replay(workspace)
        cleanupWOH!!.setWorkspace(workspace)

        val wfrServiceRegistrations = ArrayList<ServiceRegistration>()
        wfrServiceRegistrations.add(createWfrServiceRegistration(HOSTNAME_NODE1, WFR_URL_PREFIX))
        wfrServiceRegistrations.add(createWfrServiceRegistration(HOSTNAME_NODE2, WFR_URL_PREFIX))
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getServiceRegistrationsByType(EasyMock.eq(WorkingFileRepository.SERVICE_TYPE)))
                .andReturn(wfrServiceRegistrations).anyTimes()
        val currentJob = EasyMock.createNiceMock<Job>(Job::class.java)
        currentJob.arguments = EasyMock.anyObject<Any>() as List<String>
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(currentJob).anyTimes()
        EasyMock.expect(serviceRegistry.updateJob(EasyMock.anyObject<Any>() as Job)).andReturn(currentJob).anyTimes()
        EasyMock.expect(serviceRegistry.getChildJobs(EasyMock.anyLong())).andReturn(ArrayList()).anyTimes()
        EasyMock.replay(serviceRegistry, currentJob)
        cleanupWOH!!.setServiceRegistry(serviceRegistry)

        val httpClient = EasyMock.createNiceMock<TrustedHttpClient>(TrustedHttpClient::class.java)
        val httpResponse = EasyMock.createNiceMock<HttpResponse>(HttpResponse::class.java)
        val responseStatusLine = EasyMock.createNiceMock<StatusLine>(StatusLine::class.java)
        EasyMock.expect(responseStatusLine.statusCode).andReturn(HttpStatus.SC_OK).anyTimes()
        EasyMock.expect(httpResponse.statusLine).andReturn(responseStatusLine).anyTimes()
        EasyMock.expect(httpClient.execute(StoreUrisArgumentMatcher.createMatcher(deletedFilesURIs,
                UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX))!!)).andReturn(httpResponse).anyTimes()
        EasyMock.replay(httpClient, httpResponse, responseStatusLine)
        cleanupWOH!!.setTrustedHttpClient(httpClient)
    }

    private fun createWfrServiceRegistration(hostname: String, path: String): ServiceRegistration {
        val wfrServiceReg = EasyMock.createNiceMock<ServiceRegistration>(ServiceRegistration::class.java)
        EasyMock.expect(wfrServiceReg.host).andReturn(hostname).anyTimes()
        EasyMock.expect(wfrServiceReg.path).andReturn(path).anyTimes()
        EasyMock.replay(wfrServiceReg)
        return wfrServiceReg
    }

    private fun createWorkflowInstance(configuration: Map<String, String>?, mp: MediaPackage): WorkflowInstance {
        val wfOpInst = WorkflowOperationInstanceImpl()
        if (configuration != null) {
            for (confKey in configuration.keys) {
                wfOpInst.setConfiguration(confKey, configuration[confKey])
            }
        }
        wfOpInst.id = 1L
        wfOpInst.state = WorkflowOperationInstance.OperationState.RUNNING
        val wfInst = EasyMock.createNiceMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(wfInst.mediaPackage).andReturn(mp).anyTimes()
        EasyMock.expect(wfInst.currentOperation).andReturn(wfOpInst).anyTimes()
        EasyMock.expect(wfInst.operations).andReturn(Arrays.asList<WorkflowOperationInstance>(wfOpInst)).anyTimes()
        EasyMock.replay(wfInst)
        return wfInst
    }

    @Test
    @Throws(WorkflowOperationException::class, MediaPackageException::class)
    fun testCreanupWOHwithPreservedFlavorAndMediaPackagePathPrefix() {
        val wfInstConfig = Hashtable<String, String>()
        wfInstConfig[CleanupWorkflowOperationHandler.PRESERVE_FLAVOR_PROPERTY] = "*/source,smil/trimmed,security/*"
        wfInstConfig[CleanupWorkflowOperationHandler.DELETE_EXTERNAL] = "true"

        val mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        val mp = mpBuilder.createNew()
        val track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presenter", "source", null)
        track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                mp.identifier.compact(), track1.identifier, "track.mp4"))
        val track2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presentation", "work", null)
        track2.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                mp.identifier.compact(), track2.identifier, "track.mp4"))
        val att1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
                "presentation", "preview", null)
        att1.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                mp.identifier.compact(), att1.identifier, "preview.png"))
        val att2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
                "smil", "trimmed", null)
        att2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                mp.identifier.compact(), att2.identifier, "trimmed.smil"))
        val cat1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
                "dublincore", "episode", null)
        cat1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                mp.identifier.compact(), cat1.identifier, "dublincore.xml"))
        val cat2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
                "security", "xaml", null)
        cat2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                mp.identifier.compact(), cat2.identifier, "security.xml"))

        cleanupWOH!!.start(createWorkflowInstance(wfInstConfig, mp), null)
        Assert.assertEquals("Media package should contain at least tree elements", 3, mp.elements.size.toLong())
        var elementSelector = SimpleElementSelector()
        elementSelector.addFlavor("*/source")
        Assert.assertFalse("Media package doesn't contain an element with a preserved flavor '*/source'",
                elementSelector.select(mp, false).isEmpty())
        elementSelector = SimpleElementSelector()
        elementSelector.addFlavor("smil/trimmed")
        Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'smil/trimmed'",
                elementSelector.select(mp, false).isEmpty())
        elementSelector = SimpleElementSelector()
        elementSelector.addFlavor("security/*")
        Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'security/*'",
                elementSelector.select(mp, false).isEmpty())

        Assert.assertEquals("At least one file wasn't deleted on remote repository", 3, deletedFilesURIs.size.toLong())
    }

    @Test
    @Throws(WorkflowOperationException::class, MediaPackageException::class)
    fun testCreanupWOHwithPreservedFlavorAndCollectionPathPrefix() {
        val wfInstConfig = Hashtable<String, String>()
        wfInstConfig[CleanupWorkflowOperationHandler.PRESERVE_FLAVOR_PROPERTY] = "*/source,smil/trimmed,security/*"
        wfInstConfig[CleanupWorkflowOperationHandler.DELETE_EXTERNAL] = "true"

        val mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        val mp = mpBuilder.createNew()
        val track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presenter", "source", null)
        track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "asset", mp.identifier.compact(), track1.identifier, "track.mp4"))
        val track2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presentation", "work", null)
        track2.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "compose", mp.identifier.compact(), track2.identifier, "track.mp4"))
        val att1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
                "presentation", "preview", null)
        att1.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "compose", mp.identifier.compact(), att1.identifier, "preview.png"))
        val att2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
                "smil", "trimmed", null)
        att2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "silence", mp.identifier.compact(), att2.identifier, "trimmed.smil"))
        val cat1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
                "dublincore", "episode", null)
        cat1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "asset", mp.identifier.compact(), cat1.identifier, "dublincore.xml"))
        val cat2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
                "security", "xaml", null)
        cat2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "security", mp.identifier.compact(), cat2.identifier, "security.xml"))

        cleanupWOH!!.start(createWorkflowInstance(wfInstConfig, mp), null)
        Assert.assertEquals("Media package should contain at least tree elements", 3, mp.elements.size.toLong())
        var elementSelector = SimpleElementSelector()
        elementSelector.addFlavor("*/source")
        Assert.assertFalse("Media package doesn't contain an element with a preserved flavor '*/source'",
                elementSelector.select(mp, false).isEmpty())
        elementSelector = SimpleElementSelector()
        elementSelector.addFlavor("smil/trimmed")
        Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'smil/trimmed'",
                elementSelector.select(mp, false).isEmpty())
        elementSelector = SimpleElementSelector()
        elementSelector.addFlavor("security/*")
        Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'security/*'",
                elementSelector.select(mp, false).isEmpty())

        Assert.assertEquals("At least one file wasn't deleted on remote repository", 3, deletedFilesURIs.size.toLong())
    }

    @Test
    @Throws(WorkflowOperationException::class, MediaPackageException::class)
    fun testCreanupWOHwithoutPreservedFlavor() {
        val wfInstConfig = Hashtable<String, String>()
        wfInstConfig[CleanupWorkflowOperationHandler.DELETE_EXTERNAL] = "true"

        val mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        val mp = mpBuilder.createNew()
        val track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presenter", "source", null)
        track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "asset", mp.identifier.compact(), track1.identifier, "track.mp4"))

        cleanupWOH!!.start(createWorkflowInstance(wfInstConfig, mp), null)
        Assert.assertEquals("Media package shouldn't contain any elements", 0, mp.elements.size.toLong())
        Assert.assertEquals("One file wasn't deleted on remote repository", 1, deletedFilesURIs.size.toLong())
    }

    @Test
    @Throws(WorkflowOperationException::class, MediaPackageException::class)
    fun testCreanupWOHwithoutPreservedFlavorAndWithoutDeleteExternal() {
        val wfInstConfig = Hashtable<String, String>()

        val mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        val mp = mpBuilder.createNew()
        val track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presenter", "source", null)
        track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
                "asset", mp.identifier.compact(), track1.identifier, "track.mp4"))

        cleanupWOH!!.start(createWorkflowInstance(wfInstConfig, mp), null)
        Assert.assertEquals("Media package shouldn't contain any elements", 0, mp.elements.size.toLong())
        Assert.assertEquals("Delete on remote repository not allowed", 0, deletedFilesURIs.size.toLong())
    }

    @Test
    @Throws(WorkflowOperationException::class, MediaPackageException::class)
    fun testCreanupWOHwithsomeUnknowenUrl() {
        val wfInstConfig = Hashtable<String, String>()

        val mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        val mp = mpBuilder.createNew()
        val track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
                "presenter", "source", null)
        track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, "asset", "asset",
                mp.identifier.compact(), track1.identifier, 0, "track.mp4"))

        cleanupWOH!!.start(createWorkflowInstance(wfInstConfig, mp), null)
        Assert.assertEquals("Media package shouldn't contain any elements", 0, mp.elements.size.toLong())
        Assert.assertEquals("Delete on remote repository not allowed", 0, deletedFilesURIs.size.toLong())
    }

    /** This class should cache all URIs that are passed to mocked [TrustedHttpClient] execute method  */
    private class StoreUrisArgumentMatcher
    /** Constructor  */
    private constructor(uriCache: MutableList<URI>, matchBaseUri: URI) : IArgumentMatcher {

        /** URI's cache  */
        private val uriStore: MutableList<URI>? = null

        /** Base URI to test matches  */
        private val matchBaseUri: URI? = null

        init {
            this.uriStore = uriCache
            this.matchBaseUri = matchBaseUri
        }

        override fun matches(arg: Any): Boolean {
            if (arg !is HttpUriRequest)
                return false
            uriStore!!.add(arg.uri)
            return StringUtils.startsWith(arg.uri.toString(), matchBaseUri!!.toString())
        }

        override fun appendTo(sb: StringBuffer) {}

        companion object {

            /**
             * Create and initialize [StoreUrisArgumentMatcher]
             *
             * @param uriStore List, where to store URI's that are passed as argument to the mocked object
             * @param matchUri base URI to test on passed URI's
             * @return null
             */
            fun createMatcher(uriStore: MutableList<URI>, matchUri: URI): HttpUriRequest? {
                EasyMock.reportMatcher(StoreUrisArgumentMatcher(uriStore, matchUri))
                return null
            }
        }
    }

    companion object {

        private val HOSTNAME_NODE1 = "http://node1.opencast.org"
        private val HOSTNAME_NODE2 = "http://node2.opencast.org"
        private val WFR_URL_PREFIX = "/files"

        private fun addElementToMediaPackage(mp: MediaPackage, elemType: MediaPackageElement.Type,
                                             flavorType: String, flavorSubtype: String, uri: URI?): MediaPackageElement {
            val mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            val mpe = mpeBuilder.newElement(elemType, MediaPackageElementFlavor.flavor(
                    flavorType, flavorSubtype))
            mpe.identifier = UUID.randomUUID().toString()
            if (uri != null)
                mpe.setURI(uri)
            mp.add(mpe)
            return mpe
        }
    }

}
