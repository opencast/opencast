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

import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.security.api.JaxbGroup;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "GROUPS";

  public static final String DESCRIPTION = PROVIDER_PREFIX + ".DESCRIPTION";
  public static final String NAME = PROVIDER_PREFIX + ".NAME";

  protected static final String[] NAMES = { PROVIDER_PREFIX, NAME, DESCRIPTION };
  private static final Logger logger = LoggerFactory.getLogger(GroupsListProvider.class);

  private JpaGroupRoleProvider groupRoleProvider;

  protected void activate(BundleContext bundleContext) {
    logger.info("Groups list provider activated!");
  }

  /** OSGi callback for groups services. */
  public void setGroupProvider(JpaGroupRoleProvider groupRoleProvider) {
    this.groupRoleProvider = groupRoleProvider;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> groupsList = new HashMap<String, String>();
    List<JaxbGroup> groups = null;
    int limit = 0;
    int offset = 0;

    if (query != null) {
      if (query.getLimit().isSome())
        limit = query.getLimit().get();

      if (query.getOffset().isSome())
        offset = query.getOffset().get();
    }

    try {
      groups = groupRoleProvider.getGroupsAsJson(limit, offset).getGroups();
    } catch (IOException e) {
      logger.error("Not able to get the group list: " + e);
      return groupsList;
    }

    for (JaxbGroup g : groups) {
      if (NAME.equals(listName)) {
        groupsList.put(g.getName(), g.getName());
      } else if (DESCRIPTION.equals(listName)) {
        groupsList.put(g.getDescription(), g.getDescription());
      } else {
        groupsList.put(g.getGroupId(), g.getName());
      }
    }

    return groupsList;
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
