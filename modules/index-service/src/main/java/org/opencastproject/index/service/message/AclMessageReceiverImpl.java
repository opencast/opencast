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

package org.opencastproject.index.service.message;

import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.index.service.impl.index.series.SeriesIndexUtils;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.acl.AclItem;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclMessageReceiverImpl extends BaseMessageReceiverImpl<AclItem> {

  private static final Logger logger = LoggerFactory.getLogger(AclMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the acl queue.
   */
  public AclMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(AclItem aclItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (aclItem.getType()) {
      case Create:
        logger.debug("Ignoring create as we don't need to update any indices with it.");
        return;
      case Update:
        logger.debug("Received Update Managed Acl Entry");

        logger.debug("Update the events to change their managed acl name from '{}' to '{}'",
                aclItem.getCurrentAclName(), aclItem.getNewAclName());
        EventIndexUtils.updateManagedAclName(aclItem.getCurrentAclName(), aclItem.getNewAclName(), organization, user,
                getSearchIndex());

        logger.debug("Update the series to change their managed acl name from '{}' to '{}'",
                aclItem.getCurrentAclName(), aclItem.getNewAclName());
        SeriesIndexUtils.updateManagedAclName(aclItem.getCurrentAclName(), aclItem.getNewAclName(), organization, user,
                getSearchIndex());
        return;
      case Delete:
        logger.debug("Received Delete Managed Entry {}", aclItem.getCurrentAclName());

        logger.debug("Update the events to delete their managed acl name '{}'", aclItem.getCurrentAclName());
        EventIndexUtils.deleteManagedAcl(aclItem.getCurrentAclName(), organization, user, getSearchIndex());

        logger.debug("Update the series to delete their managed acl name '{}'", aclItem.getCurrentAclName());
        SeriesIndexUtils.deleteManagedAcl(aclItem.getCurrentAclName(), organization, user, getSearchIndex());
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of AclItem");
    }
  }
}
