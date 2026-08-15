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
package org.opencastproject.external.util;

import static org.opencastproject.index.service.util.JSONUtils.safeString;

import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class AclUtils {
  private static final String ACTION_JSON_KEY = "action";
  private static final String ALLOW_JSON_KEY = "allow";
  private static final String ROLE_JSON_KEY = "role";

  private AclUtils() {
  }

  /**
   * De-serialize an JSON into an {@link AccessControlList}.
   *
   * @param json
   *          The {@link AccessControlList} to serialize.
   * @param assumeAllow
   *          Assume that all entries are allows.
   * @return An {@link AccessControlList} representation of the Json
   * @throws IllegalArgumentException
   *           Thrown if essential parts of an access control element is missing.
   *           Thrown if unable to parse the json value of the acl.
   */
  /** Render a value as plain text rather than JSON, empty when absent or null. */
  private static String asPlainString(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return "";
    }
    return value.isJsonPrimitive() ? value.getAsString() : value.toString();
  }

  public static AccessControlList deserializeJsonToAcl(String json, boolean assumeAllow)
          throws IllegalArgumentException {
    JsonArray aclJson = JsonParser.parseString(json).getAsJsonArray();
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    for (JsonElement element : aclJson) {
      JsonObject aceJson = element.getAsJsonObject();
      String action = asPlainString(aceJson.get(ACTION_JSON_KEY));
      String allow;
      if (assumeAllow) {
        allow = "true";
      } else {
        allow = asPlainString(aceJson.get(ALLOW_JSON_KEY));
      }
      String role = asPlainString(aceJson.get(ROLE_JSON_KEY));
      if (StringUtils.trimToNull(action) != null && StringUtils.trimToNull(allow) != null
              && StringUtils.trimToNull(role) != null) {
        AccessControlEntry ace = new AccessControlEntry(role, action, Boolean.parseBoolean(allow));
        entries.add(ace);
      } else {
        throw new IllegalArgumentException(String.format("One of the access control elements is missing a property. "
                + "The action was '%s', allow was '%s' and the role was '%s'",
                        action, allow, role));
      }
    }
    return new AccessControlList(entries);
  }

  /**
   * Serialize an {@link AccessControlList} into json.
   *
   * @param acl
   *          The {@link AccessControlList} to serialize.
   * @return A {@link JsonArray} representation of the {@link AccessControlList}
   */
  public static JsonArray serializeAclToJson(AccessControlList acl) {
    JsonArray entries = new JsonArray();
    for (AccessControlEntry ace : acl.getEntries()) {
      JsonObject entry = new JsonObject();
      entry.addProperty(ALLOW_JSON_KEY, ace.isAllow());
      entry.addProperty(ACTION_JSON_KEY, safeString(ace.getAction()));
      entry.addProperty(ROLE_JSON_KEY, safeString(ace.getRole()));
      entries.add(entry);
    }
    return entries;
  }
}
