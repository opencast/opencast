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

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListFilter;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.impl.ListProvidersServiceImpl;
import org.opencastproject.index.service.resources.list.provider.ContributorsListProvider;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.resources.list.query.SeriesListQuery;
import org.opencastproject.index.service.resources.list.query.StringListFilter;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.easymock.EasyMock;
import org.junit.Test;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;

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
   * {@link JSONUtils#filtersToJSON(org.opencastproject.index.service.resources.list.api.ResourceListQuery, org.opencastproject.index.service.resources.list.api.ListProvidersService, org.opencastproject.security.api.Organization)}
   * (filters, listProviderService, query, org)}
   */
  @Test
  public void testFiltersToJSON() throws Exception {
    String expectedJSON = IOUtils.toString(getClass().getResource("/filters.json"));

    JaxbOrganization defaultOrganization = new DefaultOrganization();
    ListProvidersServiceImpl listProvidersService = new ListProvidersServiceImpl();
    SimpleSerializer serializer = new SimpleSerializer();

    final Map<String, Object> license = new HashMap<String, Object>();
    license.put("contributor1", "My first contributor");
    license.put("contributor2", "My second contributor");
    license.put("contributor3", "My third contributor");

    // Create test list provider
    listProvidersService.addProvider(new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { ContributorsListProvider.DEFAULT };
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
              throws ListProviderException {
        return ListProviderUtil.filterMap(license, query);
      }
    });

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

    JValue result = JSONUtils.filtersToJSON(query, listProvidersService, defaultOrganization);

    StreamingOutput stream = RestUtils.stream(serializer.toJsonFx(result));
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
