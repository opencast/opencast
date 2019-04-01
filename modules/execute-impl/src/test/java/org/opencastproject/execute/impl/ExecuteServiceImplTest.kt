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

package org.opencastproject.execute.impl

import org.opencastproject.execute.api.ExecuteException
import org.opencastproject.execute.api.ExecuteService
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.util.NotFoundException
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Hashtable

/**
 * Test suite for the Execute Service
 */
class ExecuteServiceImplTest {

    @Test
    @Throws(ExecuteException::class, NotFoundException::class)
    fun testNoElements() {
        val params = ArrayList<String>()
        params.add("echo")
        params.add(TEXT)

        try {
            executor!!.doProcess(params, null as MediaPackageElement?, null, null)
            Assert.fail("The input element should never be null")
        } catch (e: NullPointerException) {
            // This exception is expected
        }

    }

    @Test
    @Throws(ExecuteException::class, NotFoundException::class)
    fun testWithInputElement() {
        val params = ArrayList<String>()
        params.add("echo")
        params.add(pattern)
        val element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(baseDirURI)

        val result = executor!!.doProcess(params, element, null, null)

        Assert.assertEquals(result, "")
    }

    @Test
    @Throws(ExecuteException::class, NotFoundException::class)
    fun testWithGlobalConfigParam() {
        val params = ArrayList<String>()
        params.add("cat")
        params.add("#{$configKey1}")
        val mp: MediaPackage? = null

        val result = executor!!.doProcess(params, mp, null, null)

        // If it doesn't get a file not found, it is ok
        Assert.assertEquals(result, "")
    }

    @Test
    @Throws(ExecuteException::class, NotFoundException::class)
    fun testWithServiceConfigParam() {
        val params = ArrayList<String>()
        params.add("cat")
        params.add("#{$configKey2}")
        val mp: MediaPackage? = null

        val result = executor!!.doProcess(params, mp, null, null)

        // If it doesn't get a file not found, it is ok
        Assert.assertEquals(result, "")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ExecuteServiceImplTest::class.java)

        private var executor: ExecuteServiceImpl? = null
        private val TEXT = "En un lugar de la Mancha de cuyo nombre no quiero acordarme..."
        private var pattern: String? = null
        private var baseDirURI: URI? = null
        private var baseDir: File? = null

        private var bundleContext: BundleContext? = null
        private var cc: ComponentContext? = null
        private var configKey1: String? = null
        private var configKey2: String? = null

        @BeforeClass
        @Throws(URISyntaxException::class, NotFoundException::class, IOException::class)
        fun prepareTest() {
            // Get the base directory
            baseDirURI = ExecuteServiceImplTest::class.java.getResource("/").toURI()
            baseDir = File(baseDirURI!!)

            // Set up mock context
            configKey1 = "edu.harvard.dce.param1"
            val configValue1 = baseDir!!.absolutePath + "/test.txt"
            bundleContext = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
            EasyMock.expect(bundleContext!!.getProperty(configKey1)).andReturn(configValue1).anyTimes()
            EasyMock.replay(bundleContext!!)
            cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
            EasyMock.expect(cc!!.bundleContext).andReturn(bundleContext).anyTimes()
            configKey2 = "edu.harvard.dce.param2"
            val configValue2 = baseDir!!.absolutePath + "/test.txt"
            val props = Hashtable<String, Any>()
            props[configKey2!!] = configValue2
            EasyMock.expect<Dictionary<String, Any>>(cc!!.properties).andReturn(props)
            EasyMock.replay(cc!!)

            // Create the executor service
            executor = ExecuteServiceImpl()
            executor!!.activate(cc!!)

            // Create a mock workspace
            val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
            EasyMock.expect(workspace.get(baseDirURI)).andReturn(baseDir).anyTimes()
            EasyMock.replay(workspace)
            executor!!.setWorkspace(workspace)

            // Set up the text pattern to test
            pattern = String.format("The specified track (%s) is in the following location: %s",
                    ExecuteService.INPUT_FILE_PATTERN, ExecuteService.INPUT_FILE_PATTERN)
        }
    }
}
