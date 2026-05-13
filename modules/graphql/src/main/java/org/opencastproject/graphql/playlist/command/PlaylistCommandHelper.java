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

package org.opencastproject.graphql.playlist.command;

import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.graphql.execution.context.OpencastContext;
import org.opencastproject.graphql.type.input.AccessControlListInput;
import org.opencastproject.playlists.PlaylistAccessControlEntry;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.SecurityService;

import java.util.ArrayList;
import java.util.List;

final class PlaylistCommandHelper {

  private PlaylistCommandHelper() {
  }

  static List<PlaylistAccessControlEntry> toPlaylistAce(AccessControlListInput aclInput, OpencastContext context) {
    AccessControlList acl = new AccessControlList();
    if (aclInput.getEntries() != null) {
      for (var item : aclInput.getEntries()) {
        for (var action : item.getAction()) {
          acl.getEntries().add(new AccessControlEntry(item.getRole(), action, true));
        }
      }
    }
    if (aclInput.getManagedAclId() != null) {
      var aclService = context.getService(AclServiceFactory.class)
          .serviceFor(context.getService(SecurityService.class).getOrganization());
      aclService.getAcl(aclInput.getManagedAclId()).ifPresent(value -> acl.merge(value.getAcl()));
    }
    List<PlaylistAccessControlEntry> aceList = new ArrayList<>();
    for (AccessControlEntry entry : acl.getEntries()) {
      aceList.add(new PlaylistAccessControlEntry(entry.isAllow(), entry.getRole(), entry.getAction()));
    }
    return aceList;
  }

}
