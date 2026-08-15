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

package org.opencastproject.adminui.endpoint;

import static org.opencastproject.userdirectory.UserIdRoleProvider.getUserRolePrefix;
import static org.opencastproject.userdirectory.UserIdRoleProvider.isSanitize;

import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.query.StringListFilter;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.GsonUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class EndpointUtil {

  private EndpointUtil() {
  }

  /**
   * Returns a generated JSON object with key-value from given list.
   *
   * The members appear in the order the map iterates, so a map with ordered keys yields an ordered object.
   *
   * @param list
   *          The source list for the JSON object
   * @return a JSON object containing the all the key-value as parameter
   */
  public static JsonObject generateJSONObject(Map<String, String> list) {

    JsonObject jsonList = new JsonObject();

    for (Entry<String, String> entry : list.entrySet()) {
      jsonList.addProperty(entry.getKey(), entry.getValue());
    }

    return jsonList;
  }

  /**
   * Re-parses a document built with jettison into a Gson tree.
   * <p>
   * AccessInformationUtil speaks jettison, and json-simple used to splice its output into a response by falling back
   * to toString() for any type it did not recognise. Gson has no such fallback, so the conversion is explicit.
   *
   * @param json
   *          the jettison object to convert
   * @return the same document as a Gson tree
   */
  public static JsonElement fromJettison(JSONObject json) {
    return JsonParser.parseString(json.toString());
  }

  /**
   * Add the string based filters to the given list query.
   *
   * @param filterString
   *          The string based filters
   * @param query
   *          The query to update with the filters
   */
  public static void addRequestFiltersToQuery(final String filterString, DefaultResourceListQuery query) {
    for (var filter : RestUtils.parseFilter(filterString).entrySet()) {
      query.addFilter(new StringListFilter(filter.getKey(), filter.getValue()));
    }
  }

  /**
   * Transform ACL into the format the admin ui frontend uses.
   * We do this in the backend so we can attach information about users to user roles.
   */
  public static JsonArray transformAccessControList(AccessControlList acl, UserDirectoryService userDirectoryService) {
    class TransformedAcl {
      protected String role;
      protected boolean read = false;
      protected boolean write = false;
      protected List<String> actions = new ArrayList();
    }
    Map<String, TransformedAcl> newPolicies = new HashMap();
    JsonArray jsonEntryArray = new JsonArray();

    for (AccessControlEntry entry : acl.getEntries()) {
      if (!newPolicies.containsKey(entry.getRole())) {
        TransformedAcl transformedEntry = new TransformedAcl();
        transformedEntry.role = entry.getRole();
        newPolicies.put(entry.getRole(), transformedEntry);
      }

      if ("read".equals(entry.getAction())) {
        newPolicies.get(entry.getRole()).read = entry.isAllow();
      } else if ("write".equals(entry.getAction())) {
        newPolicies.get(entry.getRole()).write = entry.isAllow();
      } else if (entry.isAllow()) {
        newPolicies.get(entry.getRole()).actions.add(entry.getAction());
      }
    }

    for (TransformedAcl policy : newPolicies.values()) {
      JsonObject jsonEntry = new JsonObject();
      jsonEntry.addProperty("role", policy.role);
      jsonEntry.addProperty("read", policy.read);
      jsonEntry.addProperty("write", policy.write);
      JsonArray actions = new JsonArray();
      policy.actions.forEach(actions::add);
      jsonEntry.add("actions", actions);
      if (!isSanitize()) {
        boolean isUserRole = policy.role.startsWith(getUserRolePrefix());
        User user = userDirectoryService.loadUser(policy.role.replaceFirst(getUserRolePrefix(), ""));
        if (user != null) {
          Map<String, Object> userData = new HashMap<>();
          userData.put("username", user.getUsername());
          userData.put("name", user.getName());
          userData.put("email", user.getEmail());
          jsonEntry.add("user", GsonUtil.gson().toJsonTree(userData));
        } else if (isUserRole) {
          jsonEntry.add("user", new JsonObject());
        }
      }
      jsonEntryArray.add(jsonEntry);
    }

    return jsonEntryArray;
  }
}
