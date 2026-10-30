/*
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

package org.opencastproject.composer.layout;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.composer.layout.Offset.offset;

import com.google.gson.JsonParser;

import org.junit.Test;

public class SerializerTest {
  @Test
  public void testSerialize() throws Exception {
    final Dimension d = new Dimension(10, 30);
    assertEquals(d, Serializer.dimension(JsonParser.parseString(Serializer.json(d).toString()).getAsJsonObject()));
    final Anchor a = new Anchor(0.134, 0.982);
    assertEquals(a, Serializer.anchor(JsonParser.parseString(Serializer.json(a).toString()).getAsJsonObject()));
    final AnchorOffset ao = new AnchorOffset(a, a, offset(10, 20));
    assertEquals(ao, Serializer.anchorOffset(JsonParser.parseString(Serializer.json(ao).toString()).getAsJsonObject()));
    final TwoShapeLayout tsl = new TwoShapeLayout(new Dimension(1200, 980),
                                                  new Layout(new Dimension(300, 200), new Offset(0, 0)),
                                                  new Layout(new Dimension(900, 780), new Offset(300, 200)));
    final String tslSer = Serializer.json(tsl).toString();
    assertEquals(tsl, Serializer.twoShapeLayout(JsonParser.parseString(tslSer).getAsJsonObject()));
    final HorizontalCoverageLayoutSpec hcls = new HorizontalCoverageLayoutSpec(
            new AnchorOffset(Anchors.TOP_LEFT, Anchors.BOTTOM_RIGHT, new Offset(10, 12)),
            0.34);
    assertEquals(hcls, Serializer.horizontalCoverageLayoutSpec(
            JsonParser.parseString(Serializer.json(hcls).toString()).getAsJsonObject()));
  }

  @Test(expected = RuntimeException.class)
  public void testAccessFailure() {
    JsonParser.parseString("{\"x\": [1, 2, 3]}").getAsJsonObject().get("x").getAsString();
  }
}
