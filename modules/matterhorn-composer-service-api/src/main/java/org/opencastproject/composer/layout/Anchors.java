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

/** Some predefined anchor points. */
public final class Anchors {
  private Anchors() {
  }

  public static final Anchor TOP_LEFT = new Anchor(0.0, 0.0);
  public static final Anchor TOP_RIGHT = new Anchor(1.0, 0.0);
  public static final Anchor BOTTOM_LEFT = new Anchor(0.0, 1.0);
  public static final Anchor BOTTOM_RIGHT = new Anchor(1.0, 1.0);
  public static final Anchor CENTER = new Anchor(0.5, 0.5);
}
