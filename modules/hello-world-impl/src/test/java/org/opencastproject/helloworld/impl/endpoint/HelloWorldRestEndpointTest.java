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

package org.opencastproject.helloworld.impl.endpoint;

import org.opencastproject.helloworld.api.HelloWorldService;
import org.opencastproject.helloworld.impl.HelloWorldServiceImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Test class for Hello World Tutorial
 */
public class HelloWorldRestEndpointTest {

  private HelloWorldRestEndpoint rest;

  /**
   * Setup for the Hello World Rest Service
   */
  @Before
  public void setUp() {
    HelloWorldService service = new HelloWorldServiceImpl();
    rest = new HelloWorldRestEndpoint();
    rest.setHelloWorldService(service);
  }

  @Test
  public void testHelloWorld() throws Exception {
    Response response = rest.helloWorld();
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals("Hello World", response.getEntity());
  }

  @Test
  public void testHelloNameEmpty() throws Exception {
    Response response = rest.helloName("");
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals("Hello!", response.getEntity());
  }

  @Test
  public void testHelloName() throws Exception {
    Response response = rest.helloName("Peter");
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals("Hello Peter!", response.getEntity());
  }
}
