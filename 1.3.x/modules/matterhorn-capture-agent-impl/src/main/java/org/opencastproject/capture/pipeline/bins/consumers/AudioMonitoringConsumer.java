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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.Message;
import org.gstreamer.MessageType;
import org.gstreamer.Pad;
import org.gstreamer.PadDirection;
import org.gstreamer.State;
import org.opencastproject.capture.impl.monitoring.ConfidenceMonitorImpl;
import org.opencastproject.capture.impl.monitoring.MonitoringEntry;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElementFactory;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

/**
 * Audio monitoring consumer bin.
 */
public class AudioMonitoringConsumer extends ConsumerBin {
  
  /**
   * Simple inner class to pair RMS values with their timestamps and sort them by timestamp
   */
  private static class Pair implements Comparable<Pair> {

    private double timestamp;

    private double rms;

    // TODO: Comment me?
    public Pair(double timestamp, double rms) {
      this.timestamp = timestamp;
      this.rms = rms;
    }

    public double getTimestamp() {
      return timestamp;
    }

    public double getRMS() {
      return rms;
    }

    @Override
    public int compareTo(Pair p) {
      if (p.getTimestamp() < this.getTimestamp())
        return 1;
      else if (p.getTimestamp() > this.getTimestamp())
        return -1;
      else
        return 0;
    }

  }
  
  /** 1 sec = 1000000000 nanosec */
  public static final long GST_NANOSECONDS = 1000000000L;
  
  /** Map to store rms values for each audio device with timestamp. */
  private static HashMap<String, SortedSet<Pair>> deviceRMSValues;
  
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
    setupBusMonitoring(confidenceMonitoringProperties.getMonitoringLength(), captureDevice.getFriendlyName());
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.pipeline.bins.consumers.ConsumerBin#getSrc() 
   */
  @Override
  public Element getSrc() {
    return queue;
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
    queue = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.QUEUE, null);
    decodebin = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            GStreamerElements.DECODEBIN, null);
    level = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            "level", null);
    fakesink = GStreamerElementFactory.getInstance().createElement(captureDevice.getFriendlyName(),
            "fakesink", null);
    
    // setup structure to associate timestamps with RMS values
    if (deviceRMSValues == null) {
      deviceRMSValues = new HashMap<String, SortedSet<Pair>>();
    }
    deviceRMSValues.put(captureDevice.getFriendlyName(), new TreeSet<Pair>());
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
    level.set("message", "true");
    // interval property in nano sec. (see gstreamer docs)
    level.set("interval", "" + interval * GST_NANOSECONDS);
  }
  
  /**
   * Add gstreamer elements to the bin.
   */
  @Override
  protected void addElementsToBin() {
    bin.addMany(queue, decodebin, level, fakesink);
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
          pad.link(level.getStaticPad("sink"));
        }
      }
    });
    
    if (!queue.link(decodebin)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, decodebin);
    }
    
    if (!level.link(fakesink)) {
      throw new UnableToLinkGStreamerElementsException(captureDevice, level, fakesink);
    }
  }
  
  /**
   * Connect bus message listener to catch level element messages and get rms value from.
   * 
   * @param maxLength max rms-value-queue length
   * @param name friendly device name
   */
  private void setupBusMonitoring(final long maxLength, final String name) {
    // callback to listen for messages from the level element, giving us
    // information about the audio being recorded
    level.getBus().connect(new Bus.MESSAGE() {

      @Override
      public void busMessage(Bus bus, Message msg) {
        
        logger.debug("{}: {}", new String[] {msg.getType().toString(), msg.getStructure().toString()});
        
        if (msg.getSource().equals(level) && msg.getType() == MessageType.ELEMENT) {
          
          // message data like that:
          // 
          // level, endtime=(guint64)60103401360, timestamp=(guint64)55094784580, 
          // stream-time=(guint64)55094784580, 
          // running-time=(guint64)55094784580, duration=(guint64)5008616780, 
          // rms=(double){ -40.952087684510758, -40.984825946785662 }, 
          // peak=(double){ -36.329598612473987, -36.346987786726558 }, 
          // decay=(double){ -36.576273313948491, -36.558419474901676 };
          String data = msg.getStructure().toString();
          
          int start = data.indexOf("rms");
          int end = data.indexOf("}", start);
          String rms = data.substring(start, end + 1);
          start = rms.indexOf("{");
          end = rms.indexOf("}");
          double value = Double.parseDouble(rms.substring(start + 1, end).split(",")[0]);
          
          // add the new value (timestamp, rms) value pair to the hashmap for this device
          TreeSet<Pair> deviceRMS = (TreeSet<Pair>) deviceRMSValues.get(name);
          deviceRMS.add(new Pair(System.currentTimeMillis(), value));

          // keep the maximum number of pairs stored to be 1000 / interval
          if (deviceRMS.size() > (maxLength)) {
            deviceRMS.remove(deviceRMS.first());
          }
        } else if (msg.getSource().equals(level) && msg.getType() == MessageType.EOS) {
//          ((Element)getBin().getParent()).postMessage(msg);
          getBin().setState(State.NULL);
          
        } else if (msg.getSource().equals(level) && msg.getType() == MessageType.STATE_CHANGED) {
          
          ConfidenceMonitorImpl monitoringService = ConfidenceMonitorImpl.getInstance();
          if (monitoringService == null) return;
          // message data like that:
          //
          // GstMessageState, old-state=(GstState)GST_STATE_NULL, 
          // new-state=(GstState)GST_STATE_READY, 
          // pending-state=(GstState)GST_STATE_VOID_PENDING;
          String data = msg.getStructure().toString();
          for (String dataPart : data.split(", ")) {
            if (dataPart.startsWith("new-state=")) {
              if (dataPart.endsWith("GST_STATE_PLAYING")) {
                monitoringService.createMonitoringEntry(name, MonitoringEntry.MONITORING_TYPE.AUDIO, null);
              } else if (dataPart.endsWith("GST_STATE_NULL")) {
                monitoringService.removeMonitoringEntry(name, MonitoringEntry.MONITORING_TYPE.AUDIO);
              }
            }
          }
        }
      }
    });
  }
  
  /**
   * Return all RMS values from device 'name' that occur after Unix time 'timestamp'
   * 
   * @param name
   *          The friendly name of the device
   * @param timestamp
   *          Unix time in milliseconds marking start of RMS data
   * @return A List of RMS values that occur *after* timestamp
   */
  public static List<Double> getRMSValues(String name, double timestamp) {
    TreeSet<Pair> set = (TreeSet<Pair>) deviceRMSValues.get(name).tailSet(new Pair(timestamp, 0));
    List<Double> rmsValues = new LinkedList<Double>();
    for (Pair p : set) {
      rmsValues.add(p.getRMS());
    }
    return rmsValues;
  }
}
