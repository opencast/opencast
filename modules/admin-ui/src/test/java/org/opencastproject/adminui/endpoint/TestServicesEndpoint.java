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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.JaxbHostRegistration;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistration;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.workflow.api.WorkflowService;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestServicesEndpoint extends ServicesEndpoint {

  private static final String HOST1_NAME = "host1";
  private static final String HOST2_NAME = "host2";
  private static final String HOST3_NAME = "host3";
  private static final String HOST4_NAME = "host4";

  private ServiceRegistry serviceRegistry;
  private WorkflowService workflowService;
  private MediaPackageBuilderImpl mpBuilder;

  public TestServicesEndpoint() throws Exception {
    mpBuilder = new MediaPackageBuilderImpl();
    this.serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);

    List<HostRegistration> hosts = new ArrayList<HostRegistration>();
    hosts.add(new JaxbHostRegistration(HOST1_NAME, "1.1.1.1", "node1", 100000, 8, 8, true, false));
    hosts.add(new JaxbHostRegistration(HOST2_NAME, "1.1.1.2", "node2", 400000, 4, 8, true, true));
    hosts.add(new JaxbHostRegistration(HOST3_NAME, "1.1.1.3", "node3", 200000, 2, 8, false, false));
    hosts.add(new JaxbHostRegistration(HOST4_NAME, "1.1.1.4", "node4", 500000, 6, 8, true, true));

    JaxbServiceRegistration service1 = new JaxbServiceRegistration("service1", HOST1_NAME, "");
    JaxbServiceRegistration service2 = new JaxbServiceRegistration("service2", HOST1_NAME, "");
    JaxbServiceRegistration service3 = new JaxbServiceRegistration("service3", HOST2_NAME, "");
    JaxbServiceRegistration service4 = new JaxbServiceRegistration("service4", HOST3_NAME, "");
    JaxbServiceRegistration service5 = new JaxbServiceRegistration("service5", HOST2_NAME, "");
    JaxbServiceRegistration service6 = new JaxbServiceRegistration("service6", HOST4_NAME, "");
    service2.setServiceState(ServiceState.ERROR);
    service4.setServiceState(ServiceState.WARNING);

    List<ServiceStatistics> statistics = new ArrayList<ServiceStatistics>();
    statistics.add(new JaxbServiceStatistics(service1,      0,      0, 0, 0,  0));
    statistics.add(new JaxbServiceStatistics(service2, 123000, 456000, 0, 3,  5));
    statistics.add(new JaxbServiceStatistics(service3,  30000,  10000, 2, 5, 20));
    statistics.add(new JaxbServiceStatistics(service4,  10000,  30000, 1, 1,  0));
    statistics.add(new JaxbServiceStatistics(service5,      0,      0, 0, 0, 10));
    statistics.add(new JaxbServiceStatistics(service6,      0,  60000, 0, 1,  0));

    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(hosts).anyTimes();
    EasyMock.expect(serviceRegistry.getServiceStatistics()).andReturn(statistics).anyTimes();

    EasyMock.replay(serviceRegistry);

    this.setServiceRegistry(serviceRegistry);
    this.activate();
  }

  private MediaPackage loadMpFromResource(String name) throws Exception {
    URL test = ServicesEndpointTest.class.getResource("/" + name + ".xml");
    URI publishedMediaPackageURI = test.toURI();
    return mpBuilder.loadFromXml(publishedMediaPackageURI.toURL().openStream());
  }
}
