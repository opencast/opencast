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

/**
 * Represents one possible status result for an endpoint
 */
@Deprecated
public class Status {
  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status ok(String desc) {
    return new Status(200, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status created(String desc) {
    return new Status(201, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status noContent(String desc) {
    return new Status(204, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status badRequest(String desc) {
    return new Status(400, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status unauthorized(String desc) {
    return new Status(401, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status forbidden(String desc) {
    return new Status(403, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status notFound(String desc) {
    return new Status(404, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status error(String desc) {
    return new Status(500, desc);
  }

  /**
   * @param desc
   *          [optional] description to add to this status
   * @return the status
   */
  public static Status serviceUnavailable(String desc) {
    return new Status(503, desc);
  }

  private int code;
  private String name;
  private String description;

  public Status(int code, String description) {
    if (code < 100 || code > 1100) {
      throw new IllegalArgumentException("code (" + code + ") is outside of the valid range: 100-1100");
    }
    this.code = code;
    this.name = findName(code);
    this.description = description;
  }

  /**
   * Allows overriding of the name which is set from the code
   *
   * @param name
   *          the name to display with the code
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "SC:" + code + ":" + name;
  }

  public int getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  // CHECKSTYLE:OFF

  /**
   * This will resolve a human readable name for all known status codes
   *
   * @param code
   *          the status code
   * @return the name OR UNKNOWN if none found
   * @throws IllegalArgumentException
   *           if the code is outside the valid range
   */
  public static String findName(int code) {
    // list from http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
    String result = "UNKNOWN";
    switch (code) {
    // 1xx Informational
      case 100:
        result = "Continue";
        break;
      case 101:
        result = "Switching Protocols";
        break;
      case 102:
        result = "Processing";
        break;
      // 2xx Success
      case 200:
        result = "OK";
        break;
      case 201:
        result = "Created";
        break;
      case 202:
        result = "Accepted";
        break;
      case 203:
        result = "Non-Authoritative Information";
        break;
      case 204:
        result = "No Content";
        break;
      case 205:
        result = "Reset Content";
        break;
      case 206:
        result = "Partial Content";
        break;
      case 207:
        result = "Multi-Status";
        break;
      // 3xx Redirection
      case 300:
        result = "Multiple Choices";
        break;
      case 301:
        result = "Moved Permanently";
        break;
      case 302:
        result = "Found";
        break;
      case 303:
        result = "See Other";
        break;
      case 304:
        result = "Not Modified";
        break;
      case 305:
        result = "Use Proxy";
        break;
      case 306:
        result = "Switch Proxy";
        break;
      case 307:
        result = "Temporary Redirect";
        break;
      // 4xx Client Error
      case 400:
        result = "Bad Request";
        break;
      case 401:
        result = "Unauthorized";
        break;
      case 402:
        result = "Payment Required";
        break;
      case 403:
        result = "Forbidden";
        break;
      case 404:
        result = "Not Found";
        break;
      case 405:
        result = "Method Not Allowed";
        break;
      case 406:
        result = "Not Acceptable";
        break;
      case 407:
        result = "Proxy Authentication Required";
        break;
      case 408:
        result = "Request Timeout";
        break;
      case 409:
        result = "Conflict";
        break;
      case 410:
        result = "Gone";
        break;
      case 411:
        result = "Length Required";
        break;
      case 412:
        result = "Precondition Failed";
        break;
      case 413:
        result = "Request Entity Too Large";
        break;
      case 414:
        result = "Request URI Too Long";
        break;
      case 415:
        result = "Unsupported Media Type";
        break;
      case 416:
        result = "Requested Range Not Satisfiable";
        break;
      case 417:
        result = "Expectation Failed";
        break;
      case 418:
        result = "I'm a teapot";
        break;
      case 422:
        result = "Unprocessable Entity";
        break;
      case 423:
        result = "Locked";
        break;
      case 424:
        result = "Failed Dependency";
        break;
      case 425:
        result = "Unordered Collection";
        break;
      case 426:
        result = "Upgrade Required";
        break;
      case 449:
        result = "Retry With";
        break;
      case 450:
        result = "Blocked by Windows Parental Controls";
        break;
      // 5xx Server Error
      case 500:
        result = "Internal Server Error";
        break;
      case 501:
        result = "Not Implemented";
        break;
      case 502:
        result = "Bad Gateway";
        break;
      case 503:
        result = "Service Unavailable";
        break;
      case 504:
        result = "Gateway Timeout";
        break;
      case 505:
        result = "Version Not Supported";
        break;
      case 506:
        result = "Variant Also Negotiates";
        break;
      case 507:
        result = "Insufficient Storage";
        break;
      case 509:
        result = "Bandwidth Limit Exceeded";
        break;
      case 510:
        result = "Not Extended";
        break;
    }
    return result;
  }
  // CHECKSTYLE:ON
}
