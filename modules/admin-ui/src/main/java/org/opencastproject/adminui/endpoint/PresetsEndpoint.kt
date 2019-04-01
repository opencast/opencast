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

package org.opencastproject.adminui.endpoint

import javax.servlet.http.HttpServletResponse.SC_OK
import javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import javax.ws.rs.core.Response.Status.BAD_REQUEST

import org.opencastproject.presets.api.PresetProvider
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "PresetsProxyService", title = "UI Presets", abstractText = "This service provides the presets data for the UI.", notes = ["This service offers information about organizations and series for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
class PresetsEndpoint {
    /** A preset provider to get the presets from.  */
    private var presetProvider: PresetProvider? = null

    fun setPresetProvider(presetProvider: PresetProvider) {
        this.presetProvider = presetProvider
    }

    protected fun activate(cc: ComponentContext) {
        logger.info("Activate presets endpoint")
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{seriesId}/property/{propertyName}.json")
    @RestQuery(name = "getProperty", description = "Returns a property value if set as a preset", returnDescription = "Returns the property value", pathParameters = [RestParameter(name = "seriesId", description = "ID of series", isRequired = true, type = Type.STRING), RestParameter(name = "propertyName", description = "Name of the property which is the key for it", isRequired = true, type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "The access control list."), RestResponse(responseCode = SC_UNAUTHORIZED, description = "If the current user is not authorized to perform this action")])
    @Throws(UnauthorizedException::class, NotFoundException::class)
    fun getProperty(@PathParam("seriesId") seriesId: String, @PathParam("propertyName") propertyName: String): Response {
        if (StringUtils.isBlank(seriesId)) {
            logger.warn("Series id parameter is blank '{}'.", seriesId)
            return Response.status(BAD_REQUEST).build()
        }
        if (StringUtils.isBlank(propertyName)) {
            logger.warn("Series property name parameter is blank '{}'.", propertyName)
            return Response.status(BAD_REQUEST).build()
        }
        try {
            val propertyValue = presetProvider!!.getProperty(seriesId, propertyName)
            return Response.ok(propertyValue).build()
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not perform search query", e)
        }

        throw WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PresetsEndpoint::class.java)
    }
}
