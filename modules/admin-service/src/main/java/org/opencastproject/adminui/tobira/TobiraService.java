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

package org.opencastproject.adminui.tobira;

import org.opencastproject.util.GsonUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TobiraService {

  /** The shape TobiraException wants its errors in; JSONArray used to satisfy it by being a raw List. */
  private static final Type ERRORS_TYPE = new TypeToken<List<Map<String, Object>>>() { }.getType();

  public JsonObject getPage(String path) throws TobiraException {
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

  public JsonObject getHostPages(String seriesId) throws TobiraException {
    return asObject(request(
            "query AdminUIHostPages($seriesId: String!) {"
                    + "  series: seriesByOpencastId(id: $seriesId) {"
                    + "    id"
                    + "    hostPages: hostRealms {"
                    + "      ... RealmData"
                    + "      blocks { id }"
                    + "      ancestors {"
                    + "        ... RealmData"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}"
                    + "fragment RealmData on Realm {"
                    + "  title: name"
                    + "  segment: pathSegment"
                    + "  path"
                    + "}",
            Map.of("seriesId", seriesId))
            .get("series"));
  }

  public JsonObject getEventHostPages(String eventId) throws TobiraException {
    return asObject(request(
            "query AdminUIEventHostPages($eventId: String!) {"
                    + "  event: eventByOpencastId(id: $eventId) {"
                    + "    ...on AuthorizedEvent {"
                    + "      id"
                    + "      hostPages: hostRealms {"
                    + "        title: name"
                    + "        path"
                    + "        ancestors { title: name }"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}",
            Map.of("eventId", eventId))
            .get("event"));
  }

  public void mount(Map<String, Object> variables) throws TobiraException {
    request(
        "mutation AdminUIMountSeries($series: NewSeries!, $parentPagePath: String!, $newPages: [RealmSpecifier!]!) {"
                + "  mountSeries(series: $series, parentRealmPath: $parentPagePath, newRealms: $newPages) {"
                // We need to query something here, but we really don't care at the moment
                + "    id"
                + "  }"
                + "}",
        variables);
  }

  public Integer createRealmLineage(List<JsonObject> pathComponents) throws TobiraException {
    return request(
            "mutation AdminUICreateRealmLineage($realms: [RealmLineageComponent!]!) {"
                    + "  createRealmLineage(realms: $realms) { numCreated }"
                    + "}",
            Map.of("realms", pathComponents))
            .get("numCreated").getAsInt();
  }

  public String addSeriesMountPoint(Map<String, Object> variables) throws TobiraException {
    return GsonUtil.getStringOrNull(request(
            "mutation AdminUIAddSeriesMountPoint($seriesId: String!, $targetPath: String!) {"
                    + "  addSeriesMountPoint(seriesOcId: $seriesId, targetPath: $targetPath) {"
                    + "    id"
                    + "  }"
                    + "}",
            variables), "id");
  }

  public JsonObject removeSeriesMountPoint(Map<String, Object> variables) throws TobiraException {
    return asObject(request(
            "mutation AdminUIRemoveSeriesMountPoint($seriesId: String!, $currentPath: String!) {"
                    + "  outcome: removeSeriesMountPoint(seriesOcId: $seriesId, path: $currentPath) {"
                    + "    __typename"
                    + "  }"
                    + "}",
            variables)
            .get("outcome"));
  }

  public boolean ready() {
    return this.endpoint != null && this.trustedKey != null;
  }

  private JsonObject request(String query, Map<String, Object> variables) throws TobiraException {

    var queryObject = new JsonObject();
    queryObject.addProperty("query", query);
    queryObject.add("variables", GsonUtil.gson().toJsonTree(variables));

    var request = HttpRequest.newBuilder()
            .uri(endpoint)
            .header("content-type", "application/json")
            .header("x-tobira-trusted-external-key", trustedKey)
            .POST(HttpRequest.BodyPublishers.ofString(queryObject.toString()))
            .build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new TobiraException(response);
      }
      var responseObject = GsonUtil.gson().fromJson(response.body(), JsonObject.class);
      var errors = responseObject.getAsJsonArray("errors");
      if (errors != null) {
        throw new TobiraException(response, GsonUtil.gson().fromJson(errors, ERRORS_TYPE));
      }
      return responseObject.getAsJsonObject("data");
    } catch (IOException | InterruptedException | JsonParseException e) {
      throw new TobiraException(e);
    }
  }

  /** Read a member as an object, answering null when it is absent or JSON null. */
  private static JsonObject asObject(JsonElement value) {
    return value == null || value.isJsonNull() ? null : value.getAsJsonObject();
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
    if (origin == null) {
      this.endpoint = null;
    } else {
      try {
        this.endpoint = new URI(origin).resolve("/graphql");
      } catch (URISyntaxException e) {
        logger.error("Invalid Tobira origin {}", origin, e);
        this.origin = null;
        this.endpoint = null;
      }
    }
  }

  public void setTrustedKey(String trustedKey) {
    this.trustedKey = trustedKey;
  }

  private String origin;
  private URI endpoint;
  private String trustedKey;

  private static final HttpClient client = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private static final Logger logger = LoggerFactory.getLogger(TobiraService.class);

  private static Map<String, TobiraService> tobiras = new HashMap<>();

  public static TobiraService getTobira(String organization) {
    return tobiras.computeIfAbsent(organization, org -> new TobiraService());
  }
}
