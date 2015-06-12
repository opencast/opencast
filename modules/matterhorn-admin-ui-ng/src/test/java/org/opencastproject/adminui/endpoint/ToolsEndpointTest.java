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

package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.data.Opt.some;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.adminui.endpoint.ToolsEndpoint.EditingInfo;
import org.opencastproject.archive.api.Archive;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.smil.entity.SmilImpl;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.impl.SmilServiceImpl;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Test class for {@link ToolsEndpoint} */
public class ToolsEndpointTest {

  private static ToolsEndpoint endpoint;

  @BeforeClass
  public static void setUpClass() {
    endpoint = new ToolsEndpoint();
    endpoint.setSmilService(new SmilServiceImpl());
  }

  /** Test method for {@link ToolsEndpoint#getSegmentsFromSmil(Smil)} */
  @Test
  public void testGetSegmentsFromSmil() throws Exception {
    Smil smil = SmilImpl.fromXML(new File(ToolsEndpointTest.class.getResource("/tools/smil1.xml").toURI()));

    List<Tuple<Long, Long>> segments = endpoint.getSegmentsFromSmil(smil);
    assertEquals(4, segments.size());
    assertTrue(segments.contains(Tuple.tuple(0L, 2449L)));
    assertTrue(segments.contains(Tuple.tuple(4922L, 11284L)));
    assertTrue(segments.contains(Tuple.tuple(14721L, 15963L)));
    assertTrue(segments.contains(Tuple.tuple(15963L, 20132L)));
  }

  /** Test method for {@link ToolsEndpoint.EditingInfo#parse(JSONObject)} */
  @Test
  public void testEditingInfoParse() throws Exception {
    JSONParser parser = new JSONParser();
    final EditingInfo editingInfo = ToolsEndpoint.EditingInfo.parse((JSONObject) parser.parse(IOUtils
            .toString(getClass().getResourceAsStream("/tools/POST-editor.json"))));

    final List<Tuple<Long, Long>> segments = editingInfo.getConcatSegments();
    assertEquals(4, segments.size());
    assertTrue(segments.contains(Tuple.tuple(0L, 2449L)));
    assertTrue(segments.contains(Tuple.tuple(4922L, 11284L)));
    assertTrue(segments.contains(Tuple.tuple(14721L, 15963L)));
    assertTrue(segments.contains(Tuple.tuple(15963L, 20132L)));

    final List<String> tracks = editingInfo.getConcatTracks();
    assertEquals(1, tracks.size());

    assertEquals(some("cut-workflow"), editingInfo.getPostProcessingWorkflow());
  }

  /** Test method for {@link ToolsEndpoint#addSmilToArchive(org.opencastproject.mediapackage.MediaPackage, Smil)} */
  @Test
  public void testAddSmilToArchive() throws Exception {
    final String mpId = UUID.randomUUID().toString();
    final URI archiveElementURI = new URI("http://host.tld/archive/cut.smil");
    final String smilId = "s-afe311c6-9161-41f4-98d0-e951fe66d89e";

    Workspace workspace = createNiceMock(Workspace.class);
    expect(workspace.put(same(mpId), same(smilId), same("cut.smil"), anyObject(InputStream.class))).andReturn(
            archiveElementURI);
    replay(workspace);
    endpoint.setWorkspace(workspace);

    Archive<?> archive = createNiceMock(Archive.class);
    replay(archive);
    endpoint.setArchive(archive);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl(mpId));
    Smil smil = SmilImpl.fromXML(new File(ToolsEndpointTest.class.getResource("/tools/smil1.xml").toURI()));

    endpoint.addSmilToArchive(mp, smil);

    assertEquals(1, mp.getCatalogs().length);
    assertEquals(smilId, mp.getCatalogs()[0].getIdentifier());
    assertEquals(ToolsEndpoint.SMIL_CATALOG_FLAVOR, mp.getCatalogs()[0].getFlavor());
  }

}
