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

package org.opencastproject.textanalyzer.impl.endpoint

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.textanalyzer.api.TextAnalyzerService
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * The REST endpoint for MediaAnalysisServices
 */
@Path("")
@RestService(name = "textanalysis", title = "Text Analysis Service", abstractText = "This service enables conversion from one caption format to another.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class TextAnalysisRestEndpoint : AbstractJobProducerEndpoint() {

    /** The text analyzer  */
    protected var service: TextAnalyzerService? = null

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
     * Callback from OSGi that is called when this service is activated.
     *
     * @param cc
     * OSGi component context
     */
    fun activate(cc: ComponentContext) {
        // String serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("")
    @RestQuery(name = "analyze", description = "Submit a track for analysis.", restParameters = [RestParameter(description = "The image to analyze for text.", isRequired = true, name = "image", type = RestParameter.Type.FILE)], reponses = [RestResponse(description = "OK, The receipt to use when polling for the resulting mpeg7 catalog.", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The argument cannot be parsed into a media package element.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "The service is unavailable at the moment.", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE)], returnDescription = "The receipt to use when polling for the resulting mpeg7 catalog.")
    fun analyze(@FormParam("image") image: String): Response {
        if (service == null)
            throw WebApplicationException(Status.SERVICE_UNAVAILABLE)
        try {
            val element = MediaPackageElementParser.getFromXml(image) as? Attachment
                    ?: throw WebApplicationException(Status.BAD_REQUEST)
            val job = service!!.extract(element)
            return Response.ok(JaxbJob(job)).build()
        } catch (e: Exception) {
            logger.info(e.message, e)
            return Response.serverError().build()
        }

    }

    /**
     * Sets the text analyzer
     *
     * @param textAnalyzer
     * the text analyzer
     */
    protected fun setTextAnalyzer(textAnalyzer: TextAnalyzerService) {
        this.service = textAnalyzer
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override fun getService(): JobProducer? {
        return if (service is JobProducer)
            service as JobProducer?
        else
            null
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(TextAnalysisRestEndpoint::class.java)
    }

}
