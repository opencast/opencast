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

import static org.opencastproject.util.data.Monadics.mlist;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import java.util.List;

public final class LayoutManager {
  private LayoutManager() {
  }

  /**
   * Compose two shapes on a canvas.
   * It is guaranteed that shapes to not extend the underlying canvas.
   *
   * @param canvas
   *         the dimension of the target canvas
   * @param upper
   *         the dimension of the upper (z-axis) source shape
   * @param lower
   *         the dimension of the lower (z-axis) source shape
   * @param spec
   *         the layout specification
   */
  public static TwoShapeLayout twoShapeLayout(Dimension canvas,
                                              Dimension upper,
                                              Dimension lower,
                                              TwoShapeLayouts.TwoShapeLayoutSpec spec) {
    return new TwoShapeLayout(canvas,
                              calcLayout(canvas, upper, spec.getUpper()),
                              calcLayout(canvas, lower, spec.getLower()));
  }

  private static Layout calcLayout(Dimension canvas,
                                   Dimension shape,
                                   HorizontalCoverageLayoutSpec posSpec) {
    final Dimension slice = new Dimension(limitMin(canvas.getWidth() * posSpec.getHorizontalCoverage(), 0),
                                          canvas.getHeight());
    final Dimension scaled = scaleToFit(slice, shape);
    final AnchorOffset dist = posSpec.getAnchorOffset();
    final Offset anchorOfReference = offset(dist.getReferenceAnchor(), canvas);
    final Offset anchorOfReferring = offset(dist.getReferringAnchor(), scaled);
    return new Layout(
            scaled,
            new Offset(limitMin(anchorOfReference.getX() + dist.getOffset().getX() - anchorOfReferring.getX(), 0),
                       limitMin(anchorOfReference.getY() + dist.getOffset().getY() - anchorOfReferring.getY(), 0)));
  }

  private static Layout calcLayout(Dimension canvas,
                                   Dimension shape,
                                   AbsolutePositionLayoutSpec posSpec) {
    final AnchorOffset dist = posSpec.getAnchorOffset();
    final Offset anchorOfReference = offset(dist.getReferenceAnchor(), canvas);
    final Offset anchorOfReferring = offset(dist.getReferringAnchor(), shape);
    return new Layout(
            shape,
            new Offset(limitMin(anchorOfReference.getX() + dist.getOffset().getX() - anchorOfReferring.getX(), 0),
                       limitMin(anchorOfReference.getY() + dist.getOffset().getY() - anchorOfReferring.getY(), 0)));
  }

  /**
   * Compose a list of shapes on a canvas.
   *
   * @param canvas
   *         the dimension of the target canvas
   * @param shapes
   *         A list of shapes sorted in z-order with the first shape in the list being the lowermost one.
   *         The list consists of the dimension of the source shape tupled with a layout specification.
   */
  public static MultiShapeLayout multiShapeLayout(final Dimension canvas,
                                                  final List<Tuple<Dimension, HorizontalCoverageLayoutSpec>> shapes) {
    return new MultiShapeLayout(
            canvas,
            mlist(shapes).map(new Function<Tuple<Dimension, HorizontalCoverageLayoutSpec>, Layout>() {
              @Override public Layout apply(Tuple<Dimension, HorizontalCoverageLayoutSpec> a) {
                return calcLayout(canvas, a.getA(), a.getB());
              }
            }).value());
  }

  /**
   * Compose a list of shapes on a canvas.
   *
   * @param canvas
   *         the dimension of the target canvas
   * @param shapes
   *         A list of shapes sorted in z-order with the first shape in the list being the lowermost one.
   *         The list consists of the dimension of the source shape tupled with a layout specification.
   */
  public static MultiShapeLayout absoluteMultiShapeLayout(
          final Dimension canvas,
          final List<Tuple<Dimension, AbsolutePositionLayoutSpec>> shapes) {
    return new MultiShapeLayout(
            canvas,
            mlist(shapes).map(new Function<Tuple<Dimension, AbsolutePositionLayoutSpec>, Layout>() {
              @Override public Layout apply(Tuple<Dimension, AbsolutePositionLayoutSpec> a) {
                return calcLayout(canvas, a.getA(), a.getB());
              }
            }).value());
  }

  public static int limitMax(double v, int max) {
    return (int) Math.min(Math.round(v), max);
  }

  public static int limitMin(double v, int min) {
    return (int) Math.max(Math.round(v), min);
  }

  /** Test if <code>shape</code> fits into <code>into</code>. */
  public static boolean fits(Dimension into, Dimension shape) {
    return shape.getHeight() <= into.getHeight() && shape.getWidth() <= into.getHeight();
  }

  /** Calculate the area of a dimension. */
  public static int area(Dimension a) {
    return a.getWidth() * a.getHeight();
  }

  /** Return the dimension with the bigger area. */
  public static Dimension max(Dimension a, Dimension b) {
    return area(a) > area(b) ? a : b;
  }

  /** Get the aspect ratio of a dimension. */
  public static double aspectRatio(Dimension a) {
    return d(a.getWidth()) / d(a.getHeight());
  }

  /** Test if layouts <code>a</code> and <code>b</code> overlap. */
  public static boolean overlap(Layout a, Layout b) {
    return (between(left(a), right(a), left(b)) || between(left(a), right(a), right(b)))
            && (between(top(a), bottom(a), top(b)) || between(top(a), bottom(a), bottom(b)));
  }

  /** Get the X coordinate of the left bound of the layout. */
  public static int left(Layout a) {
    return a.getOffset().getX();
  }

  /** Get the X coordinate of the right bound of the layout. */
  public static int right(Layout a) {
    return a.getOffset().getX() + a.getDimension().getWidth();
  }

  /** Get the Y coordinate of the top bound of the layout. */
  public static int top(Layout a) {
    return a.getOffset().getY();
  }

  /** Get the Y coordinate of the bottom bound of the layout. */
  public static int bottom(Layout a) {
    return a.getOffset().getY() + a.getDimension().getHeight();
  }

  /** Calculate the offset of an anchor point for a given shape relative to its upper left corner. */
  public static Offset offset(Anchor a, Dimension dim) {
    return new Offset(limitMax(a.getLeft() * d(dim.getWidth()), dim.getWidth()),
                      limitMax(a.getTop() * d(dim.getHeight()), dim.getHeight()));
  }

  /**
   * Scale <code>shape</code> by <code>scale</code> and ensure that any rounding errors are limited so that
   * the resulting dimension does not exceed <code>limit</code>.
   */
  public static Dimension scale(Dimension limit, Dimension shape, double scale) {
    return Dimension.dimension(
            limitMax(d(shape.getWidth()) * scale, limit.getWidth()),
            limitMax(d(shape.getHeight()) * scale, limit.getHeight()));
  }

  /** Scale <code>d</code> to fit into <code>canvas</code> . */
  public static Dimension scaleToFit(Dimension canvas, Dimension d) {
    final double scaleToWidth = d(canvas.getWidth()) / d(d.getWidth());
    if (d.getHeight() * scaleToWidth > canvas.getHeight()) {
      final double scaleToHeight = d(canvas.getHeight()) / d(d.getHeight());
      return scale(canvas, d, scaleToHeight);
    } else {
      return scale(canvas, d, scaleToWidth);
    }
  }

  /** a <= x <= b */
  public static boolean between(int a, int b, int x) {
    return a <= x && x <= b;
  }

  private static double d(int v) {
    return v;
  }
}
