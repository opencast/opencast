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

package org.opencastproject.workflow.handler.acl;

import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;

import org.opencastproject.authorization.xacml.XACMLUtils;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

/**
 * The <code>HelloWorldWorkflowOperationHandler</code> provides a very simple example of how a workflow operation works
 * and can be a starting point for new developments.
 *
 * Like the other hello-world modules, this is intentionally not included in the Opencast distributions and thus not
 * listed in the documentation since people cannot actually use it.
 */
@Component(
    property = {
        "service.description=ACL Workflow Operation Handler",
        "workflow.operation=acl"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class ACLWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ACLWorkflowOperationHandler.class);

  private static final String ACL_FILENAME = "episode-security.xml";
  private static final String ACL_ELEMENT = "security-policy-episode";

  private Workspace workspace;

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    // Get reference to current operation to get configuration, …
    final var operation = workflowInstance.getCurrentOperation();
    final var mediaPackage = workflowInstance.getMediaPackage();

    // Get configuration
    // TODO: Also implement add-write-role, remove-read-role, remove-write-role, remove-role
    var addReadRole = Arrays.stream(StringUtils.split(operation.getConfiguration("add-read-roles"), ", "))
        .map(role -> new AccessControlEntry(role, "read", true))
        .toList();

    logger.info("Adding roles allowed to read: {}", addReadRole);

    // Find current episode ACL
    final var episodeXACMLs = mediaPackage.getAttachments(XACML_POLICY_EPISODE);
    if (episodeXACMLs.length > 1) {
      throw new WorkflowOperationException("More than one ACL attachment. This shouldn't happen.");
    }

    // Parse ACL or set a new one if none exists
    final AccessControlList acl;
    if (episodeXACMLs.length == 0) {
      acl = new AccessControlList();
    } else {
      try (var in = workspace.read(episodeXACMLs[0].getURI())) {
        acl = AccessControlParser.parseAcl(in);
      } catch (NotFoundException | IOException | AccessControlParsingException e) {
        throw new WorkflowOperationException("Unable to parse ACL. Corrupted XACML file?", e);
      }
    }

    // Add additional roles
    acl.getEntries().addAll(addReadRole);
    final String xacml;
    try {
      xacml = XACMLUtils.getXacml(mediaPackage, acl);
    } catch (JAXBException e) {
      throw new WorkflowOperationException("Unable to convert ACL to XACML", e);
    }

    // Create a new attachment
    Attachment aclAttachment;
    if (episodeXACMLs.length > 0) {
      aclAttachment = episodeXACMLs[0];
    } else {
      aclAttachment = new AttachmentImpl();
      aclAttachment.generateIdentifier();
      mediaPackage.add(aclAttachment);
    }

    // Store new ACL
    try (var in = IOUtils.toInputStream(xacml, StandardCharsets.UTF_8)) {
      var uri = workspace.put(mediaPackage.getIdentifier().toString(), ACL_ELEMENT, ACL_FILENAME, in);
      aclAttachment.setURI(uri);
    } catch (IOException e) {
      throw new WorkflowOperationException("Could not write ACL file", e);
    }

    // Continue the workflow, passing the possibly modified media package to the next operation
    return createResult(mediaPackage, Action.CONTINUE);
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
