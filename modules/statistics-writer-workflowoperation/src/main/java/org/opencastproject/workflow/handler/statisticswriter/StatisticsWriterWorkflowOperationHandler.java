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

package org.opencastproject.workflow.handler.statisticswriter;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.statistics.api.StatisticsWriter;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.BooleanUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class StatisticsWriterWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final String OPT_FLAVOR = "flavor";
  private static final String OPT_RETRACT = "retract";
  private static final String OPT_MEASUREMENT_NAME = "measurement-name";
  private static final String OPT_RESOURCE_ID_NAME = "organization-resource-id-name";
  private static final String OPT_RETENTION_POLICY = "retention-policy";
  private static final String OPT_TEMPORAL_RESOLUTION = "temporal-resolution";
  private static final String OPT_LENGTH_FIELD_NAME = "length-field-name";

  private StatisticsWriter statisticsWriter;

  public void setStatisticsWriter(StatisticsWriter statisticsWriter) {
    this.statisticsWriter = statisticsWriter;
  }

  private SecurityService securityService;

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *        org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    final String flavor = operation.getConfiguration(OPT_FLAVOR);
    final String measurementName = operation.getConfiguration(OPT_MEASUREMENT_NAME);
    final String resourceIdName = operation.getConfiguration(OPT_RESOURCE_ID_NAME);
    final String lengthFieldName = operation.getConfiguration(OPT_LENGTH_FIELD_NAME);
    final String retentionPolicy = operation.getConfiguration(OPT_RETENTION_POLICY);
    final String temporalResolutionStr = operation.getConfiguration(OPT_TEMPORAL_RESOLUTION);
    final TimeUnit timeResolution;
    try {
      timeResolution = TimeUnit.valueOf(operation.getConfiguration(OPT_TEMPORAL_RESOLUTION).toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException("invalid time unit '" + temporalResolutionStr + "'");
    }
    final boolean retract = BooleanUtils.toBoolean(operation.getConfiguration(OPT_RETRACT));

    for (Track track : mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(flavor))) {
      if (track.getDuration() != null) {
        Duration duration = Duration.ofMillis(track.getDuration());
        if (retract) {
          duration = duration.negated();
        }
        statisticsWriter.writeDuration(
                securityService.getOrganization().getId(),
                measurementName,
                retentionPolicy,
                resourceIdName,
                lengthFieldName,
                timeResolution,
                duration);
      }
    }

    return createResult(mediaPackage, Action.CONTINUE);
  }
}
