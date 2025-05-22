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

import java.util.Set;


public abstract class AbstractAccessControlItem {

  private final Set<AccessControlEntry> accessControlEntries;

  private final String uniqueRole;

  public AbstractAccessControlItem(AccessControlEntry accessControlEntry) {
    this(Set.of(accessControlEntry));
  }

  public AbstractAccessControlItem(Set<AccessControlEntry> accessControlEntries) {
    String uniqueRole = accessControlEntries.iterator().next().getRole();
    for (AccessControlEntry ace : accessControlEntries) {
      if (!uniqueRole.equals(ace.getRole())) {
        throw new IllegalArgumentException("All access control entries must have the same role");
      }
    }
    this.uniqueRole = uniqueRole;
    this.accessControlEntries = accessControlEntries;
  }

  public String getUniqueRole() {
    return uniqueRole;
  }

  public Set<AccessControlEntry> getAccessControlEntries() {
    return accessControlEntries;
  }

}
