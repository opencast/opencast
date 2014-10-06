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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EmbedderEngine;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.impl.qtembedder.QTSbtlEmbedderEngine;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.StreamHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Test class for QuickTime embedder engine.
 *
 */
public class EmbedderEngineTest {

  private EmbedderEngine engine;
  private File[] captions;
  private String[] languages;
  private File movie;
  private File resultingFile;

  // default path to QT subtitle embedder
  private static String defaultBinaryPath = QTSbtlEmbedderEngine.QTEMBEDDER_BINARY_DEFAULT;

  // logger
  private static final Logger logger = LoggerFactory.getLogger(EmbedderEngineTest.class);

  /** True to run the tests */
  private static boolean qtembedderInstalled = true;

  @BeforeClass
  public static void testGst() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    StringBuffer errorBuffer = new StringBuffer();
    Process p = null;
    try {
      p = new ProcessBuilder(defaultBinaryPath, "--help").start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream(), errorBuffer);
      int status = p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      if (status != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping qt embedder tests due to unsatisifed qtsbtlembedder installation");
      logger.warn(errorBuffer.toString());
      qtembedderInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  @Before
  public void setUp() throws Exception {
    // create engine
    engine = new QTSbtlEmbedderEngine();
    // load captions and movie
    File engCaptions = new File(EmbedderEngineTest.class.getResource("/captions_test_eng.srt").toURI());
    Assert.assertNotNull(engCaptions);
    File fraCaptions = new File(EmbedderEngineTest.class.getResource("/captions_test_fra.srt").toURI());
    Assert.assertNotNull(fraCaptions);
    captions = new File[] { engCaptions, fraCaptions };
    languages = new String[] { "en", "fr" };
    movie = new File(EmbedderEngineTest.class.getResource("/slidechanges.mov").toURI());
    Assert.assertNotNull(movie);
  }

  @Test
  @Ignore("The embedder does not work on Mac OS X (Segmentation fault, see MH-5415")
  public void testEmbedding() throws EmbedderException, URISyntaxException {
    if (!qtembedderInstalled)
      return;
    resultingFile = engine.embed(movie, captions, languages, new HashMap<String, String>());
    // TODO: Is there a way to test whether embedding actually succeeded?
  }

  @After
  public void tearDown() throws Exception {
    if (resultingFile != null)
      Assert.assertTrue(resultingFile.delete());
  }
}
