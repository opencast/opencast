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

import static org.opencastproject.index.service.util.ListProviderUtil.invertMap;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.security.api.Organization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ListProvidersServiceImpl implements ListProvidersService {

  private Map<String, ResourceListProvider> providers = new ConcurrentHashMap<String, ResourceListProvider>();

  /** OSGi callback for provider. */
  public void addProvider(ResourceListProvider provider) {
    for (String listName : provider.getListNames()) {
      addProvider(listName, provider);
    }
  }

  /** OSGi callback for provider. */
  public void removeProvider(ResourceListProvider provider) {
    for (String listName : provider.getListNames()) {
      removeProvider(listName);
    }
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query, Organization organization,
          boolean inverseValueKey) throws ListProviderException {
    ResourceListProvider provider = providers.get(listName);
    if (provider == null)
      throw new ListProviderException("No resources list found with the name " + listName);
    Map<String, String> list = provider.getList(listName, query, organization);
    if (inverseValueKey) {
      list = invertMap(list);
    }

    return list;
  }

  @Override
  public boolean isTranslatable(String listName) throws ListProviderException {
    ResourceListProvider provider = providers.get(listName);
    if (provider == null)
      throw new ListProviderException("No resources list found with the name " + listName);
    return provider.isTranslatable(listName);
  }

  @Override
  public String getDefault(String listName) throws ListProviderException {
    ResourceListProvider provider = providers.get(listName);
    if (provider == null)
      throw new ListProviderException("No resources list found with the name " + listName);
    return provider.getDefault();
  }

  @Override
  public void addProvider(String listName, ResourceListProvider provider) {
    providers.put(listName, provider);
  }

  @Override
  public void removeProvider(String name) {
    providers.remove(name);
  }

  @Override
  public boolean hasProvider(String name) {
    return providers.containsKey(name);
  }

  @Override
  public List<String> getAvailableProviders() {
    return new ArrayList<String>(providers.keySet());
  }
}
