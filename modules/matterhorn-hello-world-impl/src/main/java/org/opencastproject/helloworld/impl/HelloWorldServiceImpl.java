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
package org.opencastproject.helloworld.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opencastproject.helloworld.api.HelloWorldService;

/**
 * A simple tutorial class to learn about Matterhorn Services
 */
public class HelloWorldServiceImpl implements HelloWorldService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldServiceImpl.class);

  public String helloWorld() {
    logger.info("Hello World");
    return "Hello World";
  }

  public String helloName(String name) {
    logger.info("Name is {}.", name);
    if ("".equals(name)) {
      return "Hello!";
    }
    return "Hello " + name + "!";
  }
}
