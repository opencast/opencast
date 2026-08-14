/*
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to store the results of a bulk operation on an endpoint and to easily return those results.
 */
public class BulkOperationResult {
  public static final String OK_KEY = "ok";
  public static final String ACCEPTED_KEY = "accepted";
  public static final String BAD_REQUEST_KEY = "badRequest";
  public static final String UNAUTHORIZED_KEY = "unauthorized";
  public static final String NOT_FOUND_KEY = "notFound";
  public static final String ERROR_KEY = "error";

  private final List<String> ok = new ArrayList<>();
  private final List<String> accepted = new ArrayList<>();
  private final List<String> badRequest = new ArrayList<>();
  private final List<String> unauthorized = new ArrayList<>();
  private final List<String> notFound = new ArrayList<>();
  private final List<String> serverError = new ArrayList<>();

  public void addOk(String id) {
    ok.add(id);
  }

  public void addAccepted(String id) {
    accepted.add(id);
  }

  public void addBadRequest(String id) {
    badRequest.add(id);
  }

  public void addNotFound(String id) {
    notFound.add(id);
  }

  public void addServerError(String id) {
    serverError.add(id);
  }

  public void addOk(Long id) {
    addOk(Long.toString(id));
  }

  public void addBadRequest(Long id) {
    addBadRequest(Long.toString(id));
  }

  public void addUnauthorized(String id) {
    unauthorized.add(id);
  }

  public void addNotFound(Long id) {
    addNotFound(Long.toString(id));
  }

  public void addServerError(Long id) {
    addServerError(Long.toString(id));
  }

  public String toJson() {
    JsonObject bulkOperationResult = new JsonObject();
    bulkOperationResult.add(OK_KEY, toJsonArray(ok));
    bulkOperationResult.add(ACCEPTED_KEY, toJsonArray(accepted));
    bulkOperationResult.add(BAD_REQUEST_KEY, toJsonArray(badRequest));
    bulkOperationResult.add(NOT_FOUND_KEY, toJsonArray(notFound));
    bulkOperationResult.add(UNAUTHORIZED_KEY, toJsonArray(unauthorized));
    bulkOperationResult.add(ERROR_KEY, toJsonArray(serverError));
    return bulkOperationResult.toString();
  }

  private static JsonArray toJsonArray(List<String> ids) {
    JsonArray array = new JsonArray();
    ids.forEach(array::add);
    return array;
  }
}
