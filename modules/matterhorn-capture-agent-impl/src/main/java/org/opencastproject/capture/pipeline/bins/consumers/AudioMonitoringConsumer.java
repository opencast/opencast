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
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

/**
 * Audio monitoring consumer bin.
 */
public class AudioMonitoringConsumer extends ConsumerBin {
  
  public static final String LEVEL_NAME_PREFIX = "level-";
  
  /** 1 sec = 1000000000 nanosec */
  public static final long GST_NANOSECONDS = 1000000000L;
    
  private Element decodebin;
  private Element level;
  private Element fakesink;

  /**
   * Create an audio monitoring consumer bin. 
   * Gstreamer level element generate messages about raw audio data. We catch them 
   * from bus to get the rms value from. 
   * 
   * @param captureDevice
   *          This is the properties such as codec, container, bitrate etc. of the capture device output.
   * 
   * @param properties
   *          This is the confidence monitoring properties
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           If there is a problem linking any of the Elements together that make up the SinkBin this Exception will
   *           be thrown
   * @throws UnableToCreateGhostPadsForBinException
   *           If the getSrc function returns an Element that cannot be used to create a ghostpad for this SinkBin, then
   *           this Exception is thrown. This could be due to the src being null or one that doesn't have the pads
   *           available to ghost.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the setElementPropeties is called before the createElements function then the Elements will not be
   *           created, show up as null and this Exception will be thrown.
   * @throws CaptureDeviceNullPointerException
   *           Because there are essential properties that we can't infer, such as output location, if the CaptureDevice
   *           parameter is null this exception is thrown.
   * @throws UnableToCreateElementException
   *           If the current setup is not able to create an Element because either that Element is specific to another
   *           OS or because the module for that Element is not installed this Exception is thrown.
   */
  public AudioMonitoringConsumer(CaptureDevice captureDevice, Properties properties) 
          throws UnableToLinkGStreamerElementsException, 
          UnableToCreateGhostPadsForBinException, 
          UnableToSetElementPropertyBecauseElementWasNullException, 
          CaptureDeviceNullPointerException, 
          UnableToCreateElementException {
            
    super(captureDevice, properties);
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.pipeline.bins.consumers.ConsumerBin#getSrc() 
   */
  @Override
  public Element getSrc() {
    return decodebin;
  }

  /**
   * Create queue, decodebin, level and fakesink gstreamer elements. 
   * 
   * @throws UnableToCreateElementException 
   *        Thrown if the Element cannot be created because the Element 
   *        doesn't exist on this machine.
   */
  @Override
  protected void createElements() throws UnableToCreateElementException {
    decodebin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DECODEBIN, null);
    level = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.LEVEL, LEVEL_NAME_PREFIX + captureDevice.getFriendlyName());
    fakesink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FAKESINK, null);
  }
  
  /**
   * Set level element interval property. 
   * 
   * @throws IllegalArgumentException
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   */
  @Override
  protected void setElementProperties() throws IllegalArgumentException,
          UnableToSetElementPropertyBecauseElementWasNullException {

    int interval = confidenceMonitoringProperties.getInterval();
    level.set(GStreamerProperties.MESSAGE, true);
    // interval property in nano sec. (see gstreamer docs)
    level.set(GStreamerProperties.INTERVAL, "" + (interval * GST_NANOSECONDS));
  }
  
  /**
   * Add gstreamer elements to the bin.
   */
  @Override
  protected void addElementsToBin() {
    bin.addMany(decodebin, level, fakesink);
  }
  
  /**
   * Link gstreamer elements.
   * 
   * @throws UnableToLinkGStreamerElementsException 
   *        Will be thrown, if the elements can not be linked together.
   */
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    decodebin.connect(new Element.PAD_ADDED() {

      @Override
      public void padAdded(Element elmnt, Pad pad) {
        if (pad.getDirection() == PadDirection.SRC) {
          pad.link(level.getStaticPad(GStreamerProperties.SINK));
        }
      }
    });
    
    if (!level.link(fakesink)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, level, fakesink);
    }
  }
}
