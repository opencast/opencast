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

package org.opencastproject.index.service.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.index.service.resources.list.provider.ContributorsListProvider;
import org.opencastproject.index.service.resources.list.query.SeriesListQuery;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.impl.ListProvidersServiceImpl;
import org.opencastproject.list.impl.ResourceListQueryImpl;
import org.opencastproject.list.query.StringListFilter;
import org.opencastproject.list.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

/**
 * Unit tests for {@link JSONUtils}
 */
public class JSONUtilsTest {

  /**
   * Test method for {@link JSONUtils#fromMap(Map)}
   */
  @Test
  public void testFromMap() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    JSONObject json;

    map.put("key", "value");
    map.put("key with spaces", "value");
    json = JSONUtils.fromMap(map);
    assertEquals("value", json.getString("key"));
    assertEquals("value", json.getString("key with spaces"));

  }

  /**
   * Test method for {@link JSONUtils#fromMap(Map)}
   */
  @Test
  public void testFromMapWithNull() throws Exception {
    assertEquals(0, JSONUtils.fromMap(null).length());
  }

  /**
   * Test method for {@link JSONUtils#fromMap(Map)}
   */
  @Test(expected = JSONException.class)
  public void testFromMapWithNullKey() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    map.put(null, "value");

    JSONUtils.fromMap(map);
  }

  /**
   * Test method for {@link JSONUtils#toMap(JSONObject)}
   */
  @Test
  public void testToMapWith() throws Exception {
    JSONObject json = new JSONObject();
    json.put("boolean", true);
    json.put("string", "String");
    json.put("double", 1.3);

    Map<String, String> map = JSONUtils.toMap(json);
    assertEquals("true", map.get("boolean"));
    assertEquals("String", map.get("string"));
    assertEquals("1.3", map.get("double"));
  }

  /**
   * Test method for
   * {@link JSONUtils#filtersToJSON(org.opencastproject.list.api.ResourceListQuery, org.opencastproject.list.api.ListProvidersService, org.opencastproject.security.api.Organization)}
   * (filters, listProviderService, query, org)}
   */
  @Test
  public void testFiltersToJSON() throws Exception {
    String expectedJSON = IOUtils.toString(getClass().getResource("/filters.json"));

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    Organization organization = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(organization.getId()).andReturn("mh_default_org").anyTimes();
    EasyMock.replay(organization);
    EasyMock.replay(securityService);

    ListProvidersServiceImpl listProvidersService = new ListProvidersServiceImpl();
    SimpleSerializer serializer = new SimpleSerializer();

    listProvidersService.setSecurityService(securityService);

    final Map<String, String> license = new HashMap<String, String>();
    license.put("contributor1", "My first contributor");
    license.put("contributor2", "My second contributor");
    license.put("contributor3", "My third contributor");

    // Create test list provider
    listProvidersService.addProvider(ContributorsListProvider.DEFAULT, new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { ContributorsListProvider.DEFAULT };
      }

      @Override
      public Map<String, String> getList(String listName, ResourceListQuery query)
              throws ListProviderException {
        return ListProviderUtil.filterMap(license, query);
      }

        @Override
        public boolean isTranslatable(String listName) {
          return false;
        }

      @Override
      public String getDefault() {
        return null;
      }
    }, organization.getId());

    // Prepare mock query
    List<ResourceListFilter<?>> filters = new ArrayList<ResourceListFilter<?>>();
    filters.add(SeriesListQuery.createContributorsFilter(Option.<String> none()));
    filters.add(new StringListFilter(""));
    ResourceListQueryImpl query = EasyMock.createNiceMock(ResourceListQueryImpl.class);
    EasyMock.expect(query.getAvailableFilters()).andReturn(filters).anyTimes();
    EasyMock.expect(query.getFilters()).andReturn(new ArrayList<ResourceListFilter<?>>()).anyTimes();
    EasyMock.expect(query.getLimit()).andReturn(Option.<Integer> none()).anyTimes();
    EasyMock.expect(query.getOffset()).andReturn(Option.<Integer> none()).anyTimes();
    EasyMock.replay(query);

    JValue result = JSONUtils.filtersToJSON(query, listProvidersService, organization);

    StreamingOutput stream = RestUtils.stream(serializer.fn.toJson(result));
    ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
    try {
      stream.write(resultStream);
      assertThat(expectedJSON, SameJSONAs.sameJSONAs(resultStream.toString()));
    } finally {
      IOUtils.closeQuietly(resultStream);
    }
  }

  /**
   * Test method for {@link JSONUtils#toMap(JSONObject)}
   */
  @Test
  public void testToMapWithNull() throws Exception {
    assertEquals(0, JSONUtils.toMap(null).size());
  }

}
