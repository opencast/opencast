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

package org.opencastproject.workflow.handler.composer

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.util.HashMap

class ImageConvertWorkflowOperationHandlerTest {

    private var mp: MediaPackage? = null

    @Before
    @Throws(URISyntaxException::class, MalformedURLException::class, IOException::class, MediaPackageException::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.loadFromXml(javaClass.getResource("/image_convert_mediapackage.xml").toURI().toURL().openStream())
    }

    @Test
    @Throws(WorkflowOperationException::class, NotFoundException::class, IOException::class, URISyntaxException::class, EncoderException::class, MediaPackageException::class, ServiceRegistryException::class)
    fun testImageConvert() {
        // create workflow operation configuration
        val config = HashMap<String, String>()
        config["source-flavor"] = "image/intro"
        config["target-flavor"] = "image/converted"
        config["target-tags"] = "convert"
        config["encoding-profile"] = "image.convert"
        // mock workspace
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        val targetElementUri = URI("/converted.jpg")
        EasyMock.expect(workspace.moveTo(EasyMock.anyObject(URI::class.java), EasyMock.anyString(), EasyMock.anyString(),
                EasyMock.anyString())).andReturn(targetElementUri).anyTimes()
        // mock job to be created
        val job = EasyMock.createNiceMock<Job>(Job::class.java)
        val jobPayloadAttachment = IOUtils.resourceToString("/image_convert_attachment.xml", Charset.forName("UTF-8"))
        EasyMock.expect(job.payload).andReturn(jobPayloadAttachment).anyTimes()
        EasyMock.expect<Status>(job.status).andReturn(Job.Status.FINISHED)
        // mock service registry
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job)
        // mock composer service
        val composerService = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        val sourceImageAttachmentCapture = EasyMock.newCapture<Attachment>()
        val encodingProfilesCapture = EasyMock.newCapture<Array<String>>()
        EasyMock.expect(composerService.convertImage(EasyMock.capture(sourceImageAttachmentCapture),
                *EasyMock.capture(encodingProfilesCapture))).andReturn(job)
        val encodingProfile = EasyMock.createNiceMock<EncodingProfile>(EncodingProfile::class.java)
        EasyMock.expect(composerService.getProfile("image.convert")).andReturn(encodingProfile)

        EasyMock.replay(workspace, composerService, job, encodingProfile, serviceRegistry)

        val workflowInstance = mockWorkflowInstance(config)
        // initialize WOH
        val imageConvertWOH = ImageConvertWorkflowOperationHandler()
        imageConvertWOH.setServiceRegistry(serviceRegistry)
        imageConvertWOH.setWorkspace(workspace)
        imageConvertWOH.setComposerService(composerService)
        // run test
        val result = imageConvertWOH.start(workflowInstance, null)
        // check result
        val resultMP = result.mediaPackage
        val resultElements = resultMP.getAttachments(MediaPackageElementFlavor.parseFlavor("image/converted"))
        Assert.assertNotNull(resultElements)
        Assert.assertEquals(1, resultElements.size.toLong())
        val convertedImageAttachment = resultElements[0]
        Assert.assertTrue(convertedImageAttachment.containsTag("convert"))

        // check captures
        Assert.assertTrue(sourceImageAttachmentCapture.hasCaptured())
        Assert.assertEquals("image/intro", sourceImageAttachmentCapture.value.flavor.toString())

        Assert.assertTrue(encodingProfilesCapture.hasCaptured())
        Assert.assertEquals("image.convert", encodingProfilesCapture.value)
    }

    private fun mockWorkflowInstance(configurations: Map<String, String>): WorkflowInstance {
        val operation = EasyMock.createNiceMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        EasyMock.expect(operation.getConfiguration(EasyMock.anyString())).andAnswer {
            val key = EasyMock.getCurrentArguments()[0] as String
            configurations[key]
        }.anyTimes()

        val workflowInstance = EasyMock.createNiceMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.mediaPackage).andReturn(mp).anyTimes()
        EasyMock.expect(workflowInstance.currentOperation).andReturn(operation).anyTimes()
        EasyMock.replay(operation, workflowInstance)
        return workflowInstance
    }
}
