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
package org.opencastproject.external.util;

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;

import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;

import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
   * @throws ParseException
   *           Thrown if unable to parse the json value of the acl.
   */
  public static AccessControlList deserializeJsonToAcl(String json, boolean assumeAllow)
          throws IllegalArgumentException, ParseException {
    JSONParser parser = new JSONParser();
    JSONArray aclJson = (JSONArray) parser.parse(json);
    @SuppressWarnings("unchecked")
    ListIterator<Object> iterator = aclJson.listIterator();
    JSONObject aceJson;
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    while (iterator.hasNext()) {
      aceJson = (JSONObject) iterator.next();
      String action = aceJson.get(ACTION_JSON_KEY) != null ? aceJson.get(ACTION_JSON_KEY).toString() : "";
      String allow;
      if (assumeAllow) {
        allow = "true";
      } else {
        allow = aceJson.get(ALLOW_JSON_KEY) != null ? aceJson.get(ALLOW_JSON_KEY).toString() : "";
      }
      String role = aceJson.get(ROLE_JSON_KEY) != null ? aceJson.get(ROLE_JSON_KEY).toString() : "";
      if (StringUtils.trimToNull(action) != null && StringUtils.trimToNull(allow) != null
              && StringUtils.trimToNull(role) != null) {
        AccessControlEntry ace = new AccessControlEntry(role, action, Boolean.parseBoolean(allow));
        entries.add(ace);
      } else {
        throw new IllegalArgumentException(
                String.format(
                        "One of the access control elements is missing a property. The action was '%s', allow was '%s' and the role was '%s'",
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
   * @return A {@link JValue} representation of the {@link AccessControlList}
   */
  public static List<JValue> serializeAclToJson(AccessControlList acl) {
    List<JValue> entries = new ArrayList<JValue>();
    for (AccessControlEntry ace : acl.getEntries()) {
      entries.add(obj(
          f(ALLOW_JSON_KEY, v(ace.isAllow())), f(ACTION_JSON_KEY, v(ace.getAction(), Jsons.BLANK)),
          f(ROLE_JSON_KEY, v(ace.getRole(), Jsons.BLANK))));
    }
    return entries;
  }
}
