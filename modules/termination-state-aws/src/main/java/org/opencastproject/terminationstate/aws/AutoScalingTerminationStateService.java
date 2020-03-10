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
package org.opencastproject.terminationstate.aws;

import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.terminationstate.api.AbstractJobTerminationStateService;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Option;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.DescribeLifecycleHooksRequest;
import com.amazonaws.services.autoscaling.model.DescribeLifecycleHooksResult;
import com.amazonaws.services.autoscaling.model.LifecycleHook;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatResult;
import com.amazonaws.util.EC2MetadataUtils;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;

public final class AutoScalingTerminationStateService extends AbstractJobTerminationStateService {
  private static final Log logger = new Log(LoggerFactory.getLogger(AutoScalingTerminationStateService.class));

  // AWS String Constants
  private static final String AUTOSCALING_INSTANCE_TERMINATING = "autoscaling:EC2_INSTANCE_TERMINATING";

  public static final String CONFIG_ENABLE = "enable";
  public static final String CONFIG_LIFECYCLE_POLLING_ENABLE = "lifecycle.polling.enable";
  public static final String CONFIG_LIFECYCLE_POLLING_PERIOD = "lifecycle.polling.period";
  public static final String CONFIG_LIFECYCLE_HEARTBEAT_PERIOD = "lifecycle.heartbeat.period";
  public static final String CONFIG_AWS_ACCESS_KEY_ID = "access.id";
  public static final String CONFIG_AWS_SECRET_ACCESS_KEY = "access.secret";

  private static final boolean DEFAULT_ENABLE = false;
  private static final boolean DEFAULT_LIFECYCLE_POLLING_ENABLE = true;
  private static final int DEFAULT_LIFECYCLE_POLLING_PERIOD = 300; //secs
  private static final int DEFAULT_LIFECYCLE_HEARTBEAT_PERIOD = 300; // secs

  protected static final String SCHEDULE_GROUP = AbstractJobTerminationStateService.class.getSimpleName();
  protected static final String SCHEDULE_LIFECYCLE_POLLING_JOB = "PollLifeCycle";
  protected static final String SCHEDULE_LIFECYCLE_HEARTBEAT_JOB = "PollTerminationState";
  protected static final String SCHEDULE_LIFECYCLE_POLLING_TRIGGER = "TriggerPollLifeCycle";
  protected static final String SCHEDULE_LIFECYCLE_HEARTBEAT_TRIGGER = "TriggerHeartbeat";
  protected static final String SCHEDULE_JOB_PARAM_PARENT = "parent";

  private String instanceId;
  private AWSCredentialsProvider credentials;
  private AmazonAutoScaling autoScaling;
  private AutoScalingGroup autoScalingGroup;
  private LifecycleHook lifeCycleHook;

  private Scheduler scheduler;

  // This service must be explicitly enabled
  private boolean enabled = DEFAULT_ENABLE;
  private boolean lifecyclePolling = DEFAULT_LIFECYCLE_POLLING_ENABLE;
  private int lifecyclePollingPeriod = DEFAULT_LIFECYCLE_POLLING_PERIOD;
  private int lifecycleHeartbeatPeriod = DEFAULT_LIFECYCLE_HEARTBEAT_PERIOD;
  private Option<String> accessKeyIdOpt = Option.none();
  private Option<String> accessKeySecretOpt = Option.none();

  protected void activate(ComponentContext componentContext) {
    try {
      configure(componentContext.getProperties());
    } catch (ConfigurationException e) {
      logger.error("Unable to read configuration, using defaults", e.getMessage());
    }

    if (!enabled) {
      logger.info("Service is disabled by configuration");
      return;
    }

    if (accessKeyIdOpt.isNone() && accessKeySecretOpt.isNone()) {
      credentials = new DefaultAWSCredentialsProviderChain();
    } else {
      credentials = new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(accessKeyIdOpt.get(), accessKeySecretOpt.get()));
    }

