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
package org.opencastproject.textextractor.tesseract;

import org.opencastproject.textextractor.api.TextLine;

import java.awt.Rectangle;

/**
 * Representation of a line of text extracted from an image.
 */
public class TesseractLine implements TextLine {

  /** The text */
  protected String text = null;

  /**
   * Creates a representation for a piece of text
   *
   * @param word
   *          the extracted text
   */
  public TesseractLine(String line) {
    this.text = line;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.textextractor.api.TextLine#getText()
   */
  @Override
  public String getText() {
    return text;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.textextractor.api.TextLine#getBoundaries()
   */
  @Override
  public Rectangle getBoundaries() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return text;
  }

}
