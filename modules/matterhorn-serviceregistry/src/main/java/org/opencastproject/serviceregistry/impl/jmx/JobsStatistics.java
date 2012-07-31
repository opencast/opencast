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
package org.opencastproject.serviceregistry.impl.jmx;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.util.data.Tuple3;
import org.opencastproject.util.jmx.JmxUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

public class JobsStatistics extends NotificationBroadcasterSupport implements JobsStatisticsMXBean {

  private static final String DELIMITER = ";";

  private Map<Tuple3<String, String, Status>, Long> jobCounts = new HashMap<Tuple3<String, String, Status>, Long>();
  private Map<String, Long> avgRunTimes = new HashMap<String, Long>();
  private Map<String, Long> avgQueueTimes = new HashMap<String, Long>();

  // Job Table runTime, queueTime
  private long sequenceNumber = 1;
  private final String hostName;

  public JobsStatistics(String hostName) {
    this.hostName = hostName;
  }

  public void updateJobCount(List<Object[]> perHostServiceCount) {
    jobCounts.clear();
    for (Object[] result : perHostServiceCount) {
      Status status = Job.Status.values()[(Integer) result[2]];
      jobCounts.put(Tuple3.tuple3((String) result[0], (String) result[1], status), (Long) result[3]);
    }
    sendNotification(JmxUtil.createUpdateNotification(this, sequenceNumber++, "Job updated"));
  }

  public void updateAvg(List<Object[]> avgOperations) {
    avgRunTimes.clear();
    avgQueueTimes.clear();
    for (Object[] result : avgOperations) {
      Long avgRunTime = ((Double) result[1]).longValue();
      Long avgQueueTime = ((Double) result[2]).longValue();

      avgRunTimes.put((String) result[0], avgRunTime);
      avgQueueTimes.put((String) result[0], avgQueueTime);
    }
    sendNotification(JmxUtil.createUpdateNotification(this, sequenceNumber++, "Job updated"));
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    String[] types = new String[] { JmxUtil.MATTERHORN_UPDATE_NOTIFICATION };

    String name = Notification.class.getName();
    String description = "An update was executed";
    MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
    return new MBeanNotificationInfo[] { info };
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getJobCount()
   */
  @Override
  public int getJobCount() {
    return countJobs(null, null);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getRunningJobCount()
   */
  @Override
  public int getRunningJobCount() {
    return countJobs(null, Status.RUNNING);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getQueuedJobCount()
   */
  @Override
  public int getQueuedJobCount() {
    return countJobs(null, Status.QUEUED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFinishedJobCount()
   */
  @Override
  public int getFinishedJobCount() {
    return countJobs(null, Status.FINISHED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFailedJobCount()
   */
  @Override
  public int getFailedJobCount() {
    return countJobs(null, Status.FAILED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getJobCountByNode()
   */
  @Override
  public int getJobCountByNode() {
    return countJobs(hostName, null);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getRunningJobCountByNode()
   */
  @Override
  public int getRunningJobCountByNode() {
    return countJobs(hostName, Status.RUNNING);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getQueuedJobCountByNode()
   */
  @Override
  public int getQueuedJobCountByNode() {
    return countJobs(hostName, Status.QUEUED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFinishedJobCountByNode()
   */
  @Override
  public int getFinishedJobCountByNode() {
    return countJobs(hostName, Status.FINISHED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFailedJobCountByNode()
   */
  @Override
  public int getFailedJobCountByNode() {
    return countJobs(hostName, Status.FAILED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getAverageJobRunTime()
   */
  @Override
  public String[] getAverageJobRunTime() {
    List<String> avgJobList = new ArrayList<String>();
    for (Entry<String, Long> entry : avgRunTimes.entrySet()) {
      avgJobList.add(entry.getKey() + DELIMITER + entry.getValue());
    }
    return avgJobList.toArray(new String[avgJobList.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getAverageJobQueueTime()
   */
  @Override
  public String[] getAverageJobQueueTime() {
    List<String> avgJobList = new ArrayList<String>();
    for (Entry<String, Long> entry : avgQueueTimes.entrySet()) {
      avgJobList.add(entry.getKey() + DELIMITER + entry.getValue());
    }
    return avgJobList.toArray(new String[avgJobList.size()]);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getJobs()
   */
  @Override
  public String[] getJobs() {
    return toJobCountArray(null, null);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getRunningJobs()
   */
  @Override
  public String[] getRunningJobs() {
    return toJobCountArray(null, Status.RUNNING);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getQueuedJobs()
   */
  @Override
  public String[] getQueuedJobs() {
    return toJobCountArray(null, Status.QUEUED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFinishedJobs()
   */
  @Override
  public String[] getFinishedJobs() {
    return toJobCountArray(null, Status.FINISHED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFailedJobs()
   */
  @Override
  public String[] getFailedJobs() {
    return toJobCountArray(null, Status.FAILED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getJobsByNode()
   */
  @Override
  public String[] getJobsByNode() {
    return toJobCountArray(hostName, null);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getRunningJobsByNode()
   */
  @Override
  public String[] getRunningJobsByNode() {
    return toJobCountArray(hostName, Status.RUNNING);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getQueuedJobsByNode()
   */
  @Override
  public String[] getQueuedJobsByNode() {
    return toJobCountArray(hostName, Status.QUEUED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFinishedJobsByNode()
   */
  @Override
  public String[] getFinishedJobsByNode() {
    return toJobCountArray(hostName, Status.FINISHED);
  }

  /**
   * @see org.opencastproject.serviceregistry.impl.jmx.JobsStatisticsMXBean#getFailedJobsByNode()
   */
  @Override
  public String[] getFailedJobsByNode() {
    return toJobCountArray(hostName, Status.FAILED);
  }

  private int countJobs(String hostName, Status status) {
    int i = 0;
    for (Entry<Tuple3<String, String, Status>, Long> entry : jobCounts.entrySet()) {
      if (hostName != null && !hostName.equals(entry.getKey().getA()))
        continue;
      if (status != null && !status.equals(entry.getKey().getC()))
        continue;
      i += entry.getValue();
    }
    return i;
  }

  private String[] toJobCountArray(String hostName, Status status) {
    List<String> list = new ArrayList<String>();
    for (Entry<Tuple3<String, String, Status>, Long> entry : jobCounts.entrySet()) {
      if (hostName != null && !hostName.equals(entry.getKey().getA()))
        continue;
      if (status != null && !status.equals(entry.getKey().getC()))
        continue;
      list.add(entry.getKey().getA() + DELIMITER + entry.getKey().getB() + DELIMITER + entry.getValue());
    }
    return list.toArray(new String[list.size()]);
  }

}