    instanceId = EC2MetadataUtils.getInstanceId();
    logger.debug("Instance Id is {}", instanceId);

    if (instanceId == null) {
      logger.error("Unable to contact AWS metadata endpoint, Is this node running in AWS EC2?");
      return;
    }

    try {
      autoScaling = AmazonAutoScalingClientBuilder.standard()
              .withRegion(EC2MetadataUtils.getEC2InstanceRegion())
              .withCredentials(credentials).build();
      logger.debug("Created AutoScalingClient {}", autoScaling.toString());

      String autoScalingGroupName = getAutoScalingGroupName();
      logger.debug("Auto scaling group name : {}", autoScalingGroupName);

      if (autoScalingGroupName == null) {
        logger.error("AWS Instance {} is not part of an auto scaling group. Polling will be disabled", instanceId);
        stop();
        return;
      }

      autoScalingGroup = getAutoScalingGroup(autoScalingGroupName);

      if (autoScalingGroup == null) {
        logger.error("Unable to get Auto Scaling Group {}. Polling will be disabled", autoScalingGroupName);
        stop();
        return;
      }

      lifeCycleHook = getLifecycleHook(autoScalingGroupName);

      if (lifeCycleHook == null) {
        logger.error("Auto scaling group {} does not have a termination stage hook. Polling will be disabled",
                autoScalingGroupName);
        stop();
        return;
      } else if (lifecycleHeartbeatPeriod > lifeCycleHook.getHeartbeatTimeout()) {
        logger.warn("Lifecycle Heartbeat Period {} is greater than LifecycleHook's HeartbeatTimeout {}, see https://docs.aws.amazon.com/autoscaling/ec2/userguide/lifecycle-hooks.html",
                lifecycleHeartbeatPeriod, lifeCycleHook.getHeartbeatTimeout());
      }
    } catch (AmazonServiceException e) {
      logger.error("EC2 Autoscaling returned an error", e);
      stop();
      return;
    } catch (AmazonClientException e) {
      logger.error("AWS client can't communicate with EC2 Autoscaling", e);
      stop();
      return;
    }

    try {
      scheduler = new StdSchedulerFactory().getScheduler();
    } catch (SchedulerException e) {
      logger.error("Cannot create quartz scheduler", e.getMessage());
    }

