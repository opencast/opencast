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
import org.opencastproject.index.service.resources.list.query.JobsListQueryImpl;
import org.opencastproject.index.service.resources.list.query.JobsListQueryImpl.FILTERS;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "JOBS";

  public static final String CONTRIBUTOR = PROVIDER_PREFIX + ".CONTRIBUTOR";
  public static final String CREATOR = PROVIDER_PREFIX + ".CREATOR";
  public static final String LANGUAGE = PROVIDER_PREFIX + ".LANGUAGE";
  public static final String OPERATION = PROVIDER_PREFIX + ".OPERATION";
  public static final String SERIES = PROVIDER_PREFIX + ".SERIES";
  public static final String STATUS = PROVIDER_PREFIX + ".STATUS";
  public static final String SUBJECT = PROVIDER_PREFIX + ".SUBJECT";
  public static final String TITLE = PROVIDER_PREFIX + ".TITLE";
  public static final String WORKFLOW = PROVIDER_PREFIX + ".WORKFLOW";

  /** The list of filter criteria for this provider */
  public static enum JobFilter {
    CONTRIBUTOR, CREATOR, LANGUAGE, OPERATION, SERIES, STATUS, SUBJECT, TITLE, WORKFLOW;
  };

  protected static final String[] NAMES = { PROVIDER_PREFIX, CONTRIBUTOR, CREATOR, LANGUAGE, OPERATION, SERIES, STATUS,
          SUBJECT, TITLE, WORKFLOW };

  /** The names of the different list available through this provider */
  private List<String> listNames;

  private WorkflowService workflowService;
  private ServiceRegistry serviceRegistry;

  private static final Logger logger = LoggerFactory.getLogger(JobsListProvider.class);

  protected void activate(BundleContext bundleContext) {
    logger.info("Jobs list provider activated!");
    listNames = new ArrayList<String>();

    // Fill the list names
    for (JobFilter value : JobFilter.values()) {
      listNames.add(getListNameFromFilter(value));
    }

    // Standard list
    listNames.add(PROVIDER_PREFIX);
  }

  /** OSGi callback for the workflow service. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi callback for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, Object> getList(String listName, ResourceListQuery query, Organization organization)
          throws ListProviderException {

    JobsListQueryImpl jobsQuery = (JobsListQueryImpl) query;
    Map<String, Object> jobList = new HashMap<String, Object>();
    int offset = 0;
    int limit = 0;

    if (query != null) {
      if (query.getLimit().isSome())
        limit = (query.getLimit().get());

      if (query.getOffset().isSome())
        offset = (query.getOffset().get());
    }

    WorkflowQuery wQuery = new WorkflowQuery();
    WorkflowSet workflowInstances = null;
    wQuery.withStartPage(offset);
    wQuery.withCount(limit);

    // Add filters
    if (query != null && query.hasFilter(FILTERS.TEXT.toString()))
      wQuery.withText(jobsQuery.getText().get());

    try {
      workflowInstances = workflowService.getWorkflowInstances(wQuery);
    } catch (WorkflowDatabaseException e) {
      logger.error("Not able to get the list of job from the database: {}", e);
      throw new ListProviderException(e.getMessage(), e.getCause());
    }

    WorkflowInstance[] items = workflowInstances.getItems();

    // Get list name
    JobFilter listValue;
    if (PROVIDER_PREFIX.equals(listName)) {
      listValue = JobFilter.TITLE;
    } else {
      try {
        listValue = JobFilter.valueOf(listName.replace(PROVIDER_PREFIX + ".", "").toUpperCase());
      } catch (IllegalArgumentException e) {
        logger.warn("List name '{}' unavailable for jobs list provider: {}", listName, e);
        listValue = JobFilter.TITLE;
      }
    }

    for (WorkflowInstance instance : items) {

      switch (listValue) {
        case CONTRIBUTOR:
          for (String contributor : instance.getMediaPackage().getContributors())
            jobList.put(contributor, contributor);
          break;
        case CREATOR:
          User creator = instance.getCreator();
          if (creator != null)
            jobList.put(creator.getUsername(), creator.getName());
          break;
        case LANGUAGE:
          String language = instance.getMediaPackage().getLanguage();
          if (!StringUtils.isBlank(language))
            jobList.put(language, language);
          break;
        case OPERATION:
          WorkflowOperationInstance currentOperation = instance.getCurrentOperation();
          if (currentOperation != null)
            jobList.put(Long.toString(currentOperation.getId()), currentOperation.getTemplate());
          break;
        case SERIES:
          String serieId = instance.getMediaPackage().getSeries();
          String serieTitle = instance.getMediaPackage().getSeriesTitle();
          if (!StringUtils.isBlank(serieTitle) && !StringUtils.isBlank(serieId))
            jobList.put(serieId, serieTitle);
          break;
        case STATUS:
          String status = instance.getState().toString();
          if (!StringUtils.isBlank(status))
            jobList.put(status, status);
          break;
        case SUBJECT:
          for (String subject : instance.getMediaPackage().getSubjects())
            jobList.put(subject, subject);
          break;
        case WORKFLOW:
          String workflowId = instance.getTemplate();
          String workflowName = instance.getTitle();
          if (!StringUtils.isBlank(workflowId) && !StringUtils.isBlank(workflowName))
            jobList.put(workflowId, workflowName);
          break;
        case TITLE:
        default:
          String title = instance.getMediaPackage().getTitle();
          if (!StringUtils.isBlank(title))
            jobList.put(title, title);
          break;
      }

    }
    return jobList;
  }

  /**
   * Returns the list name related to the given filter
   * 
   * @param filter
   *          the filter from which the list name is needed
   * @return the list name related to the givne filter
   */
  public static String getListNameFromFilter(JobFilter filter) {
    return PROVIDER_PREFIX + "." + filter.toString();
  }

  /**
   * Get all the names of all the different list with the prefix available with this provider
   * 
   * @return an string array containing the list names
   */
  public static String[] getAvailableFilters() {
    String[] list = new String[JobFilter.values().length];
    int i = 0;
    for (JobFilter value : JobFilter.values()) {
      list[i++] = getListNameFromFilter(value);
    }
    return list;
  }
}
