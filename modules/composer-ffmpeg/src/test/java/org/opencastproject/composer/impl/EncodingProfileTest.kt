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


package org.opencastproject.composer.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfile.MediaType

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.net.URL
import java.util.Collections

/**
 * Tests for encoding format handling.
 */
class EncodingProfileTest {

    /** Map with encoding profiles  */
    private var profiles: Map<String, EncodingProfile>? = null

    /** Name of the h264 profile  */
    private val h264ProfileId = "h264-medium.http"

    /** Name of the cover ui profile  */
    private val coverProfileId = "cover-ui.http"

    @Rule
    var tmp = TemporaryFolder()

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val url = EncodingProfileTest::class.java.getResource("/encodingtestprofiles.properties")
        val mgr = EncodingProfileScanner()
        profiles = mgr.loadFromProperties(File(url.toURI()))
    }

    /**
     * Test (un)installing profiles
     */
    @Test
    @Throws(Exception::class)
    fun testInstall() {
        val url = EncodingProfileTest::class.java.getResource("/encodingtestprofiles.properties")
        val file = File(url.toURI())

        val mgr = EncodingProfileScanner()
        mgr.install(file)
        val profileCount = mgr.profiles.size
        assertTrue(profileCount > 0)
        mgr.update(file)
        assertEquals(mgr.profiles.size.toLong(), profileCount.toLong())
        mgr.uninstall(file)
        assertEquals(0, mgr.profiles.size.toLong())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl].
     */
    @Test
    fun testMediaTypes() {
        assertNotNull(EncodingProfile.MediaType.parseString("audio"))
        assertNotNull(EncodingProfile.MediaType.parseString("visual"))
        assertNotNull(EncodingProfile.MediaType.parseString("audiovisual"))
        assertNotNull(EncodingProfile.MediaType.parseString("enhancedaudio"))
        assertNotNull(EncodingProfile.MediaType.parseString("image"))
        assertNotNull(EncodingProfile.MediaType.parseString("imagesequence"))
        assertNotNull(EncodingProfile.MediaType.parseString("cover"))
        try {
            EncodingProfile.MediaType.parseString("foo")
            fail("Test should have failed for media type 'foo'")
        } catch (e: IllegalArgumentException) {
            // Expected
        }

    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl].
     */
    @Test
    fun testInitializationFromProperties() {
        assertNotNull(profiles)
        assertEquals(12, profiles!!.size.toLong())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getIdentifier].
     */
    @Test
    fun testGetIdentifier() {
        val profile = profiles!![h264ProfileId]
        assertEquals(h264ProfileId, profile.getIdentifier())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getName].
     */
    @Test
    fun testGetName() {
        val profile = profiles!![h264ProfileId]
        assertEquals("h.264 download medium quality", profile.getName())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getOutputType].
     */
    @Test
    fun testGetType() {
        val profile = profiles!![h264ProfileId]
        assertEquals(MediaType.Visual, profile.getOutputType())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getSuffix].
     */
    @Test
    fun testGetSuffix() {
        val profile = profiles!![h264ProfileId]
        assertEquals("-dm.m4v", profile.getSuffix())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getApplicableMediaTypes].
     */
    @Test
    fun testGetApplicableMediaTypes() {
        val profile = profiles!![h264ProfileId]
        val type = profile.getApplicableMediaType()
        assertNotNull(type)
        assertEquals(MediaType.Visual, type)
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getApplicableMediaTypes].
     */
    @Test
    fun testApplicableTo() {
        val profile = profiles!![h264ProfileId]
        assertTrue(profile.isApplicableTo(MediaType.Visual))
        assertFalse(profile.isApplicableTo(MediaType.Audio))
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getExtension].
     */
    @Test
    fun testGetExtension() {
        var profile = profiles!![h264ProfileId]
        assertNull(profile.getExtension("test"))

        // Test profile with existing extension
        profile = profiles!![coverProfileId]
        val commandline = "-i #{in.path} -y -r 1 -t 1 -f image2 -s 160x120 #{out.dir}/#{in.name}#{out.suffix}"
        assertEquals(commandline, profile.getExtension("ffmpeg.command"))
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.getExtensions].
     */
    @Test
    fun testGetExtensions() {
        var profile = profiles!![h264ProfileId]
        profile.isApplicableTo(MediaType.Visual)
        assertEquals(emptyMap<Any, Any>(), profile.getExtensions())

        // Test profile with existing extension
        profile = profiles!![coverProfileId]
        assertEquals(1, profile.getExtensions().size.toLong())
    }

    /**
     * Test method for [org.opencastproject.composer.api.EncodingProfileImpl.hasExtensions].
     */
    @Test
    fun testHasExtensions() {
        var profile = profiles!![h264ProfileId]
        assertFalse(profile.hasExtensions())

        // Test profile with existing extension
        profile = profiles!![coverProfileId]
        assertTrue(profile.hasExtensions())
    }

}
