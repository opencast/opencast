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

package org.opencastproject.graphql.type.output;

import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.Group;
import org.opencastproject.userdirectory.UserIdRoleProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName(GqlAccessControlList.TYPE_NAME)
@GraphQLDescription("Access control list")
public class GqlAccessControlList {

  public static final String TYPE_NAME = "AccessControlList";

  private final AccessControlList accessControlList;

  private final List<GqlAccessControlItem> items;

  public GqlAccessControlList(AccessControlList accessControlList) {
    this.accessControlList = accessControlList;
    this.items = getItems();
  }

  @GraphQLField
  public GqlAccessControlUserItem[] users() {
    return items.stream()
        .filter(i -> i instanceof GqlAccessControlUserItem)
        .toArray(GqlAccessControlUserItem[]::new);
  }

  @GraphQLField
  public GqlAccessControlItem[] entries() {
    return items.toArray(new GqlAccessControlItem[0]);
  }

  private List<GqlAccessControlItem> getItems() {
    String groupPrefix = Group.ROLE_PREFIX;
    String userPrefix = UserIdRoleProvider.getUserIdRole("");

    Map<String, Set<AccessControlEntry>> entries = accessControlList.getEntries().stream()
        .collect(Collectors.groupingBy(AccessControlEntry::getRole, Collectors.toSet()));

    return entries.entrySet().stream()
        .map(e -> {
          String role = e.getKey();
          Set<AccessControlEntry> ace = e.getValue();
          if (role.startsWith(groupPrefix)) {
            return new GqlAccessControlGroupItem(ace);
          } else if (role.startsWith(userPrefix)) {
            return new GqlAccessControlUserItem(ace);
          } else {
            return new GqlAccessControlGenericItem(ace);
          }
        })
        .collect(Collectors.toList());
  }

}
