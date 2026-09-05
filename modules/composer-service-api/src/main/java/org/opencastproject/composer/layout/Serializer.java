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

import com.google.gson.JsonObject;

public final class Serializer {
  private Serializer() {
  }

  public static JsonObject json(Dimension a) {
    JsonObject json = new JsonObject();
    json.addProperty("w", a.getWidth());
    json.addProperty("h", a.getHeight());
    return json;
  }

  public static Dimension dimension(JsonObject json) {
    return new Dimension(json.get("w").getAsInt(), json.get("h").getAsInt());
  }

  public static JsonObject json(Anchor a) {
    JsonObject json = new JsonObject();
    json.addProperty("left", a.getLeft());
    json.addProperty("top", a.getTop());
    return json;
  }

  public static Anchor anchor(JsonObject json) {
    return new Anchor(json.get("left").getAsDouble(), json.get("top").getAsDouble());
  }

  public static JsonObject json(Offset a) {
    JsonObject json = new JsonObject();
    json.addProperty("x", a.getX());
    json.addProperty("y", a.getY());
    return json;
  }

  public static Offset offset(JsonObject json) {
    return new Offset(json.get("x").getAsInt(), json.get("y").getAsInt());
  }

  public static JsonObject json(AnchorOffset a) {
    JsonObject json = new JsonObject();
    json.add("offset", json(a.getOffset()));
    json.add("reference", json(a.getReferenceAnchor()));
    json.add("referring", json(a.getReferringAnchor()));
    return json;
  }

  public static AnchorOffset anchorOffset(JsonObject json) {
    return new AnchorOffset(anchor(json.getAsJsonObject("reference")),
                            anchor(json.getAsJsonObject("referring")),
                            offset(json.getAsJsonObject("offset")));
  }

  public static JsonObject json(TwoShapeLayout a) {
    JsonObject json = new JsonObject();
    json.add("canvas", json(a.getCanvas()));
    json.add("upper", json(a.getUpper()));
    json.add("lower", json(a.getLower()));
    return json;
  }

  public static TwoShapeLayout twoShapeLayout(JsonObject json) {
    return new TwoShapeLayout(dimension(json.getAsJsonObject("canvas")),
                              layout(json.getAsJsonObject("upper")),
                              layout(json.getAsJsonObject("lower")));
  }

  public static JsonObject json(Layout a) {
    JsonObject json = new JsonObject();
    json.add("dimension", json(a.getDimension()));
    json.add("offset", json(a.getOffset()));
    return json;
  }

  public static Layout layout(JsonObject json) {
    return new Layout(dimension(json.getAsJsonObject("dimension")),
                      offset(json.getAsJsonObject("offset")));
  }

  public static JsonObject json(HorizontalCoverageLayoutSpec a) {
    JsonObject json = new JsonObject();
    json.add("anchorOffset", json(a.getAnchorOffset()));
    json.addProperty("horizontalCoverage", a.getHorizontalCoverage());
    return json;
  }

  public static HorizontalCoverageLayoutSpec horizontalCoverageLayoutSpec(JsonObject json) {
    return new HorizontalCoverageLayoutSpec(anchorOffset(json.getAsJsonObject("anchorOffset")),
                                            json.get("horizontalCoverage").getAsDouble());
  }

  public static JsonObject json(AbsolutePositionLayoutSpec a) {
    JsonObject json = new JsonObject();
    json.add("anchorOffset", json(a.getAnchorOffset()));
    return json;
  }

  public static AbsolutePositionLayoutSpec absolutePositionLayoutSpec(JsonObject json) {
    return new AbsolutePositionLayoutSpec(anchorOffset(json.getAsJsonObject("anchorOffset")));
  }
}
