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
package org.opencastproject.capture.impl.monitoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.api.ConfidenceMonitor;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.capture.impl.ConfigurationManagerListener;
import org.opencastproject.capture.impl.monitoring.MonitoringEntry.MONITORING_TYPE;
import org.opencastproject.capture.pipeline.CannotFindSourceFileOrDeviceException;
import org.opencastproject.capture.pipeline.MonitoringGStreamerPipeline;
import org.opencastproject.capture.pipeline.UnrecognizedDeviceException;
import org.opencastproject.capture.pipeline.bins.consumers.AudioMonitoringConsumer;
import org.opencastproject.util.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton Class to manage monitoring devices.
 */
public final class ConfidenceMonitorImpl implements ConfidenceMonitor, ConfigurationManagerListener {

  private Logger logger = LoggerFactory.getLogger(ConfidenceMonitorImpl.class);
  private static ConfidenceMonitorImpl instance = null;
  private static ConfigurationManager configService;
  private static List<MonitoringEntry> monitoringEntries = Collections.synchronizedList(new LinkedList<MonitoringEntry>());
  private String coreURL = null;

  /**
   * Returns an instance of ConfidenceMonitor implementation.
   * @return ConfidenceMonitor implementation
   */
  public static ConfidenceMonitorImpl getInstance() {
    return instance;
  }

  /**
   * Seta for ConfigurationManager service.
   * @param cfg ConfigurationManager
   * @throws ConfigurationException 
   */
  public void setConfigService(ConfigurationManager cfg) throws ConfigurationException {
    configService = cfg;
    configService.registerListener(this);
    refresh();
  }

  /**
   * Returns MONITORING_TYPE value from String.
   * @param type
   * @return 
   */
  public static MONITORING_TYPE getMonitoringTypeValue(String type) {
    type = type.toLowerCase().trim();
    if ("audio".equals(type)) {
      return MONITORING_TYPE.AUDIO;
    } else if ("video".equals(type)) {
      return MONITORING_TYPE.VIDEO;
    } else if ("av".equals(type)) {
      return MONITORING_TYPE.AV;
    } else {
      return MONITORING_TYPE.UNKNOWN;
    }
  }

  /**
   * Returns MonitoringEntry of the capture device with given friendlyName
   * @param friendlyName friendly Name of capture device
   * @return MonitoringEntry
   */
  MonitoringEntry getMonitoringEntry(String friendlyName) {
    for (MonitoringEntry entry : monitoringEntries) {
      if (entry.getFriendlyName().equals(friendlyName)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Creates and put the MonitoringEntry into known monitoring devices collection.
   * @param friendlyName friendly name of the capture device
   * @param type type of the capture  device (audio, video, ...)
   * @param location fileoutput location for video devices
   */
  public void createMonitoringEntry(String friendlyName, MONITORING_TYPE type, String location) {
    MonitoringEntry entry = new MonitoringEntry(friendlyName, type, location);
    monitoringEntries.add(entry);
    logger.info("Start {} monitoring for {}!", type.toString().toLowerCase(), friendlyName);
  }

  /**
   * Removes MonitoringEntry from known monitoring devices collection.
   * @param friendlyName friendly name of the capture device
   * @param type type of the capture  device (audio, video, ...)
   */
  public void removeMonitoringEntry(String friendlyName, MONITORING_TYPE type) {
    List<MonitoringEntry> entriesToRemove = new ArrayList<MonitoringEntry>();
    for (MonitoringEntry entry : monitoringEntries) {
      if (entry.getFriendlyName().equals(friendlyName)
              && (entry.getType() == type || MONITORING_TYPE.UNKNOWN == type)) {
        entriesToRemove.add(entry);
      }
    }
    monitoringEntries.removeAll(entriesToRemove);
    logger.info("Stop {} monitoring for {}!", type.toString().toLowerCase(), friendlyName);
  }

  /**
   * Removes all entiries from known monitoring devices collection.
   */
  public void removeAllMonitoringEntries() {
    monitoringEntries.clear();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ConfidenceMonitor#grabFrame(java.lang.String)
   */
  @Override
  public byte[] grabFrame(String friendlyName) {
    MonitoringEntry entry = getMonitoringEntry(friendlyName);
    if (entry == null || (entry.getType() != MONITORING_TYPE.VIDEO && entry.getType() != MONITORING_TYPE.AV)) {
      return null;
    }

    // get the image for the device specified
    String location = entry.getLocation();
    File fimage = new File(location);
    int length = (int) fimage.length();
    byte[] ibytes = new byte[length];
    try {
      // Read the bytes and dump them to the byte array
      InputStream fis = new FileInputStream(fimage);
      fis.read(ibytes, 0, length);
      fis.close();
      return ibytes;
    } catch (FileNotFoundException e) {
      logger.error("Could not read confidence image from: {}", friendlyName);
    } catch (IOException e) {
      logger.error("Confidence read error: {}", e.getMessage());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ConfidenceMonitor#getRMSValues(java.lang.String, double)
   */
  @Override
  public List<Double> getRMSValues(String friendlyName, double timestamp) {
    MonitoringEntry entry = getMonitoringEntry(friendlyName);

    if (entry == null || (entry.getType() != MONITORING_TYPE.AUDIO && entry.getType() != MONITORING_TYPE.AV)) {
      return null;
    }
    return AudioMonitoringConsumer.getRMSValues(friendlyName, timestamp);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ConfidenceMonitor#getFriendlyNames()
   */
  @Override
  public List<String> getFriendlyNames() {
    LinkedList<String> deviceList = new LinkedList<String>();
    for (MonitoringEntry entry : monitoringEntries) {
      deviceList.add(entry.getFriendlyName() + "," + entry.getType().toString().toLowerCase());
    }
    return deviceList;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.ConfidenceMonitor#getCoreUrl()
   */
  @Override
  public String getCoreUrl() {
    return coreURL;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.impl.ConfigurationManagerListener#refresh()
   */
  @Override
  public void refresh() {
    coreURL = configService.getItem(CaptureParameters.CAPTURE_CORE_URL);
    boolean confidence = Boolean.valueOf(configService.getItem(CaptureParameters.CAPTURE_CONFIDENCE_ENABLE));

    if (coreURL != null) {
      if (confidence && instance == null) {
        instance = new ConfidenceMonitorImpl();
        logger.info("Confidence monitoring enabled!");
        startMonitoring();
      }
    } else {
      if (instance != null) {
        instance = null;
        logger.info("Confidence monitoring disabled!");
      }
    }
  }

  /**
   * Create and start monitoring Pipeline without creating an recording.
   * @return true, if successful
   */
  @Override
  public boolean startMonitoring() {
    try {
      if (MonitoringGStreamerPipeline.create(configService.getAllProperties()) != null) {
        return MonitoringGStreamerPipeline.start();
      } else {
        return false;
      }
    } catch (CannotFindSourceFileOrDeviceException ex) {
      logger.error("Can not start monitoring!", ex);
    } catch (UnrecognizedDeviceException ex) {
      logger.error("Can not start monitoring!", ex);
    }
    return false;
  }

  /**
   * Stop monitoring Pipeline.
   */
  @Override
  public void stopMonitoring() {
    MonitoringGStreamerPipeline.stop();
  }
}
