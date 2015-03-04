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
package org.opencastproject.index.service.resources.list.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opencastproject.index.service.resources.list.provider.TestServiceStatistics.FINISHED_JOBS;
import static org.opencastproject.index.service.resources.list.provider.TestServiceStatistics.MEAN_QUEUETIME;
import static org.opencastproject.index.service.resources.list.provider.TestServiceStatistics.MEAN_RUNTIME;
import static org.opencastproject.index.service.resources.list.provider.TestServiceStatistics.QUEUED_JOBS;
import static org.opencastproject.index.service.resources.list.provider.TestServiceStatistics.RUNNING_JOBS;
import static org.opencastproject.index.service.resources.list.provider.TestServiceStatistics.SERVICE_TYPE;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.Service;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceState;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServicesListProviderTest {

  private ServicesListProvider servicesListProvider;
  private Map<String, Object> resultList;

  @Before
  public void setUp() throws ServiceRegistryException, ListProviderException {
    servicesListProvider = new ServicesListProvider();
    servicesListProvider.setServiceRegistry(TestServiceRegistryFactory.getStub());
    resultList = servicesListProvider.getList("", null, null);
  }

  @After
  public void cleanUp() {
    TestServiceRegistryFactory.reset();
  }

  @Test
  public void testTheServiceRegistryIsCalled() {
    TestServiceRegistryFactory.verify();
    assertNotNull(resultList);
  }

  @Test
  public void testResponseMapMeetsExpectations() throws ServiceRegistryException {
    assertEquals(2, resultList.size()); // total count, result
    Object value = resultList.get("results");
    assertNotNull(value);
  }

  @Test
  public void learningJsonArrays() throws JSONException, IOException {
    Service s = new Service(new TestServiceStatistics());

    List<Service> list = new ArrayList<Service>();
    list.add(s);
    list.add(s);
    JSONArray a = new JSONArray(list);
    JSONObject o = new JSONObject();
    o.put("test", s);
    o.put("results", a);
    JSONObject jsonObject = a.toJSONObject(a);
    System.out.println(jsonObject.toString());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testResponseJsonIsComplete() throws ParseException {
    List<Service> services = (ArrayList<Service>) resultList.get("results");
    Service service = services.get(0);
    assertEquals(MEAN_RUNTIME, service.getMeanRunTime());
    assertEquals(MEAN_QUEUETIME, service.getMeanQueueTime());
    assertEquals(ServiceState.NORMAL.name(), service.getStatus());
    assertEquals(SERVICE_TYPE, service.getName());
    assertEquals(FINISHED_JOBS, service.getCompleted().intValue());
    assertEquals(RUNNING_JOBS, service.getRunning().intValue());
    assertEquals(QUEUED_JOBS, service.getQueued().intValue());
    assertEquals(MEAN_RUNTIME, service.getMeanRunTime());
    assertEquals(MEAN_QUEUETIME, service.getMeanQueueTime());
  }
}
