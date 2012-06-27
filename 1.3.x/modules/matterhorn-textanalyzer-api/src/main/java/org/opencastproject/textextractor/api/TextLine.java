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
package org.opencastproject.textextractor.api;

import java.awt.Rectangle;

public interface TextLine {

  /**
   * Returns the text.
   * 
   * @return the text
   */
  String getText();

  /**
   * Returns the text's bounding box, if one exists. Note that the box was calculated from the line of text that
   * contained this text, so while the vertical position as well as the height will be ok, the box will most probably be
   * much wider than this single text.
   * 
   * @return the boundaries
   */
  Rectangle getBoundaries();

}