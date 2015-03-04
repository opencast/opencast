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

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.provider.ServersListProvider.SERVERS_FILTER_LIST;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.JaxbHostRegistration;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistration;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.workflow.api.WorkflowDatabaseException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ServersListProviderTest {

  private ServiceRegistry serviceRegistry;
  private ServersListProvider serverListProvider;

  private static final String HOST1 = "host1";
  private static final String HOST2 = "host2";
  private static final String HOST3 = "host3";
  private static final String HOST4 = "host4";

  @Before
  public void setUp() throws Exception {
    this.serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);

    serverListProvider = new ServersListProvider();

    List<HostRegistration> hosts = new ArrayList<HostRegistration>();
    hosts.add(new JaxbHostRegistration(HOST1, "1.1.1.1", 400000, 8, 8, true, false));
    hosts.add(new JaxbHostRegistration(HOST2, "1.1.1.2", 400000, 8, 8, true, true));
    hosts.add(new JaxbHostRegistration(HOST3, "1.1.1.3", 500000, 2, 8, false, false));
    hosts.add(new JaxbHostRegistration(HOST4, "1.1.1.4", 500000, 6, 8, true, true));

    JaxbServiceRegistration service1 = new JaxbServiceRegistration("test", HOST1, "");
    JaxbServiceRegistration service2 = new JaxbServiceRegistration("test", HOST2, "");
    JaxbServiceRegistration service3 = new JaxbServiceRegistration("test", HOST3, "");
    JaxbServiceRegistration service4 = new JaxbServiceRegistration("test", HOST4, "");

    List<ServiceStatistics> statistics = new ArrayList<ServiceStatistics>();
    statistics.add(new JaxbServiceStatistics(service1, 200, 200, 2, 2, 2));
    statistics.add(new JaxbServiceStatistics(service2, 200, 200, 4, 4, 2));
    statistics.add(new JaxbServiceStatistics(service3, 200, 200, 2, 4, 2));
    statistics.add(new JaxbServiceStatistics(service4, 200, 200, 2, 4, 2));

    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(hosts).anyTimes();
    EasyMock.expect(serviceRegistry.getServiceStatistics()).andReturn(statistics).anyTimes();

    serverListProvider.setServiceRegistry(serviceRegistry);
    serverListProvider.activate(null);

    EasyMock.replay(serviceRegistry);

  }

  @Test
  public void testListNames() throws ListProviderException {
    ResourceListQuery query = new ResourceListQueryImpl();

    org.junit.Assert.assertEquals(
            4,
            serverListProvider.getList(ServersListProvider.getListNameFromFilter(SERVERS_FILTER_LIST.HOSTNAME), query,
                    null).size());

    org.junit.Assert.assertEquals(4, serverListProvider.getList("servers", query, null).size());
    org.junit.Assert.assertEquals(4, serverListProvider.getList("non-existing-name", query, null).size());

    org.junit.Assert.assertEquals(
            3,
            serverListProvider.getList(ServersListProvider.getListNameFromFilter(SERVERS_FILTER_LIST.CORES), query,
                    null).size());

    org.junit.Assert.assertEquals(
            1,
            serverListProvider.getList(ServersListProvider.getListNameFromFilter(SERVERS_FILTER_LIST.MAXJOBS), query,
                    null).size());
    org.junit.Assert.assertEquals(
            2,
            serverListProvider.getList(ServersListProvider.getListNameFromFilter(SERVERS_FILTER_LIST.MEMORY), query,
                    null).size());
    org.junit.Assert.assertEquals(4,
            serverListProvider
                    .getList(ServersListProvider.getListNameFromFilter(SERVERS_FILTER_LIST.PATH), query, null).size());
    org.junit.Assert.assertEquals(
            1,
            serverListProvider.getList(ServersListProvider.getListNameFromFilter(SERVERS_FILTER_LIST.SERVICE), query,
                    null).size());
  }

  @Test
  public void testQueries() throws ListProviderException, WorkflowDatabaseException {
    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.setOffset(2);
    org.junit.Assert.assertEquals(2, serverListProvider.getList("servers", query, null).size());
    query.setLimit(1);
    org.junit.Assert.assertEquals(2, serverListProvider.getList("servers", query, null).size());
  }
}
