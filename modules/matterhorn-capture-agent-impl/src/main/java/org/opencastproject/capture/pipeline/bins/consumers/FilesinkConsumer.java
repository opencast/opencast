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

import java.util.Properties;
import org.gstreamer.Element;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

public class FilesinkConsumer extends ConsumerBin {

  /**
   * FilesinkConsumer write media data into a file without encode them.
   *
   * @throws UnableToLinkGStreamerElementsException
   *           If the Elements for this Consumer cannot be linked together this Exception will be thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If the getSrc function returns a different element than the Element who has a sink for this Consumer or
   *           there are no sink pads for that Element then this Exception will be thrown.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If setElementProperties is called before createElements this exception is thrown.
   * @throws CaptureDeviceNullPointerException
   *           captureDevice parameter is necessary for creating the VideoFilesinkConsumerBin so this Exception is
   *           thrown when that parameter is null.
   * @throws UnableToCreateElementException
   *           If the current machine does not have the necessary GStreamer module installed for elements required by
   *           VideoFilesinkConsumerBin this Exception is thrown. A workaround is to choose a codec and container that
   *           is compatible with the current setup.
   */
  public FilesinkConsumer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /** Set filesink location */
  @Override
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {

    if (filesink == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(filesink, GStreamerProperties.LOCATION);
    } else if (captureDevice.getOutputPath() == null || captureDevice.getOutputPath().isEmpty()) {
      throw new IllegalArgumentException("File location must be set, it cannot be an empty String.");
    } else {
      filesink.set(GStreamerProperties.LOCATION, captureDevice.getOutputPath());
    }
  }

  /** Adds queue and filesink Elements to the bin. */
  @Override
  protected void addElementsToBin() {
    bin.addMany(queue, filesink);
  }

  /**
   * Link the queue to the filesink Element.
   * @throws UnableToLinkGStreamerElementsException
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!queue.link(filesink))
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, filesink);
  }

  /**
   * Returns the queue as the Sink for this Consumer. Will be used to create the ghostpads to link this Consumer to the
   * Producer.
   */
  @Override
  public Element getSrc() {
    return queue;
  }

  @Override
  public boolean isVideoDevice() {
    return true;
  }
}
