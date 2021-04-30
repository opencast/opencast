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

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.ofChannel;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Strings.toBool;
import static org.opencastproject.util.data.functions.Strings.trimToNone;

import org.opencastproject.distribution.api.StreamingDistributionService;
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
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The workflow definition for handling "publish" operations
 */
public class PublishOaiPmhWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishOaiPmhWorkflowOperationHandler.class);

  /** Workflow configuration option keys */
  private static final String DOWNLOAD_FLAVORS = "download-flavors";
  private static final String DOWNLOAD_TAGS = "download-tags";
  private static final String STREAMING_TAGS = "streaming-tags";
  private static final String STREAMING_FLAVORS = "streaming-flavors";
  private static final String CHECK_AVAILABILITY = "check-availability";
  private static final String REPOSITORY = "repository";
  private static final String EXTERNAL_TEMPLATE = "external-template";
  private static final String EXTERNAL_CHANNEL_NAME = "external-channel";
  private static final String EXTERNAL_MIME_TYPE = "external-mime-type";

  /** The publication service */
  private OaiPmhPublicationService publicationService = null;

  /** The streaming distribution service */
  private StreamingDistributionService streamingDistributionService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param publicationService
   *          the publication service
   */
  public void setPublicationService(OaiPmhPublicationService publicationService) {
    this.publicationService = publicationService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param streamingDistributionService
   *          the streaming distribution service
   */
  protected void setStreamingDistributionService(StreamingDistributionService streamingDistributionService) {
    this.streamingDistributionService = streamingDistributionService;
  }

  /** OSGi component activation. */
  @Override
  public void activate(ComponentContext cc) {
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running distribution workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Check which tags have been configured
    String downloadTags = StringUtils
            .trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(DOWNLOAD_TAGS));
    String downloadFlavors = StringUtils
            .trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(DOWNLOAD_FLAVORS));
    String streamingTags = StringUtils
            .trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(STREAMING_TAGS));
    String streamingFlavors = StringUtils
            .trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(STREAMING_FLAVORS));
    boolean checkAvailability = option(workflowInstance.getCurrentOperation().getConfiguration(CHECK_AVAILABILITY))
            .bind(trimToNone).map(toBool).getOrElse(true);
    String repository = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(REPOSITORY));

    Opt<String> externalChannel = getOptConfig(workflowInstance.getCurrentOperation(), EXTERNAL_CHANNEL_NAME);
    Opt<String> externalTempalte = getOptConfig(workflowInstance.getCurrentOperation(), EXTERNAL_TEMPLATE);
    Opt<MimeType> externalMimetype = getOptConfig(workflowInstance.getCurrentOperation(), EXTERNAL_MIME_TYPE)
            .bind(MimeTypes.toMimeType);

    if (repository == null) {
      throw new IllegalArgumentException("No repository has been specified");
    }

    String[] sourceDownloadTags = StringUtils.split(downloadTags, ",");
    String[] sourceDownloadFlavors = StringUtils.split(downloadFlavors, ",");
    String[] sourceStreamingTags = StringUtils.split(streamingTags, ",");
    String[] sourceStreamingFlavors = StringUtils.split(streamingFlavors, ",");

    if (sourceDownloadTags.length == 0 && sourceDownloadFlavors.length == 0 && sourceStreamingTags.length == 0
            && sourceStreamingFlavors.length == 0) {
      logger.warn("No tags or flavors have been specified, so nothing will be published to the engage");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    final SimpleElementSelector downloadElementSelector = new SimpleElementSelector();
    for (String flavor : sourceDownloadFlavors) {
      downloadElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceDownloadTags) {
      downloadElementSelector.addTag(tag);
    }
    final Collection<MediaPackageElement> downloadElements = downloadElementSelector.select(mediaPackage, false);

    final Collection<MediaPackageElement> streamingElements;
    if (streamingDistributionService.publishToStreaming()) {
      final SimpleElementSelector streamingElementSelector = new SimpleElementSelector();
      for (String flavor : sourceStreamingFlavors) {
        streamingElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      }
      for (String tag : sourceStreamingTags) {
        streamingElementSelector.addTag(tag);
      }
      streamingElements = streamingElementSelector.select(mediaPackage, false);
    } else {
      streamingElements = list();
    }

    try {
      Set<String> downloadElementIds = new HashSet<>();
      Set<String> streamingElementIds = new HashSet<>();

      // Look for elements matching the tag
      for (MediaPackageElement elem : downloadElements) {
        downloadElementIds.add(elem.getIdentifier());
      }
      for (MediaPackageElement elem : streamingElements) {
        streamingElementIds.add(elem.getIdentifier());
      }

      Job publishJob = null;
      try {
        publishJob = publicationService.publish(mediaPackage, repository, downloadElementIds, streamingElementIds,
                checkAvailability);
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException("Error parsing media package", e);
      } catch (PublicationException e) {
        throw new WorkflowOperationException("Error parsing media package", e);
      }

      // Wait until the publication job has returned
      if (!waitForStatus(publishJob).isSuccess()) {
        throw new WorkflowOperationException("Mediapackage " + mediaPackage.getIdentifier()
                + " could not be published to OAI-PMH repository " + repository);
      }

      // The job has passed
      Job job = serviceRegistry.getJob(publishJob.getId());

      // If there is no payload, then the item has not been published.
      if (job.getPayload() == null) {
        logger.warn("Publish to OAI-PMH repository '{}' failed, no payload from publication job: {}", repository, job);
        return createResult(mediaPackage, Action.CONTINUE);
      }

      Publication newElement = null;
      try {
        newElement = (Publication) MediaPackageElementParser.getFromXml(job.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }

      if (newElement == null) {
        logger.warn(
            "Publication to OAI-PMH repository '{}' failed, unable to parse the payload '{}' from "
                + "job '{}' to a mediapackage element",
            repository, job.getPayload(), job.toString());
        return createResult(mediaPackage, Action.CONTINUE);
      }

      for (Publication existingPublication : $(mediaPackage.getPublications())
              .find(ofChannel(newElement.getChannel()).toFn())) {
        mediaPackage.remove(existingPublication);
      }
      mediaPackage.add(newElement);

      if (externalChannel.isSome() && externalMimetype.isSome() && externalTempalte.isSome()) {
        String template = externalTempalte.get().replace("{event}", mediaPackage.getIdentifier().toString());
        if (StringUtils.isNotBlank(mediaPackage.getSeries())) {
          template = template.replace("{series}", mediaPackage.getSeries());
        }

        Publication externalElement = PublicationImpl.publication(UUID.randomUUID().toString(), externalChannel.get(),
                URI.create(template), externalMimetype.get());
        for (Publication existingPublication : $(mediaPackage.getPublications())
                .find(ofChannel(externalChannel.get()).toFn())) {
          mediaPackage.remove(existingPublication);
        }
        mediaPackage.add(externalElement);
      }

      logger.debug("Publication to OAI-PMH repository '{}' operation completed", repository);
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }
}
