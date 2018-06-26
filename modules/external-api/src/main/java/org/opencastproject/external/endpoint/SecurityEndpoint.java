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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.util.DateTimeSupport.fromUTC;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.util.Log;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.Opt;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.Dictionary;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_0_0, ApiMediaType.VERSION_1_1_0 })
@RestService(name = "externalapisecurity", title = "External API Security Service", notes = "", abstractText = "Provides security operations related to the external API")
public class SecurityEndpoint implements ManagedService {

  protected static final String URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY = "url.signing.expires.seconds";

  /** The default time before a piece of signed content expires. 2 Hours. */
  protected static final long DEFAULT_URL_SIGNING_EXPIRE_DURATION = 2 * 60 * 60;

  /** The logging facility */
  private static final Logger log = LoggerFactory.getLogger(SecurityEndpoint.class);

  private long expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;

  /* OSGi service references */
  private UrlSigningService urlSigningService;

  /** OSGi DI */
  void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  /** OSGi activation method */
  void activate() {
    log.info("Activating External API - Security Endpoint");
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      log.info("No configuration available, using defaults");
      return;
    }

    Opt<Long> expiration = OsgiUtil.getOptCfg(properties, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY).toOpt()
            .map(com.entwinemedia.fn.fns.Strings.toLongF);
    if (expiration.isSome()) {
      expireSeconds = expiration.get();
      log.info("The property {} has been configured to expire signed URLs in {}.",
              URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, Log.getHumanReadableTimeString(expireSeconds));
    } else {
      expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;
      log.info("The property {} has not been configured, so the default is being used to expire signed URLs in {}.",
              URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, Log.getHumanReadableTimeString(expireSeconds));
    }
  }

  @POST
  @Path("sign")
  @RestQuery(name = "signurl", description = "Returns a signed URL that can be played back for the indicated period of time, while access is optionally restricted to the specified IP address.", returnDescription = "", restParameters = {
          @RestParameter(name = "url", isRequired = true, description = "The linke to encode.", type = STRING),
          @RestParameter(name = "valid-until", description = "Until when is the signed url valid", isRequired = false, type = STRING),
          @RestParameter(name = "valid-source", description = "The IP address from which the url can be accessed", isRequired = false, type = STRING) }, reponses = {
                  @RestResponse(description = "The signed URL is returned.", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "The caller is not authorized to have the link signed.", responseCode = HttpServletResponse.SC_UNAUTHORIZED) })
  public Response signUrl(@HeaderParam("Accept") String acceptHeader, @FormParam("url") String url,
          @FormParam("valid-until") String validUntilUtc, @FormParam("valid-source") String validSource) {
    if (isBlank(url))
      return R.badRequest("Query parameter 'url' is mandatory");

    final DateTime validUntil;
    if (isNotBlank(validUntilUtc)) {
      try {
        validUntil = new DateTime(fromUTC(validUntilUtc));
      } catch (IllegalStateException | ParseException e) {
        return R.badRequest("Query parameter 'valid-until' is not a valid ISO-8601 date string");
      }
    } else {
      validUntil = new DateTime(new Date().getTime() + expireSeconds * DateTimeConstants.MILLIS_PER_SECOND);
    }

    if (urlSigningService.accepts(url)) {
      String signedUrl = "";
      try {
        signedUrl = urlSigningService.sign(url, validUntil, null, validSource);
      } catch (UrlSigningException e) {
        log.warn("Error while trying to sign url '{}': {}", url, getStackTrace(e));
        return ApiResponses.Json.ok(acceptHeader, obj(f("error", "Error while signing url")));
      }
      return ApiResponses.Json.ok(acceptHeader, obj(f("url", signedUrl), f("valid-until", toUTC(validUntil.getMillis()))));
    } else {
      return ApiResponses.Json.ok(acceptHeader, obj(f("error", "Given URL cannot be signed")));
    }
  }
}
