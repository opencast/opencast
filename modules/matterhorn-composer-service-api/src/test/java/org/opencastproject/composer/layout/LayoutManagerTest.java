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

import static org.junit.Assert.assertEquals;
import static org.opencastproject.composer.layout.AnchorOffset.anchorOffset;
import static org.opencastproject.composer.layout.Dimension.dimension;
import static org.opencastproject.composer.layout.LayoutManager.aspectRatio;
import static org.opencastproject.composer.layout.LayoutManager.scaleToFit;
import static org.opencastproject.composer.layout.Offset.offset;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.util.JsonObj;
import org.opencastproject.util.data.Tuple;

import org.junit.Test;

import java.util.List;

public class LayoutManagerTest {
  private static final double TOLERANCE = 0.01;
  private static final double ASPECT_RATIO_16_9 = 16.0 / 9.0;
  private static final double ASPECT_RATIO_4_3 = 4.0 / 3.0;

  @Test
  public void testTwoShapeLayout0() throws Exception {
    TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            dimension(4000, 2000), // 2:1 canvas
            dimension(1000, 500), // 2:1 upper
            dimension(1000, 500), // 2:1 lower
            new TwoShapeLayouts.TwoShapeLayoutSpec(new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.TOP_LEFT,
                                                                                                 Anchors.TOP_LEFT, 0, 0), 0.5), new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.BOTTOM_RIGHT,
                                                                                                                                                                              Anchors.BOTTOM_RIGHT, 0, 0), 0.5)));
    assertEquals(dimension(2000, 1000), layout.getUpper().getDimension());
    assertEquals(offset(0, 0), layout.getUpper().getOffset());
    assertEquals(dimension(2000, 1000), layout.getLower().getDimension());
    assertEquals(offset(2000, 1000), layout.getLower().getOffset());
  }

  @Test
  public void testTwoShapeLayout1() throws Exception {
    TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            dimension(4000, 2000), // 2:1 canvas
            dimension(1000, 500), // 2:1 upper
            dimension(1000, 500), // 2:1 lower
            new TwoShapeLayouts.TwoShapeLayoutSpec(new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.TOP_LEFT,
                                                                                                 Anchors.TOP_LEFT, 0, 0), 0.2), new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.BOTTOM_RIGHT,
                                                                                                                                                                              Anchors.BOTTOM_RIGHT, 0, 0), 0.5)));
    assertEquals(dimension(800, 400), layout.getUpper().getDimension());
    assertEquals(offset(0, 0), layout.getUpper().getOffset());
    assertEquals(dimension(2000, 1000), layout.getLower().getDimension());
    assertEquals(offset(2000, 1000), layout.getLower().getOffset());
  }

  @Test
  public void testTwoShapeLayout2() throws Exception {
    TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            dimension(4000, 1000), // canvas
            dimension(1000, 500), // upper
            dimension(1000, 500), // lower
            new TwoShapeLayouts.TwoShapeLayoutSpec(
                    new HorizontalCoverageLayoutSpec(
                            anchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, 0, 0), 0.2),
                    new HorizontalCoverageLayoutSpec(
                            anchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, 0, 0), 0.5)));
    assertEquals(dimension(800, 400), layout.getUpper().getDimension());
    assertEquals(offset(0, 0), layout.getUpper().getOffset());
    assertEquals(dimension(2000, 1000), layout.getLower().getDimension());
    assertEquals(offset(2000, 0), layout.getLower().getOffset());
  }

  /** This tests ensures that a media is not scaled to exceed the canvas. */
  @Test
  public void testTwoShapeLayout3() throws Exception {
    TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            dimension(1920, 1080), // 1.77:1 canvas
            dimension(640, 480), // 4:3 upper (1.33:1)
            dimension(640, 480), // 4:3 lower
            new TwoShapeLayouts.TwoShapeLayoutSpec(
                    new HorizontalCoverageLayoutSpec(
                            anchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, -20, -20), 0.2),
                    new HorizontalCoverageLayoutSpec(
                            anchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, -20, -20), 0.8)));
    assertEquals("Media must not exceed canvas", dimension(384, 288), layout.getUpper().getDimension());
    assertEquals("Media must not exceed canvas", offset(0, 0), layout.getUpper().getOffset());
    assertEquals("Media must not exceed canvas", dimension(1440, 1080), layout.getLower().getDimension());
    assertEquals("Media must not exceed canvas", offset(460, 0), layout.getLower().getOffset());
  }

  @Test
  public void testTwoShapeLayoutDifferentAspectRatios() throws Exception {
    TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            dimension(4000, 4000),
            dimension(400, 300),
            dimension(1000, 500),
            new TwoShapeLayouts.TwoShapeLayoutSpec(new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.TOP_LEFT,
                                                                                                 Anchors.TOP_LEFT, 50, 50), 0.25), new HorizontalCoverageLayoutSpec(anchorOffset(
                    Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, -20, -20), 0.8)));
    assertEquals(dimension(1000, 750), layout.getUpper().getDimension());
    assertEquals(offset(50, 50), layout.getUpper().getOffset());
    assertEquals(dimension(3200, 1600), layout.getLower().getDimension());
    assertEquals(offset(780, 2380), layout.getLower().getOffset());
  }

  @Test
  public void testTwoShapeLayoutSmallerCanvas() throws Exception {
    TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            dimension(200, 200),
            dimension(400, 600),
            dimension(1000, 500),
            new TwoShapeLayouts.TwoShapeLayoutSpec(
                    new HorizontalCoverageLayoutSpec(
                            anchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, 5, 5), 1.0),
                    new HorizontalCoverageLayoutSpec(
                            anchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, -2, -2), 0.8)));
    assertEquals(200, layout.getUpper().getDimension().getHeight());
    assertEquals(400.0 / 600.0, aspectRatio(layout.getUpper().getDimension()), TOLERANCE);
    assertEquals(offset(5, 5), layout.getUpper().getOffset());
    assertEquals(dimension(160, 80), layout.getLower().getDimension());
    assertEquals(offset(38, 118), layout.getLower().getOffset());
  }

  @Test
  public void testTwoShapeLayoutCompose4to3And16to9() {
    final Dimension canvas16To9 = dimension(1600, 900);
    final Dimension upper4To3 = dimension(400, 300);
    final Dimension lower16To9 = dimension(960, 540);
    final double upperCoverage = 0.3;
    final double lowerCoverage = 0.7;
    final TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            canvas16To9,
            upper4To3,
            lower16To9,
            new TwoShapeLayouts.TwoShapeLayoutSpec(new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.TOP_LEFT,
                                                                                                 Anchors.TOP_LEFT, 0, 0), upperCoverage), new HorizontalCoverageLayoutSpec(anchorOffset(
                    Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, 0, 0), lowerCoverage)));
    assertEquals(dimension(1120, 630), layout.getLower().getDimension());
    assertEquals(ASPECT_RATIO_16_9, aspectRatio(lower16To9), TOLERANCE);
    assertEquals(ASPECT_RATIO_16_9, aspectRatio(layout.getLower().getDimension()), TOLERANCE);
    assertEquals(dimension(480, 360), layout.getUpper().getDimension());
    assertEquals(ASPECT_RATIO_4_3, aspectRatio(upper4To3), TOLERANCE);
    assertEquals(ASPECT_RATIO_4_3, aspectRatio(layout.getUpper().getDimension()), TOLERANCE);
  }

  @Test
  public void testTwoShapeLayoutPreserveAspectRatio() {
    final Dimension canvas = dimension(1900, 1080);
    final Dimension upper = dimension(1024, 768);
    final Dimension lower = dimension(1280, 720);
    final double upperCoverage = 0.3;
    final double lowerCoverage = 0.7;
    final TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            canvas,
            upper,
            lower,
            new TwoShapeLayouts.TwoShapeLayoutSpec(new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.TOP_LEFT,
                                                                                                 Anchors.TOP_LEFT, 0, 0), upperCoverage), new HorizontalCoverageLayoutSpec(anchorOffset(
                    Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, 0, 0), lowerCoverage)));
    assertEquals(aspectRatio(upper), aspectRatio(layout.getUpper().getDimension()), TOLERANCE);
    assertEquals(aspectRatio(lower), aspectRatio(layout.getLower().getDimension()), TOLERANCE);
  }

  @Test
  public void testTwoShapeLayoutPictureInPicture() {
    final Dimension canvas = dimension(1900, 1080);
    final Dimension upper = dimension(1024, 768);
    final Dimension lower = dimension(1280, 720);
    final double upperCoverage = 0.2;
    final double lowerCoverage = 1.0;
    final TwoShapeLayout layout = LayoutManager.twoShapeLayout(
            canvas,
            upper,
            lower,
            new TwoShapeLayouts.TwoShapeLayoutSpec(new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.TOP_LEFT,
                                                                                                 Anchors.TOP_LEFT, 10, 10), upperCoverage), new HorizontalCoverageLayoutSpec(anchorOffset(
                    Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, 0, 0), lowerCoverage)));
    assertEquals(dimension(1900, 1069), layout.getLower().getDimension());
    assertEquals(offset(0, 11), layout.getLower().getOffset());
    assertEquals(dimension(380, 285), layout.getUpper().getDimension());
    assertEquals(offset(10, 10), layout.getUpper().getOffset());
  }

  @Test
  public void testMultiShapeLayoutTwoVideoAndWatermark() {
    final Dimension canvas = dimension(1900, 1080);
    final Dimension upper = dimension(1024, 768);
    final Dimension lower = dimension(1280, 720);
    final Dimension watermark = dimension(80, 80);
    final double upperCoverage = 0.3;
    final double lowerCoverage = 0.7;
    final double watermarkCoverage = 0.05;
    LayoutManager.multiShapeLayout(
            canvas,
            list(tuple(lower,
                       new HorizontalCoverageLayoutSpec(anchorOffset(Anchors.BOTTOM_RIGHT, Anchors.BOTTOM_RIGHT, 0, 0),
                                                        lowerCoverage)),
                 tuple(upper, new HorizontalCoverageLayoutSpec(
                         anchorOffset(Anchors.TOP_LEFT, Anchors.TOP_LEFT, 0, 0), upperCoverage)),
                 tuple(watermark,
                       new HorizontalCoverageLayoutSpec(
                               anchorOffset(Anchors.TOP_RIGHT, Anchors.TOP_RIGHT, 20, 20), watermarkCoverage))));
    // todo
  }

  @Test
  public void testScaleToFit() {
    assertEquals(dimension(100, 100), scaleToFit(dimension(100, 100), dimension(10, 10)));
    assertEquals(dimension(50, 100), scaleToFit(dimension(100, 100), dimension(10, 20)));
    assertEquals(dimension(100, 50), scaleToFit(dimension(100, 100), dimension(20, 10)));
    assertEquals(dimension(100, 1), scaleToFit(dimension(100, 500), dimension(1000, 10)));
  }

  /** SWITCHP-337 */
  @Test
  public void testWatermarkLayout() {
    final List<Tuple<Offset, String>> fixtures = list(
            // top left
            tuple(offset(20, 20), "{\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":20},\"reference\":{\"left\":0.0,\"top\":0.0}}}"),
            // top right
            tuple(offset(1340, 20), "{\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":0.0},\"offset\":{\"y\":20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":0.0}}}"),
            // bottom left
            tuple(offset(20, 340), "{\"anchorOffset\":{\"referring\":{\"left\":0.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":20},\"reference\":{\"left\":0.0,\"top\":1.0}}}"),
            // bottom right
            tuple(offset(1340, 340), "{\"anchorOffset\":{\"referring\":{\"left\":1.0,\"top\":1.0},\"offset\":{\"y\":-20,\"x\":-20},\"reference\":{\"left\":1.0,\"top\":1.0}}}"));
    for (final Tuple<Offset, String> fixture : fixtures) {
      final AbsolutePositionLayoutSpec spec = Serializer.absolutePositionLayoutSpec(JsonObj.jsonObj(fixture.getB()));
      final MultiShapeLayout layout = LayoutManager.absoluteMultiShapeLayout(
              Dimension.dimension(1900, 1080),
              list(tuple(Dimension.dimension(540, 720), spec)));
      assertEquals(1, layout.getShapes().size());
      assertEquals(fixture.getA(), layout.getShapes().get(0).getOffset());
    }
  }
}
