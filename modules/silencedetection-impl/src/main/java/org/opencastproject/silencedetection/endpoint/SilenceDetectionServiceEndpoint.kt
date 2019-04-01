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

package org.opencastproject.silencedetection.endpoint

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.silencedetection.api.SilenceDetectionService
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * SilenceDetectionService REST Endpoint.
 */
@Path("/")
@RestService(name = "SilenceDetectionServiceEndpoint", title = "Silence Detection Service REST Endpoint", abstractText = "Detect silent sequences in audio file.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/silencedetection)"])
class SilenceDetectionServiceEndpoint : AbstractJobProducerEndpoint() {

    private var silenceDetectionService: SilenceDetectionService? = null
    override var serviceRegistry: ServiceRegistry? = null

    override val service: JobProducer?
        get() = if (silenceDetectionService is JobProducer) {
            silenceDetectionService as JobProducer?
        } else {
            null
        }

    @POST
    @Path("/detect")
    @Produces(MediaType.APPLICATION_XML)
    @RestQuery(name = "detect", description = "Create silence detection job.", returnDescription = "Silence detection job.", restParameters = [RestParameter(name = "track", type = RestParameter.Type.TEXT, description = "Track where to run silence detection.", isRequired = true), RestParameter(name = "referenceTracks", type = RestParameter.Type.TEXT, description = "Tracks referenced by resulting smil (as sources).", isRequired = false)], reponses = [RestResponse(description = "Silence detection job created successfully.", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Create silence detection job failed.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)])
    fun detect(@FormParam("track") trackXml: String, @FormParam("referenceTracks") referenceTracksXml: String?): Response {
        try {
            val track = MediaPackageElementParser.getFromXml(trackXml) as Track
            var job: Job? = null
            if (referenceTracksXml != null) {
                var referenceTracks: List<Track>? = null
                referenceTracks = MediaPackageElementParser.getArrayFromXml(referenceTracksXml) as List<Track>
                job = silenceDetectionService!!.detect(track, referenceTracks.toTypedArray())
            } else {
                job = silenceDetectionService!!.detect(track)
            }
            return Response.ok(JaxbJob(job!!)).build()
        } catch (ex: Exception) {
            return Response.serverError().entity(ex.message).build()
        }

    }

    fun setSilenceDetectionService(silenceDetectionService: SilenceDetectionService) {
        this.silenceDetectionService = silenceDetectionService
    }
}
