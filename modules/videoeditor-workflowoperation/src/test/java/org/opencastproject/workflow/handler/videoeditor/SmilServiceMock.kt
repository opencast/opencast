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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.videoeditor

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.Track
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilResponse
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.smil.entity.api.SmilBody
import org.opencastproject.smil.entity.api.SmilHead
import org.opencastproject.smil.entity.media.api.SmilMediaObject
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup

import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.xml.sax.SAXException

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList

import javax.xml.bind.JAXBException

object SmilServiceMock {

    @Throws(IOException::class, SmilException::class, URISyntaxException::class, JAXBException::class, SAXException::class)
    fun createSmilServiceMock(mpSmilURI: URI): SmilService {
        /* Start of Smil mockups */
        val smilString = IOUtils.toString(mpSmilURI)
        val trackSrc = "abc"

        val trackParamGroupId = "pg-a6d8e576-495f-44c7-8ed7-b5b47c807f0f"

        val param1 = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
        EasyMock.expect(param1.name).andReturn("track-id").anyTimes()
        EasyMock.expect(param1.value).andReturn("track-1").anyTimes()
        EasyMock.expect(param1.id).andReturn("param-e2f41e7d-caba-401b-a03a-e524296cb235").anyTimes()
        val param2 = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
        EasyMock.expect(param2.name).andReturn("track-src").anyTimes()
        EasyMock.expect(param2.value).andReturn(trackSrc).anyTimes()
        EasyMock.expect(param2.id).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes()
        val param3 = EasyMock.createNiceMock<SmilMediaParam>(SmilMediaParam::class.java)
        EasyMock.expect(param3.name).andReturn("track-flavor").anyTimes()
        EasyMock.expect(param3.value).andReturn("presenter/work").anyTimes()
        EasyMock.expect(param3.id).andReturn("param-1bd5e839-0a74-4310-b1d2-daba07914f79").anyTimes()
        EasyMock.replay(param1, param2, param3)

        val params = ArrayList<SmilMediaParam>()
        params.add(param1)
        params.add(param2)
        params.add(param3)

        val group1 = EasyMock.createNiceMock<SmilMediaParamGroup>(SmilMediaParamGroup::class.java)
        EasyMock.expect(group1.params).andReturn(params).anyTimes()
        EasyMock.expect(group1.id).andReturn(trackParamGroupId).anyTimes()
        EasyMock.replay(group1)

        val paramGroups = ArrayList<SmilMediaParamGroup>()
        paramGroups.add(group1)

        val head = EasyMock.createNiceMock<SmilHead>(SmilHead::class.java)
        EasyMock.expect(head.paramGroups).andReturn(paramGroups).anyTimes()
        EasyMock.replay(head)

        val object1 = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
        EasyMock.expect(object1.isContainer).andReturn(false).anyTimes()
        EasyMock.expect(object1.paramGroup).andReturn(trackParamGroupId).anyTimes()
        EasyMock.expect(object1.clipBeginMS).andReturn(0L).anyTimes()
        EasyMock.expect(object1.clipEndMS).andReturn(2449L).anyTimes()
        EasyMock.expect(object1.src).andReturn(URI(trackSrc)).anyTimes()
        EasyMock.replay(object1)

        val object2 = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
        EasyMock.expect(object2.isContainer).andReturn(false).anyTimes()
        EasyMock.expect(object2.paramGroup).andReturn(trackParamGroupId).anyTimes()
        EasyMock.expect(object2.clipBeginMS).andReturn(4922L).anyTimes()
        EasyMock.expect(object2.clipEndMS).andReturn(11284L).anyTimes()
        EasyMock.expect(object2.src).andReturn(URI(trackSrc)).anyTimes()
        EasyMock.replay(object2)

        val object3 = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
        EasyMock.expect(object3.isContainer).andReturn(false).anyTimes()
        EasyMock.expect(object3.paramGroup).andReturn(trackParamGroupId).anyTimes()
        EasyMock.expect(object3.clipBeginMS).andReturn(14721L).anyTimes()
        EasyMock.expect(object3.clipEndMS).andReturn(15963L).anyTimes()
        EasyMock.expect(object3.src).andReturn(URI(trackSrc)).anyTimes()
        EasyMock.replay(object3)

        val object4 = EasyMock.createNiceMock<SmilMediaElement>(SmilMediaElement::class.java)
        EasyMock.expect(object4.isContainer).andReturn(false).anyTimes()
        EasyMock.expect(object4.paramGroup).andReturn(trackParamGroupId).anyTimes()
        EasyMock.expect(object4.clipBeginMS).andReturn(15963L).anyTimes()
        EasyMock.expect(object4.clipEndMS).andReturn(20132L).anyTimes()
        EasyMock.expect(object4.src).andReturn(URI(trackSrc)).anyTimes()
        EasyMock.replay(object4)

        val objects1 = ArrayList<SmilMediaObject>()
        objects1.add(object1)
        val objects2 = ArrayList<SmilMediaObject>()
        objects2.add(object2)
        val objects3 = ArrayList<SmilMediaObject>()
        objects3.add(object3)
        val objects4 = ArrayList<SmilMediaObject>()
        objects4.add(object4)

        val objectContainer1 = EasyMock.createNiceMock<SmilMediaContainer>(SmilMediaContainer::class.java)
        EasyMock.expect(objectContainer1.isContainer).andReturn(true).anyTimes()
        EasyMock.expect<ContainerType>(objectContainer1.containerType).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes()
        EasyMock.expect(objectContainer1.elements).andReturn(objects1).anyTimes()
        EasyMock.expect(objectContainer1.id).andReturn("container1").anyTimes()
        EasyMock.replay(objectContainer1)

        val objectContainer2 = EasyMock.createNiceMock<SmilMediaContainer>(SmilMediaContainer::class.java)
        EasyMock.expect(objectContainer2.isContainer).andReturn(true).anyTimes()
        EasyMock.expect<ContainerType>(objectContainer2.containerType).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes()
        EasyMock.expect(objectContainer2.elements).andReturn(objects2).anyTimes()
        EasyMock.expect(objectContainer2.id).andReturn("container2").anyTimes()
        EasyMock.replay(objectContainer2)

        val objectContainer3 = EasyMock.createNiceMock<SmilMediaContainer>(SmilMediaContainer::class.java)
        EasyMock.expect(objectContainer3.isContainer).andReturn(true).anyTimes()
        EasyMock.expect<ContainerType>(objectContainer3.containerType).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes()
        EasyMock.expect(objectContainer3.elements).andReturn(objects3).anyTimes()
        EasyMock.expect(objectContainer3.id).andReturn("container3").anyTimes()
        EasyMock.replay(objectContainer3)

        val objectContainer4 = EasyMock.createNiceMock<SmilMediaContainer>(SmilMediaContainer::class.java)
        EasyMock.expect(objectContainer4.isContainer).andReturn(true).anyTimes()
        EasyMock.expect<ContainerType>(objectContainer4.containerType).andReturn(SmilMediaContainer.ContainerType.PAR).anyTimes()
        EasyMock.expect(objectContainer4.elements).andReturn(objects4).anyTimes()
        EasyMock.expect(objectContainer4.id).andReturn("container4").anyTimes()
        EasyMock.replay(objectContainer4)

        val containerObjects = ArrayList<SmilMediaObject>()
        containerObjects.add(objectContainer1)
        containerObjects.add(objectContainer2)
        containerObjects.add(objectContainer3)
        containerObjects.add(objectContainer4)

        val body = EasyMock.createNiceMock<SmilBody>(SmilBody::class.java)
        EasyMock.expect(body.mediaElements).andReturn(containerObjects).anyTimes()
        EasyMock.replay(body)

        val smil = EasyMock.createNiceMock<Smil>(Smil::class.java)
        EasyMock.expect<SmilObject>(smil[trackParamGroupId]).andReturn(group1).anyTimes()
        EasyMock.expect(smil.body).andReturn(body).anyTimes()
        EasyMock.expect(smil.head).andReturn(head).anyTimes()
        EasyMock.expect(smil.toXML()).andReturn(smilString).anyTimes()
        EasyMock.expect(smil.id).andReturn("s-ec404c2a-5092-4cd4-8717-7b7bbc244656").anyTimes()
        EasyMock.replay(smil)

        val response = EasyMock.createNiceMock<SmilResponse>(SmilResponse::class.java)
        EasyMock.expect(response.smil).andReturn(smil).anyTimes()
        EasyMock.expect<SmilObject>(response.entity).andReturn(object4).anyTimes()
        EasyMock.replay(response)

        val smilService = EasyMock.createNiceMock<SmilService>(SmilService::class.java)
        EasyMock.expect(smilService.createNewSmil(EasyMock.anyObject<Any>() as MediaPackage)).andReturn(response).anyTimes()
        EasyMock.expect(smilService.fromXml(EasyMock.anyString())).andReturn(response).anyTimes()
        EasyMock.expect(smilService.fromXml(EasyMock.anyObject<Any>() as File)).andReturn(response).anyTimes()
        EasyMock.expect(smilService.removeSmilElement(EasyMock.anyObject<Any>() as Smil, EasyMock.anyString()))
                .andReturn(response).anyTimes()
        EasyMock.expect(smilService.addParallel(smil)).andReturn(response).anyTimes()
        EasyMock.expect(smilService.addClips(EasyMock.anyObject<Any>() as Smil, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as Array<Track>, EasyMock.anyLong(), EasyMock.anyLong())).andReturn(response).anyTimes()

        EasyMock.replay(smilService)
        return smilService

    }

}
