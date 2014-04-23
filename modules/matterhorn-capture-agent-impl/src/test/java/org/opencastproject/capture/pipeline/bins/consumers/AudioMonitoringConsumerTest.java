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
package org.opencastproject.capture.pipeline.bins.consumers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Message;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.capture.pipeline.PipelineTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;
import org.opencastproject.util.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioMonitoringConsumerTest {

  private static final String testPath = "./target/monitoringTest";

  /** Audiotestsrc Capture Device created for unit testing **/
  private CaptureDevice captureDevice = null;
  /** Pipeline created for unit testing **/
  private Pipeline pipeline = null;
  /** Got RMS message flag. **/
  private boolean gotRmsMessage = false;

  /** True to run the tests */
  private static boolean gstreamerInstalled = true;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AudioMonitoringConsumerTest.class);

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }

  @AfterClass
  public static void tearDownGst() {
    if (gstreamerInstalled) {
      // Gst.deinit();
    }
  }

  @Before
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;

    new File(testPath).mkdir();
    Properties captureDeviceProperties = PipelineTestHelpers.createCaptureDeviceProperties(captureDevice,
            null, null, null, null, null, null);
    captureDevice = PipelineTestHelpers.createCaptureDevice(null, ProducerType.AUDIOTESTSRC, "audiotestsrc", testPath, captureDeviceProperties);

    pipeline = new Pipeline();
    Element audiotestsrc = ElementFactory.make("audiotestsrc", null);
    Element queue = ElementFactory.make("queue", null);

    pipeline.addMany(audiotestsrc, queue);
    if (!audiotestsrc.link(queue)) {
      captureDevice = null;
      pipeline = null;
      gstreamerInstalled = false;
    }
  }

  @After
  public void tearDown() {
    captureDevice = null;
    new File(testPath).delete();
  }

  private boolean addConsumerBinToPipeline(AudioMonitoringConsumer consumerBin) {
    Element queue = null;
    for (Element elem : pipeline.getElements()) {
      if (elem.getName().startsWith("queue")) {
        queue = elem;
        break;
      }
    }
    pipeline.add(consumerBin.getBin());
    return Element.linkPads(queue, "src", consumerBin.getBin(), consumerBin.GHOST_PAD_NAME);
  }

  private void hookUpBus() {
    pipeline.getBus().connect("element", new Bus.MESSAGE() {

      @Override
      public void busMessage(Bus bus, Message msg) {
        Structure msgStructure = msg.getStructure();
        if (!gotRmsMessage
                && msgStructure != null                       // level messages should have a structure
                && "level".equals(msgStructure.getName())     // structure name should be 'level'
                && msgStructure.hasField("rms")) {            // and should contain a rms value-list

          Double rms = msgStructure.getValueList("rms").getDouble(0);
          if (rms != null && Math.abs(rms) > 0) {
            logger.debug("Got audio rms value: {}", rms);
            gotRmsMessage = true;
          }
        }
      }
    });
  }

  @Test
  public void testAudioMonitoringConsumer() {
    if (!gstreamerInstalled) return;
    try {
      // create AudioMonitoringConsumer pipeline
      AudioMonitoringConsumer consumerBin = new AudioMonitoringConsumer(captureDevice, new Properties());
      if (!addConsumerBinToPipeline(consumerBin)) {
        gstreamerInstalled = false;
        Assert.fail("can not link audio monitoring bin");
      }
      hookUpBus();

      // start pipeline
      pipeline.play();
      if (pipeline.getState(3 * 1000000000L) != State.PLAYING) {
        pipeline.setState(State.NULL);
        pipeline = null;
        Assert.fail("audio monitoring pipeline not started");
      }

      // wait 3 sec
      Thread.sleep(3000);
      // stop pipeline
      pipeline.setState(State.NULL);
      pipeline = null;

      // test rms-valu-list is not empty
      if (!gotRmsMessage) {
        Assert.fail("Does not got any RMS vlaue messages on pipeline");
      }

    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    }
  }
}
