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

import org.opencastproject.capture.pipeline.bins.GStreamerProperties;

import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.elements.AppSink;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test class for {@Link
 * org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanVGA2USBV4LSubDeviceBin}.
 */
public class EpiphanVGA2USBV4LSubDeviceBinTest extends EpiphanVGA2USBV4LTest {

  @Test
  public void subDeviceBinTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    Assert.assertNotNull(epiphanBin.getDeviceBin());
    Assert.assertTrue(epiphanBin.getDeviceBin() instanceof EpiphanVGA2USBV4LSubDeviceBin);
    Assert.assertEquals(epiphanBin.getCaps(), epiphanBin.getDeviceBin().getCaps());
  }

  @Test
  public void subDeviceBinCreateElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubDeviceBin deviceBin = epiphanBin.getDeviceBin();
    Assert.assertNotNull(deviceBin.getSource());
    Assert.assertNotNull(deviceBin.getColorspace());
    Assert.assertNotNull(deviceBin.getVideoscale());
    Assert.assertNotNull(deviceBin.getCapsfilter());
    Assert.assertNotNull(deviceBin.getSink());
    Assert.assertTrue(deviceBin.getSink() instanceof AppSink);

    List<Element> elements = deviceBin.bin.getElements();
    Assert.assertEquals(elements.size(), 5);
  }

  @Test
  public void subDeviceBinSetElementPropertiesTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubDeviceBin deviceBin = epiphanBin.getDeviceBin();
    Assert.assertEquals(deviceBin.getSource().get(GStreamerProperties.DEVICE), epiphanLocation);
    Assert.assertEquals(deviceBin.getSource().get(GStreamerProperties.DO_TIMESTAP), false);
    Assert.assertEquals(deviceBin.getSink().get(GStreamerProperties.EMIT_SIGNALS), false);
    Assert.assertEquals(deviceBin.getSink().get(GStreamerProperties.DROP), true);
    Assert.assertEquals(deviceBin.getSink().get(GStreamerProperties.MAX_BUFFERS), 1);
    // if (deviceBin.getCaps() != null) {
    // TODO: can not convert to Caps
    // Assert.assertEquals(deviceBin.capsfilter.get("caps"), Caps.fromString(deviceBin.getCaps()));
    // Assert.assertEquals(deviceBin.sink.get("caps"), Caps.fromString(deviceBin.getCaps()));
    // }
  }

  @Test
  public void subDeviceBinLinkElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);
    EpiphanVGA2USBV4LSubDeviceBin deviceBin = epiphanBin.getDeviceBin();

    // src -> colorspace
    Pad pad = deviceBin.getSource().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.getColorspace());

    // colorspace -> videoscale
    pad = deviceBin.getColorspace().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.getVideoscale());

    // videoscale -> capsfilter
    pad = deviceBin.getVideoscale().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.getCapsfilter());

    // capsfilter -> sink
    pad = deviceBin.getCapsfilter().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertTrue(pad.getPeer().getParentElement() == deviceBin.getSink());
  }

  @Test
  public void subDeviceBinRemoveElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = getEpiphanVGA2USBV4LProducer(captureDevice, properties);

    epiphanBin.getDeviceBin().removeElements();
    List<Element> elements = epiphanBin.getDeviceBin().bin.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
