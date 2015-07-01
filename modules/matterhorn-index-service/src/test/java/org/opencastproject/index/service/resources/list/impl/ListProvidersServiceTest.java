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

package org.opencastproject.index.service.resources.list.impl;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.resources.list.query.StringListFilter;
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ListProvidersServiceTest {

  private static final String TEST_FILTER_NAME = "value";
  private static final String TEST_SORTBY = "value";

  private ListProvidersServiceImpl listProviderService;

  /**
   * Returns a map for the example with default filtering
   */
  private ResourceListProvider getResourceListProvider(final String name, final Map<String, String> list) {
    return new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { name };
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {

        Map<String, Object> filteredList = new HashMap<String, Object>();

        int i = 0;

        for (Entry<String, String> e : list.entrySet()) {
          if ((query.getOffset().isNone() || query.getOffset().get() <= i)
                  && (!query.hasFilter(TEST_FILTER_NAME) || e.getValue().contains(
                          (String) query.getFilter(TEST_FILTER_NAME).getValue().get())))
            filteredList.put(e.getKey(), e.getValue());

          i++;

          if ((query.getLimit().isSome() && filteredList.size() >= query.getLimit().get()))
            break;
        }

        if (query.getSortBy().isSome() && query.getSortBy().get().equals(TEST_SORTBY)) {
          return ListProviderUtil.sortMapByValue(filteredList, true);
        } else {
          return filteredList;
        }
      }
    };
  }

  @Before
  public void setUp() {
    listProviderService = new ListProvidersServiceImpl();
    listProviderService.activate(null);
  }

  @Test
  public void testAddandRemove() throws ListProviderException {
    final String providerName1 = "test1";
    final Map<String, String> list1 = new HashMap<String, String>();
    list1.put("1", "test");
    list1.put("2", "test");
    list1.put("3", "test");
    list1.put("4", "test");
    final String providerName2 = "test2";
    final Map<String, String> list2 = new HashMap<String, String>();
    list2.put("1", "test");
    list2.put("2", "test");
    list2.put("3", "test");
    list2.put("4", "test");

    int baseNumber = listProviderService.getAvailableProviders().size();
    ResourceListQuery query = new ResourceListQueryImpl();

    listProviderService.addProvider(getResourceListProvider(providerName1, list1));
    listProviderService.addProvider(getResourceListProvider(providerName2, list2));

    Assert.assertEquals(baseNumber + 2, listProviderService.getAvailableProviders().size());
    Assert.assertTrue(listProviderService.hasProvider(providerName1));
    Assert.assertTrue(listProviderService.hasProvider(providerName2));
    Assert.assertEquals(list1, listProviderService.getList(providerName1, query, null));
    Assert.assertEquals(list1, listProviderService.getList(providerName2, query, null));

    listProviderService.removeProvider(providerName2);
    Assert.assertEquals(baseNumber + 1, listProviderService.getAvailableProviders().size());
    Assert.assertFalse(listProviderService.hasProvider(providerName2));
  }

  @Test
  public void testQuery() throws ListProviderException {
    ResourceListQueryImpl query = new ResourceListQueryImpl();
    final String providerName1 = "test1";
    final Map<String, String> list1 = new HashMap<String, String>();
    list1.put("1", "x test34");
    list1.put("2", "a test12");
    list1.put("3", "c ok");
    list1.put("4", "z essai test");

    listProviderService.addProvider(getResourceListProvider(providerName1, list1));

    query.setLimit(2);
    query.setOffset(1);
    Assert.assertEquals(2, listProviderService.getList(providerName1, query, null).size());

    query.setLimit(1);
    query.setOffset(5);
    Assert.assertEquals(0, listProviderService.getList(providerName1, query, null).size());

    query.setLimit(2);
    query.setOffset(1);
    Assert.assertEquals(2, listProviderService.getList(providerName1, query, null).size());

    query.setLimit(12);
    query.setOffset(0);
    query.addFilter(new StringListFilter(TEST_FILTER_NAME, "test"));
    Assert.assertEquals(3, listProviderService.getList(providerName1, query, null).size());

    // query.setSortedBy(TEST_SORTBY);
    // Map<String, String> list = listProviderService.getList(providerName1, query, null);
    // int i = 0;
    // Iterator<Entry<String, String>> iterator = list.entrySet().iterator();
    // Assert.assertEquals(list1.get("2"), iterator.next().getValue());
  }
}
