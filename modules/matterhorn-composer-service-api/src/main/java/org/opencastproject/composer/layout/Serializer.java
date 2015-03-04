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

import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;

import org.opencastproject.util.JsonObj;
import org.opencastproject.util.Jsons;

public final class Serializer {
  private Serializer() {
  }

  public static Jsons.Obj json(Dimension a) {
    return obj(p("w", a.getWidth()),
               p("h", a.getHeight()));
  }

  public static Dimension dimension(JsonObj json) {
    return new Dimension(json.get(Integer.class, "w"), json.get(Integer.class, "h"));
  }

  public static Jsons.Obj json(Anchor a) {
    return obj(p("left", a.getLeft()),
               p("top", a.getTop()));
  }

  public static Anchor anchor(JsonObj json) {
    return new Anchor(json.get(Double.class, "left"), json.get(Double.class, "top"));
  }

  public static Jsons.Obj json(Offset a) {
    return obj(p("x", a.getX()),
               p("y", a.getY()));
  }

  public static Offset offset(JsonObj json) {
    return new Offset(json.get(Integer.class, "x"), json.get(Integer.class, "y"));
  }

  public static Jsons.Obj json(AnchorOffset a) {
    return obj(p("offset", json(a.getOffset())),
               p("reference", json(a.getReferenceAnchor())),
               p("referring", json(a.getReferringAnchor())));
  }

  public static AnchorOffset anchorOffset(JsonObj json) {
    return new AnchorOffset(anchor(json.getObj("reference")),
                            anchor(json.getObj("referring")),
                            offset(json.getObj("offset")));
  }

  public static Jsons.Obj json(TwoShapeLayout a) {
    return obj(p("canvas", json(a.getCanvas())),
               p("upper", json(a.getUpper())),
               p("lower", json(a.getLower())));
  }

  public static TwoShapeLayout twoShapeLayout(JsonObj json) {
    return new TwoShapeLayout(dimension(json.getObj("canvas")),
                              layout(json.getObj("upper")),
                              layout(json.getObj("lower")));
  }

  public static Jsons.Obj json(Layout a) {
    return obj(p("dimension", json(a.getDimension())),
               p("offset", json(a.getOffset())));
  }

  public static Layout layout(JsonObj json) {
    return new Layout(dimension(json.getObj("dimension")),
                      offset(json.getObj("offset")));
  }

  public static Jsons.Obj json(HorizontalCoverageLayoutSpec a) {
    return obj(p("anchorOffset", json(a.getAnchorOffset())),
               p("horizontalCoverage", a.getHorizontalCoverage()));
  }

  public static HorizontalCoverageLayoutSpec horizontalCoverageLayoutSpec(JsonObj json) {
    return new HorizontalCoverageLayoutSpec(anchorOffset(json.getObj("anchorOffset")),
                                            json.get(Double.class, "horizontalCoverage"));
  }

  public static Jsons.Obj json(AbsolutePositionLayoutSpec a) {
    return obj(p("anchorOffset", json(a.getAnchorOffset())));
  }

  public static AbsolutePositionLayoutSpec absolutePositionLayoutSpec(JsonObj json) {
    return new AbsolutePositionLayoutSpec(anchorOffset(json.obj("anchorOffset")));
  }
}
