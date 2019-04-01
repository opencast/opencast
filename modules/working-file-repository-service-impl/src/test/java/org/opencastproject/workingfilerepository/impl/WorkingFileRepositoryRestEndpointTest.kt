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


package org.opencastproject.workingfilerepository.impl

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.UrlSupport

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.InputStream
import java.util.Arrays
import java.util.HashMap

import javax.ws.rs.core.Response

class WorkingFileRepositoryRestEndpointTest {

    private var endpoint: WorkingFileRepositoryRestEndpoint? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.expect(organization.id).andReturn("org1").anyTimes()
        val orgProps = HashMap<String, String>()
        orgProps[OpencastConstants.WFR_URL_ORG_PROPERTY] = UrlSupport.DEFAULT_BASE_URL
        EasyMock.expect(organization.properties).andReturn(orgProps).anyTimes()
        EasyMock.replay(organization)

        val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService)

        endpoint = WorkingFileRepositoryRestEndpoint()
        endpoint!!.setSecurityService(securityService)
        endpoint!!.pathPrefix = "target/endpointroot"
        FileUtils.forceMkdir(File(endpoint!!.pathPrefix))
        endpoint!!.serverUrl = UrlSupport.DEFAULT_BASE_URL
        endpoint!!.servicePath = WorkingFileRepositoryImpl.URI_PREFIX
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteQuietly(File(endpoint!!.pathPrefix))
    }

    @Test
    @Throws(Exception::class)
    fun testExtractImageContentType() {
        val mediaPackageId = "mp"
        val image = "element1"
        var `in`: InputStream? = null
        var responseIn: InputStream? = null

        try {
            `in` = javaClass.getResourceAsStream("/opencast_header.gif")
            endpoint!!.put(mediaPackageId, image, "opencast_header.gif", `in`!!)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        // execute gets, and ensure that the content types are correct
        val response = endpoint!!.restGet(mediaPackageId, image, null)

        Assert.assertEquals("Gif content type", "image/gif", response.metadata.getFirst("Content-Type"))

        // Make sure the image byte stream was not modified by the content type detection
        try {
            `in` = javaClass.getResourceAsStream("/opencast_header.gif")
            val bytesFromClasspath = IOUtils.toByteArray(`in`!!)
            responseIn = response.entity as InputStream
            val bytesFromRepo = IOUtils.toByteArray(responseIn)
            Assert.assertTrue(Arrays.equals(bytesFromClasspath, bytesFromRepo))
        } finally {
            IOUtils.closeQuietly(`in`)
            IOUtils.closeQuietly(responseIn)
        }
    }


    @Test
    @Throws(Exception::class)
    fun testExtractImageContentTypeFromCollection() {
        var `in`: InputStream? = null
        val responseIn: InputStream? = null

        try {
            `in` = javaClass.getResourceAsStream("/opencast_header.gif")
            endpoint!!.putInCollection("collection-2", "opencast_header.gif", `in`!!)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        // execute gets, and ensure that the content types are correct
        val response = endpoint!!.restGetFromCollection("collection-2", "opencast_header.gif")

        Assert.assertEquals("Gif content type", "image/gif", response.metadata.getFirst("Content-Type"))
    }


    @Test
    @Throws(Exception::class)
    fun testExtractXmlContentType() {
        val mediaPackageId = "mp"
        val dc = "element1"
        javaClass.getResourceAsStream("/dublincore.xml").use { `in` -> endpoint!!.put(mediaPackageId, dc, "dublincore.xml", `in`) }

        // execute gets, and ensure that the content types are correct
        val response = endpoint!!.restGet(mediaPackageId, dc, null)

        Assert.assertEquals("DC content type", "text/xml", response.metadata.getFirst("Content-Type"))

        // Make sure the image byte stream was not modified by the content type detection
        javaClass.getResourceAsStream("/dublincore.xml").use { `in` ->
            val imageBytesFromClasspath = IOUtils.toByteArray(`in`)
            (response.entity as InputStream).use { responseIn ->
                val imageBytesFromRepo = IOUtils.toByteArray(responseIn)
                Assert.assertTrue(Arrays.equals(imageBytesFromClasspath, imageBytesFromRepo))
            }
        }
    }

    @Throws(Exception::class)
    fun testEtag() {
        val mediaPackageId = "mp"
        val dc = "element1"
        var `in`: InputStream? = null
        var responseIn: InputStream? = null
        try {
            `in` = javaClass.getResourceAsStream("/dublincore.xml")
            endpoint!!.put(mediaPackageId, dc, "dublincore.xml", `in`!!)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        try {
            `in` = javaClass.getResourceAsStream("/dublincore.xml")
            val md5 = DigestUtils.md5Hex(`in`!!)
            var response = endpoint!!.restGet(mediaPackageId, dc, md5)
            Assert.assertEquals(Response.Status.NOT_MODIFIED.statusCode.toLong(), response.status.toLong())
            responseIn = response.entity as InputStream
            Assert.assertNull(responseIn)
            response = endpoint!!.restGet(mediaPackageId, dc, "foo")
            Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
            responseIn = response.entity as InputStream
            Assert.assertNotNull(responseIn)
        } finally {
            IOUtils.closeQuietly(`in`)
            IOUtils.closeQuietly(responseIn)
        }

    }

}
