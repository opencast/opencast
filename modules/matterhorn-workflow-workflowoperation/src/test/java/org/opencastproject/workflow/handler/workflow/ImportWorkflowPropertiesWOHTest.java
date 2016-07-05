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

package org.opencastproject.workflow.handler.workflow;


import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workspace.api.Workspace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class ImportWorkflowPropertiesWOHTest {

  public static final String WF_PROPS_ATT_URI = "http://opencast.org/attachments/workflow-properties.xml";
  public static final String FLAVOR = "workflow/properties";

  private Path tmpPropsFile;

  @Before
  public void setUp() throws Exception {
    tmpPropsFile = Files.createTempFile("workflow-properties", ".xml");
  }

  @After
  public void tearDown() throws Exception {
    Files.deleteIfExists(tmpPropsFile);
  }

  @Test
  public void testStartOp() throws Exception {

    final WorkflowOperationInstance woi = createMock(WorkflowOperationInstance.class);
    expect(woi.getConfiguration("source-flavor")).andStubReturn(FLAVOR);
    expect(woi.getConfiguration("keys")).andStubReturn("chapter, presenter_position, cover_marker_in_s");
    replay(woi);

    final Attachment att = new AttachmentImpl();
    att.setURI(new URI(WF_PROPS_ATT_URI));
    att.setFlavor(MediaPackageElementFlavor.parseFlavor(FLAVOR));

    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.add(att);

    WorkflowInstance wi = createMock(WorkflowInstance.class);
    expect(wi.getCurrentOperation()).andStubReturn(woi);
    expect(wi.getMediaPackage()).andStubReturn(mp);

    wi.setConfiguration(eq("chapter"), eq("true"));
    expectLastCall().once();
    wi.setConfiguration(eq("presenter_position"), eq("left"));
    expectLastCall().once();
    wi.setConfiguration(eq("cover_marker_in_s"), eq("30.674"));
    expectLastCall().once();
    replay(wi);

    try (InputStream is = ImportWorkflowPropertiesWOHTest.class.getResourceAsStream("/workflow-properties.xml")) {
      Files.copy(is, tmpPropsFile, StandardCopyOption.REPLACE_EXISTING);
    }

    final Workspace workspace = createNiceMock(Workspace.class);
    expect(workspace.get(new URI(WF_PROPS_ATT_URI))).andStubReturn(tmpPropsFile.toFile());
    replay(workspace);

    final ImportWorkflowPropertiesWOH woh = new ImportWorkflowPropertiesWOH();
    woh.setWorkspace(workspace);
    woh.start(wi, null);

    verify(wi);
  }


}
