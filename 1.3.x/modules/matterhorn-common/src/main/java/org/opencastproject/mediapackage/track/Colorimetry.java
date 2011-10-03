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

package org.opencastproject.mediapackage.track;

/**
 * Describes colorimetry.
 */
public class Colorimetry {

  private int luma;
  private int cb;
  private int cr;

  public Colorimetry(int luma, int cb, int cr) {
    this.luma = luma;
    this.cb = cb;
    this.cr = cr;
  }

  public int getLuma() {
    return luma;
  }

  public void setLuma(int luma) {
    this.luma = luma;
  }

  public int getCb() {
    return cb;
  }

  public void setCb(int cb) {
    this.cb = cb;
  }

  public int getCr() {
    return cr;
  }

  public void setCr(int cr) {
    this.cr = cr;
  }

  public Colorimetry fromString(String c) {
    // todo
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public String toString() {
    return luma + ":" + cb + ":" + cr;
  }
}
