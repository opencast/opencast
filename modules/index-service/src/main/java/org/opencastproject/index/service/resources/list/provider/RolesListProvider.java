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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class RolesListProvider implements ResourceListProvider {

  public static final String ROLES = "ROLES";
  public static final String ROLE_QUERY_KEY = "role_name";
  public static final String ROLE_TARGET_KEY = "role_target";

  private static final String[] NAMES = { ROLES };
  private static final Logger logger = LoggerFactory.getLogger(RolesListProvider.class);

  private RoleDirectoryService roleDirectoryService;

  protected void activate(BundleContext bundleContext) {
    logger.info("Roles list provider activated!");
  }

  /** OSGi callback for role directory service. */
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {

    // Preserve the ordering of roles from the providers
    Map<String, String> rolesList = new LinkedHashMap<String, String>();

    int offset = 0;
    int limit = 0;

    if (query != null) {
      if (query.getLimit().isSome())
        limit = query.getLimit().get();

      if (query.getOffset().isSome())
        offset = query.getOffset().get();
    }

    String queryString = "%";

    if (query.hasFilter(ROLE_QUERY_KEY)) {
      queryString = query.getFilter(ROLE_QUERY_KEY).getValue().get() + "%";
    }

    Role.Target target = Role.Target.ALL;

    if (query.hasFilter(ROLE_TARGET_KEY)) {
      String targetString = (String) query.getFilter(ROLE_TARGET_KEY).getValue().get();
      try {
        target = Role.Target.valueOf(targetString);
      } catch (Exception e) {
        logger.warn("Invalid target filter value {}", targetString);
      }
    }

    Iterator<Role> roles = roleDirectoryService.findRoles(queryString, target, offset, limit);

    while (roles.hasNext()) {
      Role r = roles.next();
      rolesList.put(r.getName(), r.getType().toString());
    }

    return rolesList;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return false;
  }

  @Override
  public String getDefault() {
    return null;
  }
}
