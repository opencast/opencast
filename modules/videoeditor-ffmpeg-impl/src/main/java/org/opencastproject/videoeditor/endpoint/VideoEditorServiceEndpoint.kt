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

package org.opencastproject.videoeditor.endpoint

import org.opencastproject.job.api.JaxbJobList
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.videoeditor.api.VideoEditorService

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * VideoEditorService REST Endpoint.
 */
@Path("/")
@RestService(name = "VideoEditorServiceEndpoint", title = "Video Editor Service REST Endpoint", abstractText = "Video Editor Service consumes a smil document and create corresponding video files.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/videoeditor)"])
class VideoEditorServiceEndpoint : AbstractJobProducerEndpoint() {
    override var serviceRegistry: ServiceRegistry? = null
    private var videoEditorService: VideoEditorService? = null
    private var smilService: SmilService? = null

    override val service: JobProducer?
        get() = if (videoEditorService is JobProducer) {
            videoEditorService as JobProducer?
        } else {
            null
        }

    @POST
    @Path("/process-smil")
    @Produces(MediaType.APPLICATION_XML)
    @RestQuery(name = "processsmil", description = "Create smil processing jobs.", returnDescription = "Smil processing jobs.", restParameters = [RestParameter(name = "smil", type = RestParameter.Type.TEXT, description = "Smil document to process.", isRequired = true)], reponses = [RestResponse(description = "Smil processing jobs created successfully.", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Internal server error.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)])
    fun processSmil(@FormParam("smil") smilStr: String): Response {
        val smil: Smil
        try {
            smil = smilService!!.fromXml(smilStr).smil
            val jobs = videoEditorService!!.processSmil(smil)
            return Response.ok(JaxbJobList(jobs)).build()
        } catch (ex: Exception) {
            return Response.serverError().entity(ex.message).build()
        }

    }

    fun setVideoEditorService(videoEditorService: VideoEditorService) {
        this.videoEditorService = videoEditorService
    }

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VideoEditorServiceEndpoint::class.java)
    }
}
