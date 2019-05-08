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

package org.opencastproject.statistics.api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashSet;
import java.util.Set;

/**
 * Model for the json configurations files for statistics providers.
 */
public final class ConfiguredProvider {
  private static final JSONParser parser = new JSONParser();
  private String id;
  private String title;
  private String description;
  private ResourceType resourceType;
  private Set<DataResolution> resolutions;
  private String type;
  private String source;

  private ConfiguredProvider(
      String id,
      String title,
      String description,
      ResourceType resourceType,
      Set<DataResolution> resolutions,
      String type,
      String source) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.resourceType = resourceType;
    this.resolutions = resolutions;
    this.type = type;
    this.source = source;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  public Set<DataResolution> getResolutions() {
    return resolutions;
  }

  public String getType() {
    return type;
  }

  public String getSource() {
    return source;
  }

  public static ConfiguredProvider fromJson(String json) throws ParseException {
    final JSONObject jsonObject = (JSONObject) parser.parse(json);
    final JSONArray resolutionsJson = (JSONArray) jsonObject.get("resolutions");
    final Set<DataResolution> resolutions = new HashSet<>();
    for (Object resolution : resolutionsJson) {
      resolutions.add(DataResolution.fromString((String) resolution));
    }
    return new ConfiguredProvider(
        (String) jsonObject.get("id"),
        (String) jsonObject.get("title"),
        (String) jsonObject.get("description"),
        ResourceType.fromString((String) jsonObject.get("resourceType")),
        resolutions,
        (String) jsonObject.get("type"),
        (String) jsonObject.get("source")
    );
  }
}
