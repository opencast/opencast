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
package org.opencastproject.external.common;

import static java.lang.String.format;

import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A utility class for creating responses from the external api.
 */
public final class ApiResponses {

  private static final Logger logger = LoggerFactory.getLogger(ApiResponses.class);
  private static final String APPLICATION_PREFIX = "application/";

  private ApiResponses() {

  }

  public static Response notFound(String message, Object... args) {
    return Response.status(Status.NOT_FOUND).entity(format(message, args)).type(MediaType.TEXT_PLAIN_TYPE).build();
  }

  public static Response serverError(String message, Object... args) {
    return Response.serverError().entity(format(message, args)).type(MediaType.TEXT_PLAIN_TYPE).build();
  }

  /**
   * Class that handles Json responses for the external API.
   */
  public static class Json {

    private static final String JSON_SUFFIX = "+json";
    /** The serializer to use to serialize json content. **/
    private static final SimpleSerializer serializer = new SimpleSerializer();

    /**
     * Create an ok json response for the external api
     *
     * @param version
     *          The version that was requested for the api
     * @param body
     *          The body of the response.
     * @return The new {@link Response}
     */
    public static Response ok(ApiVersion version, String body) {
      // FIXME: This does not make sense. Sending content type 'application/json' for a plain string?
      // Maybe, when a new major release happens, wrap the string with a JSON object.
      // See MH-12798: The External API should consistently return JSON strings
      return Response.ok(body, APPLICATION_PREFIX + version.toExternalForm() + JSON_SUFFIX).build();
    }

    /**
     * Create an ok json response for the external api
     *
     * @param acceptHeader
     *          The accept header string that was sent by the client.
     * @param body
     *          The body of the response.
     * @return The new {@link Response}
     */
    public static Response ok(String acceptHeader, String body) {
      final ApiVersion version = ApiMediaType.parse(acceptHeader).getVersion();
      return ok(version, body);
    }

    /**
     * Create an ok json response for the external api
     *
     * @param version
     *          The version that was requested for the api
     * @param json
     *          The json body of the response.
     * @return The new {@link Response}
     */
    public static Response ok(ApiVersion version, JValue json) {
      return Response.ok(serializer.toJson(json), APPLICATION_PREFIX + version.toExternalForm() + JSON_SUFFIX).build();
    }

    /**
     * Create an ok json response for the external api
     *
     * @param acceptHeader
     *          The accept header string that was sent by the client.
     * @param json
     *          The json body of the response.
     * @return The new {@link Response}
     */
    public static Response ok(String acceptHeader, JValue json) {
      final ApiVersion version = ApiMediaType.parse(acceptHeader).getVersion();
      return ok(version, json);
    }

    /**
     * Create a created json response for the external api
     *
     * @param acceptHeader
     *          The accept header string that was sent by the client.
     * @param location
     *          The location
     * @param json
     *          The json body of the response.
     * @return The new {@link Response}
     */
    public static Response created(String acceptHeader, URI location, JValue json) {
      final ApiVersion version = ApiMediaType.parse(acceptHeader).getVersion();
      return created(version, location, json);
    }

    /**
     * Create a created json response for the external api
     *
     * @param version
     *          The version that was requested for the api
     * @param location
     *          The location
     * @param json
     *          The json body of the response.
     * @return The new {@link Response}
     */
    public static Response created(ApiVersion version, URI location, JValue json) {
      return Response.created(location).entity(serializer.toJson(json))
              .type(APPLICATION_PREFIX + version.toExternalForm() + JSON_SUFFIX).build();
    }

    /**
     * Create a conflict json response for the external api
     *
     * @param version
     *          The version that was requested for the api
     * @param json
     *          The json body of the response.
     * @return The new {@link Response}
     */
    public static Response conflict(ApiVersion version, JValue json) {
      return Response.status(Status.CONFLICT).entity(serializer.toJson(json))
          .type(APPLICATION_PREFIX + version.toExternalForm() + JSON_SUFFIX).build();
    }
  }
}
