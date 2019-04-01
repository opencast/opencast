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

package org.opencastproject.animate.impl.endpoint

import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.animate.api.AnimateService
import org.opencastproject.animate.api.AnimateServiceException
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Type
import java.net.URI
import java.net.URISyntaxException

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A service endpoint to expose the [AnimateService] via REST.
 */
@Path("/")
@RestService(name = "animate", title = "Animate Service", abstractText = "Create animated video clips using Synfig.", notes = ["Use <a href=https://www.synfig.org/>Synfig Studio</a> to create animation files"])
class AnimateServiceRestEndpoint : AbstractJobProducerEndpoint() {

    /** The inspection service  */
    private var animateService: AnimateService? = null

    /** The service registry  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getServiceRegistry
     */
    /**
     * Callback from the OSGi declarative services to set the service registry.
     *
     * @param serviceRegistry
     * the service registry
     */
    override var serviceRegistry: ServiceRegistry? = null
        protected set

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override val service: JobProducer?
        get() {
            if (animateService is JobProducer) {
                logger.debug("get animate service")
                return animateService as JobProducer?
            }
            return null
        }

    /**
     * Sets the animate service
     *
     * @param animateService
     * the animate service
     */
    fun setAnimateService(animateService: AnimateService) {
        this.animateService = animateService
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("animate")
    @RestQuery(name = "animate", description = "Create animates video clip", restParameters = [RestParameter(name = "animation", isRequired = true, type = STRING, description = "Location of to the animation"), RestParameter(name = "arguments", isRequired = true, type = STRING, description = "Synfig command line arguments as JSON array"), RestParameter(name = "metadata", isRequired = true, type = STRING, description = "Metadata for replacement as JSON object")], reponses = [RestResponse(description = "Animation created successfully", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Invalid data", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "Internal error", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "Returns the path to the generated animation video")
    fun animate(
            @FormParam("animation") animation: String,
            @FormParam("arguments") argumentsString: String,
            @FormParam("metadata") metadataString: String): Response {
        val gson = Gson()
        try {
            val metadata = gson.fromJson<Map<String, String>>(metadataString, stringMapType)
            val arguments = gson.fromJson<List<String>>(argumentsString, stringListType)
            logger.debug("Start animation")
            val job = animateService!!.animate(URI(animation), metadata, arguments)
            return Response.ok(JaxbJob(job)).build()
        } catch (e: JsonSyntaxException) {
            logger.debug("Invalid data passed to REST endpoint:\nanimation: {}\nmetadata: {}\narguments: {})",
                    animation, metadataString, argumentsString)
            return Response.status(Response.Status.BAD_REQUEST).build()
        } catch (e: URISyntaxException) {
            logger.debug("Invalid data passed to REST endpoint:\nanimation: {}\nmetadata: {}\narguments: {})", animation, metadataString, argumentsString)
            return Response.status(Response.Status.BAD_REQUEST).build()
        } catch (e: NullPointerException) {
            logger.debug("Invalid data passed to REST endpoint:\nanimation: {}\nmetadata: {}\narguments: {})", animation, metadataString, argumentsString)
            return Response.status(Response.Status.BAD_REQUEST).build()
        } catch (e: AnimateServiceException) {
            logger.error("Error animating file {}", animation, e)
            return Response.serverError().build()
        }

    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(AnimateServiceRestEndpoint::class.java)

        private val stringMapType = object : TypeToken<Map<String, String>>() {

        }.type
        private val stringListType = object : TypeToken<List<String>>() {

        }.type
    }

}
