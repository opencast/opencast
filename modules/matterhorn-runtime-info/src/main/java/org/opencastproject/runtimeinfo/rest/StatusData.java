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

import org.opencastproject.util.doc.rest.RestResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Represents a possible status result for an endpoint
 */
public class StatusData {

  /**
   * The HTTP response code.
   */
  private int code;

  /**
   * The name of this status.
   */
  private String name;

  /**
   * The description for this HTTP response.
   */
  private String description;

  /**
   * The XML schema for the response, if applicable.
   */
  private String xmlSchema;

  /**
   * A constructor that takes a RestResponse annotation type object and constructs a StatusData object.
   *
   * @param restResponse
   *          a RestResponse annotation type object that stores a response code and its description
   * @throws IllegalArgumentException
   *           if the response code is out of range (e.g. <100 or > 1100)
   */
  public StatusData(RestResponse restResponse, RestDocData restDocData) throws IllegalArgumentException {
    this(restResponse.responseCode(), restDocData.processMacro(restResponse.description()));
  }

  /**
   * A constructor that takes a HTTP response code and a description for this response, and an XML schema for the
   * response and constructs a StatusData object. A reference of response code constants can be found in <a
   * href="http://download.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html"
   * >javax.servlet.http.HttpServletResponse</a>.
   *
   * @param code
   *          a HTTP response code
   * @param description
   *          a description of the response
   * @throws IllegalArgumentException
   *           if code is out of range (e.g. <100 or > 1100)
   */
  public StatusData(int code, String description, String xmlSchema) throws IllegalArgumentException {
    if (code < 100 || code > 1100) {
      throw new IllegalArgumentException("Code (" + code + ") is outside of the valid range: 100-1100.");
    }
    this.code = code;
    name = findName(code);
    if (description.isEmpty()) {
      this.description = null;
    } else {
      this.description = description;
    }
    this.xmlSchema = xmlSchema;
  }

  /**
   * A constructor that takes a HTTP response code and a description for this response and constructs a StatusData
   * object. A reference of response code constants can be found in <a
   * href="http://download.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html"
   * >javax.servlet.http.HttpServletResponse</a>.
   *
   * @param code
   *          a HTTP response code
   * @param description
   *          a description of the response
   * @throws IllegalArgumentException
   *           if code is out of range (e.g. <100 or > 1100)
   */
  public StatusData(int code, String description) throws IllegalArgumentException {
    this(code, description, null);
  }

  @Override
  /**
   * @return a string representation of this object
   */
  public String toString() {
    return "SC:" + code + ":" + name;
  }

  /**
   * @return the response code of this status
   */
  public int getCode() {
    return code;
  }

  /**
   * @return a string name of this status
   */
  public String getName() {
    return name;
  }

  /**
   * @return a description of this status
   */
  public String getDescription() {
    return description;
  }

  // CHECKSTYLE:OFF

