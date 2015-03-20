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
package org.opencastproject.workflow.handler.coverimage;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;

/**
 * Unit tests for class {@link CoverImageWorkflowOperationHandler}
 */
@RunWith(EasyMockRunner.class)
public class CoverImageWorkflowOperationHandlerTest {

  @TestSubject
  private CoverImageWorkflowOperationHandler woh = new CoverImageWorkflowOperationHandler();

  @Mock
  private Workspace workspace;

  @Test
  public void testAppendXml() {
    StringBuilder xml = new StringBuilder();
    woh.appendXml(xml, "elem-name", "This is the <body> of the element");
    assertEquals("<elem-name>This is the &lt;body&gt; of the element</elem-name>", xml.toString());
  }

  @Test
  public void testGetPosterImageFileUriByString() throws Exception {
    String fileUrlString = "file:/path/to/the/image.png";
    String httpUrlString = "http://foo.bar/image.png";
    String fileHttpUrlString = "file:/path/to/storage/foo_bar_image.png";
    URL httpUri = new URL(httpUrlString);
    File httpFile = new File(fileHttpUrlString);

    // setup mock
    expect(workspace.get(httpUri.toURI())).andStubReturn(httpFile);
    replay(workspace);

    assertNull(woh.getPosterImageFileUrl(null));
    assertNull(woh.getPosterImageFileUrl(" "));
    assertNull(woh.getPosterImageFileUrl("{$posterImageUrl}"));
    assertEquals(fileUrlString, woh.getPosterImageFileUrl(fileUrlString));
    assertEquals(fileHttpUrlString, woh.getPosterImageFileUrl(httpUrlString));
  }

}
