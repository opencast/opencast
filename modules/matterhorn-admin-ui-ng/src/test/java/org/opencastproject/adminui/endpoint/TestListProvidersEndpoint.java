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
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.impl.ListProvidersServiceImpl;
import org.opencastproject.index.service.resources.list.provider.ServicesListProvider;
import org.opencastproject.index.service.util.ListProviderUtil;
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
  private final Map<String, Object> baseMap = new HashMap<String, Object>();

  private ListProvidersServiceImpl listProvidersService = new ListProvidersServiceImpl();
  private SecurityService securityService;

  public TestListProvidersEndpoint() {
    this.securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(null);

    for (int i = 0; i < PROVIDER_VALUES.length; i++) {
      baseMap.put(Integer.toString(i), PROVIDER_VALUES[i]);
    }

    listProvidersService.addProvider(new ResourceListProvider() {
      @Override
      public String[] getListNames() {
        return new String[] { PROVIDER_NAME };
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
        return ListProviderUtil.filterMap(baseMap, query);
      }
    });

    listProvidersService.addProvider(makeServicesListProvider());

    this.setSecurityService(securityService);
    this.setListProvidersService(listProvidersService);
    this.activate(null);
  }

  private ResourceListProvider makeServicesListProvider() {
    ServicesListProvider servicesListProvider = new ServicesListProvider();
    servicesListProvider.setServiceRegistry(TestServiceRegistryFactory.getStub());
    return servicesListProvider;
  }
}
