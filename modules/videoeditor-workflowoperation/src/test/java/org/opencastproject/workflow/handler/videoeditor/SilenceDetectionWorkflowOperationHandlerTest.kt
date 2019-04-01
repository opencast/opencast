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

package org.opencastproject.workflow.handler.videoeditor

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException
import org.opencastproject.silencedetection.api.SilenceDetectionService
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.xml.sax.SAXException

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.HashMap

import javax.xml.bind.JAXBException

/**
 * Test class for [SilenceDetectionWorkflowOperationHandler]
 */
class SilenceDetectionWorkflowOperationHandlerTest {

    private var silenceDetectionOperationHandler: SilenceDetectionWorkflowOperationHandler? = null
    private var silenceDetectionServiceMock: SilenceDetectionService? = null
    private var smilService: SmilService? = null
    private var workspaceMock: Workspace? = null

    private var mpURI: URI? = null
    private var mp: MediaPackage? = null
    private var smilURI: URI? = null

    private val defaultConfiguration: Map<String, String>
        get() {
            val configuration = HashMap<String, String>()
            configuration["source-flavors"] = "*/audio"
            configuration["smil-flavor-subtype"] = "smil"
            configuration["reference-tracks-flavor"] = "*/preview"
            return configuration
        }

    @Before
    @Throws(URISyntaxException::class, MediaPackageException::class, MalformedURLException::class, IOException::class, SmilException::class, JAXBException::class, SAXException::class)
    fun setUp() {

        val mpBuilder = MediaPackageBuilderFactory.newInstance()
                .newMediaPackageBuilder()

        mpURI = SilenceDetectionWorkflowOperationHandlerTest::class.java
                .getResource("/silencedetection_mediapackage.xml").toURI()
        mp = mpBuilder.loadFromXml(mpURI!!.toURL().openStream())
        smilURI = SilenceDetectionWorkflowOperationHandlerTest::class.java
                .getResource("/silencedetection_smil_filled.smil").toURI()

        // create service mocks
        smilService = SmilServiceMock.createSmilServiceMock(smilURI)
        silenceDetectionServiceMock = EasyMock.createNiceMock<SilenceDetectionService>(SilenceDetectionService::class.java)
        workspaceMock = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        // setup SilenceDetectionWorkflowOperationHandler
        silenceDetectionOperationHandler = SilenceDetectionWorkflowOperationHandler()
        silenceDetectionOperationHandler!!.setJobBarrierPollingInterval(0)
        silenceDetectionOperationHandler!!.setDetectionService(silenceDetectionServiceMock)
        silenceDetectionOperationHandler!!.setSmilService(smilService)
        silenceDetectionOperationHandler!!.setWorkspace(workspaceMock)
    }

    private fun getWorkflowInstance(mp: MediaPackage?, configurations: Map<String, String>): WorkflowInstanceImpl {
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowInstance.WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op", WorkflowOperationInstance.OperationState.RUNNING)
        operation.template = "silence"
        operation.state = WorkflowOperationInstance.OperationState.RUNNING
        for (key in configurations.keys) {
            operation.setConfiguration(key, configurations[key])
        }
        val operations = ArrayList<WorkflowOperationInstance>(1)
        operations.add(operation)
        workflowInstance.operations = operations
        return workflowInstance
    }


    @Test
    @Throws(WorkflowOperationException::class, SilenceDetectionFailedException::class, NotFoundException::class, ServiceRegistryException::class, MediaPackageException::class, SmilException::class, MalformedURLException::class, JAXBException::class, SAXException::class, IOException::class)
    fun testStartOperation() {

        val smil = smilService!!.fromXml(File(smilURI!!)).smil
        val job = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job.payload).andReturn(smil.toXML()).anyTimes()
        EasyMock.expect<Status>(job.status).andReturn(Job.Status.FINISHED)
        EasyMock.expect(silenceDetectionServiceMock!!.detect(
                EasyMock.anyObject<Any>() as Track,
                EasyMock.anyObject<Any>() as Array<Track>))
                .andReturn(job)
        EasyMock.expect(workspaceMock!!.put(
                EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream))
                .andReturn(smilURI)
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        silenceDetectionOperationHandler!!.setServiceRegistry(serviceRegistry)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job)
        EasyMock.replay(job, serviceRegistry, silenceDetectionServiceMock, workspaceMock)
        val workflowInstance = getWorkflowInstance(mp, defaultConfiguration)
        val result = silenceDetectionOperationHandler!!.start(workflowInstance, null)
        Assert.assertNotNull("SilenceDetectionWorkflowOperationHandler workflow operation returns null " + "but should be an instantiated WorkflowOperationResult", result)
        EasyMock.verify(silenceDetectionServiceMock, workspaceMock)

        val worflowOperationInstance = workflowInstance.currentOperation
        val smilFlavorSubtypeProperty = worflowOperationInstance!!.getConfiguration("smil-flavor-subtype")

        // test media package contains new smil catalog
        val smilPartialFlavor = MediaPackageElementFlavor("*", smilFlavorSubtypeProperty)
        val smilCatalogs = mp!!.getCatalogs(smilPartialFlavor)
        Assert.assertTrue("Media package should contain a smil catalog",
                smilCatalogs != null && smilCatalogs.size > 0)
    }
}
