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
package org.opencastproject.runtimeinfo.rest;

import org.opencastproject.util.doc.DocData;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import org.apache.commons.beanutils.BeanUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * This is the document model class which holds the data about a set of rest endpoints.
 */
public class RestDocData extends DocData {

  /**
   * The REGEX pattern used to find the macros in the REST documentation.
   */
  private static final String REST_DOC_MACRO_PATTERN = "\\$\\{(.+?)\\}";

  /**
   * The name to identify the endpoint holder for read endpoints (get/head).
   */
  private static final String READ_ENDPOINT_HOLDER_NAME = "READ";

  /**
   * The name to identify the endpoint holder for write endpoints (delete,post,put).
   */
  private static final String WRITE_ENDPOINT_HOLDER_NAME = "WRITE";

  /**
   * Regular expression used to count the number of path parameters in a path.
   */
  public static final String PATH_PARAM_COUNTING_REGEX = "\\{(.+?)\\}";

  /**
   * Regular expression used to validate a path.
   */
  // FIXME: This regex doesn't match all valid paths that can occur in rest endpoint @Path;
  public static final String PATH_VALIDATION_REGEX = "^[\\w\\/{}|\\:\\.\\*\\+|\\[\\w-\\w\\]\\+]+$";

  /**
   * A slash character.
   */
  public static final String SLASH = "/";

  /**
   * List of RestEndpointHolderData which each stores a group of endpoints. Currently there are 2 groups, READ group and
   * WRITE group.
   */
  protected List<RestEndpointHolderData> holders;

  /**
   * The service object which this RestDocData is about.
   */
  private Object serviceObject = null;

  /**
   * A map of macro values for REST documentation.
   */
  private Map<String, String> macros;

  /**
   * Create the base data object for creating REST documentation.
   *
   * @param name
   *          the name of the set of rest endpoints (must be alphanumeric (includes _) and no spaces or special chars)
   * @param title
   *          [OPTIONAL] the title of the documentation
   * @param url
   *          this is the absolute base URL for this endpoint, do not include the trailing slash (e.g. /workflow)
   * @param notes
   *          [OPTIONAL] an array of notes to add into the end of the documentation
   * @throws IllegalArgumentException
   *           if the url is null or empty
   */
  public RestDocData(String name, String title, String url, String[] notes, Object service,
          Map<String, String> globalMacro) throws IllegalArgumentException {
    super(name, title, notes);
    if (url == null || "".equals(url)) {
      throw new IllegalArgumentException("URL cannot be blank.");
    }
    meta.put("url", url);
    serviceObject = service;
    macros = globalMacro;
    // create the endpoint holders
    holders = new Vector<RestEndpointHolderData>(2);
    holders.add(new RestEndpointHolderData(READ_ENDPOINT_HOLDER_NAME, "Read"));
    holders.add(new RestEndpointHolderData(WRITE_ENDPOINT_HOLDER_NAME, "Write"));
  }

