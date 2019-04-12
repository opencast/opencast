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

package org.opencastproject.workflow.impl.jmx;

import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowStatisticsReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

public class WorkflowsStatistics extends NotificationBroadcasterSupport implements WorkflowsStatisticsMXBean {

  private static final String DELIMITER = ";";
  private long sequenceNumber = 1;

  private Map<String, Long> workflowCounts = new HashMap<>();
  private WorkflowStatisticsReport workflowStatisticsReport;

  public WorkflowsStatistics(WorkflowStatisticsReport workflowStatisticsReport, List<WorkflowInstance> workflows) {
    updateWorkflow(workflowStatisticsReport, workflows);
  }

  public void updateWorkflow(WorkflowStatisticsReport workflowStatisticsReport, List<WorkflowInstance> workflows) {
    this.workflowStatisticsReport = workflowStatisticsReport;
    for (WorkflowInstance wf : workflows) {
      Long count = workflowCounts.get(wf.getTemplate());
      if (count == null) {
        workflowCounts.put(wf.getTemplate(), 1L);
      } else {
        workflowCounts.put(wf.getTemplate(), count++);
      }
    }
    sendNotification(JmxUtil.createUpdateNotification(this, sequenceNumber++, "Workflow updated"));
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    String[] types = new String[] { JmxUtil.OPENCAST_UPDATE_NOTIFICATION };

    String name = Notification.class.getName();
    String description = "An update was executed";
    MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
    return new MBeanNotificationInfo[] { info };
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getTotal()
   */
  @Override
  public int getTotal() {
    return (int) workflowStatisticsReport.getTotal();
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getInstantiated()
   */
  @Override
  public int getInstantiated() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.INSTANTIATED);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getRunning()
   */
  @Override
  public int getRunning() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.RUNNING);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getOnHold()
   */
  @Override
  public int getOnHold() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.PAUSED);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getStopped()
   */
  @Override
  public int getStopped() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.INSTANTIATED.STOPPED);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getFinished()
   */
  @Override
  public int getFinished() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.SUCCEEDED);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getFailing()
   */
  @Override
  public int getFailing() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.FAILING);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getFailed()
   */
  @Override
  public int getFailed() {
    return (int) workflowStatisticsReport.get(WorkflowInstance.WorkflowState.FAILED);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getWorkflowsOnHold()
   */
  @Override
  public String[] getWorkflowsOnHold() {
    List<String> operationList = new ArrayList<String>();
    for (Entry<String, Long> entry : workflowCounts.entrySet()) {
      operationList.add(entry.getKey() + DELIMITER + entry.getValue());
    }
    return operationList.toArray(new String[operationList.size()]);
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getAverageWorkflowProcessingTime()
   */
  @Override
  public String[] getAverageWorkflowProcessingTime() {
    // Not implemented yet
    return new String[0];
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getAverageWorkflowQueueTime()
   */
  @Override
  public String[] getAverageWorkflowQueueTime() {
    // Not implemented yet
    return new String[0];
  }

  /**
   * @see org.opencastproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getAverageWorkflowHoldTime()
   */
  @Override
  public String[] getAverageWorkflowHoldTime() {
    // Not implemented yet
    return new String[0];
  }

}
