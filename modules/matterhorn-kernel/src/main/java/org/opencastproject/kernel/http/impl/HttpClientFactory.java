/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.kernel.http.impl;



import org.opencastproject.kernel.http.api.HttpClient;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/** Creates HttpClients that can be used for making requests such as GET, POST etc.*/
public class HttpClientFactory implements ManagedService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);


  /**
   * Callback from the OSGi container once this service is started. This is where we register our shell commands.
   *
   * @param ctx
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    logger.debug("Starting up");
  }

  /**
   * Deactivates the service
   */
  public void deactivate() {
    logger.debug("Shutting down");
  }

  /** Updates the properties for this service. */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {

  }

  /** Creates a new HttpClient to make requests.*/
  public HttpClient makeHttpClient() {
    return new HttpClientImpl();
  }
}
