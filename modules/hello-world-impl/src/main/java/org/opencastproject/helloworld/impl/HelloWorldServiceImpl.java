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

package org.opencastproject.helloworld.impl;

import org.opencastproject.helloworld.api.HelloWorldService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * A simple tutorial class to learn about Opencast Services
 */
@Component(
    property = {
        "service.description=Hello World Service"
    },
    immediate = true,
    service = HelloWorldService.class
)
public class HelloWorldServiceImpl implements HelloWorldService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldServiceImpl.class);

  /** Message to print */
  private String message = "Hello World";

  /**
   * This method is called when the bundle is activated (@Activate)
   * or whenever the service's configuration file is modified (@Modified).
   * The configuration file is determined by the class name:
   *   etc/org.opencastproject.helloworld.impl.HelloWorldServiceImpl.cfg
   */
  @Activate
  @Modified
  void activate(Map<String, Object> properties) {
    // Load service configuration
    message = Objects.toString(properties.get("message"), "Hello World");

    // Add some logging
    logger.debug("Activated/Updated hello world service");
  }

  public String helloWorld() {
    logger.info(message);
    return message;
  }

  public String helloName(String name) {
    logger.info("Name is {}.", name);
    if ("".equals(name)) {
      return "Hello!";
    }
    return "Hello " + name + "!";
  }
}
