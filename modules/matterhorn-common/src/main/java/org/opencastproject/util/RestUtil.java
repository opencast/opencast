/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.osgi.service.component.ComponentContext;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.InputStream;

import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Tuple.tuple;

/** Utility functions for REST endpoints. */
public final class RestUtil {
  private RestUtil() {
  }

  /**
   * Return the endpoint's server URL and the service path by extracting the relevant parameters from the
   * ComponentContext.
   *
   * @return (serverUrl, servicePath)
   * @throws Error
   *         if the service path is not configured for this component
   */
  public static Tuple<String, String> getEndpointUrl(ComponentContext cc) {
    final String serverUrl = option(cc.getBundleContext().getProperty("org.opencastproject.server.url")).getOrElse(UrlSupport.DEFAULT_BASE_URL);
    final String servicePath = option((String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY)).getOrElse(Option.<String>error(RestConstants.SERVICE_PATH_PROPERTY + " property not configured"));
    return tuple(serverUrl, servicePath);
  }

  /** Create a file response. */
  public static Response.ResponseBuilder fileResponse(File f, String contentType, Option<String> fileName) {
    final Response.ResponseBuilder b = Response.ok(f)
            .header("Content-Type", contentType)
            .header("Content-Length", f.length());
    for (String fn : fileName) b.header("Content-Disposition", "attachment; filename=" + fn);
    return b;
  }

  /** Create a stream response. */
  public static Response.ResponseBuilder streamResponse(InputStream in, String contentType, Option<Long> streamLength, Option<String> fileName) {
    final Response.ResponseBuilder b = Response.ok(in)
            .header("Content-Type", contentType);
    for (Long l : streamLength) b.header("Content-Length", l);
    for (String fn : fileName) b.header("Content-Disposition", "attachment; filename=" + fn);
    return b;
  }
}
