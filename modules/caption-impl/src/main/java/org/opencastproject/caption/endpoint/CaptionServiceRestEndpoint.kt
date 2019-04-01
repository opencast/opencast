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

package org.opencastproject.caption.endpoint

import org.opencastproject.caption.api.CaptionService
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

import java.io.StringWriter

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Rest endpoint for [CaptionService].
 */
@Path("/")
@RestService(name = "caption", title = "Caption Service", abstractText = "This service enables conversion from one caption format to another.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class CaptionServiceRestEndpoint : AbstractJobProducerEndpoint() {

    /** The caption service  */
    protected var service: CaptionService? = null

    /** The default server URL  */
    protected var serverUrl = UrlSupport.DEFAULT_BASE_URL

    /** The default sample location URL  */
    protected var sampleLocation = "$serverUrl/workflow"

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

    /**
     * Sets the caption service
     *
     * @param service
     * the caption service to set
     */
    protected fun setCaptionService(service: CaptionService) {
        this.service = service
    }

    /**
     * Removes the caption service
     *
     * @param service
     * the caption service to remove
     */
    protected fun unsetCaptionService(service: CaptionService) {
        this.service = null
    }

    /**
     * Convert captions in catalog from one format to another.
     *
     * @param inputType
     * input format
     * @param outputType
     * output format
     * @param catalogAsXml
     * catalog containing captions
     * @param lang
     * caption language
     * @return a Response containing receipt of for conversion
     */
    @POST
    @Path("convert")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "convert", description = "Convert captions from one format to another.", restParameters = [RestParameter(description = "Captions to be converted.", isRequired = true, name = "captions", type = RestParameter.Type.TEXT), RestParameter(description = "Caption input format (for example: dfxp, subrip,...).", isRequired = false, defaultValue = "dfxp", name = "input", type = RestParameter.Type.STRING), RestParameter(description = "Caption output format (for example: dfxp, subrip,...).", isRequired = false, defaultValue = "subrip", name = "output", type = RestParameter.Type.STRING), RestParameter(description = "Caption language (for those formats that store such information).", isRequired = false, defaultValue = "en", name = "language", type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "OK, Conversion successfully completed.", responseCode = HttpServletResponse.SC_OK)], returnDescription = "The converted captions file")
    fun convert(@FormParam("input") inputType: String, @FormParam("output") outputType: String,
                @FormParam("captions") catalogAsXml: String, @FormParam("language") lang: String): Response {
        val element: MediaPackageElement
        try {
            element = MediaPackageElementParser.getFromXml(catalogAsXml)
            if (!Catalog.TYPE.equals(element.elementType))
                return Response.status(Response.Status.BAD_REQUEST).entity("Captions must be of type catalog.").build()
        } catch (e: Exception) {
            logger.info("Unable to parse serialized captions")
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        try {
            val job: Job
            if (StringUtils.isNotBlank(lang)) {
                job = service!!.convert(element as Catalog, inputType, outputType, lang)
            } else {
                job = service!!.convert(element as Catalog, inputType, outputType)
            }
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: Exception) {
            logger.error(e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Parses captions in catalog for language information.
     *
     * @param inputType
     * caption format
     * @param catalogAsXml
     * catalog containing captions
     * @return a Response containing XML with language information
     */
    @POST
    @Path("languages")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "languages", description = "Get information about languages in caption catalog (if such information is available).", restParameters = [RestParameter(description = "Captions to be examined.", isRequired = true, name = "captions", type = RestParameter.Type.TEXT), RestParameter(description = "Caption input format (for example: dfxp, subrip,...).", isRequired = false, defaultValue = "dfxp", name = "input", type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "OK, information was extracted and retrieved", responseCode = HttpServletResponse.SC_OK)], returnDescription = "Returned information about languages present in captions.")
    fun languages(@FormParam("input") inputType: String, @FormParam("captions") catalogAsXml: String): Response {
        try {
            val element = MediaPackageElementParser.getFromXml(catalogAsXml)
            if (!Catalog.TYPE.equals(element.elementType)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Captions must be of type catalog").build()
            }

            val languageArray = service!!.getLanguageList(element as Catalog, inputType)

            // build response
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.newDocument()
            val root = doc.createElement("languages")
            root.setAttribute("type", inputType)
            root.setAttribute("url", element.getURI().toString())
            for (lang in languageArray) {
                val language = doc.createElement("language")
                language.appendChild(doc.createTextNode(lang))
                root.appendChild(language)
            }

            val domSource = DOMSource(root)
            val writer = StringWriter()
            val result = StreamResult(writer)
            val transformer: Transformer
            transformer = TransformerFactory.newInstance().newTransformer()
            transformer.transform(domSource, result)

            return Response.status(Response.Status.OK).entity(writer.toString()).build()
        } catch (e: Exception) {
            logger.error(e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    protected fun generateCatalog(): String {
        return ("<catalog id=\"catalog-1\" type=\"captions/dfxp\">" + "  <mimetype>text/xml</mimetype>" + "  <url>"
                + sampleLocation + "/samples/captions.dfxp.xml</url>"
                + "  <checksum type=\"md5\">08b58d152be05a85f877cf160ee6608c</checksum>" + "</catalog>")
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

        /** The logger  */
        private val logger = LoggerFactory.getLogger(CaptionServiceRestEndpoint::class.java)
    }

}
