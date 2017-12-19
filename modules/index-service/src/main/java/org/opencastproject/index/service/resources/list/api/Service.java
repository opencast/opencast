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

package org.opencastproject.index.service.resources.list.api;

import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.ServiceStatistics;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model class representing the displayed fields in the services screen.
 *
 * @author ademasi
 *
 */
public class Service implements JSONAware {
  private static final String MEAN_RUN_TIME = "meanRunTime";
  private static final String MEAN_QUEUE_TIME = "meanQueueTime";
  private static final String QUEUED = "queued";
  private static final String RUNNING = "running";
  private static final String COMPLETED = "completed";
  private static final String STATUS = "status";
  private static final String NAME = "name";
  private static final String HOST = "host";
  private Map<String, String> map = new HashMap<String, String>();

  public Service(ServiceStatistics stats) {
    setStatus(stats.getServiceRegistration().getServiceState());
    setName(stats.getServiceRegistration().getServiceType());
    setCompleted(stats.getFinishedJobs());
    setMeanQueueTime(stats.getMeanQueueTime());
    setMeanRunTime(stats.getMeanRunTime());
    setQueued(stats.getQueuedJobs());
    setRunning(stats.getRunningJobs());
    setHost(stats.getServiceRegistration().getHost());
  }

  public void setHost(String host) {
    map.put(HOST, host);
  }

  public String toJson() {
    return JSONObject.toJSONString(map);
  }

  /**
   * @return the name
   */
  public String getName() {
    return map.get(NAME);
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    map.put(NAME, name);
  }

  /**
   * @return the status
   */
  public String getStatus() {
    return map.get(STATUS);
  }

  /**
   * @param status
   *          the status to set
   */
  public void setStatus(ServiceState status) {
    map.put(STATUS, status.name());
  }

  /**
   * @return the completed
   */
  public Integer getCompleted() {
    return Integer.valueOf(map.get(COMPLETED));
  }

  /**
   * @param completed
   *          the completed to set
   */
  public void setCompleted(Integer completed) {
    map.put(COMPLETED, String.valueOf(completed));
  }

  /**
   * @return the running
   */
  public Integer getRunning() {
    return Integer.valueOf(map.get(RUNNING));
  }

  /**
   * @param running
   *          the running to set
   */
  public void setRunning(Integer running) {
    map.put(RUNNING, String.valueOf(running));
  }

  /**
   * @return the queued
   */
  public Integer getQueued() {
    return Integer.valueOf(map.get(QUEUED));
  }

  /**
   * @param queued
   *          the queued to set
   */
  public void setQueued(Integer queued) {
    map.put(QUEUED, String.valueOf(queued));
  }

  public void setMeanQueueTime(long meanQueueTime) {
    map.put(MEAN_QUEUE_TIME, String.valueOf(meanQueueTime));
  }

  public long getMeanQueueTime() {
    return Long.valueOf(map.get(MEAN_QUEUE_TIME));
  }

  public void setMeanRunTime(long meanRunTime) {
    map.put(MEAN_RUN_TIME, String.valueOf(meanRunTime));
  }

  public long getMeanRunTime() {
    return Long.valueOf(map.get(MEAN_RUN_TIME));
  }

  public String getHost() {
    return map.get(HOST);
  }

  @Override
  public String toJSONString() {
    return JSONObject.toJSONString(map);
  }

  /**
   * Checks if the query filters this service or not. Allowed filtering criteria per definition (EAU-473): name,
   * host(s), freetext (i.e. either name or host(s) or both.
   *
   * @param query
   * @return True if the given filter applies to this record, false if it doesn't.
   */
  public boolean isCompliant(ResourceListQuery query) {
    if (query == null) {
      return true; // no filter
    }
    List<ResourceListFilter<?>> filters = query.getFilters();
    for (ResourceListFilter<?> resourceListFilter : filters) {
      String filterName = resourceListFilter.getName();
      Object value = resourceListFilter.getValue().get();
      if (!(value instanceof String)) {
        return false; // Only String filters allowed here.
      }
      String filterValue = (String) value;
      if (filterName.equals(ResourceListFilter.FREETEXT)) {
        return complies(NAME, filterValue) || complies(HOST, filterValue);
      }
      if (!complies(filterName, filterValue)) {
        return false;
      }
    }
    return true;
  }

  private boolean complies(String criteria, String filterValue) {
    String value = map.get(criteria);
    if (value == null) {
      // the criteria doesn't exist
      return false;
    }
    return value.toLowerCase().contains(filterValue.toLowerCase());
  }
}
