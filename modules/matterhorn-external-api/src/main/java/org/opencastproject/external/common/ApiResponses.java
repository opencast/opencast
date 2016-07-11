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

import org.opencastproject.external.util.XMLListWrapper;
import org.opencastproject.index.service.impl.index.IndexObject;

import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

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
      return Response.ok(body, APPLICATION_PREFIX + version.toExternalForm() + JSON_SUFFIX).build();
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
     * Create a no content response for the external api
     *
     * @param version
     *          The version that was requested for the api
     * @return The new {@link Response}
     */
    public static Response noContent(ApiVersion version) {
      return Response.noContent().build();
    }

    /**
     * Create a created json response for the external api
     *
     * @param version
     *          The version that was requested for the api
     * @param location
     *          The location
     * @return The new {@link Response}
     */
    public static Response created(ApiVersion version, URI location) {
      return Response.created(location).build();
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

  }

  /**
   * Class that handles Xml responses for the external API.
   */
  public static class Xml {
    private static final String XML_SUFFIX = "+xml";

    /**
     * Create a ok xml response for the external api
     *
     * @param version
     *          The version that was requested for the api.
     * @param body
     *          The body of the response.
     * @return The new {@link Response}
     */
    public static Response ok(ApiVersion version, String body) {
      return Response.ok(body, APPLICATION_PREFIX + version.toExternalForm() + XML_SUFFIX).build();
    }

    /**
     * Serialize a list of index objects into an XML response.
     *
     * @param version
     *          The version that was requested of the api
     * @param indexObjects
     *          The objects to serialize.
     * @param clazz
     *          The class that these objects represent
     * @param xmlSurroundingTag
     *          The surrounding tag for the objects (usually the plural form)
     * @return The {@link Response}
     */
    public static Response getXmlListResponse(ApiVersion version, List<IndexObject> indexObjects, Class clazz,
            String xmlSurroundingTag) {
      try {
        JAXBContext jc = JAXBContext.newInstance(XMLListWrapper.class, clazz);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        String result = XMLListWrapper.marshal(marshaller, indexObjects, xmlSurroundingTag);
        return ok(version, result);
      } catch (JAXBException e) {
        logger.error("Unable to create xml response because {}", ExceptionUtils.getStackTrace(e));
        throw new WebApplicationException(e);
      }
    }

  }

}
