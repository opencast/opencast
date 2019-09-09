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
import static org.junit.Assert.assertFalse;

import org.opencastproject.textextractor.tesseract.TesseractTextExtractor;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Test case for class {@link TesseractTextExtractor}.
 */
public class TesseractTextExtractorTest {

  /** Path to the test image */
  protected String testPath = "/image.tiff";

  /** Test image */
  protected File testFile = null;

  /** Path to the tesseract binary */
  protected static String tesseractBinary = TesseractTextExtractor.TESSERACT_BINARY_DEFAULT;

  /** The tesseract text analyzer */
  protected TesseractTextExtractor analyzer = null;

  /** Additional options for tesseract */
  protected String additionalOptions = "--psm 3";

  /** True to run the tests */
  private static boolean tesseractInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TesseractTextExtractorTest.class);

  @ClassRule
  public static TemporaryFolder testFolder = new TemporaryFolder();

  @BeforeClass
  public static void testTesseract() {
    final String[] command = { tesseractBinary, "-v"};
    Process p = null;
    try {
      p = new ProcessBuilder(command).start();
      int status = p.waitFor();
      if (status != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping text analysis tests due to missing tesseract installation");
      logger.warn(t.getMessage(), t);
      tesseractInstalled = false;
    } finally {
      IoSupport.closeQuietly(p);
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    final URL imageUrl = this.getClass().getResource(testPath);
    testFile = testFolder.newFile();
    FileUtils.copyURLToFile(imageUrl, testFile);
    analyzer = new TesseractTextExtractor(tesseractBinary);
    analyzer.setAdditionalOptions(additionalOptions);
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.tesseract.TesseractTextExtractor#getAdditionalOptions()}.
   */
  @Test
  public void testGetAdditionalOptions() {
    assertEquals(additionalOptions, analyzer.getAdditionalOptions());
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.tesseract.TesseractTextExtractor#analyze(java.io.File)}.
   */
  @Test
  public void testAnalyze() throws Exception {
    if (!tesseractInstalled)
      return;

    List<String> output = analyzer.extract(testFile);
    assertFalse(output.isEmpty());
  }

}
