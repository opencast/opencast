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

package org.opencastproject.adminui.tobira;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public final class TobiraService {

  public JSONObject getPage(String path) throws TobiraException {
    return request(
            "query AdminUiPage($path: String!) {"
                    + "  page: realmByPath(path: $path) {"
                    + "    ... RealmData"
                    + "    children {"
                    + "      ... RealmData"
                    + "      blocks {"
                    // We only need the number of blocks, but we need to query something from them, unfortunately.
                    + "        id"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}"
                    + "fragment RealmData on Realm {"
                    + "  title: name"
                    + "  segment: pathSegment"
                    + "  path"
                    + "}",
            Map.of("path", path));
  }

  public void mount(Map<String, Object> variables) throws TobiraException {
    request(
            "mutation AdminUIMountSeries($seriesId: String!, $parentPagePath: String!, $newPages: [RealmSpecifier!]!) {"
                    + "  mountSeries(ocSeriesId: $seriesId, parentRealmPath: $parentPagePath, newRealms: $newPages) {"
                    // We need to query something here, but we really don't care at the moment
                    + "    id"
                    + "  }"
                    + "}",
            variables);
  }

  public boolean ready() {
    return this.endpoint != null && this.trustedKey != null;
  }

  private JSONObject request(String query, Map<String, Object> variables) throws TobiraException {

    var queryObject = new JSONObject(Map.of(
            "query", query,
            "variables", new JSONObject(variables)));

    var request = HttpRequest.newBuilder()
            .uri(endpoint)
            .header("content-type", "application/json")
            .header("x-tobira-trusted-external-key", trustedKey)
            .POST(HttpRequest.BodyPublishers.ofString(queryObject.toJSONString()))
            .build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new TobiraException(response);
      }
      var responseObject = (JSONObject) new JSONParser().parse(response.body());
      var errors = (JSONArray) responseObject.get("errors");
      if (errors != null) {
        throw new TobiraException(response, errors);
      }
      return (JSONObject) responseObject.get("data");
    } catch (IOException | InterruptedException | ParseException e) {
      throw new TobiraException(e);
    }
  }

  public void setOrigin(String origin) {
    if (origin == null) {
      this.endpoint = null;
    } else {
      try {
        this.endpoint = new URI(origin).resolve("/graphql");
      } catch (URISyntaxException e) {
        logger.error("Invalid Tobira origin {}", origin, e);
        this.endpoint = null;
      }
    }
  }

  public void setTrustedKey(String trustedKey) {
    this.trustedKey = trustedKey;
  }

  private URI endpoint;
  private String trustedKey;

  private static final HttpClient client = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private static final Logger logger = LoggerFactory.getLogger(TobiraService.class);
}
