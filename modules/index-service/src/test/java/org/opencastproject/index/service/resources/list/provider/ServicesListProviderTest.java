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

package org.opencastproject.index.service.resources.list.provider;

import static org.junit.Assert.assertEquals;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.query.ServicesListQuery;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistrationInMemoryImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceState;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class ServicesListProviderTest {

  private ServiceRegistry serviceRegistry;
  private ServicesListProvider servicesListProvider;
  private ServicesListQuery servicesQuery;

  private static final String SERVICE_TYPE_1 = "type1";
  private static final String SERVICE_TYPE_2 = "type2";
  private static final String SERVICE_TYPE_3 = "type3";

  @Before
  public void setUp() throws Exception {
    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);

    ServiceRegistration sr1 = new ServiceRegistrationInMemoryImpl(SERVICE_TYPE_1, "host1", "service-path-1", true);
    ServiceRegistration sr2 = new ServiceRegistrationInMemoryImpl(SERVICE_TYPE_2, "host1", "service-path-2", false);
    ServiceRegistration sr3 = new ServiceRegistrationInMemoryImpl(SERVICE_TYPE_3, "host1", "service-path-3", true);
    EasyMock.expect(serviceRegistry.getServiceRegistrations()).andReturn(Arrays.asList(sr1, sr2, sr3)).anyTimes();

    servicesListProvider = new ServicesListProvider();
    servicesListProvider.setServiceRegistry(serviceRegistry);
    servicesListProvider.activate(null);

    servicesQuery = new ServicesListQuery();

    EasyMock.replay(serviceRegistry);
  }

  @Test
  public void testListNames() throws ListProviderException {
    assertEquals(3, servicesListProvider.getList("", servicesQuery).size());
    assertEquals(3, servicesListProvider.getList(ServicesListProvider.LIST_NAME, servicesQuery).size());
    assertEquals(3, servicesListProvider.getList(ServicesListProvider.LIST_STATUS, servicesQuery).size());
  }

  @Test
  public void testNameList() throws ListProviderException {
    Map<String, String> list = servicesListProvider.getList(ServicesListProvider.LIST_NAME, servicesQuery);
    assertEquals(SERVICE_TYPE_1, list.get(SERVICE_TYPE_1));
    assertEquals(SERVICE_TYPE_2, list.get(SERVICE_TYPE_2));
    assertEquals(SERVICE_TYPE_3, list.get(SERVICE_TYPE_3));
  }

  @Test
  public void testStatusList() throws ListProviderException {
    Map<String, String> list = servicesListProvider.getList(ServicesListProvider.LIST_STATUS, servicesQuery);
    for (ServiceState state : ServiceState.values()) {
      assertEquals(ServicesListProvider.SERVICE_STATUS_FILTER_PREFIX + state.toString(), list.get(state.toString()));
    }
  }
}
