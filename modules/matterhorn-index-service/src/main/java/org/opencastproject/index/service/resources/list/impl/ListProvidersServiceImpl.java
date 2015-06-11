/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.index.service.resources.list.impl;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListProvidersServiceImpl implements ListProvidersService {

  private static final Logger logger = LoggerFactory.getLogger(ListProvidersServiceImpl.class);
  private static final String FILTER_SUFFIX = "Filter";

  private Map<String, ResourceListProvider> providers = new HashMap<String, ResourceListProvider>();

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

  public void activate(BundleContext bundleContext) {
    addCountries();
    addWorkflowStatus();

    // TODO create a file for each resource and made it dynamic

    providers.put("locationFilter", new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { "locationFilter" };
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
        Map<String, Object> list = new HashMap<String, Object>();
        list.put("Location 1", "EVENTS.EVENT.TABLE.FILTER.LOCATION.LOCATION1");
        list.put("Location 2", "EVENTS.EVENT.TABLE.FILTER.LOCATION.LOCATION2");
        list.put("Location 3", "EVENTS.EVENT.TABLE.FILTER.LOCATION.LOCATION3");
        return list;
      }
    });

    // TODO create a file for each resource and made it dynamic
    providers.put("eventSourcesFilter", new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { "eventSourcesFilter" };
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
        Map<String, Object> list = new HashMap<String, Object>();
        list.put("Twitter", "EVENTS.EVENT.TABLE.FILTER.SOURCE.TWITTER");
        list.put("Github", "EVENTS.EVENT.TABLE.FILTER.SOURCE.GITHUB");
        list.put("Facebook", "EVENTS.EVENT.TABLE.FILTER.SOURCE.FACEBOOK");
        return list;
      }
    });

    // TODO create a file for each resource and made it dynamic
    providers.put("eventStatusFilter", new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return new String[] { "eventStatusFilter" };
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
        Map<String, Object> list = new HashMap<String, Object>();
        list.put("Scheduled", "EVENTS.EVENT.TABLE.FILTER.STATUS.SCHEDULED");
        list.put("Recording", "EVENTS.EVENT.TABLE.FILTER.STATUS.RECORDING");
        list.put("Ingesting", "EVENTS.EVENT.TABLE.FILTER.STATUS.INGESTING");
        list.put("Processing", "EVENTS.EVENT.TABLE.FILTER.STATUS.PROCESSING");
        list.put("Archive", "EVENTS.EVENT.TABLE.FILTER.STATUS.ARCHIVE");
        list.put("upload", "EVENTS.EVENT.TABLE.FILTER.STATUS.UPLOAD");
        list.put("On hold", "EVENTS.EVENT.TABLE.FILTER.STATUS.ONHOLD");
        return list;
      }
    });

    logger.info("Activate the list provider");
  }

  // ====================================
  // Workflow status
  // ====================================

  private void addWorkflowStatus() {
    final String[] title = new String[] { "recording_states" };
    final Map<String, Object> workflowStatus = new HashMap<String, Object>();

    for (WorkflowState s : WorkflowInstance.WorkflowState.values()) {
      workflowStatus.put(s.name(), "EVENTS.EVENT.TABLE.FILTER.STATUS." + s.name());
    }

    providers.put(title[0], new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return title;
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
        return ListProviderUtil.filterMap(workflowStatus, query);

      }

    });
  }

  // ====================================
  // Countries
  // ====================================

  private void addCountries() {
    final String[] title = new String[] { "countries" };
    String[] countriesISO = Locale.getISOCountries();
    final Map<String, Object> countries = new HashMap<String, Object>();
    for (String countryCode : countriesISO) {
      Locale obj = new Locale("", countryCode);
      countries.put(obj.getCountry(), obj.getDisplayCountry());
    }

    providers.put(title[0], new ResourceListProvider() {

      @Override
      public String[] getListNames() {
        return title;
      }

      @Override
      public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization) {
        return ListProviderUtil.filterMap(countries, query);
      }

    });
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    ResourceListProvider provider = providers.get(listName);
    if (provider == null)
      throw new ListProviderException("No resources list found with the name " + listName);

    return provider.getList(listName, query, organization);
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
