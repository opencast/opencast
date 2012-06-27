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

import org.opencastproject.textanalyzer.api.TextAnalyzerException;
import org.opencastproject.textextractor.api.TextExtractor;
import org.opencastproject.textextractor.api.TextExtractorException;
import org.opencastproject.util.ProcessExcecutorException;
import org.opencastproject.util.ProcessExecutor;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Commandline wrapper around ocropus' <code>ocrocmd</code> command.
 */
public class OcropusTextExtractor implements TextExtractor {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OcropusTextExtractor.class);

  /** Default name of the ocrocmd binary */
  public static final String OCROPUS_BINARY_DEFAULT = "ocrocmd";

  /** Binary of the ocropus command */
  protected String binary = OCROPUS_BINARY_DEFAULT;

  /**
   * Creates a new ocropus command wrapper that will be using the default binary.
   * 
   * @param binary
   *          the ocropus binary
   */
  public OcropusTextExtractor() {
    this(OCROPUS_BINARY_DEFAULT);
  }

  /**
   * Creates a new ocropus command wrapper that will be using the given binary.
   * 
   * @param binary
   *          the ocropus binary
   */
  public OcropusTextExtractor(String binary) {
    this.binary = binary;
  }

  /**
   * Returns the path to the <code>ocrocmd</code> binary.
   * 
   * @return path to the binary
   */
  public String getBinary() {
    return binary;
  }

  /**
   * Sets the path to the <code>ocrocmd</code> binary.
   * 
   * @param binary
   */
  public void setBinary(String binary) {
    this.binary = binary;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textextractor.api.TextExtractor#extract(java.io.File)
   */
  public OcropusTextFrame extract(File image) throws IOException, TextExtractorException {
    if (binary == null)
      throw new IllegalStateException("Ocropus binary is not set");

    /** The text frame containing the result of the analysis operation */
    final OcropusTextFrame textFrame;

    /** Output of the commandline process */
    final StringBuffer ocrocmdOutput = new StringBuffer();

    // Do the extraction
    ProcessExecutor<TextAnalyzerException> analyzer = new ProcessExecutor<TextAnalyzerException>(binary,
            image.getAbsolutePath()) {
      @Override
      protected boolean onLineRead(String line) {
        logger.trace(line);
        ocrocmdOutput.append(line).append('\n');
        return true;
      }

      @Override
      protected void onProcessFinished(int exitCode) throws TextAnalyzerException {
        // MH-6246: could not extract text from image
        if (exitCode == 134) {
          logger.warn(ocrocmdOutput.toString());
          return;
        }
        // Windows binary will return -1 when queried for options
        if (exitCode != -1 && exitCode != 0 && exitCode != 255) {
          logger.error(ocrocmdOutput.toString());
          throw new TextAnalyzerException("Text extractor " + binary + " exited with code " + exitCode);
        }
      }
    };

    try {
      analyzer.execute();
    } catch (ProcessExcecutorException e) {
      throw new TextExtractorException("Error running text extractor " + binary, e);
    }

    InputStream is = null;
    try {
      is = IOUtils.toInputStream(ocrocmdOutput.toString(), "UTF-8");
      textFrame = OcropusTextFrame.parse(is);
    } catch (IOException e) {
      throw new TextExtractorException(e);
    } finally {
      IOUtils.closeQuietly(is);
    }

    return textFrame;
  }

}
