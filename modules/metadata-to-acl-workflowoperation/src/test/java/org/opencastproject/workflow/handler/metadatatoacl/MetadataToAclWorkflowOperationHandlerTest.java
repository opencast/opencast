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

package org.opencastproject.workflow.handler.metadatatoacl;

import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Test class for Metadata to ACL operation
 */
public class MetadataToAclWorkflowOperationHandlerTest {

  @Test
  public void testAcl() throws Exception {
    // Prepare workflow
    var mediaPackage = new MediaPackageBuilderImpl().createNew();
    var catalog = CatalogImpl.newInstance();
    catalog.setFlavor(MediaPackageElements.EPISODE);
    mediaPackage.add(catalog);
    var operation = new WorkflowOperationInstance("test-id", WorkflowOperationInstance.OperationState.RUNNING);
    var workflowInstance = new WorkflowInstance();
    workflowInstance.setMediaPackage(mediaPackage);
    workflowInstance.setOperations(Collections.singletonList(operation));

    // Mock workspace
    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.read(EasyMock.anyObject()))
            .andAnswer(() -> this.getClass().getResourceAsStream("/dublincore.xml"))
            .anyTimes();

    // Mock user directory
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    var janeDoe = new JaxbUser("jane.doe", null, new DefaultOrganization());
    EasyMock.expect(userDirectoryService.loadUser("jane.doe")).andReturn(janeDoe).anyTimes();
    EasyMock.expect(userDirectoryService.loadUser("john.doe")).andReturn(null).anyTimes();

    // Mock authorization service
    AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    final Capture<AccessControlList> acl = EasyMock.newCapture();
    EasyMock.expect(authorizationService.setAcl(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.capture(acl)))
            .andReturn(null)
            .once();
    EasyMock.expect(authorizationService.getActiveAcl(EasyMock.anyObject(MediaPackage.class)))
            .andReturn(new Tuple<>(new AccessControlList(), AclScope.Global))
            .once();

    EasyMock.replay(workspace, userDirectoryService, authorizationService);

    // Initialize operation
    var service = new MetadataToAclWorkflowOperationHandler();
    service.setWorkspace(workspace);
    service.setUserDirectory(userDirectoryService);
    service.setAuthorizationService(authorizationService);

    // Run operation
    service.start(workflowInstance, null);

    // We should have two entries for Jane
    Assert.assertEquals(2, acl.getValue().getEntries().size());
  }
}
