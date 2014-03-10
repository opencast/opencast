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
package org.opencastproject.silencedetection.gstreamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wsmirnow
 */
public abstract class GstreamerAbstractTest {
  
  /** The logging instance */
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GstreamerAbstractTest.class);
  
  public static final int WAIT_SEC = 3;
  
  protected String audioFilePath;
    
  public GstreamerAbstractTest() {
      try {
      audioFilePath = new File(getClass().getResource("/testresources/testvideo-a.mp4").toURI()).getAbsolutePath();
    } catch (URISyntaxException ex) {
      logger.error(ex.getMessage());
    }
  }
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    Gst.setUseDefaultContext(true);
    Gst.init();
  }
  
  @Before
  public void bla() throws FileNotFoundException {
    if (!new File(audioFilePath).exists())
      throw new FileNotFoundException("Audio test file does not exist");
  }
  
  /**
   * Test if Gstreamer {@code ElementFactory} can find element with given name.
   * @param factoryName Gstreamer Element factory name
   * @return true if Gstreamer Element installed, flase otherwise
   */
  public static boolean testGstreamerElementInstalled(String factoryName) {
    try {
      return null != ElementFactory.make(factoryName, null);
    } catch (Exception e) {
      return false;
    }
  }
}
