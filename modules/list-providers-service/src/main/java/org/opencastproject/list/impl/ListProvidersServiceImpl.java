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

package org.opencastproject.list.impl;

import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.list.util.ListProviderUtil;
import org.opencastproject.security.api.SecurityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ListProvidersServiceImpl implements ListProvidersService {

  static final String ALL_ORGANIZATIONS = "*";

  private SecurityService securityService;
  private Map<ResourceTuple, ResourceListProvider> providers = new ConcurrentHashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(ListProvidersServiceImpl.class);

  /**
   * Instances of this class represent unique keys for the parent {@link ConcurrentHashMap},
   * made up of both a provider ID and its associated organisation ID.
   */
  static class ResourceTuple {
    private final String resourceName;
    private final String organizationId;

    ResourceTuple(String resourceName, String organizationId) {
      this.resourceName = resourceName;
      this.organizationId = organizationId;
    }


    String getResourceName() {
      return resourceName;
    }

    String getOrganizationId() {
      return organizationId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      return o instanceof ResourceTuple
              && Objects.equals(resourceName, ((ResourceTuple) o).resourceName)
              && Objects.equals(organizationId, ((ResourceTuple) o).organizationId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(resourceName, organizationId);
    }
  }

    /** OSGi callback for security service */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

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

  /**
   *
   * @param resourceName the name of the provider
   * @return the queried list provider
   * @throws ListProviderNotFoundException if no list provider matches query
   */
  private ResourceListProvider getProvider(String resourceName)
          throws ListProviderNotFoundException {
    ResourceListProvider provider;
    String organizationId;

    if (securityService.getOrganization() != null) {
      organizationId = securityService.getOrganization().getId();
      provider = providers.get(new ResourceTuple(resourceName, organizationId));
      if (provider != null) {
        return provider;
      }
      // use default if no specific provider is set
      provider = providers.get(new ResourceTuple(resourceName, ALL_ORGANIZATIONS));
    } else {
      organizationId = ALL_ORGANIZATIONS;
      provider = providers.get(new ResourceTuple(resourceName, ALL_ORGANIZATIONS));
    }
    if (provider != null) {
      return provider;
    } else {
      throw new ListProviderNotFoundException("No provider found for organisation <"
            + organizationId + "> with the name " + resourceName);
    }
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query, boolean inverseValueKey)
          throws ListProviderException {
    ResourceListProvider provider = getProvider(listName);
    Map<String, String> list = provider.getList(listName, query);
    if ("SERIES".equals(listName)) {
      for (Map.Entry<String,String> entry : list.entrySet()) {
        int repeated = Collections.frequency(list.values(), entry.getValue());
        if (repeated > 1) {
          String newSeriesName = null;
          //If a series name is repeated, will add the first 7 characters of the series ID to the display name on the
          //admin-ui
          try {
            newSeriesName = entry.getValue() + " " + "(ID: " + entry.getKey().substring(0, 7) + "...)";
          } catch (StringIndexOutOfBoundsException e) {
            newSeriesName = entry.getValue() + " " + "(ID: " + entry.getKey() + ")";
          }
          logger.debug(String.format("Repeated series title \"%s\" found, changing to \"%s\" for admin-ui display",
              entry.getValue(), newSeriesName));
          list.put(entry.getKey(), newSeriesName);
        }
      }
    }
    return inverseValueKey ? ListProviderUtil.invertMap(list) : list;
  }

  @Override
  public boolean isTranslatable(String listName) throws ListProviderNotFoundException {
    ResourceListProvider provider = getProvider(listName);
    return provider.isTranslatable(listName);
  }

  @Override
  public String getDefault(String listName) throws ListProviderNotFoundException {
    ResourceListProvider provider = getProvider(listName);
    return provider.getDefault();
  }

  @Override
  public void addProvider(String listName, ResourceListProvider provider) {
    addProvider(listName, provider, ALL_ORGANIZATIONS);
  }

  @Override
  public void addProvider(String listName, ResourceListProvider provider, String organizationId) {
    providers.put(new ResourceTuple(listName, organizationId), provider);
  }

  @Override
  public void removeProvider(String name) {
    removeProvider(name, ALL_ORGANIZATIONS);
  }

  @Override
  public void removeProvider(String name, String organizationId) {
    providers.remove(new ResourceTuple(name, organizationId));
  }

  @Override
  public boolean hasProvider(String name) {
    return hasProvider(name, ALL_ORGANIZATIONS);
  }

  @Override
  public boolean hasProvider(String name, String organizationId) {
    return providers.containsKey(new ResourceTuple(name, organizationId));
  }

  @Override
  public List<String> getAvailableProviders() {
    List<String> sources = new ArrayList<>();
    for (ResourceTuple key : providers.keySet()) {
      sources.add(key.getResourceName());
    }
    return sources;
  }
}
