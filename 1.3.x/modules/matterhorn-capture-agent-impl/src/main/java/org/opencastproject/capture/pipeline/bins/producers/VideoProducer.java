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

import java.util.Properties;

/**
 * TODO: Comment me!
 */
public abstract class VideoProducer extends ProducerBin {

  // Filter used to make a change in the FPS in a video output
  protected Element fpsfilter;

  // Corrects the time stamps for the output media after an FPS change.
  protected Element videorate;

  public VideoProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /**
   * The main difference between this and a general Producer is that we will need a caps filter to support being able to
   * change the FPS of the output media and a videorate Element to fix the output media's timestamp in case of an fps
   * change.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if any of the GStreamer Element's modules are not installed.
   **/
  @Override
  protected void createElements() throws UnableToCreateElementException {
    super.createElements();
    createVideoRate();
    createFramerateCaps();
  }

  /**
   * Creates a videorate GST Element that adjusts the timestamps in case of a FPS change in the output file.
   * 
   * @throws UnableToCreateElementException
   *           If the module required for videorate Element is not installed.
   **/
  private void createVideoRate() throws UnableToCreateElementException {
    videorate = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VIDEORATE, null);
  }

  /**
   * Creates a CAPs filter to change the FPS of the incoming media.
   * 
   * @throws UnableToCreateElementException
   *           Thrown if the capsfilter module is not installed.
   **/
  private void createFramerateCaps() throws UnableToCreateElementException {
    fpsfilter = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.CAPSFILTER, null);
  }

  /**
   * Set the FPS capsfilter properties.
   * 
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Thrown if the fpsfilter is null.
   **/
  @Override
  protected void setElementProperties() throws UnableToSetElementPropertyBecauseElementWasNullException {
    if (fpsfilter == null) {
      throw new UnableToSetElementPropertyBecauseElementWasNullException(fpsfilter, "Caps");
    }
    Caps fpsCaps;
    if (captureDeviceProperties.getFramerate() != null) {
      // Setup the user specified FPS
      fpsCaps = new Caps(GStreamerProperties.VIDEO_X_RAW_YUV + ", " + GStreamerProperties.FRAMERATE + "="
              + captureDeviceProperties.getFramerate() + "/1");
      logger.debug("{} fps: {}", captureDevice.getName(), captureDeviceProperties.getFramerate());
    } else {
      // No FPS is set, so set the Caps to any so that it just passes through data.
      fpsCaps = Caps.anyCaps();
    }
    fpsfilter.setCaps(fpsCaps);
  }

  /**
   * Returns the frame per second filter.
   * 
   * @return the fpsfilter
   */
  public Element getFpsfilter() {
    return fpsfilter;
  }

  /**
   * Returns the producer's video rate.
   * 
   * @return the videorate
   */
  public Element getVideorate() {
    return videorate;
  }

  /** Set the isVideoDevice return value to true so that all descendants will be attached to video Consumers. **/
  @Override
  public boolean isVideoDevice() {
    return true;
  }

}
