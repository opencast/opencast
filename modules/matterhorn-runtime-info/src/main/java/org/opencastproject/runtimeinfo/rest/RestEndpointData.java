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

package org.opencastproject.runtimeinfo.rest;

import org.opencastproject.util.JaxbXmlSchemaGenerator;
import org.opencastproject.util.doc.DocData;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestResponse;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RestEndpointData implements Comparable<RestEndpointData> {

  /**
   * The name of the endpoint, which should be unique. In the documentation page, the same type of endpoints are shown
   * in ascending order of name.
   */
  private String name;

  /**
   * The HTTP method used to invoke the endpoint.
   */
  private String httpMethod;

  /**
   * The path for this endpoint (e.g. /search OR /add/{id}).
   */
  private String path;

  /**
   * The description of this endpoint.
   */
  private String description;

  /**
   * The body parameter of this endpoint.
   */
  private RestParamData bodyParam;

  /**
   * The list of path parameters of this endpoint.
   */
  private List<RestParamData> pathParams;

  /**
   * The list of required query parameters of this endpoint.
   */
  private List<RestParamData> requiredParams;

  /**
   * The list of optional query parameters of this endpoint.
   */
  private List<RestParamData> optionalParams;

  /**
   * The list of notes (i.e. extra information) of this endpoint.
   */
  private List<String> notes;

  /**
   * The list of formats returned by this endpoint.
   */
  private List<RestFormatData> formats;

  /**
   * The list of HTTP responses returned by this endpoint.
   */
  private List<StatusData> statuses;

  /**
   * The form for testing this endpoint in the documentation page.
   */
  private RestFormData form;

  /** The XML schema for data returned by this endpoint. */
  private String returnTypeSchema = null;

  /**
   * Create a new basic endpoint, you should use the add methods to fill in the rest of the information about the
   * endpoint data
   *
   * @param returnType
   *          the endpoint's return type
   * @param name
   *          the endpoint's name (this should be unique in the same type of endpoints)
   * @param method
   *          the HTTP method used for this endpoint
   * @param path
   *          the path for this endpoint (e.g. /search OR /add/{id})
   * @param description
   *          [optional] the description of this endpoint
   * @throws IllegalArgumentException
   *           if name is null, name is not alphanumeric, method is null, path is null or path is not valid.
   */
  public RestEndpointData(Class<?> returnType, String name, String httpMethod, String path, String description)
          throws IllegalArgumentException {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("Name must not be null and must be alphanumeric.");
    }
    if ((httpMethod == null) || (httpMethod.isEmpty())) {
      throw new IllegalArgumentException("Method must not be null and must not be empty.");
    }
    if (!RestDocData.isValidPath(path)) {
      throw new IllegalArgumentException("Path must not be null and must look something like /a/b/{c}.");
    }
    this.returnTypeSchema = JaxbXmlSchemaGenerator.getXmlSchema(returnType);
    this.name = name;
    this.httpMethod = httpMethod.toUpperCase();
    this.path = path;
    this.description = description;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "ENDP:" + name + ":" + httpMethod + " " + path + " :body=" + bodyParam + " :req=" + requiredParams
            + " :opt=" + optionalParams + " :formats=" + formats + " :status=" + statuses + " :form=" + form;
  }

  /**
   * Adds a body parameter to this endpoint. Once added, the body parameter becomes a required parameter.
   *
   * @param restParam
   *          a RestParameter annotation object corresponding to the body parameter
   *
   * @return the new RestParamData object in case you want to set attributes
   */
  public RestParamData addBodyParam(RestParameter restParam, RestDocData restDocData) {
    RestParamData.Type type = RestParamData.Type.valueOf(restParam.type().name());
    RestParamData param = new RestParamData("BODY", type, restDocData.processMacro(restParam.defaultValue()),
            restDocData.processMacro(restParam.description()), null);
    param.setRequired(true);
    bodyParam = param;
    return param;
  }

  /**
   * Adds a path parameter for this endpoint, this would be a parameter which is passed as part of the path (e.g.
   * /my/path/{param}) and thus must use a name which is safe to be placed in a URL and does not contain a slash (/)
   *
   * @param param
   *          the path parameter to add
   * @throws IllegalStateException
   *           if the type of the path parameter is FILE or TEXT
   */
  public void addPathParam(RestParamData param) throws IllegalStateException {
    if (RestParamData.Type.FILE.name().equals(param.getType())
            || RestParamData.Type.TEXT.name().equals(param.getType())) {
      throw new IllegalStateException("Cannot add path param of type FILE or TEXT.");
    }
    param.setRequired(true);
    param.setPath(true);
    if (pathParams == null) {
      pathParams = new Vector<RestParamData>(3);
    }
    pathParams.add(param);
  }

  /**
   * Adds a required form parameter for this endpoint, this would be a parameter which is passed encoded as part of the
   * request body (commonly referred to as a post or form parameter). <br/>
   * WARNING: This should generally be reserved for endpoints which are used for processing, it is better to use path
   * params unless the required parameter is not part of an identifier for the resource.
   *
   * @param param
   *          the required parameter to add
   */
  public void addRequiredParam(RestParamData param) throws IllegalStateException {
    param.setRequired(true);
    param.setPath(false);
    if (requiredParams == null) {
      requiredParams = new Vector<RestParamData>(3);
    }
    requiredParams.add(param);
  }

  /**
   * Adds an optional parameter for this endpoint, this would be a parameter which is passed in the query string (for
   * GET) or encoded as part of the body otherwise (often referred to as a post or form parameter).
   *
   * @param param
   *          the optional parameter to add
   */
  public void addOptionalParam(RestParamData param) {
    param.setRequired(false);
    param.setPath(false);
    if (optionalParams == null) {
      optionalParams = new Vector<RestParamData>(3);
    }
    optionalParams.add(param);
  }

  /**
   * Adds a format for the return data for this endpoint.
   *
   * @param format
   *          a RestFormatData object
   */
  public void addFormat(RestFormatData format) {
    if (formats == null) {
      formats = new Vector<RestFormatData>(2);
    }
    formats.add(format);
  }

  /**
   * Adds a response status for this endpoint.
   *
   * @param restResponse
   *          a RestResponse object containing the HTTP response code and description
   */
  public void addStatus(RestResponse restResponse, RestDocData restDocData) {
    if (statuses == null) {
      statuses = new Vector<StatusData>(3);
    }
    statuses.add(new StatusData(restResponse, restDocData));
  }

  /**
   * Adds a note for this endpoint.
   *
   * @param note
   *          a string providing more information about this endpoint
   * @throws IllegalArgumentException
   *           if note is blank (e.g. null, empty string)
   */
  public void addNote(String note) throws IllegalArgumentException {
    if (DocData.isBlank(note)) {
      throw new IllegalArgumentException("Note must not be null or blank.");
    }
    if (notes == null) {
      notes = new Vector<String>(3);
    }
    notes.add(note);
  }

  /**
   * Sets the test form for this endpoint, if this is null then no test form is rendered for this endpoint.
   *
   * @param form
   *          the test form object (null to clear the form)
   */
  public void setTestForm(RestFormData form) {
    this.form = form;
  }

  /**
   * Returns whether this endpoint's HTTP method is GET
   *
   * @return true if this endpoint method is GET, otherwise false
   */
  public boolean isGetMethod() {
    return "GET".equals(httpMethod);
  }

  /**
   * Returns the URL-encoded query string for a GET endpoint.
   *
   * @return the calculated query string for a GET endpoint (e.g. ?blah=1), will be urlencoded for html display
   */
  public String getQueryString() {
    String qs = "";
    if (isGetMethod()) {
      if (optionalParams != null && !optionalParams.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (RestParamData p : optionalParams) {
          if (sb.length() > 2) {
            sb.append("&");
          }
          sb.append(p.getName());
          sb.append("=");
          if (p.getDefaultValue() != null) {
            sb.append(p.getDefaultValue());
          } else {
            sb.append("{");
            sb.append(p.getName());
            sb.append("}");
          }
        }
        qs = StringEscapeUtils.escapeHtml(sb.toString());
      }
    }
    return qs;
  }

  /**
   * Gets the name of this endpoint.
   *
   * @return the name of this endpoint
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the name of HTTP method used to invoke this endpoint.
   *
   * @return the name of HTTP method used to invoke this endpoint
   */
  public String getMethod() {
    return httpMethod;
  }

  /**
   * Gets the path for this endpoint.
   *
   * @return the path for this endpoint
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the description of this endpoint.
   *
   * @return the description of this endpoint
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the body parameter of this endpoint.
   *
   * @return the body parameter of this endpoint
   */
  public RestParamData getBodyParam() {
    return bodyParam;
  }

  /**
   * Gets the list of path parameters of this endpoint.
   *
   * @return the list of path parameters of this endpoint
   */
  public List<RestParamData> getPathParams() {
    if (pathParams == null) {
      pathParams = new ArrayList<RestParamData>(0);
    }
    return pathParams;
  }

  /**
   * Gets the list of required parameters of this endpoint.
   *
   * @return the list of required parameters of this endpoint
   */
  public List<RestParamData> getRequiredParams() {
    if (requiredParams == null) {
      requiredParams = new ArrayList<RestParamData>(0);
    }
    return requiredParams;
  }

  /**
   * Gets the list of optional parameters of this endpoint.
   *
   * @return list of optional parameters of this endpoint
   */
  public List<RestParamData> getOptionalParams() {
    if (optionalParams == null) {
      optionalParams = new ArrayList<RestParamData>(0);
    }
    return optionalParams;
  }

  /**
   * Gets the list of formats returned by this endpoint.
   *
   * @return the list of formats returned by this endpoint
   */
  public List<RestFormatData> getFormats() {
    if (formats == null) {
      formats = new ArrayList<RestFormatData>(0);
    }
    return formats;
  }

  /**
   * Gets the list of HTTP responses returned by this endpoint.
   *
   * @return the list of HTTP responses returned by this endpoint
   */
  public List<StatusData> getStatuses() {
    if (statuses == null) {
      statuses = new ArrayList<StatusData>(0);
    }
    return statuses;
  }

  /**
   * Gets list of notes (i.e. extra information) of this endpoint.
   *
   * @return the list of notes (i.e. extra information) of this endpoint
   */
  public List<String> getNotes() {
    if (notes == null) {
      notes = new ArrayList<String>(0);
    }
    return notes;
  }

  /**
   * Gets the form for testing this endpoint in the documentation page.
   *
   * @return the form for testing this endpoint in the documentation page
   */
  public RestFormData getForm() {
    return form;
  }

  /**
   * Compares two RestEndpointData by their names so that the list of endpoints can be sorted.
   *
   * @param otherEndpoint
   *          the other endpoint object to compare to
   *
   * @return a negative integer, zero, or a positive integer as the name of the supplied endpoint is greater than, equal
   *         to, or less than this endpoint, ignoring case considerations.
   */
  @Override
  public int compareTo(RestEndpointData otherEndpoint) {
    return name.compareToIgnoreCase(otherEndpoint.name);
  }

  /**
   * @return the XML schema for this endpoint's return type
   */
  public String getReturnTypeSchema() {
    return returnTypeSchema;
  }

  public String getEscapedReturnTypeSchema() {
    return StringEscapeUtils.escapeXml(returnTypeSchema);
  }

}
