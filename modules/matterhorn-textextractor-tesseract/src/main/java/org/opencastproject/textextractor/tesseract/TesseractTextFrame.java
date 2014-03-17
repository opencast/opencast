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

import org.opencastproject.textextractor.api.TextFrame;
import org.opencastproject.textextractor.api.TextLine;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class represents a tesseract output frame that holds a number of lines found on an image. Note that Tesseract
 * does not inlcude positioning or other information with the text output.
 */
public class TesseractTextFrame implements TextFrame {

  /** Words found on an output frame */
  protected ArrayList<TextLine> lines = new ArrayList<TextLine>();

  /**
   * Parses the tesseract output file and extracts the text information contained therein.
   * 
   * @param is
   *          the input stream
   * @return the ocropus text information
   * @throws IOException
   *           if reading the ocropus output fails
   */
  public static TextFrame parse(InputStream is) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    String line;
    TesseractTextFrame textFrame = new TesseractTextFrame();

    while ((line = in.readLine()) != null) {
      List<String> words = new ArrayList<String>();
      for (String word : StringUtils.split(line)) {
        String result = word.replaceAll("^[\\W]*|[\\W]*$", "");
        if (StringUtils.isNotBlank(result)) {
          words.add(result);
        }
      }
      if (words.size() == 0) {
        continue;
      }
      textFrame.lines.add(new TesseractLine(StringUtils.join(
              words.toArray(new String[words.size()]), ' ')));
    }

    return textFrame;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textextractor.api.TextFrame#hasText()
   */
  @Override
  public boolean hasText() {
    return lines.size() > 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textextractor.api.TextFrame#getLines()
   */
  @Override
  public TextLine[] getLines() {
    return lines.toArray(new TesseractLine[lines.size()]);
  }

}
