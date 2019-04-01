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
package org.opencastproject.security.urlsigning.service.impl

import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import org.apache.commons.lang3.StringUtils.trimToNull
import org.opencastproject.util.doc.rest.RestParameter.Type.STRING

import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.joda.time.DateTime
import org.joda.time.DateTimeConstants

import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@RestService(name = "urlsigning", title = "URL Signing Endpoint", notes = ["This is a testing endpoint to play around with the URL Signing Service"], abstractText = "")
class UrlSigningEndpoint {

    private var signingService: UrlSigningService? = null

    /** OSGi DI callback  */
    internal fun setUrlSigningService(signingService: UrlSigningService) {
        this.signingService = signingService
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("accepts")
    @RestQuery(name = "accepts", description = "Checks if the signing service accepts to sign the URL", restParameters = [RestParameter(name = "baseUrl", isRequired = true, description = "The URL to sign", type = STRING)], reponses = [RestResponse(description = "'true' or 'false'", responseCode = 200)], returnDescription = "")
    fun accepts(@QueryParam("baseUrl") baseUrl: String): Response {
        return if (signingService!!.accepts(baseUrl))
            Response.ok(java.lang.Boolean.TRUE.toString()).build()
        else
            Response.ok(java.lang.Boolean.FALSE.toString()).build()
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("sign")
    @RestQuery(name = "sign", description = "Return a signed URL", restParameters = [RestParameter(name = "baseUrl", isRequired = true, description = "The URL to sign", type = STRING), RestParameter(defaultValue = "0", description = "The UNIX epoch time until when a signed URL should remain valid", isRequired = true, name = "validUntil", type = RestParameter.Type.INTEGER), RestParameter(defaultValue = "0", description = "The UNIX epoch time from when a signed URL should become valid", isRequired = false, name = "validFrom", type = RestParameter.Type.INTEGER), RestParameter(defaultValue = "", description = "The IP addresse of the user that is allowed to access the resource", type = STRING, isRequired = false, name = "ipAddr")], reponses = [RestResponse(description = "A URL", responseCode = 200)], returnDescription = "")
    fun sign(@QueryParam("baseUrl") baseUrl: String, @QueryParam("validUntil") validUntil: Long,
             @QueryParam("validFrom") @DefaultValue("0") validFrom: Long,
             @QueryParam("ipAddr") @DefaultValue("") ipAddr: String): Response {
        try {
            if (signingService!!.accepts(baseUrl)) {
                val signedUrl = signingService!!.sign(baseUrl, DateTime(validUntil * DateTimeConstants.MILLIS_PER_SECOND), if (validFrom > 0)
                    DateTime(validFrom * DateTimeConstants.MILLIS_PER_SECOND)
                else
                    null, trimToNull(ipAddr))
                return Response.ok(signedUrl).build()
            } else {
                return Response.status(SC_BAD_REQUEST).build()
            }
        } catch (e: UrlSigningException) {
            return Response.status(SC_BAD_REQUEST).entity(e.message).build()
        }

    }

}
