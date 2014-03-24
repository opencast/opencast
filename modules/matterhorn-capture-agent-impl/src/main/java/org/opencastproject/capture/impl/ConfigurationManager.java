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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.util.XProperties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for retrieving, storing and accessing both local and centralised configuration files for the CaptureAgent. Uses
 * java.util.Properties to store configuration file which can read/write INI style config files.
 *
 * FIXME: Is this class thread safe (answer: currently, no)? Does it need to be? (jt)
 */
public class ConfigurationManager implements ManagedService {

  /** slf4j logging */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

  /** Hashtable that represents config file in memory */
  private XProperties properties = new XProperties();

  /** should point to a centralised config file */
  private URL url;

  /**
   * Timer that will every at a specified interval to retrieve the centralised configuration file from a server
   */
  private Timer timer;

  /** Is set to true when the Config Manager has been updated by felix populating its properties. **/
  private boolean initialized = false;

  /**
   * Used to store the listeners that need to be updated when a change is made to the properties of this
   * ConfigurationManager.
   **/
  private List<ConfigurationManagerListener> listeners  = Collections.synchronizedList(new LinkedList<ConfigurationManagerListener>());

  /** If this ConfigurationManager has had its properties updated this will be initialised. **/
  public boolean isInitialized() {
    return initialized;
  }

  public void activate(ComponentContext ctx) {
    if (ctx != null) {
      properties.setBundleContext(ctx.getBundleContext());
    }
  }

  public void deactivate() {
    if (timer != null) {
      timer.cancel();
    }
    properties.setBundleContext(null);
  }

   private void updateTimezone(XProperties properties) {
    Calendar cal = Calendar.getInstance();
    properties.setProperty(
            "capture.device.timezone.offset",
            Long.toString((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET))
                    / (1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS)));
    properties.setProperty("capture.device.timezone", TimeZone.getDefault().getID());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void updated(Dictionary props) throws ConfigurationException {
    if (props == null) {
      logger.debug("Null properties in updated!");
      return;
    }
    logger.debug("Merging new properties into in-memory structure.");

    // Filter out all of the non-string objects from the incoming dictionary
    // If we don't do this then serialization of the RecordingImpl in CaptureAgentImpl doesn't work
    Enumeration<Object> keys = props.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = props.get(key);
      if (String.class.isInstance(key) && String.class.isInstance(value)) {
        // Trim the value before we allow it to end up in the properties.
        properties.put(key, ((String)value).trim());
      }
    }

