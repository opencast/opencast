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
package org.opencastproject.editor;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.editor.api.SegmentData;
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
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

/** Test class for {@link EditorServiceImpl} */
public class EditorServiceImplTest {
  private static EditorServiceImpl editorService;
  private static Smil smil;

  @BeforeClass
  public static void setUpClass() throws Exception {

    /* Start of Smil mockups */
    // Ugly, but strictly the smil APIs

    String trackSrc = "http://mh-allinone.localdomain/archive/archive/mediapackage/"
            + "0f2a2ada-0584-4d4d-a248-111f654aa217/6ec443e7-b097-4470-a618-5e0d848f5252/0/track.mp4";
    URI smilUrl = EditorServiceImplTest.class.getResource("/smil1.xml").toURI();
    String smilString = IOUtils.toString(smilUrl);

    String trackParamGroupId = "pg-a6d8e576-495f-44c7-8ed7-b5b47c807f0f";

    SmilMediaParam param1 = createNiceMock(SmilMediaParam.class);
    expect(param1.getName()).andReturn("track-id").anyTimes();
    expect(param1.getValue()).andReturn("track-1").anyTimes();
    expect(param1.getId()).andReturn("param-e2f41e7d-caba-401b-a03a-e524296cb235").anyTimes();
    SmilMediaParam param2 = createNiceMock(SmilMediaParam.class);
    expect(param2.getName()).andReturn("track-src").anyTimes();
    expect(param2.getValue()).andReturn(trackSrc).anyTimes();
    expect(param2.getId()).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes();
    SmilMediaParam param3 = createNiceMock(SmilMediaParam.class);
    expect(param3.getName()).andReturn("track-flavor").anyTimes();
    expect(param3.getValue()).andReturn("presenter/work").anyTimes();
    expect(param3.getId()).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes();
    replay(param1, param2, param3);

    List<SmilMediaParam> params = new ArrayList<>();
    params.add(param1);
    params.add(param2);
    params.add(param3);

    SmilMediaParamGroup group1 = createNiceMock(SmilMediaParamGroup.class);
    expect(group1.getParams()).andReturn(params).anyTimes();
    expect(group1.getId()).andReturn(trackParamGroupId).anyTimes();
    replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<>();
    paramGroups.add(group1);

    SmilHead head = createNiceMock(SmilHead.class);
    expect(head.getParamGroups()).andReturn(paramGroups).anyTimes();
    replay(head);

    SmilMediaElement object1 = createNiceMock(SmilMediaElement.class);
    expect(object1.isContainer()).andReturn(false).anyTimes();
    expect(object1.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    expect(object1.getClipBeginMS()).andReturn(0L).anyTimes();
    expect(object1.getClipEndMS()).andReturn(2449L).anyTimes();
    expect(object1.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    replay(object1);

    SmilMediaElement object2 = createNiceMock(SmilMediaElement.class);
    expect(object2.isContainer()).andReturn(false).anyTimes();
    expect(object2.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    expect(object2.getClipBeginMS()).andReturn(4922L).anyTimes();
    expect(object2.getClipEndMS()).andReturn(11284L).anyTimes();
    expect(object2.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    replay(object2);

    SmilMediaElement object3 = createNiceMock(SmilMediaElement.class);
    expect(object3.isContainer()).andReturn(false).anyTimes();
    expect(object3.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    expect(object3.getClipBeginMS()).andReturn(14721L).anyTimes();
    expect(object3.getClipEndMS()).andReturn(15963L).anyTimes();
    expect(object3.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    replay(object3);

    SmilMediaElement object4 = createNiceMock(SmilMediaElement.class);
    expect(object4.isContainer()).andReturn(false).anyTimes();
    expect(object4.getParamGroup()).andReturn(trackParamGroupId).anyTimes();
    expect(object4.getClipBeginMS()).andReturn(15963L).anyTimes();
    expect(object4.getClipEndMS()).andReturn(20132L).anyTimes();
    expect(object4.getSrc()).andReturn(new URI(trackSrc)).anyTimes();
    replay(object4);

    List<SmilMediaObject> objects1 = new ArrayList<>();
    objects1.add(object1);
    List<SmilMediaObject> objects2 = new ArrayList<>();
    objects2.add(object2);
    List<SmilMediaObject> objects3 = new ArrayList<>();
    objects3.add(object3);
    List<SmilMediaObject> objects4 = new ArrayList<>();
    objects4.add(object4);

    SmilMediaContainer objectContainer1 = createNiceMock(SmilMediaContainer.class);
    expect(objectContainer1.isContainer()).andReturn(true).anyTimes();
    expect(objectContainer1.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    expect(objectContainer1.getElements()).andReturn(objects1).anyTimes();
    replay(objectContainer1);

    SmilMediaContainer objectContainer2 = createNiceMock(SmilMediaContainer.class);
    expect(objectContainer2.isContainer()).andReturn(true).anyTimes();
    expect(objectContainer2.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    expect(objectContainer2.getElements()).andReturn(objects2).anyTimes();
    replay(objectContainer2);

    SmilMediaContainer objectContainer3 = createNiceMock(SmilMediaContainer.class);
    expect(objectContainer3.isContainer()).andReturn(true).anyTimes();
    expect(objectContainer3.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    expect(objectContainer3.getElements()).andReturn(objects3).anyTimes();
    replay(objectContainer3);

    SmilMediaContainer objectContainer4 = createNiceMock(SmilMediaContainer.class);
    expect(objectContainer4.isContainer()).andReturn(true).anyTimes();
    expect(objectContainer4.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    expect(objectContainer4.getElements()).andReturn(objects4).anyTimes();
    replay(objectContainer4);

    List<SmilMediaObject> containerObjects = new ArrayList<>();
    containerObjects.add(objectContainer1);
    containerObjects.add(objectContainer2);
    containerObjects.add(objectContainer3);
    containerObjects.add(objectContainer4);

    SmilBody body = createNiceMock(SmilBody.class);
    expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    replay(body);

    smil = createNiceMock(Smil.class);
    expect(smil.get(trackParamGroupId)).andReturn(group1).anyTimes();
    expect(smil.getBody()).andReturn(body).anyTimes();
    expect(smil.getHead()).andReturn(head).anyTimes();
    expect(smil.toXML()).andReturn(smilString).anyTimes();
    expect(smil.getId()).andReturn("s-ec404c2a-5092-4cd4-8717-7b7bbc244656").anyTimes();
    replay(smil);

    SmilResponse response = createNiceMock(SmilResponse.class);
    expect(response.getSmil()).andReturn(smil).anyTimes();
    replay(response);

    SmilService smilService = createNiceMock(SmilService.class);
    expect(smilService.fromXml((String) anyObject())).andReturn(response).anyTimes();
    replay(smilService);
    /* End of Smil API mockups */

    editorService = new EditorServiceImpl();
    editorService.setSmilService(smilService);

    Hashtable<String, Object> dictionary = new Hashtable<>();
    dictionary.put(EditorServiceImpl.OPT_PREVIEW_SUBTYPE, "preview");
    dictionary.put(EditorServiceImpl.OPT_SMIL_CATALOG_FLAVOR, "smil/cutting");
    dictionary.put(EditorServiceImpl.OPT_SMIL_SILENCE_FLAVOR, "*/silence");

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    expect(cc.getProperties()).andReturn(dictionary).anyTimes();
    replay(cc);

    editorService.activate(cc);
  }

  /** Test method for {@link EditorServiceImpl#getSegmentsFromSmil(Smil)} */
  @Test
  public void testGetSegmentsFromSmil() throws Exception {

    List<SegmentData> segments = editorService.getSegmentsFromSmil(smil);
    assertEquals(4, segments.size());
    assertTrue(segments.contains(new SegmentData(0L, 2449L)));
    assertTrue(segments.contains(new SegmentData(4922L, 11284L)));
    assertTrue(segments.contains(new SegmentData(14721L, 15963L)));
    assertTrue(segments.contains(new SegmentData(15963L, 20132L)));
  }

  /** Test method for {@link EditorServiceImpl#mergeSegments(List, List))} */
  @Test
  public void testMergeSegments() throws Exception {
    List<SegmentData> segments = new ArrayList<>();
    segments.add(new SegmentData(0L, 2449L));
    segments.add(new SegmentData(4922L, 11284L));
    segments.add(new SegmentData(14000L, 15000L));
    List<SegmentData> segments2 = new ArrayList<>();
    segments2.add(new SegmentData(1449L, 3449L));
    segments2.add(new SegmentData(11285L, 11290L));
    segments2.add(new SegmentData(15000L, 16000L));
    List<SegmentData> mergedSegments = editorService.mergeSegments(segments, segments2);
    assertEquals(5, mergedSegments.size());
  }

  @Test
  public void testDeletedSegments() throws Exception {
    List<SegmentData> segments = new ArrayList<>();
    segments.add(new SegmentData(0L, 2449L));
    segments.add(new SegmentData(4922L, 11284L));

    MediaPackage mediaPackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.expect(mediaPackage.getDuration()).andReturn(14000L).anyTimes();
    EasyMock.replay(mediaPackage);

    List<SegmentData> deleted = editorService.getDeletedSegments(mediaPackage, segments);
    assertNotNull(deleted);
    assertEquals(2, deleted.size());
    assertTrue(deleted.contains(new SegmentData(2449L, 4922L, true)));
    assertTrue(deleted.contains(new SegmentData(11284L, 14000L, true)));
  }

  @Test
  public void testAddSmilToArchive() throws Exception {
    final String mpId = UUID.randomUUID().toString();
    final URI archiveElementURI = new URI("http://host.tld/archive/cut.smil");
    final String smilId = "s-afe311c6-9161-41f4-98d0-e951fe66d89e";

    Workspace workspace = createNiceMock(Workspace.class);
    expect(workspace.put(same(mpId), same(smilId), same("cut.smil"), anyObject(InputStream.class)))
            .andReturn(archiveElementURI);
    replay(workspace);
    editorService.setWorkspace(workspace);

    AssetManager assetManager = createNiceMock(AssetManager.class);
    replay(assetManager);
    editorService.setAssetManager(assetManager);

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl(mpId));

    editorService.addSmilToArchive(mp, smil);

    assertEquals(1, mp.getCatalogs().length);
    assertEquals(smil.getId(), mp.getCatalogs()[0].getIdentifier());
    assertEquals("smil/cutting", mp.getCatalogs()[0].getFlavor().toString());
  }
}
