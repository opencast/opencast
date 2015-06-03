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
package org.opencastproject.workflow.handler.distribution;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * WOH that distributes selected elements to an internal distribution channel and adds reflective publication elements
 * to the media package.
 */
public class PublishInternalWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishInternalWorkflowOperationHandler.class);

  /** The identifier of the channel this workflow operation handler is publishing to */
  public static final String CHANNEL_ID = "internal";

  // service references
  private DownloadDistributionService distributionService;

  // workflow configuration options
  static final String SOURCE_TAGS = "source-tags";
  static final String SOURCE_FLAVORS = "source-flavors";

  /** OSGi DI */
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    this.distributionService = distributionService;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    RequireUtil.notNull(workflowInstance, "workflowInstance");

    final MediaPackage mp = workflowInstance.getMediaPackage();
    final WorkflowOperationInstance op = workflowInstance.getCurrentOperation();

    final String[] sourceFlavors = StringUtils.split(StringUtils.trimToEmpty(op.getConfiguration(SOURCE_FLAVORS)), ",");
    final String[] sourceTags = StringUtils.split(StringUtils.trimToEmpty(op.getConfiguration(SOURCE_TAGS)), ",");

    if (sourceTags.length == 0 && sourceFlavors.length == 0) {
      logger.warn(
              "No tags or flavors have been specified, so nothing will be published to the publication channel '{}'",
              CHANNEL_ID);
      return createResult(mp, Action.CONTINUE);
    }

    // Configure the element selector
    final SimpleElementSelector selector = new SimpleElementSelector();
    for (String flavor : sourceFlavors) {
      selector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceTags) {
      selector.addTag(tag);
    }

    Map<Job, MediaPackageElement> jobs = new HashMap<>();
    for (final MediaPackageElement element : selector.select(mp, false)) {
      logger.info("Start publishing element '{}' of media package '{}' to publication channel '{}'", new Object[] {
              element, mp, CHANNEL_ID });
      try {
        final Job job = distributionService.distribute(CHANNEL_ID, mp, element.getIdentifier(), true);
        jobs.put(job, element);
        logger.debug("Distribution job '{}' for element '{}' of media package '{}' created.", new Object[] { job,
                element, mp });
      } catch (DistributionException | MediaPackageException e) {
        logger.error("Creating the distribution job for element '{}' of media package '{}' failed: {}", new Object[] {
                element, mp, getStackTrace(e) });
        throw new WorkflowOperationException(e);
      }
    }

    if (jobs.size() < 1) {
      logger.info("No mediapackage element was found to distribute");
      return createResult(mp, Action.CONTINUE);
    }

    // Wait until all distribution jobs have returned
    if (!waitForStatus(jobs.keySet().toArray(new Job[jobs.keySet().size()])).isSuccess())
      throw new WorkflowOperationException("One of the distribution jobs did not complete successfully");

    for (Entry<Job, MediaPackageElement> job : jobs.entrySet()) {
      MediaPackageElement element;
      try {
        element = MediaPackageElementParser.getFromXml(job.getKey().getPayload());
      } catch (MediaPackageException e) {
        logger.error("Job '{}' returned payload ({}) that could not be parsed to media package element: {}",
                new Object[] { job, job.getKey().getPayload(), ExceptionUtils.getStackTrace(e) });
        throw new WorkflowOperationException(e);
      }

      Publication publication = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_ID, element.getURI(),
              element.getMimeType());
      publication.setFlavor(element.getFlavor());
      mp.add(publication);
    }

    return createResult(mp, Action.CONTINUE);
  }

}
