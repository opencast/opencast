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

import static org.opencastproject.index.service.impl.index.event.EventIndexUtils.getOrCreateEvent;
import static org.opencastproject.index.service.impl.index.event.EventIndexUtils.updateEvent;

import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventIndexUtils;
import org.opencastproject.index.service.util.AccessInformationUtil;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.archive.ArchiveItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ArchiveMessageReceiverImpl extends BaseMessageReceiverImpl<ArchiveItem> {

  private static final Logger logger = LoggerFactory.getLogger(ArchiveMessageReceiverImpl.class);

  private Workspace workspace;

  private AclServiceFactory aclServiceFactory;

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the archive queue.
   */
  public ArchiveMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(ArchiveItem archiveItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();

    switch (archiveItem.getType()) {
      case Update:
        logger.debug("Received Update Archive Entry");

        MediaPackage mp = archiveItem.getMediapackage();
        Option<DublinCoreCatalog> loadedDC = DublinCoreUtil.loadEpisodeDublinCore(workspace, mp);

        // Load or create the corresponding recording event
        Event event = null;
        try {
          event = getOrCreateEvent(mp.getIdentifier().toString(), organization, user, getSearchIndex());

          if (archiveItem.getAcl() != null) {
            List<ManagedAcl> acls = aclServiceFactory.serviceFor(getSecurityService().getOrganization()).getAcls();
            Option<ManagedAcl> managedAcl = AccessInformationUtil.matchAcls(acls, archiveItem.getAcl());

            if (managedAcl.isSome()) {
              event.setManagedAcl(managedAcl.get().getName());
            }
            event.setAccessPolicy(AccessControlParser.toJsonSilent(archiveItem.getAcl()));
          }

          event.setArchiveVersion(archiveItem.getVersion().value());
          event.setCreator(getSecurityService().getUser().getName());
          updateEvent(event, mp);

          if (loadedDC.isSome())
            updateEvent(event, loadedDC.get());

        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", e.getMessage());
          return;
        }

        // Update series name if not already done
        try {
          EventIndexUtils.updateSeriesName(event, organization, user, getSearchIndex());
        } catch (SearchIndexException e) {
          logger.error("Error updating the series name of the event to index: {}", ExceptionUtils.getStackTrace(e));
        }

        // Persist the scheduling event
        try {
          getSearchIndex().addOrUpdate(event);
          logger.debug("Archive entry {} updated in the adminui search index", event.getIdentifier());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", e.getMessage());
          return;
        }

        return;
      case Delete:
        String eventId = archiveItem.getMediapackageId();
        logger.debug("Received Delete Archive Entry {}", eventId);

        // Remove the archived entry from the search index
        try {
          getSearchIndex().deleteArchive(organization, user, eventId);
          logger.debug("Archived mediapackage {} removed from adminui search index", eventId);
        } catch (NotFoundException e) {
          logger.warn("Archived mediapackage {} not found for deletion", eventId);
        } catch (SearchIndexException e) {
          logger.error("Error deleting the archived entry {} from the search index: {}", eventId,
                  ExceptionUtils.getStackTrace(e));
        }
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of ArchiveItem");
    }
  }

  /** OSGi DI. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi callback for acl services. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }
}