    if (lifecyclePolling && lifecyclePollingPeriod > 0) {
      startPollingLifeCycleHook();
    }
  }

  private String getAutoScalingGroupName() {
    DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId);
    DescribeAutoScalingInstancesResult result = autoScaling.describeAutoScalingInstances(request);
    List<AutoScalingInstanceDetails> instances = result.getAutoScalingInstances();
    logger.debug("Found {} autoscaling instances", instances.size());

    if (!instances.isEmpty()) {
      AutoScalingInstanceDetails autoScalingInstance = instances.get(0);
      return autoScalingInstance.getAutoScalingGroupName();
    }
    return null;
  }

  private AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) {
    DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest()
            .withAutoScalingGroupNames(autoScalingGroupName);
    DescribeAutoScalingGroupsResult result = autoScaling.describeAutoScalingGroups(request);

    List<AutoScalingGroup> groups = result.getAutoScalingGroups();

    if (!groups.isEmpty()) {
      AutoScalingGroup group = groups.get(0);
      return group;
    }

    return null;
  }

  private LifecycleHook getLifecycleHook(String autoScalingGroupName) {
    DescribeLifecycleHooksRequest request = new DescribeLifecycleHooksRequest()
            .withAutoScalingGroupName(autoScalingGroupName);
    DescribeLifecycleHooksResult result = autoScaling.describeLifecycleHooks(request);

    for (LifecycleHook hook : result.getLifecycleHooks()) {
      if (AUTOSCALING_INSTANCE_TERMINATING.equalsIgnoreCase(hook.getLifecycleTransition())) {
        return hook;
      }
    }

    return null;
  }

  protected void configure(Dictionary config) throws ConfigurationException {
    this.enabled = OsgiUtil.getOptCfgAsBoolean(config, CONFIG_ENABLE).getOrElse(DEFAULT_ENABLE);
    this.lifecyclePolling = OsgiUtil.getOptCfgAsBoolean(config, CONFIG_LIFECYCLE_POLLING_ENABLE).getOrElse(DEFAULT_LIFECYCLE_POLLING_ENABLE);
    this.lifecyclePollingPeriod = OsgiUtil.getOptCfgAsInt(config, CONFIG_LIFECYCLE_POLLING_PERIOD).getOrElse(DEFAULT_LIFECYCLE_POLLING_PERIOD);
    this.lifecycleHeartbeatPeriod = OsgiUtil.getOptCfgAsInt(config, CONFIG_LIFECYCLE_HEARTBEAT_PERIOD).getOrElse(DEFAULT_LIFECYCLE_HEARTBEAT_PERIOD);
    this.accessKeyIdOpt = OsgiUtil.getOptCfg(config, CONFIG_AWS_ACCESS_KEY_ID);
    this.accessKeySecretOpt = OsgiUtil.getOptCfg(config, CONFIG_AWS_SECRET_ACCESS_KEY);
  }

  @Override
  public void setState(TerminationState state) {
    if (enabled && autoScaling != null) {
      super.setState(state);

      if (getState() != TerminationState.NONE) {
        // As this might also be called via Endpoint terminate polling if required
        if (lifecyclePolling) {
          stopPollingLifeCycleHook();
        }

        // stop accepting new jobs
        try {
          String host = getServiceRegistry().getRegistryHostname();
          getServiceRegistry().setMaintenanceStatus(host, true);
        } catch (ServiceRegistryException | NotFoundException e) {
          logger.error("Cannot put this host into maintenance", e);
        }
        startPollingTerminationState();
      }
    }
  }

  protected void startPollingLifeCycleHook() {
    try {
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(SCHEDULE_GROUP, SCHEDULE_LIFECYCLE_POLLING_JOB, CheckLifeCycleState.class);
      job.getJobDataMap().put(SCHEDULE_JOB_PARAM_PARENT, this);
      final Trigger trigger = TriggerUtils.makeSecondlyTrigger(lifecyclePollingPeriod);
      trigger.setGroup(SCHEDULE_GROUP);
      trigger.setName(SCHEDULE_LIFECYCLE_POLLING_TRIGGER);
      scheduler.scheduleJob(job, trigger);
      scheduler.start();
      logger.info("Started polling for Lifecycle state change");
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  protected void stopPollingLifeCycleHook() {
    try {
      scheduler.deleteJob(SCHEDULE_GROUP, SCHEDULE_LIFECYCLE_POLLING_JOB);
    } catch (SchedulerException e) {
      // ignore
    }
  }

  public static class CheckLifeCycleState implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      AutoScalingTerminationStateService parent = (AutoScalingTerminationStateService) context.getJobDetail().getJobDataMap().get(SCHEDULE_JOB_PARAM_PARENT);
      if (parent.autoScaling != null) {
        DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest().withInstanceIds(parent.instanceId);
        DescribeAutoScalingInstancesResult result = parent.autoScaling.describeAutoScalingInstances(request);
        List<AutoScalingInstanceDetails> instances = result.getAutoScalingInstances();

        if (!instances.isEmpty()) {
          AutoScalingInstanceDetails autoScalingInstance = instances.get(0);

          if (LifecycleState.TerminatingWait.toString().equalsIgnoreCase(autoScalingInstance.getLifecycleState())) {
            logger.info("Lifecycle state changed to Terminating:Wait");
            parent.stopPollingLifeCycleHook();
            parent.setState(TerminationState.WAIT);
          } else {
            logger.debug("Lifecycle state is {}", autoScalingInstance.getLifecycleState());
          }
        }
      }
    }
  }

  protected void startPollingTerminationState() {
    try {
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(SCHEDULE_GROUP, SCHEDULE_LIFECYCLE_HEARTBEAT_JOB, CheckTerminationState.class);
      job.getJobDataMap().put(SCHEDULE_JOB_PARAM_PARENT, this);
      final Trigger trigger = TriggerUtils.makeSecondlyTrigger(lifecycleHeartbeatPeriod);
      trigger.setGroup(SCHEDULE_GROUP);
      trigger.setName(SCHEDULE_LIFECYCLE_HEARTBEAT_TRIGGER);
      scheduler.scheduleJob(job, trigger);
      scheduler.start();
      logger.info("Started emitting heartbeat until jobs are complete");
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  protected void stopPollingTerminationState() {
    try {
      scheduler.deleteJob(SCHEDULE_GROUP, SCHEDULE_LIFECYCLE_HEARTBEAT_JOB);
    } catch (SchedulerException e) {
      // ignore
    }
  }

  public static class CheckTerminationState implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      AutoScalingTerminationStateService parent = (AutoScalingTerminationStateService) context.getJobDetail().getJobDataMap().get(SCHEDULE_JOB_PARAM_PARENT);

      if (parent.readyToTerminate()) {
        // signal AWS node is ready to terminate
        logger.debug("No jobs running, trying to complete Lifecycle action");
        if (parent.autoScaling != null) {
          CompleteLifecycleActionRequest request = new CompleteLifecycleActionRequest()
                  .withLifecycleActionResult("CONTINUE")
                  .withAutoScalingGroupName(parent.autoScalingGroup.getAutoScalingGroupName())
                  .withLifecycleHookName(parent.lifeCycleHook.getLifecycleHookName())
                  .withInstanceId(parent.instanceId);
          CompleteLifecycleActionResult result = parent.autoScaling.completeLifecycleAction(request);
          logger.info("No jobs running, sent complete Lifecycle action");
        }

        // stop monitoring
        parent.stopPollingTerminationState();
      } else if (parent.getState() == TerminationState.WAIT) {
        // emit heart beat
        logger.debug("Jobs still running, trying to send Lifecycle heartbeat");
        if (parent.autoScaling != null) {
          RecordLifecycleActionHeartbeatRequest request = new RecordLifecycleActionHeartbeatRequest()
                  .withAutoScalingGroupName(parent.autoScalingGroup.getAutoScalingGroupName())
                  .withLifecycleHookName(parent.lifeCycleHook.getLifecycleHookName())
                  .withInstanceId(parent.instanceId);
          RecordLifecycleActionHeartbeatResult result = parent.autoScaling.recordLifecycleActionHeartbeat(request);
          logger.info("Jobs still running, sent Lifecycle heartbeat");
        }
      }
    }
  }

  /**
   * Stop scheduled jobs and free resources
   */
  private void stop() {
    lifecyclePolling = false;
    if (autoScaling != null) {
      autoScaling.shutdown();
      autoScaling = null;
    }

    try {
      if (scheduler != null) {
        this.scheduler.shutdown();
      }
    } catch (SchedulerException e) {
      logger.error("Failed to stop scheduler", e);
    }
  }

  /**
   * OSGI deactivate callback
   */
  public void deactivate() {
    stop();
  }

  /** Methods below are used by test class */

  protected void setAutoScaling(AmazonAutoScaling autoScaling) {
    this.autoScaling = autoScaling;
  }

  protected void setAutoScalingGroup(AutoScalingGroup autoScalingGroup) {
    this.autoScalingGroup = autoScalingGroup;
  }

  protected void setLifecycleHook(LifecycleHook lifecycleHook) {
    this.lifeCycleHook = lifecycleHook;
  }

  protected void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }
}
