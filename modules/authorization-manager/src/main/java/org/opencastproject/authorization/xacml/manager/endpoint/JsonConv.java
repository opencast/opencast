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

package org.opencastproject.authorization.xacml.manager.endpoint;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.function.Function;

/** Converter functions from business objects to JSON structures. */
public final class JsonConv {

  public static final String KEY_ID = "id";
  public static final String KEY_NAME = "name";
  public static final String KEY_ORGANIZATION_ID = "organizationId";
  public static final String KEY_ACL = "acl";
  public static final String KEY_ACE = "ace";
  public static final String KEY_ROLE = "role";
  public static final String KEY_ACTION = "action";
  public static final String KEY_ALLOW = "allow";

  private JsonConv() {
  }

  public static JsonObject digest(ManagedAcl acl) {
    JsonObject json = new JsonObject();
    json.addProperty(KEY_ID, acl.getId());
    json.addProperty(KEY_NAME, acl.getName());
    return json;
  }

  public static JsonObject full(ManagedAcl acl) {
    JsonObject json = new JsonObject();
    json.addProperty(KEY_ID, acl.getId());
    json.addProperty(KEY_NAME, acl.getName());
    json.addProperty(KEY_ORGANIZATION_ID, acl.getOrganizationId());
    json.add(KEY_ACL, full(acl.getAcl()));
    return json;
  }

  public static final Function<ManagedAcl, JsonObject> fullManagedAcl = JsonConv::full;

  public static JsonObject full(AccessControlList acl) {
    JsonArray entries = new JsonArray();
    for (AccessControlEntry entry : acl.getEntries()) {
      entries.add(full(entry));
    }
    JsonObject json = new JsonObject();
    json.add(KEY_ACE, entries);
    return json;
  }

  public static JsonObject full(AccessControlEntry ace) {
    JsonObject json = new JsonObject();
    json.addProperty(KEY_ROLE, ace.getRole());
    json.addProperty(KEY_ACTION, ace.getAction());
    json.addProperty(KEY_ALLOW, ace.isAllow());
    return json;
  }

  public static final Function<AccessControlEntry, JsonObject> fullAccessControlEntry = JsonConv::full;
}
