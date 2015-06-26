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
import org.opencastproject.message.broker.api.workflow.WorkflowItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WorkflowMessageReceiverImpl extends BaseMessageReceiverImpl<WorkflowItem> {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowMessageReceiverImpl.class);

  private Workspace workspace;
  private AuthorizationService authorizationService;
  private AclServiceFactory aclServiceFactory;

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the workflow queue.
   */
  public WorkflowMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(WorkflowItem workflowItem) {
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    String eventId = null;
    switch (workflowItem.getType()) {
      case UpdateInstance:
        logger.debug("Received Update Workflow instance Entry");

        WorkflowInstance wf = workflowItem.getWorkflowInstance();
        MediaPackage mp = wf.getMediaPackage();
        eventId = mp.getIdentifier().toString();

        Option<DublinCoreCatalog> loadedDC = DublinCoreUtil.loadEpisodeDublinCore(workspace, mp);

        // Load or create the corresponding recording event
        Event event = null;
        try {
          event = getOrCreateEvent(eventId, organization, user, getSearchIndex());
          event.setCreator(getSecurityService().getUser().getName());
          event.setWorkflowId(wf.getId());
          event.setWorkflowDefinitionId(wf.getTemplate());
          event.setWorkflowState(wf.getState());
          Tuple<AccessControlList, AclScope> activeAcl = authorizationService.getActiveAcl(mp);
          if (activeAcl != null && activeAcl.getA() != null) {
            List<ManagedAcl> acls = aclServiceFactory.serviceFor(getSecurityService().getOrganization()).getAcls();
            Option<ManagedAcl> managedAcl = AccessInformationUtil.matchAcls(acls, activeAcl.getA());

            if (managedAcl.isSome()) {
              event.setManagedAcl(managedAcl.get().getName());
            }
            event.setAccessPolicy(AccessControlParser.toJsonSilent(activeAcl.getA()));
          }

          if (loadedDC.isSome())
            updateEvent(event, loadedDC.get());

          updateEvent(event, mp);
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
          logger.debug("Worfklow instance {} updated in the adminui search index", event.getIdentifier());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving the recording event from the search index: {}", e.getMessage());
          return;
        }

        return;
      case DeleteInstance:
        logger.debug("Received Delete Workflow instance Entry {}", eventId);
        eventId = workflowItem.getWorkflowInstance().getMediaPackage().getIdentifier().toString();

        // Remove the Workflow instance entry from the search index
        try {
          getSearchIndex().deleteWorkflow(organization, user, eventId);
          logger.debug("Workflow instance mediapackage {} removed from adminui search index", eventId);
        } catch (NotFoundException e) {
          logger.warn("Workflow instance mediapackage {} not found for deletion", eventId);
        } catch (SearchIndexException e) {
          logger.error("Error deleting the Workflow instance entry {} from the search index: {}", eventId,
                  ExceptionUtils.getStackTrace(e));
        }
        return;
      case AddDefinition:
        // TODO: Update the index with it as soon as the definition are part of it
        return;
      case DeleteDefinition:
        // TODO: Update the index with it as soon as the definition are part of it
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of WorkflowItem");
    }
  }

  /** OSGi DI. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi DI. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi callback for acl services. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

}
