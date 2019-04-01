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

package org.opencastproject.sox.impl.endpoint

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.Track
import org.opencastproject.sox.api.SoxService

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import javax.ws.rs.core.Response

/**
 * Tests the behavior of the SoX rest endpoint, using a mock SoX service.
 */
class SoxRestServiceTest {

    private var job: Job? = null
    private var audioTrack: Track? = null
    private var restService: SoxRestService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
        // Set up our arguments and return values
        audioTrack = builder.newElement(Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE) as Track
        audioTrack!!.identifier = "audio1"

        job = JobImpl()
        job!!.status = Job.Status.QUEUED
        job!!.jobType = SoxService.JOB_TYPE

        // Train a mock composer with some known behavior
        val sox = EasyMock.createNiceMock<SoxService>(SoxService::class.java)
        EasyMock.expect(sox.analyze(audioTrack!!)).andReturn(job).anyTimes()
        EasyMock.expect(sox.normalize(audioTrack!!, -30f)).andReturn(job).anyTimes()
        EasyMock.replay(sox)

        // Set up the rest endpoint
        restService = SoxRestService()
        restService!!.setSoxService(sox)
        restService!!.activate(null)
    }

    @Test
    @Throws(Exception::class)
    fun testAnalyze() {
        val response = restService!!.analyze(MediaPackageElementParser.getAsXml(audioTrack))
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertEquals(JaxbJob(job!!), response.entity)
    }

    @Test
    @Throws(Exception::class)
    fun testNormalize() {
        val response = restService!!.normalize(MediaPackageElementParser.getAsXml(audioTrack), -30f)
        Assert.assertEquals(Response.Status.OK.statusCode.toLong(), response.status.toLong())
        Assert.assertEquals(JaxbJob(job!!), response.entity)
    }

}
