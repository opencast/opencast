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
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import com.sun.jna.Pointer;

import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.gstreamer.Buffer;
import org.gstreamer.Bus.STATE_CHANGED;
import org.gstreamer.Caps;
import org.gstreamer.ClockTime;
import org.gstreamer.Element;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.State;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.AppSrc;
import org.gstreamer.elements.AppSrc.NEED_DATA;

import java.io.File;
import java.util.Properties;

/**
 * Extended {@link V4LProducer} for Epiphan VGA2USB devices. {@code EpiphanVGA2USBV4LProducer} will create a
 * {@link EpiphanVGA2USBV4LSubDeviceBin} where to grab video signal from and a {@link EpiphanVGA2USBV4LSubBin} if
 * epiphan device is unplugged. This concept is needed, because Epiphan kernel module does not support unplugging VGA
 * cable.
 */
public class EpiphanVGA2USBV4LProducer extends V4LProducer {

  /** The default resolution (width). */
  public static final int DEFAULT_CAPTURE_WIDTH = 1024;

  /** The default resolution (height). */

  public static final int DEFAULT_CAPTURE_HEIGHT = 768;
  /** The default framerate. */

  public static final int DEFAULT_CAPTURE_FRAMERATE = 25;

  /** The default start time for device bin (in seconds). */
  private static final int DEVICEBIN_START_TIME = 5;

  /** The default start time for sub bin (in seconds). */
  private static final int SUBBIN_START_TIME = -1;

  /** Device sub bin. */
  private EpiphanVGA2USBV4LSubDeviceBin deviceBin;

  /** Secondary sub bin. */
  private EpiphanVGA2USBV4LSubBin subBin;

  /** Thread poll permanently if vga signal was lost to restore it (when replugged the cable). */
  private EpiphanPoll epiphanPoll;

  /** Elements. */
  private Element identity;

  private Element colorspace;

  /** Source element. */
  private AppSrc src;

  /** Caps (from device or default). */
  private String caps;

  /**
   * Creates a Producer specifically designed to captured from the Epiphan VGA2USB cards to the main pipeline.
   * 
   * The video data will be grabed by AppSrc from {@link EpiphanVGA2USBV4LSubDeviceBin} or
   * {@link EpiphanVGA2USBV4LSubBin} if the VGA cable was unplugged.
   * 
   * @param captureDevice
   *          The VGA2USB {@code CaptureDevice} to create a source out of
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           If the Elements are not created before we try to set their properties this Exception will be thrown.
   * @throws UnableToCreateGhostPadsForBinException
   *           If this Producer cannot create its ghost pads this Exception will be thrown.
   * @throws UnableToLinkGStreamerElementsException
   *           If our elements fail to link together we will throw an exception.
   * @throws CaptureDeviceNullPointerException
   *           The captureDevice parameter is required and this Exception will be thrown if it is null.
   * @throws UnableToCreateElementException
   *           If the required GStreamer Modules are not installed to create all of the Elements this Exception will be
   *           thrown.
   */
  public EpiphanVGA2USBV4LProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {

    super(captureDevice, properties);

    // creates Epiphan sub bin
    deviceBin = new EpiphanVGA2USBV4LSubDeviceBin(captureDevice, getCaps());

    // get fallback property (if property set, image will be shown when loosing vga signal)
    String fallbackPng = properties.getProperty(CaptureParameters.FALLBACK_PNG);
    if (fallbackPng != null) {
      if (!new File(fallbackPng).isFile()) {
        logger.warn("'" + CaptureParameters.FALLBACK_PNG + "' does not reference a png file (" + fallbackPng + "). "
                + "VideoTestSrc will be shown by loosing VGA signal!");
        fallbackPng = null;
      }
    }

    // create secondary sub bin
    if (fallbackPng != null) {
      subBin = new EpiphanVGA2USBV4LSubPngBin(captureDevice, getCaps(), fallbackPng);
    } else {
      subBin = new EpiphanVGA2USBV4LSubTestSrcBin(captureDevice, getCaps());
    }
    // link AppSrc to sub bin AppSinks
    linkAppSrcToSink();
    // propagate state changes to sub bins
    propagateStateChangeToSubBin();

    // start device polling thread
    epiphanPoll = new EpiphanPoll(captureDevice.getLocation());
  }

  /**
   * Returns the device bin
   * 
   * @return the bin
   */
  public EpiphanVGA2USBV4LSubDeviceBin getDeviceBin() {
    return deviceBin;
  }

  /**
   * Returns the sub bin.
   * 
   * @return the bin
   */
  public EpiphanVGA2USBV4LSubBin getSubBin() {
    return subBin;
  }

  /**
   * Returns the epiphan polling thread.
   * 
   * @return the polling thread
   */
  public Thread getEpiphanPoll() {
    return epiphanPoll;
  }

