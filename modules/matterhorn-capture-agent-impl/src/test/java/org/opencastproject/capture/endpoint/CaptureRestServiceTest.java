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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.CaptureAgent;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the behavior of the composer rest endpoint, using a mock composer service.
 */
public class CaptureRestServiceTest {
  private CaptureRestService service;

  @Before
  public void setUp() throws Exception {
    CaptureAgent agent = EasyMock.createNiceMock(CaptureAgent.class);

    EasyMock.expect(agent.startCapture()).andReturn("Unscheduled-12354356");

    service = new CaptureRestService();
    service.setService(agent);
  }

  @Test
  @Ignore
  public void testGetEndpoints() {
    Assert.assertEquals(200, service.startCapture().getStatus());
  }
}
