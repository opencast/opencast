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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.index.service.resources.list.api.ResourceListProvider;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.job.api.Job;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/** Jobs list provider. */
public class JobsListProvider implements ResourceListProvider {

  private static final Logger logger = LoggerFactory.getLogger(JobsListProvider.class);

  /** Prefix for list names. */
  private static final String PROVIDER_PREFIX = "JOBS";
  /** Status list name. */
  public static final String LIST_STATUS = PROVIDER_PREFIX + ".STATUS";
  /** Workflow list name. */
  public static final String LIST_WORKFLOW = PROVIDER_PREFIX + ".WORKFLOW";

  /** Filter label prefix. */
  private static final String FILTER_LABEL_PREFIX = "FILTERS." + PROVIDER_PREFIX;
  /** Job status label prefix. */
  public static final String JOB_STATUS_FILTER_PREFIX = FILTER_LABEL_PREFIX + ".STATUS.";

  /** The names of the different list available through this provider. */
  protected static final String[] NAMES = { PROVIDER_PREFIX, LIST_STATUS, LIST_WORKFLOW };

  /** Workflow service instance. */
  private WorkflowService workflowService;

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {

    String listNameTrimmed = StringUtils.trimToEmpty(listName);
    Map<String, String> jobList = new HashMap<String, String>();
    if (StringUtils.equalsIgnoreCase(LIST_STATUS, listName)) {
      jobList.put(Job.Status.PAUSED.toString(), JOB_STATUS_FILTER_PREFIX + Job.Status.PAUSED.toString());
      jobList.put(Job.Status.QUEUED.toString(), JOB_STATUS_FILTER_PREFIX + Job.Status.QUEUED.toString());
      jobList.put(Job.Status.RUNNING.toString(), JOB_STATUS_FILTER_PREFIX + Job.Status.RUNNING.toString());
      jobList.put(Job.Status.WAITING.toString(), JOB_STATUS_FILTER_PREFIX + Job.Status.WAITING.toString());
    } else if (StringUtils.equalsIgnoreCase(LIST_WORKFLOW, listNameTrimmed)) {
      try {
        for (WorkflowDefinition workflowDef : workflowService.listAvailableWorkflowDefinitions()) {
          if (StringUtils.isNotBlank(workflowDef.getTitle())
                  && !jobList.containsKey(workflowDef.getId())
                  && !jobList.containsValue(workflowDef.getTitle())) {
            jobList.put(workflowDef.getId(), workflowDef.getTitle());
          }
        }
      } catch (WorkflowDatabaseException ex) {
        logger.error("Failed to get available workflow definitions from workflow service: {}", ex.getMessage());
      }
    }

    return jobList;
  }

  /** OSGi service activation callback. */
  protected void activate(BundleContext bundleContext) {
    logger.info("Jobs list provider activated!");
  }

  /** OSGi callback for the workflow service. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return StringUtils.equalsIgnoreCase(LIST_STATUS, listName);
  }

  @Override
  public String getDefault() {
    return null;
  }
}
