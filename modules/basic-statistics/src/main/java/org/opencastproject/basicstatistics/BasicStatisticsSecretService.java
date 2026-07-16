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
package org.opencastproject.basicstatistics;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.systems.OpencastConstants;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A service for the rotating daily secret used in sessions hashes for Opencast basic statistics
 */
@Component(
    property = {
        "service.description=Basic Statistics Secret Service",
        "service.pid=org.opencastproject.basicstatistics.BasicStatisticsSecretService"
    },
    immediate = true,
    service = BasicStatisticsSecretService.class
)
public class BasicStatisticsSecretService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(BasicStatisticsSecretService.class);

  private volatile byte[] currentSecret;

  private boolean isLeaderNode = false;

  private String leaderUrl;

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  private final SecureRandom secureRandom = new SecureRandom();

  private Scheduler quartz = null;

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor();

  private ScheduledFuture<?> pollingTask;

  /** Sets the trusted http client */
  @Reference
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  @Activate
  public void activate(ComponentContext cc) {
    // TODO: Remove next line.
    rotateSecret();
    try {
      if (cc != null) {
        String serverUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
        // Effectively the admin ui url for now
        leaderUrl = cc.getBundleContext().getProperty("org.opencastproject.basicstatistics.leader.url");
        isLeaderNode = serverUrl.equals(leaderUrl);
      }

      if (isLeaderNode) {
        rotateSecret();

        quartz = new StdSchedulerFactory().getScheduler();
        quartz.start();

        JobDetail job = new JobDetail();
        job.setName("basicstatistics-secret-rotation");
        job.setJobClass(SecretRotationJob.class);

        CronTrigger trigger = new CronTrigger();
        trigger.setName("basicstatistics-secret-rotation-trigger");
        trigger.setCronExpression("0 0 4 * * ?");

        quartz.scheduleJob(job, trigger);
      }

      if (!isLeaderNode) {
        // Fetch immediately so we have a secret before handling requests.
        fetchSecret();

        // Then poll every minute.
        pollingTask = scheduler.scheduleWithFixedDelay(this::pollLeader, 1, 1, TimeUnit.MINUTES);
      }
      // TODO: Error handling
    } catch (SchedulerException | ParseException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Deactivate
  public void deactivate() {
    if (pollingTask != null) {
      pollingTask.cancel(true);
    }
    scheduler.shutdownNow();

    if (currentSecret != null) {
      Arrays.fill(currentSecret, (byte) 0);
    }
  }

  public byte[] getCurrentSecret() {
    return currentSecret;
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

  private void pollLeader() {
    try {
      fetchSecret();
    } catch (Exception e) {
      logger.warn("Failed to fetch daily secret from leader", e);
    }
  }

  private void fetchSecret() throws IOException {
    // TODO: This likely does not work
    HttpGet get = new HttpGet(leaderUrl + "/basicstatistics-internal/daily-secret");
    HttpResponse response = client.execute(get);

    int status = response.getStatusLine().getStatusCode();
    if (status != HttpStatus.SC_OK) {
      throw new IOException("Failed to fetch secret: HTTP " + status);
    }

    String secret = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8).trim();
    byte[] newSecret = Base64.getDecoder().decode(secret);

    if (!Arrays.equals(currentSecret, newSecret)) {
      byte[] old = currentSecret;
      currentSecret = newSecret;

      if (old != null) {
        Arrays.fill(old, (byte) 0);
      }
    }
  }

  public class SecretRotationJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      BasicStatisticsSecretService service =
          (BasicStatisticsSecretService) context.getMergedJobDataMap().get("service");

      service.rotateSecret();
    }
  }
}
