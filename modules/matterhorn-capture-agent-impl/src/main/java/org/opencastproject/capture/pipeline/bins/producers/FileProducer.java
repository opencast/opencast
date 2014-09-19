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

public class FileProducer extends VideoProducer {
  private Element filesrc;
  private Element decodebin;

  /**
   * FileProducer handles file sources (both hardware like Hauppauges or regular source files) where we don't know what
   * kind of codec or container we are getting in. To handle this we use a decodebin to do our decoding and pass it off
   * to the sinks.
   *
   * @throws CaptureDeviceNullPointerException
   *           If the mandatory captureDevice parameter is null we throw this Exception.
   * @throws UnableToCreateElementException
   *           If any Element doesn't have its required GStreamer module present, this Exception is thrown.
   **/
  public FileProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /** Creates all of the Elements required for a FileProducer including a filesrc, queue and decodebin. **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    filesrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESRC, null);
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
    decodebin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DECODEBIN, null);
  }

  /**
   * Sets the file location and creates the listener for the decodebin's src pad because it is only present when it has
   * something connected to it's sink.
   *
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If filesrc or decodebin is null then this Exception is thrown.
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    super.setElementProperties();
    setFilesrcProperties();
    setDecodeBinProperties();
  }

  /**
   * Sets the location to read the file from.
   *
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if filesrc is null
   * @throws IllegalArgumentException
   *           If the location cannot be read from.
   **/
  private synchronized void setFilesrcProperties() throws UnableToSetElementPropertyBecauseElementWasNullException,
          IllegalArgumentException {
    if (filesrc == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesrc, captureDevice.getLocation());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException("FileProducer cannot read from location " + captureDevice.getLocation());
    }
    filesrc.set(GStreamerProperties.LOCATION, captureDevice.getLocation());
  }

  /**
   * Add a listener to decodebin to connect it to the videorate Element once its src pad becomes available.
   *
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If decodebin is null this exception is thrown
   * **/
  private synchronized void setDecodeBinProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    // decodebin source pad is only available sometimes, therefore we need to add a listener to accept dynamic pads
    if (decodebin == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(decodebin, "dynamic pad listener");
    }
    decodebin.connect(new Element.PAD_ADDED() {
      public void padAdded(Element arg0, Pad newPad) {
        if (newPad.getName().contains(GStreamerProperties.VIDEO)) {
          PadLinkReturn padLinkReturn = newPad.link(videorate.getStaticPad(GStreamerProperties.SINK));
          if (padLinkReturn != PadLinkReturn.OK) {
            try {
              throw new UnableToLinkGStreamerElementsException(captureDevice, decodebin, videorate);
            } catch (UnableToLinkGStreamerElementsException e) {
              logger.error("Unable to link gstreamer element because PadLinkReturn was " + padLinkReturn.toString()
                      + " on Pad " + newPad.getName() + e);
            }
          }
        }
      }
    });
  }

  /** Add all of the Elements to the bin **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(filesrc, queue, decodebin, videorate, fpsfilter);
  }

  /** Return the fpsfilter as the last Element in the chain. **/
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }

  /**
   * Links all of the FileProducer's Elements together.
   *
   * @throws UnableToLinkGStreamerElementsException
   *           Thrown if any Element will not link to another.
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!filesrc.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, queue);
    } else if (!queue.link(decodebin))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, decodebin);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }
}
