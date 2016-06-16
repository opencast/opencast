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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for Hello World Tutorial
 */
public class HelloWorldServiceTest {

  private HelloWorldService service;

  /**
   * Setup for the Hello World Service
   */
  @Before
  public void setUp() {
    service = new HelloWorldServiceImpl();
  }

  @Test
  public void testHelloWorld() throws Exception {
    Assert.assertEquals("Hello World", service.helloWorld());
  }

  @Test
  public void testHelloNameEmpty() throws Exception {
    Assert.assertEquals("Hello!", service.helloName(""));
  }

  @Test
  public void testHelloName() throws Exception {
    Assert.assertEquals("Hello Johannes!", service.helloName("Johannes"));
  }
}
