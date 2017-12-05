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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ExportWorkflowPropertiesWOHTest {

  public static final String FLAVOR = "processing/defaults";

  private Workspace workspace;
  private URI uri;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    workspace = EasyMock.createMock(Workspace.class);
    final Capture<InputStream> in = EasyMock.newCapture();
    final Capture<URI> uriCapture = EasyMock.newCapture();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.capture(in))).andAnswer(() -> {
              final File file = temporaryFolder.newFile();
              FileUtils.copyInputStreamToFile(in.getValue(), file);
              return file.toURI();
            }).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.capture(uriCapture))).andAnswer(() -> new File(uriCapture.getValue())).anyTimes();
    EasyMock.replay(workspace);
    uri = ExportWorkflowPropertiesWOHTest.class.getResource("/workflow-properties.xml").toURI();
  }

  @Test
  public void testExport() throws Exception {
    final WorkflowOperationInstance woi = createMock(WorkflowOperationInstance.class);
    expect(woi.getConfiguration("target-flavor")).andStubReturn(FLAVOR);
    expect(woi.getConfiguration("target-tags")).andStubReturn("archive");
    expect(woi.getConfiguration("keys")).andStubReturn("chapter,presenter_position");
    replay(woi);

    final Attachment att = new AttachmentImpl();
    att.setURI(uri);
    att.setFlavor(MediaPackageElementFlavor.parseFlavor(FLAVOR));

    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.add(att);

    WorkflowInstance wi = createMock(WorkflowInstance.class);
    expect(wi.getCurrentOperation()).andStubReturn(woi);
    expect(wi.getMediaPackage()).andStubReturn(mp);
    Set<String> keys = new HashSet<>();
    keys.add("presenter_position");
    keys.add("cover_marker_in_s");
    expect(wi.getConfigurationKeys()).andStubReturn(keys);
    expect(wi.getConfiguration("presenter_position")).andStubReturn("right");
    expect(wi.getConfiguration("cover_marker_in_s")).andStubReturn("30.674");

    replay(wi);

    final ExportWorkflowPropertiesWOH woh = new ExportWorkflowPropertiesWOH();
    woh.setWorkspace(workspace);
    WorkflowOperationResult result = woh.start(wi, null);
    Attachment[] attachments = result.getMediaPackage().getAttachments();
    Assert.assertTrue(attachments.length == 1);
    Attachment attachment = attachments[0];
    assertEquals("processing/defaults", attachment.getFlavor().toString());
    assertEquals("archive", attachment.getTags()[0]);
    Assert.assertNotNull(attachment.getURI());

    File file = workspace.get(attachment.getURI());
    Properties props = new Properties();
    try (InputStream is = new FileInputStream(file)) {
      props.loadFromXML(is);
    }

    assertEquals("30.674", props.get("cover_marker_in_s"));
    assertEquals("right", props.get("presenter_position"));
    Assert.assertFalse(props.contains("chapter"));

    verify(wi);
  }

}
