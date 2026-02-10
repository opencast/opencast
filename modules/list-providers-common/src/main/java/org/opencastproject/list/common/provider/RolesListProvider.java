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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.util.ListProviderUtil;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
    service = ResourceListProvider.class,
    property = {
        "service.description=Roles list provider",
        "opencast.service.type=org.opencastproject.index.service.resources.list.provider.RolesListProvider"
    }
)
public class RolesListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "ROLES";

  public static final String TARGET_ACL = PROVIDER_PREFIX + ".TARGET.ACL";
  public static final String TARGET_USER = PROVIDER_PREFIX + ".TARGET.USER";

  private static final String[] NAMES = { PROVIDER_PREFIX, TARGET_ACL, TARGET_USER };
  private static final Logger logger = LoggerFactory.getLogger(RolesListProvider.class);

  private RoleDirectoryService roleDirectoryService;

  @Activate
  protected void activate(BundleContext bundleContext) {
    logger.info("Roles list provider activated!");
  }

  /** OSGi callback for role directory services. */
  @Reference
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> roles = new HashMap<>();
    int offset = 0;
    int limit = 0;

    Role.Target target;
    if (TARGET_ACL.equals(listName)) {
      target = Role.Target.ACL;
    } else if (TARGET_USER.equals(listName)) {
      target = Role.Target.USER;
    } else {
      target = Role.Target.ALL;
    }

    if (query != null) {
      if (query.getLimit().isPresent()) {
        limit = query.getLimit().get();
      }

      if (query.getOffset().isPresent()) {
        offset = query.getOffset().get();
      }
    }

    for (Role r : roleDirectoryService.findRoles("%", target, offset, limit)) {
      roles.put(r.getName(), r.getName());
    }

    return ListProviderUtil.filterMap(roles, query);
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