  /**
   * Returns the app source.
   * 
   * @return the source
   */
  public AppSrc getSource() {
    return src;
  }

  /**
   * Returns the color space.
   * 
   * @return the color space
   */
  public Element getColorspace() {
    return colorspace;
  }

  /**
   * Returns the identity.
   * 
   * @return the identity
   */
  public Element getIdentity() {
    return identity;
  }
  
  /**
   * Create all of the Elements that we will need to grab videodata from sub bins.
   * 
   * @throws UnableToCreateElementException
   *           If any of the Elements fail to be created because the GStreamer module for the Element isn't present then
   *           this Exception will be thrown.
   */
  @Override
  protected void createElements() throws UnableToCreateElementException {
    src = (AppSrc) GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.APPSRC, captureDevice.getFriendlyName() + "_appsrc");
    identity = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.IDENTITY, captureDevice.getFriendlyName() + "_identity");
    colorspace = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.FFMPEGCOLORSPACE, captureDevice.getFriendlyName() + "_ffmpegcolorspace");
    videorate = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.VIDEORATE, captureDevice.getFriendlyName() + "_videorate");
  }

  /**
   * Set element properties.
   */
  @Override
  protected void setElementProperties() {
    src.set(GStreamerProperties.IS_LIVE, "true");
    src.set(GStreamerProperties.DO_TIMESTAP, "true");
    src.set(GStreamerProperties.BLOCK, "true");
    src.setStreamType(AppSrc.Type.STREAM);
    src.setCaps(Caps.fromString(getCaps()));
    identity.set(GStreamerProperties.SINGLE_SEGMENT, "true");
  }

  /**
   * Add all elements to bin.
   */
  @Override
  protected void addElementsToBin() {
    bin.addMany(src, identity, videorate, colorspace);
  }

  /**
   * Link elements.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           if Elements can not be linked together.
   */
  @Override
  public void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!src.link(identity)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, src, identity);
    }
    if (!identity.link(videorate)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, identity, videorate);
    }
    if (!videorate.link(colorspace)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, colorspace);
    }
  }

  /**
   * Return the ffmpegcolorspace as the sink for our Epiphan source bin to be used to create ghost pads to connect this
   * Producer to the tee in {@code CaptureDeviceBin} and from that tee to the Consumers.
   * 
   * @return ffmpegcolorspace srcpad
   */
  @Override
  public Pad getSrcPad() {
    return colorspace.getStaticPad(GStreamerProperties.SRC);
  }

  /**
   * Returns Gstreamer device caps or defaults.
   * 
   * @return Caps
   */
  protected String getCaps() {

    if (caps == null || caps.isEmpty()) {
      try {
        V4LInfo v4linfo = JV4LInfo.getV4LInfo(captureDevice.getLocation());
        
        // test equality of min and max width (or height)
        // new epiphan kernel module can get resolution information without vga cable pluged in
        // so it returns possible min and max value of epiphan capture device
        if (v4linfo.getVideoCapability().getMinwidth() != v4linfo.getVideoCapability().getMaxwidth()) {
          throw new JV4LInfoException("No VGA-signal!");
        }
        
        int width = v4linfo.getVideoCapability().getMaxwidth();
        int height = v4linfo.getVideoCapability().getMaxheight();
        caps = GStreamerProperties.VIDEO_X_RAW_YUV + ", " + GStreamerProperties.WIDTH + "=" + width + ", "
                + GStreamerProperties.HEIGHT + "=" + height + ", format=(fourcc)I420, " + // this part is needed by
                                                                                          // AppSrc
                // static framerate, otherwise the pipeline will not start
                // framerate limitations should be done in ConsumerBin
                GStreamerProperties.FRAMERATE + "=" + DEFAULT_CAPTURE_FRAMERATE + "/1";

      } catch (JV4LInfoException e) {
        caps = GStreamerProperties.VIDEO_X_RAW_YUV + ", " + GStreamerProperties.WIDTH + "=" + DEFAULT_CAPTURE_WIDTH
                + ", " + GStreamerProperties.HEIGHT + "=" + DEFAULT_CAPTURE_HEIGHT + ", format=(fourcc)I420, " + // this
                                                                                                                 // part
                                                                                                                 // is
                                                                                                                 // needed
                                                                                                                 // by
                                                                                                                 // AppSrc
                // static framerate, otherwise the pipeline will not start
                // framerate limitations should be done in ConsumerBin
                GStreamerProperties.FRAMERATE + "=" + DEFAULT_CAPTURE_FRAMERATE + "/1";
      }
    }
    return caps;
  }

  /**
   * Link sub bins AppSinks to AppSrc.
   */
  protected void linkAppSrcToSink() {
    src.connect(new NEED_DATA() {

      @Override
      public void needData(Element elem, int size, Pointer userData) {
        AppSrc src = (AppSrc) elem;
        AppSink sink = null;
        Buffer buffer = null;
        try {
          // try to get buffer from epiphan pipeline
          sink = deviceBin.getSink();
          if (sink == null)
            throw new NullPointerException("AppSink is null");
          if (sink.isEOS())
            throw new IllegalStateException("AppSink is EOS");
          buffer = sink.pullBuffer();
          if (buffer == null)
            throw new NullPointerException("Buffer is null");
        } catch (Exception ex) {
          //logger.debug(ex.getMessage());
          // epiphan pipeline is down, try to get buffer from testsrc pipeline
          sink = subBin.getSink();
          if (sink == null)
            src.endOfStream();
          buffer = sink.pullBuffer();
          if (buffer == null) {
            src.endOfStream();
            return;
          }
        }
        buffer.setTimestamp(ClockTime.NONE);
        src.pushBuffer(buffer);
        buffer.dispose();
      }
    });
  }

  /**
   * Propagate Pipeline State changes to sub bins.
   */
  protected void propagateStateChangeToSubBin() {
    src.getBus().connect(new STATE_CHANGED() {

      @Override
      public void stateChanged(GstObject source, State old, State current, State pending) {
        logger.debug("Pipeline state change. {}", old.toString() + "," + current.toString() + "," + pending.toString());
        if (source == src) {

          if (old == State.NULL) {
            // start sub bins
            subBin.start(SUBBIN_START_TIME);
            deviceBin.start(DEVICEBIN_START_TIME);
            epiphanPoll.start();
          }

          if (current == State.NULL) {
            // stop sub bins
            epiphanPoll.stopPolling();
            subBin.stop();
            deviceBin.stop();
          }
        }
      }
    });
  }

  /**
   * Send an EOS to all of the source elements for this Bin.
   **/
  @Override
  public void shutdown() {
    // Send the EOS to our Epiphan Device bin
    deviceBin.shutdown();
    // Send the EOS to our backup source.
    subBin.shutdown();
    // Kill the polling thread.
    epiphanPoll.stopPolling();
  }
  
  /** TODO - Make this part platform independent **/
  /**
   * When we have lost a VGA signal, this method can be continually executed to test for a new signal.
   * 
   * @param device
   *          the absolute path to the device
   * @return true if there is a VGA signal
   */
  protected static synchronized boolean checkEpiphan(String device) {
    try {
      V4LInfo v4linfo = JV4LInfo.getV4LInfo(device);
      String deviceName = v4linfo.getVideoCapability().getName();
      
      if (deviceName.contains("VGA2USB") || deviceName.contains("VGA2PCI")) {
        return true;
      }
    } catch (JV4LInfoException e) {
      
      return false;
    }
    return false;
  }

  /**
   * Epiphan polling thread. Restore Epiphan sub pipeline, if the signal has been reconnected.
   */
  class EpiphanPoll extends Thread {

    private static final int DELAY_BETWEEN_POLLS = 1000;

    /** Location of the Epiphan device */
    private String location;

    /** Keeps track of when the thread should stop polling gracefully. **/
    private boolean polling = true;
    
    public EpiphanPoll(String location) {
      this.location = location;
      polling = true;
    }

    public synchronized void stopPolling() {
      polling = false;
    }

    public void run() {

      logger.debug("Start Epiphan VGA2USB polling thread!");

      while (!interrupted() && polling) {
        logger.debug("Thread not interrupted. Device: " + deviceBin.isBroken() + " Epiphan: " + checkEpiphan(location));
        if (deviceBin.isBroken() && checkEpiphan(location)) {
          logger.debug("Device broken, attempting to reconnect pipeline.");
          try {
            EpiphanVGA2USBV4LSubDeviceBin newBin = new EpiphanVGA2USBV4LSubDeviceBin(captureDevice, caps);
            if (!newBin.start(DEVICEBIN_START_TIME)) {
              newBin.stop();
              logger.debug("Can not start Epiphan VGA2USB pipeline!");
            } else { 
              deviceBin = newBin;
              logger.debug("Epiphan VGA2USB pipeline restored!");
            }
          } catch (UnableToLinkGStreamerElementsException ex) {
            logger.error("Can not create epiphan bin!", ex);
          } catch (UnableToCreateElementException ex) {
            logger.error("Can not create epiphan bin!", ex);
          }
        }

        try {
          sleep(DELAY_BETWEEN_POLLS);
        } catch (InterruptedException ex) {
          interrupt();
        }
      }
      logger.debug("Shutting down Epiphan VGA2USB polling thread!");
    }
  }
}