  /**
   * Verify the integrity of this object. If its data is verified to be okay, it return a map representation of this
   * RestDocData object.
   *
   * @return a map representation of this RestDocData object if this object passes the verification
   *
   * @throws IllegalStateException
   *           if any path parameter is not present in the endpoint's path
   */
  @Override
  public Map<String, Object> toMap() throws IllegalStateException {
    LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
    m.put("meta", meta);
    m.put("notes", notes);
    // only pass through the holders with things in them
    ArrayList<RestEndpointHolderData> holdersList = new ArrayList<RestEndpointHolderData>();
    for (RestEndpointHolderData holder : holders) {
      if (!holder.getEndpoints().isEmpty()) {
        for (RestEndpointData endpoint : holder.getEndpoints()) {
          // Validate the endpoint path matches the specified path parameters.
          // First, it makes sure that every path parameter is present in the endpoint's path.
          if (!endpoint.getPathParams().isEmpty()) {
            for (RestParamData param : endpoint.getPathParams()) {
              // Some endpoints allow for arbitrary characters, including slashes, in their path parameters, so we
              // must check for both {param} and {param:.*}.
              if (!endpoint.getPath().contains("{" + param.getName() + "}")
                      && !endpoint.getPath().contains("{" + param.getName() + ":")) {
                throw new IllegalStateException("Path (" + endpoint.getPath() + ") does not match path parameter ("
                        + param.getName() + ") for endpoint (" + endpoint.getName()
                        + "), the path must contain all path parameter names.");
              }
            }
          }
          // Then, it makes sure that the number of path parameter patterns in the path is the same as the number of
          // path parameters in the endpoint.
          // The following part uses a regular expression to find patterns like {something}.
          Pattern pattern = Pattern.compile(PATH_PARAM_COUNTING_REGEX);
          Matcher matcher = pattern.matcher(endpoint.getPath());

          int count = 0;
          while (matcher.find()) {
            count++;
          }
          if (count != endpoint.getPathParams().size()) {
            throw new IllegalStateException("Path (" + endpoint.getPath() + ") does not match path parameters ("
                    + endpoint.getPathParams() + ") for endpoint (" + endpoint.getName()
                    + "), the path must contain the same number of path parameters (" + count
                    + ") as the pathParams list (" + endpoint.getPathParams().size() + ").");
          }
        }
        holdersList.add(holder);
      }
    }
    m.put("endpointHolders", holdersList);
    return m;
  }

  /**
   * Gets the path to the default template (a .xhtml file).
   *
   * @return the path to the default template file
   */
  @Override
  public String getDefaultTemplatePath() {
    return TEMPLATE_DEFAULT;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "DOC:meta=" + meta + ", notes=" + notes + ", " + holders;
  }

  /**
   * Add an endpoint to this documentation using and assign it to the correct type group (read/write).
   *
   * @param type
   *          the type of this endpoint (RestEndpointData.Type.READ or RestEndpointData.Type.WRITE)
   * @param endpoint
   *          the endpoint to be added
   * @throws IllegalStateException
   *           if the endpoint cannot be assigned to a group
   */
  private void addEndpoint(String type, RestEndpointData endpoint) throws IllegalStateException {
    RestEndpointHolderData currentHolder = null;
    for (RestEndpointHolderData holder : holders) {
      if (type.equalsIgnoreCase(holder.getName())) {
        currentHolder = holder;
        break;
      }
    }
    if (currentHolder == null) {
      throw new IllegalStateException("Could not find holder of type: " + type + ".");
    }
    currentHolder.addEndPoint(endpoint);
  }

  /**
   * Creates an abstract section which is displayed at the top of the documentation page.
   *
   * @param abstractText
   *          any text to place at the top of the document, can be html markup but must be valid
   */
  public void setAbstract(String abstractText) {
    if (isBlank(abstractText)) {
      meta.remove("abstract");
    } else {
      meta.put("abstract", abstractText);
    }
  }

  /**
   * Validates paths: VALID: /sample , /sample/{thing} , /{my}/{path}.xml , /my/fancy_path/is/{awesome}.{FORMAT}
   * INVALID: sample, /sample/, /sa#$%mple/path
   *
   * @param path
   *          the path value to check
   * @return true if this path is valid, false otherwise
   */
  public static boolean isValidPath(String path) {
    boolean valid = true;
    if (isBlank(path)) {
      valid = false;
    } else {
      if (SLASH.equals(path)) {
        valid = true;
      } else if (path.endsWith(SLASH) || !path.startsWith(SLASH)) {
        valid = false;
      } else {
        valid = path.matches(PATH_VALIDATION_REGEX);
      }
    }
    return valid;
  }