  /**
   * This will resolve a human readable name for all known status codes.
   *
   * @param code
   *          the status code
   * @return the name OR UNKNOWN if none found
   * @throws IllegalArgumentException
   *           if the code is outside the valid range
   */
  public static String findName(int code) throws IllegalArgumentException {
    if (code < 100 || code > 1100) {
      throw new IllegalArgumentException("Code (" + code + ") is outside of the valid range: 100-1100.");
    }

    // list from http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
    String result = "UNKNOWN";
    switch (code) {
    // 1xx Informational
      case HttpServletResponse.SC_CONTINUE: // 100
        result = "Continue";
        break;
      case HttpServletResponse.SC_SWITCHING_PROTOCOLS: // 101
        result = "Switching Protocols";
        break;
      // 2xx Success
      case HttpServletResponse.SC_OK: // 200
        result = "OK";
        break;
      case HttpServletResponse.SC_CREATED: // 201
        result = "Created";
        break;
      case HttpServletResponse.SC_ACCEPTED: // 202
        result = "Accepted";
        break;
      case HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION: // 203
        result = "Non-Authoritative Information";
        break;
      case HttpServletResponse.SC_NO_CONTENT: // 204
        result = "No Content";
        break;
      case HttpServletResponse.SC_RESET_CONTENT: // 205
        result = "Reset Content";
        break;
      case HttpServletResponse.SC_PARTIAL_CONTENT: // 206
        result = "Partial Content";
        break;
      // 3xx Redirection
      case HttpServletResponse.SC_MULTIPLE_CHOICES: // 300
        result = "Multiple Choices";
        break;
      case HttpServletResponse.SC_MOVED_PERMANENTLY: // 301
        result = "Moved Permanently";
        break;
      case HttpServletResponse.SC_MOVED_TEMPORARILY: // 302
        result = "Found";
        break;
      case HttpServletResponse.SC_SEE_OTHER: // 303
        result = "See Other";
        break;
      case HttpServletResponse.SC_NOT_MODIFIED: // 304
        result = "Not Modified";
        break;
      case HttpServletResponse.SC_USE_PROXY: // 305
        result = "Use Proxy";
        break;
      case HttpServletResponse.SC_TEMPORARY_REDIRECT: // 307
        result = "Temporary Redirect";
        break;
      // 4xx Client Error
      case HttpServletResponse.SC_BAD_REQUEST: // 400
        result = "Bad Request";
        break;
      case HttpServletResponse.SC_UNAUTHORIZED: // 401
        result = "Unauthorized";
        break;
      case HttpServletResponse.SC_PAYMENT_REQUIRED: // 402
        result = "Payment Required";
        break;
      case HttpServletResponse.SC_FORBIDDEN: // 403
        result = "Forbidden";
        break;
      case HttpServletResponse.SC_NOT_FOUND: // 404
        result = "Not Found";
        break;
      case HttpServletResponse.SC_METHOD_NOT_ALLOWED: // 405
        result = "Method Not Allowed";
        break;
      case HttpServletResponse.SC_NOT_ACCEPTABLE: // 406
        result = "Not Acceptable";
        break;
      case HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED: // 407
        result = "Proxy Authentication Required";
        break;
      case HttpServletResponse.SC_REQUEST_TIMEOUT: // 408
        result = "Request Timeout";
        break;
      case HttpServletResponse.SC_CONFLICT: // 409
        result = "Conflict";
        break;
      case HttpServletResponse.SC_GONE: // 410
        result = "Gone";
        break;
      case HttpServletResponse.SC_LENGTH_REQUIRED: // 411
        result = "Length Required";
        break;
      case HttpServletResponse.SC_PRECONDITION_FAILED: // 412
        result = "Precondition Failed";
        break;
      case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE: // 413
        result = "Request Entity Too Large";
        break;
      case HttpServletResponse.SC_REQUEST_URI_TOO_LONG: // 414
        result = "Request URI Too Long";
        break;
      case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE: // 415
        result = "Unsupported Media Type";
        break;
      case HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE: // 416
        result = "Requested Range Not Satisfiable";
        break;
      case HttpServletResponse.SC_EXPECTATION_FAILED: // 417
        result = "Expectation Failed";
        break;
      // 5xx Server Error
      case HttpServletResponse.SC_INTERNAL_SERVER_ERROR: // 500
        result = "Internal Server Error";
        break;
      case HttpServletResponse.SC_NOT_IMPLEMENTED: // 501
        result = "Not Implemented";
        break;
      case HttpServletResponse.SC_BAD_GATEWAY: // 502
        result = "Bad Gateway";
        break;
      case HttpServletResponse.SC_SERVICE_UNAVAILABLE: // 503
        result = "Service Unavailable";
        break;
      case HttpServletResponse.SC_GATEWAY_TIMEOUT: // 504
        result = "Gateway Timeout";
        break;
      case HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED: // 505
        result = "Version Not Supported";
        break;
    }
    return result;
  }

  // CHECKSTYLE:ON

  /**
   * @return the xmlSchema
   */
  public String getXmlSchema() {
    return xmlSchema;
  }

  /**
   * @param xmlSchema
   *          the xmlSchema to set
   */
  public void setXmlSchema(String xmlSchema) {
    this.xmlSchema = xmlSchema;
  }
}
