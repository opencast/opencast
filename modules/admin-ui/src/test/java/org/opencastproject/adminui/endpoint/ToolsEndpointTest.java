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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.adminui.endpoint.ToolsEndpoint.EditingInfo;
import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilBody;
import org.opencastproject.smil.entity.api.SmilHead;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Test class for {@link ToolsEndpoint} */
public class ToolsEndpointTest {

  private static ToolsEndpoint endpoint;
  private static Smil smil;

  @BeforeClass
  public static void setUpClass() throws Exception {

    /* Start of Smil mockups */
    // Ugly, but strictly the smil APIs

    String trackSrc = "http://mh-allinone.localdomain/archive/archive/mediapackage/0f2a2ada-0584-4d4d-a248-111f654aa217/6ec443e7-b097-4470-a618-5e0d848f5252/0/track.mp4";
    URL smilUrl = ToolsEndpoint.class.getResource("/tools/smil1.xml");
    String smilString = IOUtils.toString(smilUrl);

    String trackParamGroupId = "pg-a6d8e576-495f-44c7-8ed7-b5b47c807f0f";

    SmilMediaParam param1 = EasyMock.createNiceMock(SmilMediaParam.class);
    EasyMock.expect(param1.getName()).andReturn("track-id").anyTimes();
    EasyMock.expect(param1.getValue()).andReturn("track-1").anyTimes();
    EasyMock.expect(param1.getId()).andReturn("param-e2f41e7d-caba-401b-a03a-e524296cb235").anyTimes();
    SmilMediaParam param2 = EasyMock.createNiceMock(SmilMediaParam.class);
    EasyMock.expect(param2.getName()).andReturn("track-src").anyTimes();
    EasyMock.expect(param2.getValue()).andReturn(trackSrc).anyTimes();
    EasyMock.expect(param2.getId()).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes();
    SmilMediaParam param3 = EasyMock.createNiceMock(SmilMediaParam.class);
    EasyMock.expect(param3.getName()).andReturn("track-flavor").anyTimes();
    EasyMock.expect(param3.getValue()).andReturn("presenter/work").anyTimes();
    EasyMock.expect(param3.getId()).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes();
    EasyMock.replay(param1, param2, param3);

    List<SmilMediaParam> params = new ArrayList<>();
    params.add(param1);
    params.add(param2);
    params.add(param3);

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(trackParamGroupId).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = EasyMock.createNiceMock(SmilHead.class);
    EasyMock.expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    EasyMock.replay(head);

    SmilMediaElement object1 = EasyMock.createNiceMock(SmilMediaElement.class);
    EasyMock.expect(object1.isContainer()).andReturn(false).anyTimes();
    EasyMock.expect(object1.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    EasyMock.expect(object1.getClipBeginMS()).andReturn(0L).anyTimes();
    EasyMock.expect(object1.getClipEndMS()).andReturn(2449L).anyTimes();
    EasyMock.expect(object1.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    EasyMock.replay(object1);

    SmilMediaElement object2 = EasyMock.createNiceMock(SmilMediaElement.class);
    EasyMock.expect(object2.isContainer()).andReturn(false).anyTimes();
    EasyMock.expect(object2.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    EasyMock.expect(object2.getClipBeginMS()).andReturn(4922L).anyTimes();
    EasyMock.expect(object2.getClipEndMS()).andReturn(11284L).anyTimes();
    EasyMock.expect(object2.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    EasyMock.replay(object2);

    SmilMediaElement object3 = EasyMock.createNiceMock(SmilMediaElement.class);
    EasyMock.expect(object3.isContainer()).andReturn(false).anyTimes();
    EasyMock.expect(object3.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    EasyMock.expect(object3.getClipBeginMS()).andReturn(14721L).anyTimes();
    EasyMock.expect(object3.getClipEndMS()).andReturn(15963L).anyTimes();
    EasyMock.expect(object3.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    EasyMock.replay(object3);

    SmilMediaElement object4 = EasyMock.createNiceMock(SmilMediaElement.class);
    EasyMock.expect(object4.isContainer()).andReturn(false).anyTimes();
    EasyMock.expect(object4.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    EasyMock.expect(object4.getClipBeginMS()).andReturn(15963L).anyTimes();
    EasyMock.expect(object4.getClipEndMS()).andReturn(20132L).anyTimes();
    EasyMock.expect(object4.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    EasyMock.replay(object4);

    List<SmilMediaObject> objects1 = new ArrayList<>();
    objects1.add(object1);
    List<SmilMediaObject> objects2 = new ArrayList<>();
    objects2.add(object2);
    List<SmilMediaObject> objects3 = new ArrayList<>();
    objects3.add(object3);
    List<SmilMediaObject> objects4 = new ArrayList<>();
    objects4.add(object4);

    SmilMediaContainer objectContainer1 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer1.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer1.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer1.getElements()).andReturn(objects1).anyTimes();
    EasyMock.replay(objectContainer1);

    SmilMediaContainer objectContainer2 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer2.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer2.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer2.getElements()).andReturn(objects2).anyTimes();
    EasyMock.replay(objectContainer2);

    SmilMediaContainer objectContainer3 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer3.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer3.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer3.getElements()).andReturn(objects3).anyTimes();
    EasyMock.replay(objectContainer3);

    SmilMediaContainer objectContainer4 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer4.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer4.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer4.getElements()).andReturn(objects4).anyTimes();
    EasyMock.replay(objectContainer4);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer1);
    containerObjects.add(objectContainer2);
    containerObjects.add(objectContainer3);
    containerObjects.add(objectContainer4);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(trackParamGroupId)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(smilString).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-ec404c2a-5092-4cd4-8717-7b7bbc244656").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.fromXml((String) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(smilService);
    /* End of Smil API mockups */

    endpoint = new ToolsEndpoint();
    endpoint.setSmilService(smilService);

    AdminUIConfiguration adminUIConfiguration = new AdminUIConfiguration();
    Hashtable<String, String> dictionary = new Hashtable<>();
    dictionary.put(AdminUIConfiguration.OPT_PREVIEW_SUBTYPE, "preview");
    dictionary.put(AdminUIConfiguration.OPT_WAVEFORM_SUBTYPE, "waveform");
    dictionary.put(AdminUIConfiguration.OPT_SMIL_CATALOG_FLAVOR, "smil/cutting");
    dictionary.put(AdminUIConfiguration.OPT_SMIL_SILENCE_FLAVOR, "*/silence");
    adminUIConfiguration.updated(dictionary);
    endpoint.setAdminUIConfiguration(adminUIConfiguration);

  }

  /** Test method for {@link ToolsEndpoint#getSegmentsFromSmil(Smil)} */
  @Test
  public void testGetSegmentsFromSmil() throws Exception {

    List<Tuple<Long, Long>> segments = endpoint.getSegmentsFromSmil(smil);
    assertEquals(4, segments.size());
    assertTrue(segments.contains(Tuple.tuple(0L, 2449L)));
    assertTrue(segments.contains(Tuple.tuple(4922L, 11284L)));
    assertTrue(segments.contains(Tuple.tuple(14721L, 15963L)));
    assertTrue(segments.contains(Tuple.tuple(15963L, 20132L)));
  }

  /** Test method for {@link ToolsEndpoint#mergeSegments(List, List))} */
  @Test
  public void testMergeSegments() throws Exception {
    List<Tuple<Long, Long>> segments = new ArrayList<>();
    segments.add(tuple(0L, 2449L));
    segments.add(tuple(4922L, 11284L));
    segments.add(tuple(14000L, 15000L));
    List<Tuple<Long, Long>> segments2 = new ArrayList<>();
    segments2.add(tuple(1449L, 3449L));
    segments2.add(tuple(11285L, 11290L));
    segments2.add(tuple(15000L, 16000L));
    List<Tuple<Long, Long>> mergedSegments = endpoint.mergeSegments(segments, segments2);
    assertEquals(5, mergedSegments.size());
  }

  /** Test method for {@link ToolsEndpoint.EditingInfo#parse(JSONObject)} */
  @Test
  public void testEditingInfoParse() throws Exception {
    JSONParser parser = new JSONParser();
    final EditingInfo editingInfo = ToolsEndpoint.EditingInfo.parse(
            (JSONObject) parser.parse(IOUtils.toString(getClass().getResourceAsStream("/tools/POST-editor.json"))));

    final List<Tuple<Long, Long>> segments = editingInfo.getConcatSegments();
    assertEquals(4, segments.size());
    assertTrue(segments.contains(Tuple.tuple(0L, 2449L)));
    assertTrue(segments.contains(Tuple.tuple(4922L, 11284L)));
    assertTrue(segments.contains(Tuple.tuple(14721L, 15963L)));
    assertTrue(segments.contains(Tuple.tuple(15963L, 20132L)));

    final List<String> tracks = editingInfo.getConcatTracks();
    assertEquals(1, tracks.size());

    assertEquals(Optional.of("cut-workflow"), editingInfo.getPostProcessingWorkflow());
  }

  /** Test method for {@link ToolsEndpoint#addSmilToArchive(org.opencastproject.mediapackage.MediaPackage, Smil)} */
  @Test
  public void testAddSmilToArchive() throws Exception {
    final String mpId = UUID.randomUUID().toString();
    final URI archiveElementURI = new URI("http://host.tld/archive/cut.smil");
    final String smilId = "s-afe311c6-9161-41f4-98d0-e951fe66d89e";

    Workspace workspace = createNiceMock(Workspace.class);
    expect(workspace.put(same(mpId), same(smilId), same("cut.smil"), anyObject(InputStream.class)))
            .andReturn(archiveElementURI);
    replay(workspace);
    endpoint.setWorkspace(workspace);

    AssetManager assetManager = createNiceMock(AssetManager.class);
    replay(assetManager);
    endpoint.setAssetManager(assetManager);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl(mpId));

    endpoint.addSmilToArchive(mp, smil);

    assertEquals(1, mp.getCatalogs().length);
    assertEquals(smil.getId(), mp.getCatalogs()[0].getIdentifier());
    assertEquals("smil/cutting", mp.getCatalogs()[0].getFlavor().toString());
  }

}
