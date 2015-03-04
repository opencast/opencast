/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.index.service.message;

import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.index.service.impl.index.group.GroupIndexUtils;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.group.GroupItem;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class GroupMessageReceiverImpl extends BaseMessageReceiverImpl<GroupItem> {

  private static final Logger logger = LoggerFactory.getLogger(GroupMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the external API destination of the group queue.
   */
  public GroupMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(GroupItem groupItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (groupItem.getType()) {
      case Update:
        org.opencastproject.security.api.Group jaxbGroup = groupItem.getGroup();

        logger.debug(
                "Update the group with id '{}', name '{}', description '{}', organization '{}', roles '{}', members '{}'",
                new Object[] { jaxbGroup.getGroupId(), jaxbGroup.getName(), jaxbGroup.getDescription(),
                        jaxbGroup.getOrganization(), jaxbGroup.getRoles(), jaxbGroup.getMembers() });
        try {
          Group group = GroupIndexUtils.getOrCreate(jaxbGroup.getGroupId(), organization, user, getSearchIndex());
          group.setName(jaxbGroup.getName());
          group.setDescription(jaxbGroup.getDescription());
          group.setMembers(jaxbGroup.getMembers());
          Set<String> roles = new HashSet<String>();
          for (Role role : jaxbGroup.getRoles()) {
            roles.add(role.getName());
          }
          group.setRoles(roles);
          getSearchIndex().addOrUpdate(group);
        } catch (SearchIndexException e) {
          logger.error("Error storing the group {} to the search index: {}", jaxbGroup.getGroupId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }
        break;
      case Delete:
        logger.debug("Received Delete Group Event {}", groupItem.getGroupId());

        // Remove the group from the search index
        try {
          getSearchIndex().delete(Group.DOCUMENT_TYPE, groupItem.getGroupId().concat(organization));
          logger.debug("Group {} removed from external search index", groupItem.getGroupId());
        } catch (SearchIndexException e) {
          logger.error("Error deleting the group {} from the search index: {}", groupItem.getGroupId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of GroupItem");
    }
  }
}
