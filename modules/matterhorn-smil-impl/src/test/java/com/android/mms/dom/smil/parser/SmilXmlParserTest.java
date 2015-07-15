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
package com.android.mms.dom.smil.parser;

import com.android.mms.dom.smil.TimeImpl;

import junit.framework.Assert;

import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;

public class SmilXmlParserTest {

  @Test
  public void testParsePartialSmil() throws Exception {
    SMILDocument smilDocument = new SmilXmlParser().parse(getClass().getResourceAsStream("/source+partial.smil"));
    Assert.assertNotNull(smilDocument);
  }

  @Test
  public void testParseCuttingSmil() throws Exception {
    SMILDocument smilDocument = new SmilXmlParser().parse(getClass().getResourceAsStream("/source.smil"));
    Assert.assertNotNull(smilDocument);

    boolean test = false;
    SMILElement head = smilDocument.getHead();
    SMILElement paramGroup = (SMILElement) head.getElementsByTagName("paramgroup").item(0);
    String trackParamGroupId = paramGroup.getId();
    NodeList params = head.getElementsByTagName("param");
    for (int i = 0; i < params.getLength(); i++) {
      SMILElement elem = (SMILElement) params.item(i);
      if ("track-id".equals(elem.getAttribute("name"))) {
        test = true;
      }
    }

    Assert.assertTrue(test);
    Assert.assertNotNull(trackParamGroupId);

    SMILElement body = smilDocument.getBody();
    NodeList childNodes = body.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      SMILElement element = (SMILElement) childNodes.item(i);
      Assert.assertTrue(element.hasChildNodes());
      if (element instanceof SMILParElement) {
        // par element should contain media elements
        NodeList mediaElements = element.getChildNodes();
        for (int j = 0; j < mediaElements.getLength(); j++) {
          Node e = mediaElements.item(j);
          if (!e.hasChildNodes()) {
            SMILMediaElement media = (SMILMediaElement) e;
            if (trackParamGroupId.equals(media.getAttribute("paramGroup"))) {
              Float begin = TimeImpl.parseClockValue(media.getClipBegin());
              Float end = TimeImpl.parseClockValue(media.getClipEnd());
              Assert.assertEquals(0, begin.longValue());
              Assert.assertEquals(10440, end.longValue());
            }
          } else {
            Assert.fail("Smil container is not supportet yet");
          }
        }
      } else {
        Assert.fail("Couldn't parse Smil element");
      }
    }

  }

}
