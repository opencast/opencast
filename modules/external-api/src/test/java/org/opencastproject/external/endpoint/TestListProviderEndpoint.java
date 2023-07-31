/*
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
package org.opencastproject.external.endpoint;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListQuery;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

@Path("")
@Ignore
public class TestListProviderEndpoint extends ListProviderEndpoint {

  public TestListProviderEndpoint() throws Exception {
    List<String> providers = new ArrayList<>();
    providers.add("LANGUAGES");
    providers.add("LICENSES");
    providers.add("YES");

    Map<String, String> languages = new HashMap<>();
    languages.put("ara", "LANGUAGES.ARABIC");
    languages.put("dan", "LANGUAGES.DANISH");
    languages.put("deu", "LANGUAGES.GERMAN");

    Map<String, String> licenses = new HashMap<>();
    licenses.put("CC0", "{\"label\":\"EVENTS.LICENSE.CC0\", \"order\":8, \"selectable\": true}");
    licenses.put("CC-BY-SA", "{\"label\":\"EVENTS.LICENSE.CCBYSA\", \"order\":3, \"selectable\": true}");
    licenses.put("CC-BY-NC-ND", "{\"label\":\"EVENTS.LICENSE.CCBYNCND\", \"order\":7, \"selectable\": true}");

    ListProvidersService service = createNiceMock(ListProvidersService.class);
    expect(service.getAvailableProviders()).andReturn(providers);
    expect(service.getList(EasyMock.matches("LANGUAGES"), EasyMock.anyObject(ResourceListQuery.class),
            EasyMock.anyBoolean())).andReturn(languages);
    expect(service.getList(EasyMock.matches("LICENSES"), EasyMock.anyObject(ResourceListQuery.class),
            EasyMock.anyBoolean())).andReturn(licenses);
    replay(service);

    setListProvidersService(service);
  }
}
