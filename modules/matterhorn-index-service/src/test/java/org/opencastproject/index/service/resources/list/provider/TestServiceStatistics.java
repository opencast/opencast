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

import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.ServiceStatistics;

import org.junit.Ignore;

import java.util.Date;

/**
 * An implementation of ServiceStatistics which returns constant values, ideal for testing. Here are the values you can
 * expect: long MEAN_RUNTIME = 99l; long MEAN_QUEUETIME = 22l; public static final int FINISHED_JOBS = 33; int
 * RUNNING_JOBS = 11; int QUEUED_JOBS = 5; String HOST = "HOST 1"; String SERVICE_TYPE = "Service Type";
 *
 * @author ademasi
 *
 */
@Ignore
public class TestServiceStatistics implements ServiceStatistics {

  public static final long MEAN_RUNTIME = 99L;
  public static final long MEAN_QUEUETIME = 22L;
  public static final int FINISHED_JOBS = 33;
  public static final int RUNNING_JOBS = 11;
  public static final int QUEUED_JOBS = 5;
  public static final String HOST = "HOST 1";
  public static final String SERVICE_TYPE = "Service Type";

  @Override
  public ServiceRegistration getServiceRegistration() {
    return new ServiceRegistration() {

      @Override
      public boolean isOnline() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isJobProducer() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isInMaintenanceMode() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public int getWarningStateTrigger() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public Date getStateChanged() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getServiceType() {
        return SERVICE_TYPE;
      }

      @Override
      public ServiceState getServiceState() {
        return ServiceState.NORMAL;
      }

      @Override
      public String getPath() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Date getOnlineFrom() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getHost() {
        return HOST;
      }

      @Override
      public int getErrorStateTrigger() {
        // TODO Auto-generated method stub
        return 0;
      }
    };
  }

  @Override
  public long getMeanRunTime() {
    return MEAN_RUNTIME;
  }

  @Override
  public long getMeanQueueTime() {
    return MEAN_QUEUETIME;
  }

  @Override
  public int getFinishedJobs() {
    return FINISHED_JOBS;
  }

  @Override
  public int getRunningJobs() {
    return RUNNING_JOBS;
  }

  @Override
  public int getQueuedJobs() {
    return QUEUED_JOBS;
  }
}
