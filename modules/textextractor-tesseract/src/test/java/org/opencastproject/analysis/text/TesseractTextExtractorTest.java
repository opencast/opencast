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

package org.opencastproject.analysis.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.textextractor.api.TextFrame;
import org.opencastproject.textextractor.tesseract.TesseractTextExtractor;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.StreamHelper;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * Test case for class {@link TesseractTextExtractor}.
 */
public class TesseractTextExtractorTest {

  /** Path to the test image */
  protected String testPath = "/image.tiff";

  /** Test image */
  protected File testFile = null;

  /** Path to the tesseract binary */
  protected static String tesseractbinary = TesseractTextExtractor.TESSERACT_BINARY_DEFAULT;

  /** The tesseract text analyzer */
  protected TesseractTextExtractor analyzer = null;

  /** The text without punctuation */
  protected String text = "Land and Vegetation Key players on the";

  /** Additional options for tesseract */
  protected String addopts = "-psm 3";

  /** True to run the tests */
  private static boolean tesseractInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TesseractTextExtractorTest.class);

  @BeforeClass
  public static void testTesseract() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    StringBuffer errorBuffer = new StringBuffer();
    Process p = null;
    try {
      String[] command = {tesseractbinary, "-v"};
      p = new ProcessBuilder(command).start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream(), errorBuffer);
      int status = p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      if (status != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping text analysis tests due to unsatisifed tesseract installation");
      logger.warn(t.getMessage(), t);
      logger.warn(errorBuffer.toString());
      tesseractInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    URL imageUrl = this.getClass().getResource(testPath);
    testFile = File.createTempFile("ocrtest", ".jpg");
    FileUtils.copyURLToFile(imageUrl, testFile);
    analyzer = new TesseractTextExtractor(tesseractbinary);
    analyzer.setAdditionalOptions(addopts);
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(testFile);
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.tesseract.TesseractTextExtractor#getBinary()}.
   */
  @Test
  public void testGetBinary() {
    assertEquals(tesseractbinary, analyzer.getBinary());
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.tesseract.TesseractTextExtractor#getAdditionalOptions()}.
   */
  @Test
  public void testGetAdditionalOptions() {
    assertEquals(addopts, analyzer.getAdditionalOptions());
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.tesseract.TesseractTextExtractor#analyze(java.io.File)}.
   */
  @Test
  public void testAnalyze() throws Exception {
    if (!tesseractInstalled)
      return;

    TextFrame frame = analyzer.extract(testFile);
    assertTrue(frame.hasText());
  }

}
