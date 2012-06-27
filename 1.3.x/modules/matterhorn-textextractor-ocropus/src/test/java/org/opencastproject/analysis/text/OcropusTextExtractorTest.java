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
import static org.junit.Assert.assertTrue;

import org.opencastproject.textextractor.ocropus.OcropusTextExtractor;
import org.opencastproject.textextractor.ocropus.OcropusTextFrame;
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
 * Test case for class {@link OcropusTextExtractor}.
 */
public class OcropusTextExtractorTest {

  /** Path to the test image */
  protected String testPath = "/image.jpg";

  /** Test image */
  protected File testFile = null;

  /** Path to the ocropus binary */
  protected static String ocropusbinary = OcropusTextExtractor.OCROPUS_BINARY_DEFAULT;

  /** The ocropus text analyzer */
  protected OcropusTextExtractor extractor = null;
  
  /** The text without punctuation */
  protected String text = "Land and Vegetation Key players on the";

  /** True to run the tests */
  private static boolean ocropusInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OcropusTextExtractorTest.class);

  @BeforeClass
  public static void testOcropus() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    Process p = null;
    try {
      p = new ProcessBuilder(ocropusbinary).start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream());
      if (p.waitFor() != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping text analysis tests due to unsatisifed ocropus installation");
      ocropusInstalled = false;
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
    extractor = new OcropusTextExtractor(ocropusbinary);
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(testFile);
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.ocropus.OcropusTextExtractor#getBinary()}.
   */
  @Test
  public void testGetBinary() {
    assertEquals(ocropusbinary, extractor.getBinary());
  }

  /**
   * Test method for {@link org.opencastproject.textextractor.ocropus.OcropusTextExtractor#analyze(java.io.File)}.
   */
  @Test
  public void testAnalyze() throws Exception {
    if (!ocropusInstalled)
      return;
    
    if (!new File(ocropusbinary).exists())
      return;
    OcropusTextFrame frame = extractor.extract(testFile);
    assertTrue(frame.hasText());
    assertEquals(text, frame.getLines()[0].getText());
  }

}
