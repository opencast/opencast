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


package org.opencastproject.composer.impl.endpoint

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfileImpl
import org.opencastproject.composer.api.EncodingProfileList
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.Track
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Collections

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.ArrayList

import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * Tests the behavior of the composer rest endpoint, using a mock composer service.
 */
class ComposerRestServiceTest {

    private var job: JobImpl? = null
    private var profile: EncodingProfileImpl? = null
    private var profile2: EncodingProfileImpl? = null
    private var profileList: EncodingProfileList? = null
    private var audioTrack: Track? = null
    private var videoTrack: Track? = null
    private var profileId: String? = null
    private var profileId2: String? = null
    private var profileIdsList: MutableList<String>? = null
    private var profileIds: String? = null
    private var restService: ComposerRestService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        // Set up our arguments and return values
        audioTrack = builder.newElement(Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE) as Track
        audioTrack!!.identifier = "audio1"

        videoTrack = builder.newElement(Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE) as Track
        videoTrack!!.identifier = "video1"

        profileId = "profile1"
        profileId2 = "profile2"

        job = JobImpl(1)
        job!!.status = Job.Status.QUEUED
        job!!.jobType = ComposerService.JOB_TYPE
        profile = EncodingProfileImpl()
        profile!!.identifier = profileId
        profile2 = EncodingProfileImpl()
        profile2!!.identifier = profileId2
        profileIds = "$profileId,$profileId2"
        profileIdsList = ArrayList()
        profileIdsList!!.add(profileId)
        profileIdsList!!.add(profileId2)
        val list = ArrayList<EncodingProfileImpl>()
        list.add(profile)
        list.add(profile2)
        profileList = EncodingProfileList(list)

        // Train a mock composer with some known behavior
        val composer = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composer.encode(videoTrack, profileId)).andReturn(job).anyTimes()
        EasyMock.expect(composer.multiEncode(videoTrack, profileIdsList)).andReturn(job).anyTimes()
        EasyMock.expect(composer.mux(videoTrack, audioTrack, profileId)).andReturn(job).anyTimes()
        EasyMock.expect(composer.listProfiles()).andReturn(list.toTypedArray<EncodingProfile>())
        EasyMock.expect(composer.getProfile(profileId)).andReturn(profile)
        EasyMock.expect(composer.getProfile(profileId2)).andReturn(profile2)
        EasyMock.expect(composer.concat(EasyMock.eq<String>(profileId), EasyMock.eq(Dimension(640, 480)), EasyMock.anyBoolean(),
                EasyMock.notNull<Any>() as Track, EasyMock.notNull<Any>() as Track)).andReturn(job)
        EasyMock.expect(composer.concat(EasyMock.eq<String>(profileId), EasyMock.eq(Dimension(640, 480)),
                EasyMock.gt(0.0f), EasyMock.anyBoolean(), EasyMock.notNull<Any>() as Track, EasyMock.notNull<Any>() as Track))
                .andReturn(job)
        EasyMock.replay(composer)

        // Set up the rest endpoint
        restService = ComposerRestService()
        restService!!.setComposerService(composer)
        restService!!.activate(null)
    }

    @Test
    @Throws(Exception::class)
    fun testMissingArguments() {
        // Ensure the rest endpoint tests for missing parameters
        var response = restService!!.encode(generateVideoTrack(), null)
        Assert.assertEquals(Status.BAD_REQUEST.statusCode.toLong(), response.status.toLong())

        response = restService!!.encode(null, "profile")
        Assert.assertEquals(Status.BAD_REQUEST.statusCode.toLong(), response.status.toLong())

        response = restService!!.mux(generateAudioTrack(), null, "profile")
        Assert.assertEquals(Status.BAD_REQUEST.statusCode.toLong(), response.status.toLong())

        response = restService!!.mux(null, generateVideoTrack(), "profile")
        Assert.assertEquals(Status.BAD_REQUEST.statusCode.toLong(), response.status.toLong())

        response = restService!!.mux(generateAudioTrack(), generateVideoTrack(), null)
        Assert.assertEquals(Status.BAD_REQUEST.statusCode.toLong(), response.status.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testEncode() {
        val response = restService!!.encode(generateVideoTrack(), profileId)
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertEquals(JaxbJob(job!!), response.entity)
    }

    @Test
    @Throws(Exception::class)
    fun testMux() {
        val response = restService!!.mux(generateAudioTrack(), generateVideoTrack(), profileId)
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertEquals(JaxbJob(job!!), response.entity)
    }

    @Test
    @Throws(Exception::class)
    fun testMultiEncode() {
        val response = restService!!.multiEncode(generateVideoTrack(), profileIds)
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertEquals(JaxbJob(job!!), response.entity)
    }

    @Test
    @Throws(Exception::class)
    fun testProfiles() {
        val response = restService!!.getProfile(profileId)
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertEquals(profile, response.entity)

        try {
            restService!!.getProfile("some other ID")
            Assert.fail("This ID should cause the rest endpoint to throw")
        } catch (e: NotFoundException) {
            // expected
        }

        val list = restService!!.listProfiles()
        Assert.assertEquals(profileList, list)
    }

    @Test
    @Throws(Exception::class)
    fun testConcat() {
        val dimension = Dimension(640, 480)
        val videoTrack = MediaPackageElementParser.getFromXml(generateVideoTrack()) as Track
        val sourceTracks = MediaPackageElementParser.getArrayAsXml(Collections.list(videoTrack, videoTrack))
        val response = restService!!.concat(sourceTracks, profileId, Serializer.json(dimension).toJson(), "25", "false")
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertNotNull("Concat rest endpoint should send a job in response", response.entity)
    }

    protected fun generateVideoTrack(): String {
        return ("<track xmlns=\"http://mediapackage.opencastproject.org\" id=\"video1\" type=\"presentation/source\">\n"
                + "  <mimetype>video/quicktime</mimetype>\n"
                + "  <url>serverUrl/workflow/samples/camera.mpg</url>\n"
                + "  <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
                + "  <duration>14546</duration>\n" + "  <video>\n"
                + "    <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
                + "    <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
                + "    <resolution>640x480</resolution>\n" + "    <scanType type=\"progressive\" />\n"
                + "    <bitrate>540520</bitrate>\n" + "    <frameRate>2</frameRate>\n" + "  </video>\n" + "</track>")
    }

    protected fun generateAudioTrack(): String {
        return ("<track xmlns=\"http://mediapackage.opencastproject.org\" id=\"audio1\" type=\"presentation/source\">\n"
                + "  <mimetype>audio/mp3</mimetype>\n"
                + "  <url>serverUrl/workflow/samples/audio.mp3</url>\n"
                + "  <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n"
                + "  <duration>10472</duration>\n" + "  <audio>\n" + "    <channels>2</channels>\n"
                + "    <bitdepth>0</bitdepth>\n" + "    <bitrate>128004.0</bitrate>\n"
                + "    <samplingrate>44100</samplingrate>\n" + "  </audio>\n" + "</track>")
    }

}
