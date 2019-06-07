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
package org.opencastproject.runtimeinfo;

import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.impl.jpa.HostRegistrationJpaImpl;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;

import org.easymock.EasyMock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

public class HealthCheckTest {

  private RuntimeInfo runtimeInfo;
  private HostRegistrationJpaImpl hostRegistration;
  private ServiceRegistrationJpaImpl serviceReg1;
  private ServiceRegistrationJpaImpl serviceReg2;

  @Before
  public void setUp() throws Exception {
    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    hostRegistration = new HostRegistrationJpaImpl(
            "http://localhost", "127.0.0.1", "node", 1024L, 4, 4, true, false);
    serviceReg1 = new ServiceRegistrationJpaImpl(
            hostRegistration, "service1", "service1", false);
    serviceReg2 = new ServiceRegistrationJpaImpl(
            hostRegistration, "service2", "service2", false);
    List<ServiceRegistration> services = new ArrayList<>();
    services.add(serviceReg1);
    services.add(serviceReg2);
    EasyMock.expect(serviceRegistry.getRegistryHostname()).andReturn("http://localhost").anyTimes();
    EasyMock.expect(serviceRegistry.getHostRegistration(EasyMock.anyString())).andReturn(hostRegistration).anyTimes();
    EasyMock.expect(serviceRegistry.getServiceRegistrationsByHost(EasyMock.anyString())).andReturn(services).anyTimes();
    EasyMock.replay(serviceRegistry);

    runtimeInfo = new RuntimeInfo();
    runtimeInfo.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testHealthCheckResponsePass() throws Exception {
    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    EasyMock.replay(response);

    // Test pass
    String json = runtimeInfo.getHealth(response);
    JSONParser parser = new JSONParser();
    JSONObject j = (JSONObject) parser.parse(json);
    Assert.assertEquals("pass", j.get("status"));
  }

  @Test
  public void testHealthCheckResponseWarn() throws Exception {
    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    EasyMock.replay(response);

    // test warn service WARNING
    serviceReg1.setServiceState(ServiceState.WARNING);
    String json = runtimeInfo.getHealth(response);
    JSONParser parser = new JSONParser();
    JSONObject j = (JSONObject) parser.parse(json);
    Assert.assertEquals("warn", j.get("status"));
    JSONObject checks = (JSONObject) j.get("checks");
    Assert.assertNotNull(checks);
    JSONArray serviceStates = (JSONArray) checks.get("service:states");
    Assert.assertEquals(1, serviceStates.size());
    JSONObject serviceState = (JSONObject) serviceStates.get(0);
    Assert.assertEquals("WARNING", serviceState.get("observedValue"));

    // test warn service ERROR
    serviceReg2.setServiceState(ServiceState.ERROR);
    json = runtimeInfo.getHealth(response);
    parser = new JSONParser();
    j = (JSONObject) parser.parse(json);
    Assert.assertEquals("warn", j.get("status"));
    checks = (JSONObject) j.get("checks");
    Assert.assertNotNull(checks);
    serviceStates = (JSONArray) checks.get("service:states");
    Assert.assertEquals(2, serviceStates.size());
    serviceState = (JSONObject) serviceStates.get(1);
    Assert.assertEquals("ERROR", serviceState.get("observedValue"));
  }

  @Test
  public void testHealthCheckResponseFail() throws Exception {
    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    EasyMock.replay(response);

    // maintenance mode
    hostRegistration.setMaintenanceMode(true);
    String json = runtimeInfo.getHealth(response);
    JSONParser parser = new JSONParser();
    JSONObject j = (JSONObject) parser.parse(json);
    Assert.assertEquals("fail", j.get("status"));
    JSONArray notes = (JSONArray) j.get("notes");
    Assert.assertNotNull(notes);
    Assert.assertEquals(1, notes.size());
    Assert.assertTrue(notes.get(0).toString().contains("maintenance"));


    // disabled
    hostRegistration.setActive(false);
    json = runtimeInfo.getHealth(response);
    parser = new JSONParser();
    j = (JSONObject) parser.parse(json);
    Assert.assertEquals("fail", j.get("status"));
    notes = (JSONArray) j.get("notes");
    Assert.assertNotNull(notes);
    Assert.assertEquals(1, notes.size());
    Assert.assertTrue(notes.get(0).toString().contains("disabled"));
  }

}
