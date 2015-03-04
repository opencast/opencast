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
package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.resources.list.api.Service;
import org.opencastproject.index.service.util.ListProviderUtil;
import org.opencastproject.security.api.Organization;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;

import org.json.simple.JSONAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A service which provides statistical information about the various matterhorn services.
 *
 * @author ademasi
 *
 */
public class ServicesListProvider implements ResourceListProvider {
  private static final Logger logger = LoggerFactory.getLogger(ServicesListProvider.class);
  private static final String[] NAMES = { "services" };
  /** The remote service manager */
  protected ServiceRegistry serviceRegistry = null;

  public void activate() {
    logger.info("ServicesListProvider is activated!");
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {
    Map<String, Object> result = new HashMap<String, Object>();
    ServiceQueryResult services = getFilteredList(query);
    // TODO: Limit, Offset
    result.put("results", services.filteredResult);
    result.put("total", String.valueOf(services.totalCount));
    if (query != null) {
      if (query.getLimit().isSome()) {
        result.put("limit", Integer.toString(query.getLimit().get()));
      }
      if (query.getOffset().isSome()) {
        result.put("offset", Integer.toString(query.getOffset().get()));
      }
    }
    return result;
  }

  private ServiceQueryResult getFilteredList(ResourceListQuery query) throws ListProviderException {
    ServiceQueryResult result = new ServiceQueryResult();
    List<JSONAware> services = new ArrayList<JSONAware>();
    try {
      List<ServiceStatistics> serviceStatistics = serviceRegistry.getServiceStatistics();
      result.totalCount = serviceStatistics.size();
      for (ServiceStatistics stats : serviceStatistics) {
        Service service = new Service(stats);
        if (service.isCompliant(query)) {
          services.add(service);
        }
      }
    } catch (ServiceRegistryException e) {
      throw new ListProviderException(e.getLocalizedMessage());
    }
    result.filteredResult = ListProviderUtil.filterMap(services, query);
    return result;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  /**
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  class ServiceQueryResult {
    private int totalCount;
    private List<JSONAware> filteredResult;

    public int getTotalCount() {
      return totalCount;
    }

    public void setTotalCount(int totalCount) {
      this.totalCount = totalCount;
    }

    public List<JSONAware> getFilteredResult() {
      return filteredResult;
    }

    public void setFilteredResult(List<JSONAware> filteredResult) {
      this.filteredResult = filteredResult;
    }
  }

}