  /**
   * Takes a string and replaces any REST doc macros in it with the corresponding values.
   *
   * @param value
   *          the string to check
   * @return a string where all the macros are replaced by the corresponding values
   */
  public String processMacro(String value) {
    Pattern pattern = Pattern.compile(REST_DOC_MACRO_PATTERN);
    Matcher matcher = pattern.matcher(value);

    StringBuilder sb = new StringBuilder();
    int begin = 0;
    while (matcher.find()) {
      sb.append(value.substring(begin, matcher.start()));
      String macro = matcher.group(1);
      // All macros that start with "this." is a "local" macro.
      if (macro.startsWith("this.")) {
        try {
          sb.append(BeanUtils.getProperty(serviceObject, macro.substring(5)));
        } catch (Exception e) {
          // If there is any problem (e.g. the property cannot be found), the macro would be displayed directly.
          sb.append(matcher.group());
        }
      } else {
        if (macros.containsKey(matcher.group(1))) {
          sb.append(macros.get(matcher.group(1)));
        } else {
          // If the macro cannot be found in the list of global macro, it would be displayed directly.
          sb.append(matcher.group());
        }
      }
      begin = matcher.end();
    }
    sb.append(value.substring(begin, value.length()));

    return sb.toString();
  }

  /**
   * Add an endpoint to the Rest documentation.
   *
   * @param restQuery
   *          the RestQuery annotation type storing information of an endpoint
   * @param returnType
   *          the return type for this endpoint. If this is {@link javax.xml.bind.annotation.XmlRootElement} or
   *          {@link javax.xml.bind.annotation.XmlRootElement}, the XML schema for the class will be made available to
   *          clients
   * @param produces
   *          the return type(s) of this endpoint, values should be constants from <a
   *          href="http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html"
   *          >javax.ws.rs.core.MediaType</a> or ExtendedMediaType
   *          (org.opencastproject.util.doc.rest.ExtendedMediaType).
   * @param httpMethodString
   *          the HTTP method of this endpoint (e.g. GET, POST)
   * @param path
   *          the path of this endpoint
   */
  public void addEndpoint(RestQuery restQuery, Class<?> returnType, Produces produces, String httpMethodString,
          Path path) {
    String pathValue = path.value().startsWith("/") ? path.value() : "/" + path.value();
    RestEndpointData endpoint = new RestEndpointData(returnType, this.processMacro(restQuery.name()), httpMethodString,
            pathValue, processMacro(restQuery.description()));
    // Add return description if needed
    if (!restQuery.returnDescription().isEmpty()) {
      endpoint.addNote("Return value description: " + processMacro(restQuery.returnDescription()));
    }

    // Add formats
    if (produces != null) {
      for (String format : produces.value()) {
        endpoint.addFormat(new RestFormatData(format));
      }
    }

    // Add responses
    for (RestResponse restResp : restQuery.reponses()) {
      endpoint.addStatus(restResp, this);
    }

    // Add body parameter
    if (restQuery.bodyParameter().type() != RestParameter.Type.NO_PARAMETER) {
      endpoint.addBodyParam(restQuery.bodyParameter(), this);
    }

    // Add path parameter
    for (RestParameter pathParam : restQuery.pathParameters()) {
      endpoint.addPathParam(new RestParamData(pathParam, this));
    }

    // Add query parameter (required and optional)
    for (RestParameter restParam : restQuery.restParameters()) {
      if (restParam.isRequired()) {
        endpoint.addRequiredParam(new RestParamData(restParam, this));
      } else {
        endpoint.addOptionalParam(new RestParamData(restParam, this));
      }
    }

    // Set the test form after all parameters are added.
    endpoint.setTestForm(new RestFormData(endpoint));

    // Add the endpoint to the corresponding group based on its HTTP method
    if (("GET".equalsIgnoreCase(httpMethodString)) || ("HEAD".equalsIgnoreCase(httpMethodString))) {
      addEndpoint(READ_ENDPOINT_HOLDER_NAME, endpoint);
    } else if (("DELETE".equalsIgnoreCase(httpMethodString)) || ("POST".equalsIgnoreCase(httpMethodString))
            || ("PUT".equalsIgnoreCase(httpMethodString))) {
      addEndpoint(WRITE_ENDPOINT_HOLDER_NAME, endpoint);
    }
  }

}
