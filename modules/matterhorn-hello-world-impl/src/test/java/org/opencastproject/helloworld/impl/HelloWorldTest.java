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

import org.opencastproject.helloworld.api.HelloWorldService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for Hello World Tutorial
 */
public class HelloWorldTest {

  private HelloWorldService service;


  /**
   * Setup for the Hello World Service
   *
   * @throws Exception
   *           if setup fails
   */
  @Before
  public void setUp() throws Exception {
    service = new HelloWorldServiceImpl();
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    service = null;
  }

  @Test
  public void testHelloWorld() throws Exception {

    assertNotNull(service.helloWorld(null));
    assertNotNull(service.helloWorld("John"));

    assertEquals(service.helloWorld(null), "Hello World!");
    assertEquals(service.helloWorld("Tom"), "Hello Tom!");
  }

}
