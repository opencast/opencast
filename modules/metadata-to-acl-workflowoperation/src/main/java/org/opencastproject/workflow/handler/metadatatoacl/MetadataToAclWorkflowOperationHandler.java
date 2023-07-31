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

package org.opencastproject.workflow.handler.metadatatoacl;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.userdirectory.UserIdRoleProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

/**
 * The <code>MetadataToAclWorkflowOperationHandler</code> can be used to automatically add access rights for users
 * mentioned in metadata fields to the access control list.
 */
@Component(
    property = {
        "service.description=Metadata to ACL Workflow Operation Handler",
        "workflow.operation=metadata-to-acl"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class MetadataToAclWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MetadataToAclWorkflowOperationHandler.class);

  private AuthorizationService authorizationService;
  private UserDirectoryService userDirectory;
  private Workspace workspace;

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    final String field = Objects.toString(
        workflowInstance.getCurrentOperation().getConfiguration("field"),
        "publisher");

    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Get Dublin Core catalog
    Catalog[] catalogs = mediaPackage.getCatalogs(MediaPackageElements.EPISODE);
    if (catalogs.length < 1) {
      return createResult(mediaPackage, Action.SKIP);
    }
    DublinCoreCatalog dcCatalog;
    try {
      dcCatalog = DublinCoreXmlFormat.read(workspace.read(catalogs[0].getURI()));
    } catch (IOException | SAXException | ParserConfigurationException | NotFoundException e) {
      throw new WorkflowOperationException("Unable to load Dublin Core catalog from media package " + mediaPackage, e);
    }

    // Update the ACL based on the Dublin Core field
    updateAcl(mediaPackage, dcCatalog, field);

    // Continue the workflow, passing the possibly modified media package to the next operation
    return createResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * Resolve Dublin Core field to user and set update the ACL so that this user gets read/write permissions.
   *
   * @param mediaPackage
   *          Media package to work with
   * @param dcCatalog
   *          Dublin Core catalog to extract username from
   * @param field
   *          Catalog field to get username from
   * @throws WorkflowOperationException
   *          If the ACL could not be updated
   */
  private void updateAcl(final MediaPackage mediaPackage, final DublinCoreCatalog dcCatalog, final String field)
          throws WorkflowOperationException {
    final AccessControlList acl = authorizationService.getActiveAcl(mediaPackage).getA();
    final var ace = acl.getEntries();

    final EName eName = new EName(DublinCore.TERMS_NS_URI, field);
    for (DublinCoreValue username: dcCatalog.get(eName)) {
      // Find user
      final User user = userDirectory.loadUser(username.getValue());
      if (user == null) {
        logger.debug("Skipping {}. No user with this username found.", username.getValue());
        continue;
      }

      // Update ACL
      final String role = UserIdRoleProvider.getUserIdRole(user.getUsername());
      ace.add(new AccessControlEntry(role, "read", true));
      ace.add(new AccessControlEntry(role, "write", true));
    }

    // Update ACL in media package
    try {
      authorizationService.setAcl(mediaPackage, AclScope.Episode, acl);
    } catch (MediaPackageException e) {
      throw new WorkflowOperationException("Unable to update ACL for media package " + mediaPackage, e);
    }
  }

  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Reference
  public void setUserDirectory(UserDirectoryService userDirectory) {
    this.userDirectory = userDirectory;
  }
}
