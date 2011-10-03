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
package org.opencastproject.textextractor.ocropus;

import org.opencastproject.textextractor.api.TextLine;

import org.apache.commons.lang.StringUtils;

import java.awt.Rectangle;

/**
 * Representation of a line of text extracted from an image.
 */
public class OcropusTextLine implements TextLine {

  /** The text */
  protected String text = null;

  /** Text bounding box */
  protected Rectangle boundingBox = null;

  /**
   * Creates a representation for a piece of text along with it's bounding box.
   * 
   * @param word
   *          the extracted text
   * @param box
   *          the text's location and boundaries
   */
  public OcropusTextLine(String word, Rectangle box) {
    this.text = word;
    this.boundingBox = box;
  }

  /**
   * Creates a representation for a collection of lines along with its bounding box.
   * 
   * @param words
   *          the extracted lines
   * @param box
   *          the line's location and boundaries
   */
  public OcropusTextLine(String[] words, Rectangle box) {
    this(StringUtils.join(words, ' '), box);
  }

  /**
   * Returns the text.
   * 
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * Returns the text's bounding box. Note that the box was calculated from the line of text that contained this text,
   * so while the vertical position as well as the height will be ok, the box will most probably be much wider than this
   * single text.
   * 
   * @return the boundaries
   */
  public Rectangle getBoundaries() {
    return boundingBox;
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
