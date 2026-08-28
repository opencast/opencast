/*
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
package org.opencastproject.basicstatisticssecret.impl;

import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretService;
import org.opencastproject.basicstatisticssecret.api.BasicStatisticsSecretServiceException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Arrays;

/**
 * A service for the rotating daily secret used in sessions hashes for Opencast basic statistics
 *
 * Must only run on a single node in the cluster.
 */
@Component(
    property = {
        "service.description=Basic Statistics Secret Service",
    },
    immediate = true,
    service = BasicStatisticsSecretService.class
)
public class BasicStatisticsSecretServiceImpl implements BasicStatisticsSecretService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsSecretServiceImpl.class);

  private volatile byte[] currentSecret;

  private final SecureRandom secureRandom = new SecureRandom();

  private Scheduler quartz = null;

  @Activate
  public void activate(ComponentContext cc) {
    try {
      rotateSecret();

      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();

      JobDetail job = new JobDetail();
      job.setName("basicstatistics-secret-rotation");
      job.setJobClass(SecretRotationJob.class);

      // Make the service available to the job
      job.getJobDataMap().put("service", this);

      CronTrigger trigger = new CronTrigger();
      trigger.setName("basicstatistics-secret-rotation-trigger");
      trigger.setCronExpression("0 0 4 * * ?");

      quartz.scheduleJob(job, trigger);
    } catch (SchedulerException | ParseException e) {
      logger.error("Could not schedule the daily secret rotation; "
          + "the basic statistics secret service will not start", e);
      throw new RuntimeException(e);
    }
  }

  @Deactivate
  public void deactivate() {
    if (quartz != null) {
      try {
        quartz.shutdown(true);
      } catch (SchedulerException e) {
        logger.warn("Unable to shut down Quartz scheduler", e);
      }
    }

    if (currentSecret != null) {
      Arrays.fill(currentSecret, (byte) 0);
    }
  }

  public byte[] getCurrentSecret() throws BasicStatisticsSecretServiceException {
    byte[] secret = currentSecret;
    if (secret == null) {
      throw new BasicStatisticsSecretServiceException("No secret has been generated yet");
    }
    return secret;
  }

  private void rotateSecret() {
    byte[] secret = new byte[32];
    secureRandom.nextBytes(secret);

    byte[] old = currentSecret;
    currentSecret = secret;

    if (old != null) {
      Arrays.fill(old, (byte) 0);
    }
  }

  public static class SecretRotationJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
      try {
        BasicStatisticsSecretServiceImpl service =
            (BasicStatisticsSecretServiceImpl) context.getMergedJobDataMap().get("service");
        service.rotateSecret();
      } catch (Exception e) {
        logger.error("Failed to rotate the daily secret; it will keep using the previous one until the next "
            + "scheduled rotation", e);
      }
    }
  }
}
