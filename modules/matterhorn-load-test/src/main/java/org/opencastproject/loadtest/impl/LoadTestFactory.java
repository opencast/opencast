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
package org.opencastproject.loadtest.impl;

import org.opencastproject.security.api.TrustedHttpClient;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

public class LoadTestFactory implements ManagedService {
  // The logger.
  private static final Logger logger = LoggerFactory.getLogger(LoadTestFactory.class);

  /** The http client used to communicate with the core */
  private TrustedHttpClient client = null;

  // Configuration for Load Testing.
  private Dictionary<String, String> configuration = null;
  
  private ComponentContext componentContext = null;
  
  /**
   * Sets the http client which this service uses to communicate with the core.
   * 
   * @param client
   *          The client object to ingest the load testing to.
   */
  void setTrustedClient(TrustedHttpClient client) {
    logger.debug("Setting TrustedHttpClient " + client.toString());
    this.client = client;
  }

  /**
   * Updates when the configuration for the load testing changes.
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   **/
  @SuppressWarnings("unchecked")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Updating Properties");
    if (properties != null) {
      logger.debug("The new properties are " + properties.toString());
      this.configuration = properties;
    } else {
      logger.warn("No default properties for Load Testing. Missing or empty {FELIX_HOME}/conf/services/org.opencastproject.loadtest.impl.LoadTestFactory.properties?");
    }
  }
   
  /**
   * Create a new load testing instance that will run by itself based on properties.
   * 
   * @param properties
   *          The configuration to use for load testing.
   * @throws ConfigurationException
   *           Thrown if no configuration is given
   */
  public void startLoadTesting(Properties properties) throws ConfigurationException {
    if (properties != null) {
        LoadTest loadTest = new LoadTest((Properties)properties.clone(), client, componentContext);
        Thread thread = new Thread(loadTest);
        thread.start();
    } else {
      throw new ConfigurationException("null", "Null configuration in updated!");
    }
  }

  /**
   * 
   * @return A properties object containing the default configuration specified in the
   *         org.opencastproject.loadtest.impl.LoadTestFactory.properties
   * @throws ConfigurationException
   *           Thrown if configuration is null.
   */
  public Properties getProperties() throws ConfigurationException {
    if (configuration != null) {
      Properties properties = new Properties();
      Enumeration<String> keys = configuration.keys();
      Enumeration<String> elements = configuration.elements();
      while (keys.hasMoreElements() && elements.hasMoreElements()) {
        String key = keys.nextElement();
        String element = elements.nextElement();
        logger.info("Key: " + key + " Element: " + element);
        properties.put(key, element);
      }
      return properties;
    } else {
      throw new ConfigurationException("null", "Null configuration in updated!");
    }
  }
  
  /**
   * Callback from the OSGi container once this service is started. This is where we register our shell commands.
   * 
   * @param ctx
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    logger.info("Activating Load Test Factory.");
    this.componentContext = componentContext;
  }

  /**
   * Shuts down the load testing.
   */
  public void deactivate() {
    while (ThreadCounter.getCount() > 0) {
      ThreadCounter.subtract();
    }
    logger.info("Deactivating Load Test Factory.");
  }
}
