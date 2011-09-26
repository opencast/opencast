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
package org.opencastproject.analysis.text;

import static org.junit.Assert.assertEquals;

import org.opencastproject.textextractor.api.TextLine;
import org.opencastproject.textextractor.tesseract.TesseractLine;

import org.junit.Before;
import org.junit.Test;

/**
 * Test case for class {@link TesseractLine}.
 */
public class TesseractTextLineTest {

  /** The text item */
  protected TextLine textItem = null;

  /** The text */
  protected String text = "Hello world";

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    textItem = new TesseractLine(text);
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.tesseract.TesseractLine#getText()}.
   */
  @Test
  public void testGetText() {
    assertEquals(text, textItem.getText());
  }

}
