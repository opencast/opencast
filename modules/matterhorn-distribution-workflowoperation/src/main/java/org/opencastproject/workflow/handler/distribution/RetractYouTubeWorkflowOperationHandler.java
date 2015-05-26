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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.YouTubePublicationService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for retracting a media package from youtube publication channel.
 */
public class RetractYouTubeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  private static final Logger logger = LoggerFactory.getLogger(RetractYouTubeWorkflowOperationHandler.class);

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS = new TreeMap<String, String>();

  /** The youtube publication service */
  private YouTubePublicationService publicationService = null;

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
   * OSGi declarative service configuration callback.
   *
   * @param publicationService
   *          the publication service
   */
  public void setPublicationService(YouTubePublicationService publicationService) {
    this.publicationService = publicationService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(WorkflowInstance, JobContext)
   */
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    try {
      logger.info("Retracting media package {} from youtube publication channel", mediaPackage);

      // Wait for youtube retraction to finish
      Job retractJob = publicationService.retract(mediaPackage);
      if (!waitForStatus(retractJob).isSuccess())
        throw new WorkflowOperationException("The youtube retract job did not complete successfully");

      logger.debug("Retraction from youtube operation complete");

      // Remove the retracted elements from the mediapackage
      Job job = serviceRegistry.getJob(retractJob.getId());
      if (job.getPayload() != null) {
        logger.info("Removing youtube publication element from media package {}", mediaPackage);
        Publication retractedElement = (Publication) MediaPackageElementParser.getFromXml(job.getPayload());
        mediaPackage.remove(retractedElement);
        logger.debug("Remove youtube publication element '{}' complete", retractedElement);
      } else {
        logger.info("No youtube publication found to retract in mediapackage {}!", mediaPackage);
        return createResult(mediaPackage, Action.CONTINUE);
      }
      return createResult(mediaPackage, Action.CONTINUE);
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }
  }
}
