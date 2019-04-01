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

package org.opencastproject.animate.impl

import org.easymock.EasyMock.capture

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.IoSupport
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.HashMap

class AnimateServiceImplTest {

    private var animateService: AnimateServiceImpl? = null

    @Rule
    var testFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Skip tests if synfig is not installed
        Assume.assumeTrue(runSynfigTests)

        // create animate service
        animateService = AnimateServiceImpl()

        // create the needed mocks
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()

        val workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        val directory = testFolder.newFolder().absolutePath
        EasyMock.expect(workspace.rootDirectory()).andReturn(directory).anyTimes()
        val collection = EasyMock.newCapture<String>()
        val name = EasyMock.newCapture<String>()
        val `in` = EasyMock.newCapture<InputStream>()
        EasyMock.expect(workspace.putInCollection(capture(collection), capture(name), capture(`in`))).andAnswer {
            val output = File(directory, "out.mp4")
            FileUtils.copyInputStreamToFile(`in`.value, output)
            output.toURI()
        }.once()

        // Finish setting up the mocks
        EasyMock.replay(bc, cc, workspace)

        val serviceRegistry = EasyMock.createMock<ServiceRegistry>(ServiceRegistry::class.java)
        val type = EasyMock.newCapture<String>()
        val operation = EasyMock.newCapture<String>()
        val args = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
                .andAnswer {
                    // you could do work here to return something different if you needed.
                    val job = JobImpl(0)
                    job.jobType = type.value
                    job.operation = operation.value
                    job.arguments = args.getValue()
                    job.payload = animateService!!.process(job)
                    job
                }.anyTimes()
        EasyMock.replay(serviceRegistry)

        animateService!!.serviceRegistry = serviceRegistry
        animateService!!.setWorkspace(workspace)
    }


    @Test
    @Throws(Exception::class)
    fun testAnimate() {
        val animation = javaClass.getResource("/synfig-test-animation.sif").toURI()
        val metadata = HashMap<String, String>()
        metadata["episode.title"] = "Test"
        metadata["episode.author"] = "John Doe"
        metadata["series.title"] = "The Art Of Animation"
        val options = ArrayList<String>(2)
        options.add("-t")
        options.add("ffmpeg")
        val job = animateService!!.animate(animation, metadata, options)
        val output = File(URI(job.payload))
        Assert.assertTrue(output.isFile)
    }


    @Test
    @Throws(Exception::class)
    fun testBrokenEncodingOptions() {
        val animation = javaClass.getResource("/synfig-test-animation.sif").toURI()
        val metadata = HashMap<String, String>()
        val options = ArrayList<String>(0)
        options.add("-t")
        options.add("santa-claus")
        var job: Job? = null
        try {
            job = animateService!!.animate(animation, metadata, options)
            logger.error("The test should have never reached this.")
        } catch (e: Exception) {
            // we expect this
        }

        Assert.assertNull(job)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnimateServiceImplTest::class.java)

        private var runSynfigTests = false

        @BeforeClass
        fun setupClass() {
            var process: Process? = null
            try {
                process = ProcessBuilder(AnimateServiceImpl.SYNFIG_BINARY_DEFAULT).start()
                runSynfigTests = true
            } catch (t: Throwable) {
                logger.warn("Skipping tests due to unsatisfied synfig installation")
            } finally {
                IoSupport.closeQuietly(process)
            }
        }
    }
}
