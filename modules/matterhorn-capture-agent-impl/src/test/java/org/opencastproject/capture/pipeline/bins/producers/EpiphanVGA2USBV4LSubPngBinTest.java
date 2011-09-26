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

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.util.ConfigurationException;

import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.elements.AppSink;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.EpiphanVGA2USBV4LSubPngBin}.
 */
public class EpiphanVGA2USBV4LSubPngBinTest extends EpiphanVGA2USBV4LTest {

  private String imageMockPath = null;

  @Before
  @Override
  public void setUp() throws ConfigurationException, IOException, URISyntaxException {
    super.setUp();

    if (!readyTestEnvironment())
      return;

    // create fallback image mock
    File imageMock = new File("./target", "testpipe/fallback.png");
    imageMock.createNewFile();
    imageMockPath = imageMock.getAbsolutePath();

    properties.setProperty(CaptureParameters.FALLBACK_PNG, imageMockPath);
  }

  @Test
  public void subTestSrcBinTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    Assert.assertNotNull(epiphanBin.getSubBin());
    Assert.assertTrue(epiphanBin.getSubBin() instanceof EpiphanVGA2USBV4LSubPngBin);
    Assert.assertEquals(epiphanBin.getCaps(), ((EpiphanVGA2USBV4LSubPngBin) epiphanBin.getSubBin()).getCaps());
  }

  @Test
  public void subTestSrcBinCreateElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.getSubBin() instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.getSubBin();
    Assert.assertNotNull(subBin.getSource());
    Assert.assertNotNull(subBin.getPngdec());
    Assert.assertNotNull(subBin.getColorspace());
    Assert.assertNotNull(subBin.getScale());
    Assert.assertNotNull(subBin.getCapsFilter());
    Assert.assertNotNull(subBin.getSink());
    Assert.assertTrue(subBin.getSink() instanceof AppSink);

    List<Element> elements = subBin.bin.getElements();
    Assert.assertEquals(elements.size(), 6);
  }

  @Test
  public void subTestSrcBinSetElementPropertiesTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.getSubBin() instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.getSubBin();
    Assert.assertEquals(subBin.getSource().get(GStreamerProperties.LOCATION), imageMockPath);
    Assert.assertEquals(subBin.getSink().get(GStreamerProperties.EMIT_SIGNALS), false);
    Assert.assertEquals(subBin.getSink().get(GStreamerProperties.DROP), false);
    Assert.assertEquals(subBin.getSink().get(GStreamerProperties.ASYNC), true);
    Assert.assertEquals(subBin.getSink().get(GStreamerProperties.MAX_BUFFERS), 5);
    // if (subBin.caps != null) {
    // //TODO: can not convert to Caps
    // Assert.assertEquals(subBin.caps_filter.get("caps"), Caps.fromString(subBin.caps));
    // }
  }

  @Test
  public void subTestSrcBinBinLinkElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.getSubBin() instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.getSubBin();

    // src -> jpegdec
    Pad pad = subBin.getSource().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.getPngdec());

    // jpegdec -> colorspace
    pad = subBin.getPngdec().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.getColorspace());

    // colorspace -> scale
    pad = subBin.getColorspace().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.getScale());

    // scale -> caps_filter
    pad = subBin.getScale().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.getCapsFilter());

    // caps_filter -> sink
    pad = subBin.getCapsFilter().getSrcPads().get(0);
    Assert.assertTrue(pad.isLinked());
    Assert.assertEquals(pad.getPeer().getParentElement(), subBin.getSink());
  }

  @Test
  public void subTestSrcBinBinRemoveElementsTest() throws Exception {
    if (!readyTestEnvironment())
      return;

    EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);

    if (!(epiphanBin.getSubBin() instanceof EpiphanVGA2USBV4LSubPngBin)) {
      Assert.fail();
      return;
    }

    EpiphanVGA2USBV4LSubPngBin subBin = (EpiphanVGA2USBV4LSubPngBin) epiphanBin.getSubBin();
    subBin.removeElements();

    List<Element> elements = subBin.bin.getElements();
    Assert.assertTrue(elements.isEmpty());
  }
}
