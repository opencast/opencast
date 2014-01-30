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
package org.opencastproject.workflow.handler;

import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.EpisodeServiceException;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for deleting a media package from the episode service.
 */
public class DeleteArchiveWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(DeleteArchiveWorkflowOperationHandler.class);

  /** The episode service */
  private EpisodeService episodeService = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the search service. Implementation
   * assumes that the reference is configured as being static.
   * 
   * @param episodeService
   *          an instance of the search service
   */
  protected void setEpisodeService(EpisodeService episodeService) {
    this.episodeService = episodeService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(WorkflowInstance, JobContext)
   */
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    boolean deleted;
    try {
      deleted = episodeService.delete(mediaPackage.getIdentifier().compact());
    } catch (EpisodeServiceException e) {
      logger.warn("Unable to delete mediapackge {} from the archive: {}", mediaPackage, e);
      throw new WorkflowOperationException("Unable to delete medipackage from the archive!", e);
    }

    if (!deleted) {
      logger.info("There's no mediapackage {} to delete from the archive, see logs for more accurate info.",
              mediaPackage);
    } else {
      logger.info("Sucessfully deleted mediapackage {} from the archive.", mediaPackage);
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }

}
