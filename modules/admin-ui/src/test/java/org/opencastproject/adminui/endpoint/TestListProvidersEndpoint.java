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

import org.opencastproject.adminui.util.TestServiceRegistryFactory;
import org.opencastproject.index.service.resources.list.provider.ServersListProvider;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.impl.ListProvidersServiceImpl;
import org.opencastproject.list.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestListProvidersEndpoint extends ListProvidersEndpoint {

  public static final String PROVIDER_NAME = "test";
  public static final String[] PROVIDER_VALUES = { "x", "a", "c", "z", "t", "h" };
  private final Map<String, String> baseMap = new HashMap<String, String>();

  private ListProvidersServiceImpl listProvidersService = new ListProvidersServiceImpl();
  private SecurityService securityService;
  private Organization organization;

  public TestListProvidersEndpoint() {
    this.securityService = EasyMock.createNiceMock(SecurityService.class);
    organization = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(organization.getId()).andReturn("mh_default_org").anyTimes();
    EasyMock.replay(organization, securityService);
    listProvidersService.setSecurityService(securityService);

    for (int i = 0; i < PROVIDER_VALUES.length; i++) {
      baseMap.put(Integer.toString(i), PROVIDER_VALUES[i]);
    }

    listProvidersService.addProvider(new ResourceListProvider() {
      @Override
      public String[] getListNames() {
        return new String[] { PROVIDER_NAME };
      }

      @Override
      public Map<String, String> getList(String listName, ResourceListQuery query) {
        return ListProviderUtil.filterMap(baseMap, query);
      }

      @Override
      public boolean isTranslatable(String listName) {
        return false;
      }

      @Override
      public String getDefault() {
        return null;
      }
    });

    listProvidersService.addProvider(makeServicesListProvider());

    this.setSecurityService(securityService);
    this.setListProvidersService(listProvidersService);
    this.activate(null);
  }

  private ResourceListProvider makeServicesListProvider() {
    ServersListProvider serversListProvider = new ServersListProvider();
    serversListProvider.setServiceRegistry(TestServiceRegistryFactory.getStub());
    return serversListProvider;
  }
}
