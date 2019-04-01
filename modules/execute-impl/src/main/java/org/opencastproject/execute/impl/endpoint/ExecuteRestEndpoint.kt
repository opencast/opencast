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


package org.opencastproject.execute.impl.endpoint

import org.opencastproject.execute.api.ExecuteException
import org.opencastproject.execute.api.ExecuteService
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
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
 * The REST endpoint for [ExecuteService]s
 */
@Path("/")
// Endpoint to the execute service, that runs CLI commands using MediaPackageElement's as parameters
@RestService(name = "execute", title = "Execute Service", notes = [""], abstractText = "Runs CLI commands with MediaPackageElement's as parameters")
class ExecuteRestEndpoint : AbstractJobProducerEndpoint() {

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

    /** The execute service  */
    protected var service: ExecuteService

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path(ExecuteService.ENDPOINT_NAME)
    @RestQuery(name = "name", description = "Executes the given command", restParameters = [RestParameter(description = "The command to execute", isRequired = true, name = ExecuteService.EXEC_FORM_PARAM, type = RestParameter.Type.STRING), RestParameter(description = "The arguments to the command", isRequired = true, name = ExecuteService.PARAMS_FORM_PARAM, type = RestParameter.Type.STRING), RestParameter(description = "The estimated load placed on the system by this command", isRequired = false, name = ExecuteService.LOAD_FORM_PARAM, type = RestParameter.Type.FLOAT), RestParameter(description = "The mediapackage to apply the command to. Either this or " + ExecuteService.INPUT_ELEM_FORM_PARAM + " are required", isRequired = false, name = ExecuteService.INPUT_MP_FORM_PARAM, type = RestParameter.Type.TEXT), RestParameter(description = "The mediapackage element to apply the command to. Either this or " + ExecuteService.INPUT_MP_FORM_PARAM + " are required", isRequired = false, name = ExecuteService.INPUT_ELEM_FORM_PARAM, type = RestParameter.Type.TEXT), RestParameter(description = "The mediapackage element produced by the command", isRequired = false, name = ExecuteService.OUTPUT_NAME_FORM_PARAMETER, type = RestParameter.Type.STRING), RestParameter(description = "The type of the returned element", isRequired = false, name = ExecuteService.TYPE_FORM_PARAMETER, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "XML-encoded Job is returned.", responseCode = HttpServletResponse.SC_NO_CONTENT), RestResponse(description = "Service unavailabe or not currently present", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE), RestResponse(description = "Incorrect parameters", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "Problem executing the command or serializing the arguments/results", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun execute(@FormParam(ExecuteService.EXEC_FORM_PARAM) exec: String,
                @FormParam(ExecuteService.PARAMS_FORM_PARAM) params: String,
                @FormParam(ExecuteService.LOAD_FORM_PARAM) loadParam: Float?,
                @FormParam(ExecuteService.INPUT_ELEM_FORM_PARAM) inputElementStr: String,
                @FormParam(ExecuteService.INPUT_MP_FORM_PARAM) inputMpStr: String,
                @FormParam(ExecuteService.OUTPUT_NAME_FORM_PARAMETER) outputFileName: String,
                @FormParam(ExecuteService.TYPE_FORM_PARAMETER) elementTypeStr: String): Response {

        checkNotNull(service)
        try {

            var expectedType: MediaPackageElement.Type? = null
            if (StringUtils.isNotBlank(elementTypeStr)) {
                for (candidateType in MediaPackageElement.Type.values())
                    if (candidateType.toString().equals(elementTypeStr, ignoreCase = true)) {
                        expectedType = candidateType
                        break
                    }
                if (expectedType == null) {
                    logger.error("Wrong element type specified: {}", elementTypeStr)
                    return Response.status(Response.Status.BAD_REQUEST).build()
                }
            }

            var load = 1.0f
            if (loadParam != null) {
                load = loadParam
            }

            var retJob: Job? = null
            if (StringUtils.isNotBlank(inputElementStr) && StringUtils.isNotBlank(inputMpStr)) {
                logger.error("Only one input MediaPackage OR input MediaPackageElement can be set at the same time")
                return Response.status(Response.Status.BAD_REQUEST).build()
            } else if (StringUtils.isNotBlank(inputElementStr)) {
                val inputElement = MediaPackageElementParser.getFromXml(inputElementStr)
                retJob = service.execute(exec, params, inputElement, outputFileName, expectedType!!, load)
            } else if (StringUtils.isNotBlank(inputMpStr)) {
                val inputMp = MediaPackageParser.getFromXml(inputMpStr)
                retJob = service.execute(exec, params, inputMp, outputFileName, expectedType!!, load)
            } else {
                logger.error("A MediaPackage OR MediaPackageElement must be provided")
                return Response.status(Response.Status.BAD_REQUEST).build()
            }

            return Response.ok(JaxbJob(retJob)).build()

        } catch (e: IllegalArgumentException) {
            logger.error("The expected element type is required if an output filename is specified")
            return Response.status(Response.Status.BAD_REQUEST).build()
        } catch (e: MediaPackageException) {
            logger.error("Received excepcion: {}", e.message)
            return Response.serverError().build()
        } catch (e: ExecuteException) {
            logger.error("Received error from the execute service: {}", e.message)
            return Response.serverError().build()
        }

    }


    /**
     * Sets the service
     *
     * @param service
     */
    fun setExecuteService(service: ExecuteService) {
        this.service = service
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override fun getService(): JobProducer? {
        return if (service is JobProducer)
            service as JobProducer
        else
            null
    }

    /**
     * Checks if the service or services are available, if not it handles it by returning a 503 with a message
     *
     * @param services
     * an array of services to check
     */
    protected fun checkNotNull(vararg services: Any) {
        if (services != null) {
            for (`object` in services) {
                if (`object` == null) {
                    throw javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE)
                }
            }
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ExecuteRestEndpoint::class.java)
    }
}
