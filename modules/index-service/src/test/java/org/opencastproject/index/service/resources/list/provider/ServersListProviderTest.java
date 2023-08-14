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

import org.opencastproject.index.service.resources.list.query.ServersListQuery;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.JaxbHostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    hosts.add(new JaxbHostRegistration(HOST1, "1.1.1.1", "node1", 400000, 8, 8, true, false));
    hosts.add(new JaxbHostRegistration(HOST2, "1.1.1.2", "node2", 400000, 8, 8, true, true));
    hosts.add(new JaxbHostRegistration(HOST3, "1.1.1.3", "node3", 500000, 2, 8, false, false));
    hosts.add(new JaxbHostRegistration(HOST4, "1.1.1.4", "node4", 500000, 6, 8, true, true));

    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(hosts).anyTimes();

    serverListProvider.setServiceRegistry(serviceRegistry);
    serverListProvider.activate(null);

    EasyMock.replay(serviceRegistry);
  }

  @Test
  public void testListNames() throws ListProviderException {
    ResourceListQuery query = new ServersListQuery();

    assertEquals(4, serverListProvider.getList("non-existing-name", query).size());
    assertEquals(4, serverListProvider.getList(ServersListProvider.LIST_HOSTNAME, query).size());
    assertEquals(4, serverListProvider.getList(ServersListProvider.LIST_NODE_NAME, query).size());
    assertEquals(3, serverListProvider.getList(ServersListProvider.LIST_STATUS, query).size());
  }

  @Test
  public void testHostnameList() throws ListProviderException {
    ResourceListQuery query = new ServersListQuery();

    Map<String, String> list = serverListProvider.getList(ServersListProvider.LIST_HOSTNAME, query);
    assertEquals(HOST1, list.get(HOST1));
    assertEquals(HOST2, list.get(HOST2));
    assertEquals(HOST3, list.get(HOST3));
    assertEquals(HOST4, list.get(HOST4));
  }

  @Test
  public void testNodeNameList() throws ListProviderException {
    ResourceListQuery query = new ServersListQuery();

    Map<String, String> list = serverListProvider.getList(ServersListProvider.LIST_NODE_NAME, query);
    assertEquals("node1", list.get("node1"));
    assertEquals("node2", list.get("node2"));
    assertEquals("node3", list.get("node3"));
    assertEquals("node4", list.get("node4"));
  }

  @Test
  public void testStatusList() throws ListProviderException {
    ResourceListQuery query = new ServersListQuery();

    Map<String, String> list = serverListProvider.getList(ServersListProvider.LIST_STATUS, query);
    assertEquals(ServersListProvider.SERVER_STATUS_LABEL_ONLINE, list.get(ServersListProvider.SERVER_STATUS_ONLINE));
    assertEquals(ServersListProvider.SERVER_STATUS_LABEL_OFFLINE, list.get(ServersListProvider.SERVER_STATUS_OFFLINE));
    assertEquals(ServersListProvider.SERVER_STATUS_LABEL_MAINTENANCE, list.get(ServersListProvider.SERVER_STATUS_MAINTENANCE));
  }
}