    // Attempt to parse the location of the configuration server
    try {
      url = new URL(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_REMOTE_ENDPOINT_URL));
    } catch (MalformedURLException e) {
      logger.warn("Malformed URL for {}, disabling polling.", CaptureParameters.CAPTURE_CONFIG_REMOTE_ENDPOINT_URL);
    }

    // If there's no URL then throw up an info just to be safe
    if (url == null) {
      logger.info("No remote configuration endpoint was found, relying on local config.");
    }

    createCoreDirectories();

    // Grab the remote config file, dump it to disk and then merge it into the current config
    Properties server = retrieveConfigFromServer();
    if (server != null) {
      writeConfigFileToDisk();
      merge(server, true);
    }

    // Shut down the old timer if it exists
    if (timer != null) {
      timer.cancel();
    }

    // if reload property specified, query server for update at that interval
    String reload = getItem(CaptureParameters.CAPTURE_CONFIG_REMOTE_POLLING_INTERVAL);
    if (url != null && reload != null) {
      long delay = 0;
      try {
        // Times in the config file are in seconds, so don't forget to multiply by 1000 later
        delay = Long.parseLong(reload);
        if (delay < 1) {
          logger.info("Polling time has been set to less than 1 second, polling disabled.");
          return;
        }
        delay = delay * 1000L;

        timer = new Timer();
        timer.schedule(new UpdateConfig(), delay, delay);
      } catch (NumberFormatException e) {
        logger.warn("Invalid polling time for parameter {}.", CaptureParameters.CAPTURE_CONFIG_REMOTE_POLLING_INTERVAL);
      }
    }

    /**
     * This synchronized is required! If a new listener were to add itself to the list of observers such that it missed
     * getting called in the refreshes and initialization was still false, refresh would not be automatically called
     * when it registers. This would result in refresh never being called for that listener.
     **/
    synchronized (listeners) {
      for (ConfigurationManagerListener listener : listeners) {
        RefreshRunner refreshRunner = new RefreshRunner(listener);
        Thread thread = new Thread(refreshRunner);
        thread.start();
      }
      // Set initialized to true so that every listener registered after this point will be updated immediately.
      initialized = true;
    }
  }

  /**
   * A thread to run the refresh for each ConfigurationManagerListener. This will ensure that the listeners don't get
   * blocked waiting for another one to finish executing hence causing deadlock.
   **/
  class RefreshRunner implements Runnable {
    /** The listener to call refresh on. **/
    private ConfigurationManagerListener listener = null;

    public RefreshRunner(ConfigurationManagerListener listener) {
      this.listener = listener;
    }

    @Override
    public void run() {
      if (listener != null) {
        listener.refresh();
        logger.info("ConfigurationManager has refreshed " + listener);
      }
    }
  }

  /**
   * Creates the core Opencast directories.
   */
  protected void createCoreDirectories() {
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_CACHE_URL, "cache" + File.separator + "captures");
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_VOLATILE_URL, "volatile");
  }

  /**
   * Creates a file or directory.
   *
   * @param key
   *          The key to set in this configuration manager. Key is set equal to name.
   * @param fallback
   *          The directory structure that should be created under java.io.tmpdir should the key not be found in the
   *          configuration data.
   */
  protected void createFileObj(String key, String fallback) {
    if (this.getItem(key) == null) {
      File parent = new File(System.getProperty("java.io.tmpdir"));
      File dir = new File(parent, fallback);
      this.setItem(key, dir.getAbsolutePath());
      logger.warn("Unable to find value for key {} defaulting to {}.", key, dir.getAbsolutePath());
    }

    File target = null;
    try {
      target = new File(this.getItem(key));
      FileUtils.forceMkdir(target);
      if (!target.exists()) {
        throw new RuntimeException("Unable to create directory " + target + ".");
      }
      this.setItem(key, target.toString());
    } catch (IOException e) {
      logger.error("Unable to create directory: {}, because {} happened.", target, e);
    } catch (NullPointerException e) {
      logger.error("No value found for key {}.", key);
    }
  }

  /**
   * Retrieve property for configuration.
   *
   * @param key
   *          the key to retrieve from the property list.
   * @return the value corresponding to the key.
   */
  public String getItem(String key) {
    if (key == null) {
      return null;
    } else if (StringUtils.equals(key, "capture.device.timezone.offset")) {
      Calendar cal = Calendar.getInstance();
      return Long.toString((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET))
                      / (1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS));
    } else if (StringUtils.equals(key, "capture.device.timezone")) {
      return TimeZone.getDefault().getID();
    } else {
      return properties.getProperty(key);
    }
  }

  /**
   * Returns the value of an expanded variable
   *
   * @param variable
   *          The name of the variable (ie, java.io.tmpdir, or M2_REPO)
   * @return The value of that variable, or null if the variable is not found
   */
  public String getVariable(String variable) {
    return properties.expandVariable(variable);
  }

  /**
   * Retrieve property for configuration. The return value for this function do *not* have its variable(s) expanded.
   *
   * @param key
   *          the key to retrieve from the property list.
   * @return the value corresponding to the key.
   */
  public String getUninterpretedItem(String key) {
    if (key == null) {
      return null;
    } else if (StringUtils.equals(key, "capture.device.timezone.offset")) {
      Calendar cal = Calendar.getInstance();
      return Long.toString((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET))
                      / (1 * CaptureParameters.MINUTES * CaptureParameters.MILLISECONDS));
    } else if (StringUtils.equals(key, "capture.device.timezone")) {
      return TimeZone.getDefault().getID();
    } else {
      return properties.getUninterpretedProperty(key);
    }
  }

  /**
   * Add a key, value pair to the property list.
   *
   * @param key
   *          the key to be placed in the properties list.
   * @param value
   *          the corresponding value.
   */
  public void setItem(String key, String value) {
    if (key == null) {
      return;
    }

    if (value != null) {
      // this will overwrite the previous value is there is a conflict
      properties.setProperty(key, value);
    } else {
      properties.remove(key);
    }
  }

  /**
   * Read a remote properties file and load it into memory. The URL it attempts to read from is defined by
   * CaptureParameters.CAPTURE_CONFIG_ENDPOINT_URL
   *
   * @return The properties (if any) fetched from the server
   * @see org.opencastproject.capture.CaptureParameters#CAPTURE_CONFIG_REMOTE_ENDPOINT_URL
   */
  Properties retrieveConfigFromServer() {
    if (url == null) {
      return null;
    }

    // TODO: This URL might be protected. In this case, the proper headers (e.g. X509) would need to be in place.
    // This isn't something that needs to be fixed today, but it needs to be addressed project-wide at some point
    // (soon!). (jt)
    Properties p = new Properties();
    InputStream is = null;
    try {
      URLConnection urlc = url.openConnection();
      is = urlc.getInputStream();
      p.load(is);
    } catch (Exception e) {
      logger.warn("Could not get config file from server: {}.", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
    return p;
  }

  /**
   * Stores a local copy of the properties on disk.
   *
   * @see org.opencastproject.capture.CaptureParameters#CAPTURE_CONFIG_CACHE_URL
   */
  void writeConfigFileToDisk() {
    File cachedConfig = new File(properties.getProperty(CaptureParameters.CAPTURE_CONFIG_CACHE_URL));

    FileOutputStream fout = null;
    try {
      if (!cachedConfig.isFile()) {
        if (cachedConfig.getParentFile().mkdirs()) {
          if (!cachedConfig.createNewFile()) {
            logger.warn("Unable to create the cached config file: " + cachedConfig.getAbsolutePath());
          }
        } else {
          logger.warn("Unable to create the directory: " + cachedConfig.getParentFile().getAbsolutePath());
        }
      }
      fout = new FileOutputStream(cachedConfig);
      properties.store(fout, "Autogenerated config file, do not edit.");
    } catch (Exception e) {
      logger.warn("Could not write config file to disk: {}.", e);
    } finally {
      IOUtils.closeQuietly(fout);
    }
  }

  /**
   * Returns a Dictionary of all the properties associated with this configuration manager.
   *
   * @return the key/value pair mapping.
   */
  public XProperties getAllProperties() {
    XProperties cloneProps = (XProperties) properties.clone();
    updateTimezone(cloneProps);
    return cloneProps;
  }

  /**
   * Filters the capabilities (mappings between friendly names and recording devices) from this agent's properties
   *
   * @return A Properties object containing the capabilities --friendly names as keys and device locations as values
   */
  public Properties getCapabilities() {
    Properties capabilities = new Properties();

    String names = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (names == null) {
      logger.error("Null friendly name list.  Capabilities filtering aborted.");
      return null;
    }
    capabilities.put(CaptureParameters.CAPTURE_DEVICE_NAMES, names);
    // Get the names and setup a hash map of them
    String[] friendlyNames = names.split(",");
    HashMap<String, Integer> propertyCounts = new HashMap<String, Integer>();
    for (String name : friendlyNames) {
      propertyCounts.put(name, 0);
    }

    // For each key
    for (String key : properties.stringPropertyNames()) {
      // For each device
      for (String name : friendlyNames) {
        String check = CaptureParameters.CAPTURE_DEVICE_PREFIX + name;
        // If the key looks like a device prefix + the name, copy it
        if (key.contains(check)) {
          String property = properties.getProperty(key);
          if (property == null) {
            logger.error("Unable to expand variable in value for key {}, returning null!", key);
            return null;
          }
          capabilities.setProperty(key, property);
          propertyCounts.put(name, propertyCounts.get(name) + 1);
        }
      }
    }

    // Check to make sure the counts are all correct, otherwise return null/error
    for (String name : friendlyNames) {
      // TODO: This is a stupid check, how else can we do this? They might have three properties, but none of the
      // required ones...
      if (propertyCounts.get(name) < 3) {
        logger.error("Invalid configuration data for device {}, agent capabilities are null!", name);
        return null;
      }
    }

    return capabilities;
  }

  /**
   * Merges the given Properties with the ConfigurationManager's properties. Will not overwrite the ConfigurationManager
   * if specified.
   *
   * @param p
   *          Properties object to be merged with ConfigurationManager.
   * @param overwrite
   *          true if this should overwrite the ConfigurationManager's properties, false if not.
   * @return the merged properties.
   */
  public XProperties merge(Properties p, boolean overwrite) {
    // if no properties are specified, just return current configuration
    if (p == null) {
      return getAllProperties();
    }
    // overwrite the current properties in the ConfigurationManager
    if (overwrite) {
      for (String key : p.stringPropertyNames()) {
        properties.setProperty(key, p.getProperty(key));
      }
      return getAllProperties();
    }
    // do not overwrite the ConfigurationManager, but merge the properties
    else {
      XProperties merged = getAllProperties();
      for (String key : p.stringPropertyNames()) {
        String property = p.getProperty(key);
        if (property != null) {
          merged.setProperty(key, property);
        } else {
          logger.error("Unable to merge properties!");
          return null;
        }
      }
      return merged;
    }

  }

  /**
   * Registers a ConfigurationManagerListener with this ConfigurationManager so that when the properties are updated,
   * each listener will be updated automatically.
   *
   * @param ConfigurationManagerListener
   *          A new listener to update when the configuration properties are updated.
   **/
  public void registerListener(ConfigurationManagerListener listener) {
    listeners.add(listener);
    if (isInitialized()) {
      listener.refresh();
    }
  }

  /**
   * Used in a timer that will fire every specified time interval to attempt to get a new version of the capture
   * configuration from a centralised server.
   */
  class UpdateConfig extends TimerTask {

    @Override
    public void run() {
      Properties server = retrieveConfigFromServer();
      if (server != null) {
        writeConfigFileToDisk();
        merge(server, true);
      }
    }
  }

}
