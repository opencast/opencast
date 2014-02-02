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

import java.io.File;
import java.util.Properties;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

public class UndecodedFileProducer extends ProducerBin {

  private Element filesrc;

  /**
   * UndecodedFileProducer handles file sources (both hardware like Hauppauges or regular source files)
   * and produce an undecoded media stream (Hauppauges TV cards produce an MPEG AV stream)
   * which should be handled by ConsumerBins (e.g. FilesinkConsumer).
   *
   * @throws CaptureDeviceNullPointerException
   *           If the mandatory captureDevice parameter is null we throw this Exception.
   * @throws UnableToCreateElementException
   *           If any Element doesn't have its required GStreamer module present, this Exception is thrown.
   **/
  public UndecodedFileProducer(CaptureDevice captureDevice, Properties properties) throws UnableToLinkGStreamerElementsException,
          UnableToCreateGhostPadsForBinException, UnableToSetElementPropertyBecauseElementWasNullException,
          CaptureDeviceNullPointerException, UnableToCreateElementException {

    super(captureDevice, properties);
  }

  /** Creates filesrc and queue Elements required for a UndecodedFileProducer. **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    filesrc = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FILESRC, null);
  }

  /**
   * Sets the location to read the file from.
   *
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If filesrc is null then this Exception is thrown.
   **/
  @Override
  protected synchronized void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (filesrc == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesrc, captureDevice.getLocation());
    }
    if (!new File(captureDevice.getLocation()).canRead()) {
      throw new IllegalArgumentException("FileProducer cannot read from location " + captureDevice.getLocation());
    }
    filesrc.set(GStreamerProperties.LOCATION, captureDevice.getLocation());
  }

  /** Add all of the Elements to the bin **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(filesrc, queue);
  }

  /**
   * Link filesrc to queue.
   * @throws UnableToLinkGStreamerElementsException if Elements can't be linked together
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!filesrc.link(queue)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, filesrc, queue);
    }
  }

  /**
   * Returns queue src-pad as the last Element in the chain.
   *
   * @return
   * @throws UnableToCreateGhostPadsForBinException
   */
  @Override
  protected Pad getSrcPad() throws UnableToCreateGhostPadsForBinException {
    return queue.getStaticPad(GStreamerProperties.SRC);
  }

  @Override
  public boolean isVideoDevice() {
    return true;
  }
}
