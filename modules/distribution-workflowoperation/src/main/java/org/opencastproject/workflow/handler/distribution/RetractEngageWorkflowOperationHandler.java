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

import static org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Workflow operation for retracting a media package from the engage player.
 */
public class RetractEngageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RetractEngageWorkflowOperationHandler.class);

  /** The streaming distribution service */
  protected StreamingDistributionService streamingDistributionService = null;

  /** The download distribution service */
  protected DownloadDistributionService downloadDistributionService = null;

  /** The search service */
  protected SearchService searchService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param streamingDistributionService
   *          the streaming distribution service
   */
  protected void setStreamingDistributionService(StreamingDistributionService streamingDistributionService) {
    this.streamingDistributionService = streamingDistributionService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param downloadDistributionService
   *          the download distribution service
   */
  protected void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    this.downloadDistributionService = downloadDistributionService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the search service. Implementation
   * assumes that the reference is configured as being static.
   *
   * @param searchService
   *          an instance of the search service
   */
  protected void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
  }

  /**
   * Generate the jobs retracted the selected elements
   * @param retractElementIds The list of element ids to retract
   * @param searchMediaPackage The mediapackage from the search service
   * @return
   * @throws DistributionException
   */
  protected List<Job> retractElements(Set<String> retractElementIds, MediaPackage searchMediaPackage) throws
          DistributionException {
    List<Job> jobs = new ArrayList<Job>();
    if (retractElementIds.size() > 0) {
      Job retractDownloadDistributionJob
          = downloadDistributionService.retract(CHANNEL_ID, searchMediaPackage, retractElementIds);
      if (retractDownloadDistributionJob != null) {
        jobs.add(retractDownloadDistributionJob);
      }
    }
    if (streamingDistributionService.publishToStreaming()) {
      for (MediaPackageElement element : searchMediaPackage.getElements()) {
        Job retractStreamingJob = streamingDistributionService.retract(CHANNEL_ID, searchMediaPackage,
                element.getIdentifier());
        if (retractStreamingJob != null) {
          jobs.add(retractStreamingJob);
        }
      }
    }
    return jobs;
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
    List<Job> jobs;
    try {
      SearchQuery query = new SearchQuery().withId(mediaPackage.getIdentifier().toString());
      SearchResult result = searchService.getByQuery(query);
      if (result.size() == 0) {
        logger.info("The search service doesn't know mediapackage {}", mediaPackage);
        return createResult(mediaPackage, Action.SKIP);
      } else if (result.size() > 1) {
        logger.warn("More than one mediapackage with id {} returned from search service", mediaPackage.getIdentifier());
        throw new WorkflowOperationException("More than one mediapackage with id " + mediaPackage.getIdentifier()
                + " found");
      } else {
        Set<String> retractElementIds = new HashSet<String>();
        MediaPackage searchMediaPackage = result.getItems()[0].getMediaPackage();
        logger.info("Retracting media package {} from download/streaming distribution channel", searchMediaPackage);
        for (MediaPackageElement element : searchMediaPackage.getElements()) {
          retractElementIds.add(element.getIdentifier());
        }
        jobs = retractElements(retractElementIds, searchMediaPackage);
      }

      // Wait for retraction to finish
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("One of the download/streaming retract job did not complete successfully");
      }

      logger.debug("Retraction operation complete");

      logger.info("Removing media package {} from the search index", mediaPackage);
      Job deleteFromSearch = searchService.delete(mediaPackage.getIdentifier().toString());
      if (!waitForStatus(deleteFromSearch).isSuccess()) {
        throw new WorkflowOperationException("Removing media package from search did not complete successfully");
      }

      logger.debug("Remove from search operation complete");

      // Remove publication element
      logger.info("Removing engage publication element from media package {}", mediaPackage);
      Publication[] publications = mediaPackage.getPublications();
      for (Publication publication : publications) {
        if (CHANNEL_ID.equals(publication.getChannel())) {
          mediaPackage.remove(publication);
          logger.debug("Remove engage publication element '{}' complete", publication);
        }
      }

      return createResult(mediaPackage, Action.CONTINUE);
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }
  }

}
