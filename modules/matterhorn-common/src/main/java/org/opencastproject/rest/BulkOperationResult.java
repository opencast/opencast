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

package org.opencastproject.rest;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * This class is used to store the results of a bulk operation on an endpoint and to easily return those results.
 */
public class BulkOperationResult {
  public static final String OK_KEY = "ok";
  public static final String BAD_REQUEST_KEY = "badRequest";
  public static final String NOT_FOUND_KEY = "notFound";
  public static final String ERROR_KEY = "error";

  /** A parser for parsing raw JSON into a  BulkOperationResult **/
  private static JSONParser parser = new JSONParser();

  private JSONArray ok = new JSONArray();
  private JSONArray badRequest = new JSONArray();
  private JSONArray notFound = new JSONArray();
  private JSONArray serverError = new JSONArray();

  @SuppressWarnings("unchecked")
  public void addOk(String id) {
    ok.add(id);
  }

  @SuppressWarnings("unchecked")
  public void addBadRequest(String id) {
    badRequest.add(id);
  }

  @SuppressWarnings("unchecked")
  public void addNotFound(String id) {
    notFound.add(id);
  }

  @SuppressWarnings("unchecked")
  public void addServerError(String id) {
    serverError.add(id);
  }

  public void addOk(Long id) {
    addOk(Long.toString(id));
  }

  public void addBadRequest(Long id) {
    addBadRequest(Long.toString(id));
  }

  public void addNotFound(Long id) {
    addNotFound(Long.toString(id));
  }

  public void addServerError(Long id) {
    addServerError(Long.toString(id));
  }

  public JSONArray getOks() {
    return ok;
  }

  public JSONArray getBadRequests() {
    return badRequest;
  }

  public JSONArray getNotFound() {
    return notFound;
  }

  public JSONArray getServerError() {
    return serverError;
  }

  @SuppressWarnings("unchecked")
  public String toJson() {
    JSONObject bulkOperationResult = new JSONObject();
    bulkOperationResult.put(OK_KEY, ok);
    bulkOperationResult.put(BAD_REQUEST_KEY, badRequest);
    bulkOperationResult.put(NOT_FOUND_KEY, notFound);
    bulkOperationResult.put(ERROR_KEY, serverError);
    return bulkOperationResult.toJSONString();
  }

  public void fromJson(InputStream jsonContent) throws IOException, ParseException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(jsonContent, writer);
    JSONObject result =  (JSONObject) parser.parse(writer.toString());
    this.ok = (JSONArray) result.get(OK_KEY);
    this.badRequest = (JSONArray) result.get(BAD_REQUEST_KEY);
    this.notFound = (JSONArray) result.get(NOT_FOUND_KEY);
    this.serverError = (JSONArray) result.get(ERROR_KEY);
  }

}
