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

import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;

import java.io.File;
import java.util.Properties;

/**
 * TODO: Comment me!
 */
public class HauppaugePVR350VideoProducer extends FileProducer {

  private Element filesrc;
  private Element mpegpsdemux;
  private Element decoder;
  private Element mpegvideoparse;
  private Element queue;

  /**
   * Creates a Producer specifically designed to capture from the Hauppauge WinTv cards
   * 
   * @param captureDevice
   *          The Hauppauge {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The confidence monitoring properties.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the properties are set while any of the Elements are null this Exception is thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the Element whose pads are ghosted is unsuitible then this Exception is thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If any of the Elements won't link together then this Exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           The parameter captureDevice is required, this Exception is thrown if it is null.
   * @throws UnableToCreateElementException
   *           If an Element's GStreamer module is not present then this Exception is thrown
   */
  public HauppaugePVR350VideoProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * Create all of the Elements for the Producer.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if an Element's GStreamer module is not present.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    filesrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESRC, null);
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
    mpegpsdemux = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MPEGPSDEMUX, null);
    mpegvideoparse = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MPEGVIDEOPARSE, null);
    decoder = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.MPEG2DEC, null);
  }

  /**
   * Sets the filesrc and demuxer's properties
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If filesrc or demuxer is null then this Exception is thrown.
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    setFileSrcProperties();
    setMPEGPSDemuxProperties();
  }

  /**
   * Sets the filesrc location and makes sure that it can be read from.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if filesrc is null
   **/
  private synchronized void setFileSrcProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (filesrc == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesrc, captureDevice.getLocation());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException("HauppaugePVR350VideoProducer cannot read from " 
              + captureDevice.getLocation());
    }
    filesrc.set(GStreamerProperties.LOCATION, captureDevice.getLocation());
  }

  /**
   * Creates the event listener to connect the sometimes pad of the mpegpsdemux to the mpegvideoparse.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if mpegpsdemux is null
   **/
  private synchronized void setMPEGPSDemuxProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (mpegpsdemux == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(mpegpsdemux, "dynamic pad listener");
    }
    // mpegpsdemux source pad is only available sometimes, therefore we need to add a listener to accept dynamic pads
    mpegpsdemux.connect(new Element.PAD_ADDED() {
      public void padAdded(Element arg0, Pad newPad) {
        if (newPad.getName().contains(GStreamerProperties.VIDEO)) {
          PadLinkReturn padLinkReturn = newPad.link(mpegvideoparse.getStaticPad(GStreamerProperties.SINK));
          if (padLinkReturn != PadLinkReturn.OK) {
            try {
              throw new UnableToLinkGStreamerElementsException(captureDevice, mpegpsdemux, mpegvideoparse);
            } catch (UnableToLinkGStreamerElementsException e) {
              logger.error(e.getMessage() + " because PadLinkReturn was " + padLinkReturn.toString() + " on Pad "
                      + newPad.getName());
            }
          }
        }
      }
    });
  }

  /** Adds all of the necessary Elements to the internal Bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(filesrc, queue, mpegpsdemux, mpegvideoparse, decoder, videorate, fpsfilter);
  }

  /** Returns the src pad of the fpsfilter as it is the last element in the chain for this producer. **/
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }

  /** Link all of the gstreamer Elements together for this Producer **/
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!filesrc.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, queue);
    } else if (!queue.link(mpegpsdemux))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, mpegpsdemux);
    else if (!mpegvideoparse.link(decoder))
      throw new UnableToLinkGStreamerElementsException(captureDevice, mpegvideoparse, decoder);
    else if (!decoder.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, decoder, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }
}
