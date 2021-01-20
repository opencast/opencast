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
package org.opencastproject.workflow.handler.distribution;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * WOH that retracts elements from an internal distribution channel and removes the reflective publication elements from
 * the media package.
 */
public class ConfigurableRetractWorkflowOperationHandler extends ConfigurableWorkflowOperationHandlerBase {

  private static final String CHANNEL_ID_KEY = "channel-id";

  static final String RETRACT_STREAMING = "retract-streaming";
  static final boolean RETRACT_STREAMING_DEFAULT = false;

  // service references
  private DownloadDistributionService downloadDistributionService;
  private StreamingDistributionService streamingDistributionService;

  /** OSGi DI */
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    this.downloadDistributionService = distributionService;
  }

  void setStreamingDistributionService(StreamingDistributionService distributionService) {
    this.streamingDistributionService = distributionService;
  }

  @Override
  protected DownloadDistributionService getDownloadDistributionService() {
    assert (downloadDistributionService != null);
    return downloadDistributionService;
  }

  @Override
  protected StreamingDistributionService getStreamingDistributionService() {
    assert (streamingDistributionService != null);
    return streamingDistributionService;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    notNull(workflowInstance, "workflowInstance");

    boolean retractStreaming = RETRACT_STREAMING_DEFAULT;
    final WorkflowOperationInstance op = workflowInstance.getCurrentOperation();
    String retractStreamingString = op.getConfiguration(RETRACT_STREAMING);
    if (retractStreamingString != null) {
      retractStreaming = BooleanUtils.toBoolean(StringUtils.trimToEmpty(retractStreamingString));
    }

    final MediaPackage mp = workflowInstance.getMediaPackage();
    final String channelId = StringUtils.trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(
            CHANNEL_ID_KEY));
    if (StringUtils.isBlank((channelId))) {
      throw new WorkflowOperationException("Unable to publish this mediapackage as the configuration key "
              + CHANNEL_ID_KEY + " is missing. Unable to determine where to publish these elements.");
    }

    retract(mp, channelId, retractStreaming);

    return createResult(mp, Action.CONTINUE);
  }

}
