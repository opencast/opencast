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

import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.HostStatistics;
import org.opencastproject.serviceregistry.api.JaxbHostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestServerEndpoint extends ServerEndpoint {

  private static final String HOST1 = "host1";
  private static final String HOST2 = "host2";
  private static final String HOST3 = "host3";
  private static final String HOST4 = "host4";

  private ServiceRegistry serviceRegistry;
  private MediaPackageBuilderImpl mpBuilder;

  public TestServerEndpoint() throws Exception {
    mpBuilder = new MediaPackageBuilderImpl();
    this.serviceRegistry = EasyMock.createMock(ServiceRegistry.class);

    List<HostRegistration> hosts = Arrays.asList(
        new JaxbHostRegistration(HOST1, "1.1.1.1", "node1", 100000, 8, 8, true, false),
        new JaxbHostRegistration(HOST2, "1.1.1.2", "node2", 400000, 4, 8, true, true),
        new JaxbHostRegistration(HOST3, "1.1.1.3", "node3", 200000, 2, 8, false, false),
        new JaxbHostRegistration(HOST4, "1.1.1.4", "node4", 500000, 6, 8, true, true));

    HostStatistics statistics = new HostStatistics();
    statistics.addRunning(HOST1.hashCode(), 2);
    statistics.addRunning(HOST2.hashCode(), 4);
    statistics.addRunning(HOST3.hashCode(), 2);
    statistics.addRunning(HOST4.hashCode(), 2);
    statistics.addQueued(HOST1.hashCode(), 2);
    statistics.addQueued(HOST2.hashCode(), 4);
    statistics.addQueued(HOST3.hashCode(), 4);
    statistics.addQueued(HOST4.hashCode(), 4);

    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(hosts).anyTimes();
    EasyMock.expect(serviceRegistry.getHostStatistics()).andReturn(statistics).anyTimes();

    EasyMock.replay(serviceRegistry);

    this.setServiceRegistry(serviceRegistry);
    this.activate(null);
  }

}
