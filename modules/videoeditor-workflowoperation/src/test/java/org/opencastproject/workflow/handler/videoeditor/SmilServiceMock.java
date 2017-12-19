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

package org.opencastproject.workflow.handler.videoeditor;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilException;
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

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

public final class SmilServiceMock {

  private SmilServiceMock() {
  }

  public static SmilService createSmilServiceMock(URI mpSmilURI)
          throws IOException, SmilException, URISyntaxException, JAXBException, SAXException {
    /* Start of Smil mockups */
    String smilString = IOUtils.toString(mpSmilURI);
    String trackSrc = "abc";

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

    List<SmilMediaParam> params = new ArrayList<SmilMediaParam>();
    params.add(param1);
    params.add(param2);
    params.add(param3);

    SmilMediaParamGroup group1 = EasyMock.createNiceMock(SmilMediaParamGroup.class);
    EasyMock.expect(group1.getParams()).andReturn(params).anyTimes();
    EasyMock.expect(group1.getId()).andReturn(trackParamGroupId).anyTimes();
    EasyMock.replay(group1);

    List<SmilMediaParamGroup> paramGroups = new ArrayList<SmilMediaParamGroup>();
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

    List<SmilMediaObject> objects1 = new ArrayList<SmilMediaObject>();
    objects1.add(object1);
    List<SmilMediaObject> objects2 = new ArrayList<SmilMediaObject>();
    objects2.add(object2);
    List<SmilMediaObject> objects3 = new ArrayList<SmilMediaObject>();
    objects3.add(object3);
    List<SmilMediaObject> objects4 = new ArrayList<SmilMediaObject>();
    objects4.add(object4);

    SmilMediaContainer objectContainer1 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer1.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer1.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer1.getElements()).andReturn(objects1).anyTimes();
    EasyMock.expect(objectContainer1.getId()).andReturn("container1").anyTimes();
    EasyMock.replay(objectContainer1);

    SmilMediaContainer objectContainer2 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer2.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer2.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer2.getElements()).andReturn(objects2).anyTimes();
    EasyMock.expect(objectContainer2.getId()).andReturn("container2").anyTimes();
    EasyMock.replay(objectContainer2);

    SmilMediaContainer objectContainer3 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer3.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer3.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer3.getElements()).andReturn(objects3).anyTimes();
    EasyMock.expect(objectContainer3.getId()).andReturn("container3").anyTimes();
    EasyMock.replay(objectContainer3);

    SmilMediaContainer objectContainer4 = EasyMock.createNiceMock(SmilMediaContainer.class);
    EasyMock.expect(objectContainer4.isContainer()).andReturn(true).anyTimes();
    EasyMock.expect(objectContainer4.getContainerType()).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes();
    EasyMock.expect(objectContainer4.getElements()).andReturn(objects4).anyTimes();
    EasyMock.expect(objectContainer4.getId()).andReturn("container4").anyTimes();
    EasyMock.replay(objectContainer4);

    List<SmilMediaObject> containerObjects = new ArrayList<SmilMediaObject>();
    containerObjects.add(objectContainer1);
    containerObjects.add(objectContainer2);
    containerObjects.add(objectContainer3);
    containerObjects.add(objectContainer4);

    SmilBody body = EasyMock.createNiceMock(SmilBody.class);
    EasyMock.expect(body.getMediaElements()).andReturn(containerObjects).anyTimes();
    EasyMock.replay(body);

    Smil smil = EasyMock.createNiceMock(Smil.class);
    EasyMock.expect(smil.get(trackParamGroupId)).andReturn(group1).anyTimes();
    EasyMock.expect(smil.getBody()).andReturn(body).anyTimes();
    EasyMock.expect(smil.getHead()).andReturn(head).anyTimes();
    EasyMock.expect(smil.toXML()).andReturn(smilString).anyTimes();
    EasyMock.expect(smil.getId()).andReturn("s-ec404c2a-5092-4cd4-8717-7b7bbc244656").anyTimes();
    EasyMock.replay(smil);

    SmilResponse response = EasyMock.createNiceMock(SmilResponse.class);
    EasyMock.expect(response.getSmil()).andReturn(smil).anyTimes();
    EasyMock.expect(response.getEntity()).andReturn(object4).anyTimes();
    EasyMock.replay(response);

    SmilService smilService = EasyMock.createNiceMock(SmilService.class);
    EasyMock.expect(smilService.createNewSmil((MediaPackage) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.expect(smilService.fromXml(EasyMock.anyString())).andReturn(response).anyTimes();
    EasyMock.expect(smilService.fromXml((File) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.expect(smilService.removeSmilElement((Smil) EasyMock.anyObject(), EasyMock.anyString()))
            .andReturn(response).anyTimes();
    EasyMock.expect(smilService.addParallel(smil)).andReturn(response).anyTimes();
    EasyMock.expect(smilService.addClips((Smil) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (Track[]) EasyMock.anyObject(), EasyMock.anyLong(), EasyMock.anyLong())).andReturn(response).anyTimes();

    EasyMock.replay(smilService);
    return smilService;

  }

}
