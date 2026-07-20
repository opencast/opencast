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

import org.opencastproject.adminui.exception.JsonCreationException;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.query.StringListFilter;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
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
   * Note that JSONObject (and JSON in general) does not preserve key ordering,
   * so while the Map passed to this function may have ordered keys, the resulting
   * JSONObject is not ordered.
   *
   * @param list
   *          The source list for the JSON object
   * @return a JSON object containing the all the key-value as parameter
   * @throws JsonCreationException
   */
  public static <T> JSONObject generateJSONObject(Map<String, T> list) throws JsonCreationException {

    if (list == null) {
      throw new JsonCreationException("List is null");
    }

    JSONObject jsonList = new JSONObject();

    for (Entry<String, T> entry : list.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String) {
        jsonList.put(entry.getKey(), value);
      } else if (value instanceof JSONObject) {
        jsonList.put(entry.getKey(), value);
      } else if (value instanceof List) {
        Collection collection = (Collection) value;
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(collection);
        jsonList.put(entry.getKey(), jsonArray);
      } else {
        throw new JsonCreationException("Could not deal with " + value);
      }
    }

    return jsonList;
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
  public static JSONArray transformAccessControList(AccessControlList acl, UserDirectoryService userDirectoryService) {
    class TransformedAcl {
      protected String role;
      protected boolean read = false;
      protected boolean write = false;
      protected List<String> actions = new ArrayList();
    }
    Map<String, TransformedAcl> newPolicies = new HashMap();
    JSONArray jsonEntryArray = new JSONArray();

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
      JSONObject jsonEntry = new JSONObject();
      jsonEntry.put("role", policy.role);
      jsonEntry.put("read", policy.read);
      jsonEntry.put("write", policy.write);
      jsonEntry.put("actions", policy.actions);
      if (!isSanitize()) {
        boolean isUserRole = policy.role.startsWith(getUserRolePrefix());
        User user = userDirectoryService.loadUser(policy.role.replaceFirst(getUserRolePrefix(), ""));
        if (user != null) {
          Map<String, Object> userData = new HashMap<>();
          userData.put("username", user.getUsername());
          userData.put("name", user.getName());
          userData.put("email", user.getEmail());
          jsonEntry.put("user", userData);
        } else if (isUserRole) {
          jsonEntry.put("user", new HashMap<String, Object>());
        }
      }
      jsonEntryArray.add(jsonEntry);
    }

    return jsonEntryArray;
  }
}
