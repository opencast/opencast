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
package org.opencastproject.terminationstate.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractJobTerminationStateServiceTest {
  private static final String LOCALHOST = "http://localhost:8080";

  private ServiceRegistryJpaImpl serviceRegistry;

  @Before
  public void setUp() throws Exception {
    serviceRegistry = EasyMock.createMock(ServiceRegistryJpaImpl.class);
    EasyMock.expect(serviceRegistry.getRegistryHostname()).andReturn(LOCALHOST).anyTimes();
    EasyMock.expect(serviceRegistry.countByHost(null, LOCALHOST, Job.Status.RUNNING)).andReturn(3L).once();
    EasyMock.expect(serviceRegistry.countByHost(null, LOCALHOST, Job.Status.RUNNING)).andReturn(0L).once();
    EasyMock.replay(serviceRegistry);
  }

  @Test
  public void testReadyToTerminate() throws Exception {
    AbstractJobTerminationStateService service = new AbstractJobTerminationStateService() { };
    service.setServiceRegistry(serviceRegistry);

    // jobs running, state NONE
    Assert.assertEquals(false, service.readyToTerminate());
    // jobs running state WAIT
    service.setState(TerminationStateService.TerminationState.WAIT);
    Assert.assertEquals(false, service.readyToTerminate());
    // no jobs running state WAIT
    Assert.assertEquals(true, service.readyToTerminate());
    Assert.assertEquals(TerminationStateService.TerminationState.READY, service.getState());
  }
}
