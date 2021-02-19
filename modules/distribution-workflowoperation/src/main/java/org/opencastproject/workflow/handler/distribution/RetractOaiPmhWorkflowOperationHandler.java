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
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow operation for retracting a media package from OAI-PMH publication repository.
 */
public class RetractOaiPmhWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RetractOaiPmhWorkflowOperationHandler.class);

  /** Workflow configuration option keys */
  private static final String REPOSITORY = "repository";

  /** The OAI-PMH publication service */
  private OaiPmhPublicationService publicationService = null;

  /**
   * OSGi declarative service configuration callback.
   *
   * @param publicationService
   *          the publication service
   */
  public void setPublicationService(OaiPmhPublicationService publicationService) {
    this.publicationService = publicationService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(ComponentContext)
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
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    String repository = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(REPOSITORY));
    if (repository == null) {
      throw new IllegalArgumentException("No repository has been specified");
    }

    try {
      logger.info("Retracting media package {} publication from OAI-PMH repository {}", mediaPackage, repository);

      // Wait for OAI-PMH retraction to finish
      Job retractJob = publicationService.retract(mediaPackage, repository);
      if (!waitForStatus(retractJob).isSuccess()) {
        throw new WorkflowOperationException("The OAI-PMH retract job did not complete successfully");
      }

      logger.debug("Retraction from OAI-PMH operation complete");

      // Remove the retracted elements from the mediapackage
      Job job = serviceRegistry.getJob(retractJob.getId());
      if (job.getPayload() != null) {
        logger.info("Removing OAI-PMH publication element from media package {}", mediaPackage);
        Publication retractedElement = (Publication) MediaPackageElementParser.getFromXml(job.getPayload());
        mediaPackage.remove(retractedElement);
        logger.debug("Remove OAI-PMH publication element '{}' complete", retractedElement);
      } else {
        logger.info("No OAI-PMH publication found to retract in mediapackage {}!", mediaPackage);
        return createResult(mediaPackage, Action.CONTINUE);
      }
      return createResult(mediaPackage, Action.CONTINUE);
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }
  }
}
