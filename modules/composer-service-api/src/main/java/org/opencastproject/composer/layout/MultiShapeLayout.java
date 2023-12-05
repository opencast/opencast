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

package org.opencastproject.composer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Layout of multiple shapes on a common canvas. */
public final class MultiShapeLayout {
  private final Dimension canvas;
  private final List<Layout> shapes;

  /**
   * Create a new layout for multiple shapes on a common canvas.
   *
   * @param canvas
   *          the dimension of the target canvas
   * @param shapes
   *          a list of shape positions sorted in z-order with the first shape in the list being the lowermost one
   */
  public MultiShapeLayout(Dimension canvas, List<Layout> shapes) {
    this.canvas = canvas;
    this.shapes = Collections.unmodifiableList(new ArrayList<Layout>(shapes));
  }

  public Dimension getCanvas() {
    return canvas;
  }

  public List<Layout> getShapes() {
    return shapes;
  }
}
