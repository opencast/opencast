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

import static com.entwinemedia.fn.fns.Strings.trimToNone;
import static java.lang.String.format;
import static org.opencastproject.util.JobUtil.waitForJobs;
import static org.opencastproject.util.data.Collections.set;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.util.data.Collections;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Workflow operation for handling "republish" operations to OAI-PMH repositories. */
public final class RepublishOaiPmhWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RepublishOaiPmhWorkflowOperationHandler.class);

  private OaiPmhPublicationService oaiPmhPublicationService = null;

  /** The configuration options */
  private static final String OPT_SOURCE_FLAVORS = "source-flavors";
  private static final String OPT_SOURCE_TAGS = "source-tags";
  private static final String OPT_REPOSITORY = "repository";

  @Override
  public WorkflowOperationResult start(WorkflowInstance wi, JobContext context) throws WorkflowOperationException {
    final MediaPackage mp = wi.getMediaPackage();
    // The flavors of the elements that are to be published
    final Set<String> flavors = new HashSet<>();
    // Check which flavors have been configured
    final List<String> configuredFlavors = getOptConfig(wi, OPT_SOURCE_FLAVORS).bind(trimToNone).map(asList.toFn())
            .getOr(Collections.<String> nil());
    for (String flavor : configuredFlavors) {
      flavors.add(flavor);
    }
    // Get the configured tags
    final Set<String> tags = set(getOptConfig(wi, OPT_SOURCE_TAGS).getOr(""));
    // repository
    final String repository = getConfig(wi, OPT_REPOSITORY);

    logger.debug("Start updating metadata of the media package {} in OAI-PMH repository {}",
            mp.getIdentifier().compact(), repository);
    try {
      Job updateMetadataJob = oaiPmhPublicationService.updateMetadata(mp, repository, flavors, tags, true);
      if (updateMetadataJob == null) {
        logger.info("Unable to create an OAI-PMH update metadata job for the media package {} in repository {}",
                mp.getIdentifier().compact(), repository);
        return createResult(mp, Action.CONTINUE);
      }

      if (!waitForJobs(serviceRegistry, updateMetadataJob).isSuccess()) {
        throw new WorkflowOperationException(format(
                "OAI-PMH update metadata job for the media package %s did not end successfully",
                mp.getIdentifier().compact()));
      }
    } catch (MediaPackageException | PublicationException | IllegalArgumentException | IllegalStateException e) {
      throw new WorkflowOperationException(format(
              "Unable to create an OAI-PMH update metadata job for the media package %s in repository %s",
              mp.getIdentifier().compact(), repository), e);
    }
    logger.debug("Updating metadata of the media package {} in OAI-PMH repository {} done",
            mp.getIdentifier().compact(), repository);
    return createResult(mp, Action.CONTINUE);
  }

  /** OSGI DI */
  public void setOaiPmhPublicationService(OaiPmhPublicationService oaiPmhPublicationService) {
    this.oaiPmhPublicationService = oaiPmhPublicationService;
  }

}
