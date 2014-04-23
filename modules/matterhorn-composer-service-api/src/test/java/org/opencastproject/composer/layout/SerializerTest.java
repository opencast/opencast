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
package org.opencastproject.composer.layout;

import org.junit.Test;
import org.opencastproject.util.JsonObj;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.composer.layout.Offset.offset;

public class SerializerTest {
  @Test
  public void testSerialize() throws Exception {
    final Dimension d = new Dimension(10, 30);
    assertEquals(d, Serializer.dimension(JsonObj.jsonObj(Serializer.json(d).toJson())));
    final Anchor a = new Anchor(0.134, 0.982);
    assertEquals(a, Serializer.anchor(JsonObj.jsonObj(Serializer.json(a).toJson())));
    final AnchorOffset ao = new AnchorOffset(a, a, offset(10, 20));
    assertEquals(ao, Serializer.anchorOffset(JsonObj.jsonObj(Serializer.json(ao).toJson())));
    final TwoShapeLayout tsl = new TwoShapeLayout(new Dimension(1200, 980),
                                                  new Layout(new Dimension(300, 200), new Offset(0, 0)),
                                                  new Layout(new Dimension(900, 780), new Offset(300, 200)));
    final String tslSer = Serializer.json(tsl).toJson();
    assertEquals(tsl, Serializer.twoShapeLayout(JsonObj.jsonObj(tslSer)));
    final HorizontalCoverageLayoutSpec hcls = new HorizontalCoverageLayoutSpec(
            new AnchorOffset(Anchors.TOP_LEFT, Anchors.BOTTOM_RIGHT, new Offset(10, 12)),
            0.34);
    assertEquals(hcls, Serializer.horizontalCoverageLayoutSpec(JsonObj.jsonObj(Serializer.json(hcls).toJson())));
  }

  @Test(expected = RuntimeException.class)
  public void testAccessFailure() {
    JsonObj.jsonObj("{\"x\": [1, 2, 3]}").get(String.class, "x");
  }
}
