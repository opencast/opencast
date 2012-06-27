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

import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pad;

import java.util.Properties;

public class DV1394Producer extends VideoProducer {

  private static final String VIDEO_X_DV = "video/x-dv";
  private Element dv1394src;
  private Element demux;
  private Element decoder;
  private Element ffmpegcolorspace;

  /**
   * Creates a Producer specifically designed to capture from a DV Camera attached by firewire
   * 
   * @deprecated This function has not been maintained in a long time and has many problems. If you need DV support let
   *             the list know.
   * @param captureDevice
   *          DV Camera attached to firewire {@code CaptureDevice} to create Producer around
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the setElementProperties function is called before createElements then one of the GStreamer Elements
   *           this Producer uses will be null and this Exception will be thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the ghost pads cannot be created for this Producer then this Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If any Element cannot be linked to another in the Bin then this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           The parameter captureDevice is necessary for creating a DV1394 Producer so this Exception will be thrown
   *           if it is null.
   * @throws UnableToCreateElementException
   *           If any of the necessary Elements cannot be created then this Exception is thrown.
   */
  public DV1394Producer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Creates the Elements required to capture from a DV1394 device.
   * 
   * @throws UnableToCreateElementException
   *           If the dv1394src, dvdemux, dvdecoder or ffmpegcolorspace cannot be created this Exception is thrown.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    dv1394src = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DV1394SRC, null);
    /* Setup the demuxer to get the data out of the dv container. */
    demux = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DVDEMUX, null);
    /* Setup dv stream decoding */
    decoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DVDEC, null);
    /* Use the ffmpegcolorspace to change the color space to the proper one. */
    ffmpegcolorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, null);
  }

  /**
   * Creates the demuxer's pad listener because it is only available once the sink has a source. We will only be
   * capturing the video.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If demux Element is null this Exception is thrown
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    if (demux == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(demux, "dynamic pad listener");
    }
    /* handle demuxer's sometimes pads. Filter for just video. */
    demux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        logger.info("Element: {}, Pad: {}", element.getName(), pad.getName());
        Element.linkPadsFiltered(demux, GStreamerProperties.VIDEO, decoder, GStreamerProperties.SINK,
                Caps.fromString(VIDEO_X_DV));
      }
    });
  }

  /** Add all of the GStreamer Elements required for this Producer to the Bin **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(dv1394src, demux, decoder, videorate, fpsfilter, ffmpegcolorspace, queue);
    // TODO - Figure out if this connect or the one above is required!
    demux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element element, Pad pad) {
        pad.link(decoder.getStaticPad(GStreamerProperties.SINK));
      }
    });
  }

  /**
   * Link together all of the Elements together for this producer except for the demuxer because the pads are only on
   * sometimes.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If any of these Elements fail to link together this Exception is thrown.
   **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!dv1394src.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, dv1394src, queue);
    } else if (!queue.link(demux)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, demux);
    } else if (!decoder.link(ffmpegcolorspace)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, decoder, ffmpegcolorspace);
    } else if (!ffmpegcolorspace.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, ffmpegcolorspace, videorate);
    } else if (!videorate.link(fpsfilter)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
  }

  /** Returns the decoder's source pad so that this Producer can have ghost pads that link it to Consumers. **/
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }
}
