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
package org.opencastproject.capture.pipeline.bins.producers;

import org.opencastproject.capture.pipeline.GStreamerPipeline;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.State;
import org.gstreamer.elements.AppSrc;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanVGA2USBV4LProducer}.
 */
public class EpiphanVGA2USBV4LProducerTest extends EpiphanVGA2USBV4LTest {

  @Test
  public void epiphanProducerTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.getBin());
    Assert.assertNotNull(epiphanBin.getDeviceBin());
    Assert.assertNotNull(epiphanBin.getSubBin());
    Assert.assertNotNull(epiphanBin.getEpiphanPoll());
  }

  @Test
  public void epiphanProducerFailTest() {
    if (!readyTestEnvironment())
      return;

    try {
      @SuppressWarnings("unused")
      EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(null, properties);
      Assert.fail();
    } catch (NullPointerException ex) {
    } catch (Exception ex) {
      Assert.fail();
    }

    try {
      @SuppressWarnings("unused")
      EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, null);
      Assert.fail();
    } catch (NullPointerException ex) {
    } catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void createElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.getSource());
    Assert.assertTrue(epiphanBin.getSource() instanceof AppSrc);
    Assert.assertNotNull(epiphanBin.getIdentity());
    Assert.assertNotNull(epiphanBin.getColorspace());
    Assert.assertNotNull(epiphanBin.getVideorate());
    epiphanBin = null;
  }

  @Test
  public void setElementPropertiesTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertEquals(epiphanBin.getSource().get(GStreamerProperties.IS_LIVE), true);
    Assert.assertEquals(epiphanBin.getSource().get(GStreamerProperties.DO_TIMESTAP), true);
    Assert.assertEquals(epiphanBin.getSource().get(GStreamerProperties.BLOCK), true);
    Assert.assertEquals(epiphanBin.getSource().getCaps(), Caps.fromString(epiphanBin.getCaps()));
    Assert.assertEquals(epiphanBin.getIdentity().get(GStreamerProperties.SINGLE_SEGMENT), true);
    epiphanBin = null;
  }

  @Test
  public void addElementsToBinTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    List<Element> elements = epiphanBin.getBin().getElements();
    Assert.assertEquals(elements.size(), 4);
    epiphanBin = null;
  }

  @Test
  public void linkElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);

    // AppSrc -> identity
    Pad pad = epiphanBin.getSource().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.getIdentity());

    // identity -> videorate
    pad = epiphanBin.getIdentity().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.getVideorate());

    // videorate -> collorspace
    pad = epiphanBin.getVideorate().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.getColorspace());

    // colorspace -> null
    pad = epiphanBin.getColorspace().getSrcPads().get(0);
    Assert.assertNull(pad.getPeer().getParentElement());
    epiphanBin = null;
  }

  @Test
  public void getSrcPadTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.getSrcPad());
    Assert.assertTrue(epiphanBin.getSrcPad() instanceof Pad);
    epiphanBin = null;
  }

  @Test
  public void getCapsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    String caps = epiphanBin.getCaps();
    Assert.assertNotNull(caps);
    Assert.assertFalse(caps.isEmpty());
    Assert.assertTrue(caps.contains(GStreamerProperties.VIDEO_X_RAW_YUV) && caps.contains(GStreamerProperties.WIDTH)
            && caps.contains(GStreamerProperties.HEIGHT) && caps.contains(GStreamerProperties.FRAMERATE)
            && caps.contains("format="));
    epiphanBin = null;
  }

  @Ignore
  public void startPipeline() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);

    // start Bin
    epiphanBin.getBin().setState(State.PLAYING);

    State state = epiphanBin.getBin().getState(15 * GStreamerPipeline.GST_SECOND);
    Assert.assertEquals(state, State.PLAYING);

    // state = epiphanBin.deviceBin.pipeline.getState();
    // Assert.assertEquals(state, State.PLAYING);

    state = ((EpiphanVGA2USBV4LSubAbstractBin) epiphanBin.getSubBin()).bin.getState();
    Assert.assertEquals(state, State.PLAYING);

    // stop Bin
    epiphanBin.getBin().setState(State.NULL);

    state = epiphanBin.getBin().getState(15 * GStreamerPipeline.GST_SECOND);
    Assert.assertEquals(state, State.NULL);

    // state = epiphanBin.deviceBin.pipeline.getState();
    // Assert.assertEquals(state, State.NULL);

    state = ((EpiphanVGA2USBV4LSubAbstractBin) epiphanBin.getSubBin()).bin.getState();
    Assert.assertEquals(state, State.NULL);
  }
}
