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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.security.urlsigning.service.impl;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "urlsigning", title = "URL Signing Endpoint", notes = "This is a testing endpoint to play around with the URL Signing Service", abstractText = "")
public class UrlSigningEndpoint {

  private UrlSigningService signingService;

  /** OSGi DI callback */
  void setUrlSigningService(UrlSigningService signingService) {
    this.signingService = signingService;
  }

  @GET
  @Produces({ MediaType.TEXT_PLAIN })
  @Path("accepts")
  @RestQuery(name = "accepts", description = "Checks if the signing service accepts to sign the URL", restParameters = { @RestParameter(name = "baseUrl", isRequired = true, description = "The URL to sign", type = STRING) }, responses = { @RestResponse(description = "'true' or 'false'", responseCode = 200) }, returnDescription = "")
  public Response accepts(@QueryParam("baseUrl") final String baseUrl) {
    if (signingService.accepts(baseUrl))
      return Response.ok(Boolean.TRUE.toString()).build();
    else
      return Response.ok(Boolean.FALSE.toString()).build();
  }

  @GET
  @Produces({ MediaType.TEXT_PLAIN })
  @Path("sign")
  @RestQuery(name = "sign", description = "Return a signed URL", restParameters = {
          @RestParameter(name = "baseUrl", isRequired = true, description = "The URL to sign", type = STRING),
          @RestParameter(defaultValue = "0", description = "The UNIX epoch time until when a signed URL should remain valid", isRequired = true, name = "validUntil", type = RestParameter.Type.INTEGER),
          @RestParameter(defaultValue = "0", description = "The UNIX epoch time from when a signed URL should become valid", isRequired = false, name = "validFrom", type = RestParameter.Type.INTEGER),
          @RestParameter(defaultValue = "", description = "The IP addresse of the user that is allowed to access the resource", type = STRING, isRequired = false, name = "ipAddr") }, responses = { @RestResponse(description = "A URL", responseCode = 200) }, returnDescription = "")
  public Response sign(@QueryParam("baseUrl") final String baseUrl, @QueryParam("validUntil") final long validUntil,
          @QueryParam("validFrom") @DefaultValue("0") long validFrom,
          @QueryParam("ipAddr") @DefaultValue("") String ipAddr) {
    try {
      if (signingService.accepts(baseUrl)) {
        final String signedUrl = signingService.sign(baseUrl, new DateTime(validUntil
                * DateTimeConstants.MILLIS_PER_SECOND), (validFrom > 0 ? new DateTime(validFrom
                * DateTimeConstants.MILLIS_PER_SECOND) : null), trimToNull(ipAddr));
        return Response.ok(signedUrl).build();
      } else {
        return Response.status(SC_BAD_REQUEST).build();
      }
    } catch (UrlSigningException e) {
      return Response.status(SC_BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

}
