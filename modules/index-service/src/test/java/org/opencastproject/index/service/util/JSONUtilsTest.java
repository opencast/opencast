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

package org.opencastproject.index.service.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;


import org.opencastproject.list.api.DefaultResourceListQuery;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListFilter;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.query.StringListFilter;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import com.google.gson.JsonObject;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

/**
 * Unit tests for {@link JSONUtils}
 */
public class JSONUtilsTest {

  private static final String CONTRIBUTORS_PROVIDER = "CONTRIBUTORS";


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
   * {@link JSONUtils#filtersToJSON(ResourceListQuery, org.opencastproject.list.api.ListProvidersService, Organization)}
   * (filters, listProviderService, query, org)}
   */
  @Test
  public void testFiltersToJSON() throws Exception {
    String expectedJSON = IOUtils.toString(getClass().getResource("/filters.json"));
    String expectedJSONreduced = IOUtils.toString(getClass().getResource("/filters_reduced.json"));

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    Organization organization = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(organization.getId()).andReturn("mh_default_org").anyTimes();
    EasyMock.replay(organization);
    EasyMock.replay(securityService);

    final Map<String, String> license = new HashMap<>();
    license.put("contributor1", "My first contributor");
    license.put("contributor2", "My second contributor");
    license.put("contributor3", "My third contributor");

    // Use a mock ListProvidersService instead of a concrete implementation
    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    // Expect that the provider exists for the organization
    EasyMock.expect(listProvidersService.hasProvider(CONTRIBUTORS_PROVIDER, organization.getId()))
            .andReturn(true).anyTimes();
    // Return the license map for any ResourceListQuery and inverseValueKey == false
    EasyMock.expect(listProvidersService.getList(EasyMock.eq(CONTRIBUTORS_PROVIDER),
            EasyMock.anyObject(ResourceListQuery.class), EasyMock.eq(false))).andReturn(license).anyTimes();
    // Not translatable
    EasyMock.expect(listProvidersService.isTranslatable(CONTRIBUTORS_PROVIDER)).andReturn(false).anyTimes();
    EasyMock.replay(listProvidersService);

    // Prepare mock query
    List<ResourceListFilter<?>> filters = new ArrayList<>();
    // Create a simple contributors filter that points to the CONTRIBUTORS provider
    ResourceListFilter<String> contributorsFilter = new ResourceListFilter<>() {
      @Override
      public String getName() {
        return "contributors";
      }

      @Override
      public Optional<String> getValue() {
        return Optional.empty();
      }

      @Override
      public ResourceListFilter.SourceType getSourceType() {
        return ResourceListFilter.SourceType.SELECT;
      }

      @Override
      public String getLabel() {
        return "FILTERS.SERIES.CONTRIBUTORS.LABEL";
      }

      @Override
      public Optional<String> getValuesListName() {
        return Optional.of(CONTRIBUTORS_PROVIDER);
      }
    };
    filters.add(contributorsFilter);
    filters.add(new StringListFilter(""));
    DefaultResourceListQuery query = EasyMock.createNiceMock(DefaultResourceListQuery.class);
    EasyMock.expect(query.getAvailableFilters()).andReturn(filters).anyTimes();
    EasyMock.expect(query.getFilters()).andReturn(new ArrayList<>()).anyTimes();
    EasyMock.expect(query.getLimit()).andReturn(Optional.empty()).anyTimes();
    EasyMock.expect(query.getOffset()).andReturn(Optional.empty()).anyTimes();
    EasyMock.replay(query);

    JSONUtils.setUserRegex(".*"); //allow all users
    JsonObject result = JSONUtils.filtersToJSON(query, listProvidersService, organization);
    assertThat(expectedJSON, SameJSONAs.sameJSONAs(result.toString()));

    JSONUtils.setUserRegex("contributor2"); //allow just one user
    result = JSONUtils.filtersToJSON(query, listProvidersService, organization);
    assertThat(expectedJSONreduced, SameJSONAs.sameJSONAs(result.toString()));
  }

  /**
   * Test method for {@link JSONUtils#toMap(JSONObject)}
   */
  @Test
  public void testToMapWithNull() throws Exception {
    assertEquals(0, JSONUtils.toMap(null).size());
  }

}
