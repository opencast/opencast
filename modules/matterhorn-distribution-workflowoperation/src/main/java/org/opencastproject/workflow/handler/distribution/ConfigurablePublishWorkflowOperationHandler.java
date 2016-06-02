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
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.doc.DocUtil;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/** WOH that distributes selected elements to an internal distribution channel and adds reflective publication elements
 *  to the media package. */

public class ConfigurablePublishWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurablePublishWorkflowOperationHandler.class);

  /** The template key for adding the mediapackage / event id to the publication path. */
  protected static final String EVENT_ID_TEMPLATE_KEY = "event_id";
  /** The template key for adding the player location path to the publication path. */
  protected static final String PLAYER_PATH_TEMPLATE_KEY = "player_path";
  /** The template key for adding the publication id to the publication path. */
  protected static final String PUBLICATION_ID_TEMPLATE_KEY = "publication_id";
  /** The template key for adding the series id to the publication path. */
  protected static final String SERIES_ID_TEMPLATE_KEY = "series_id";
  /** The configuration property value for the player location. */
  protected static final String PLAYER_PROPERTY = "player";
  // service references
  private DownloadDistributionService distributionService;

  // workflow configuration options
  static final String CHANNEL_ID_KEY = "channel-id";
  static final String MIME_TYPE = "mimetype";
  static final String SOURCE_TAGS = "source-tags";
  static final String SOURCE_FLAVORS = "source-flavors";
  static final String WITH_PUBLISHED_ELEMENTS = "with-published-elements";
  static final String STRATEGY = "strategy";

  /** The workflow configuration key for defining the url pattern. */
  static final String URL_PATTERN = "url-pattern";

  private SecurityService securityService;

  /** OSGi DI */
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /** OSGi DI */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Replace possible variables in the url-pattern configuration for this workflow operation handler.
   *
   * @param urlPattern
   *          The operation's template for replacing the variables.
   * @param mp
   *          The {@link MediaPackage} used to get the event / mediapackage id.
   * @param pubUUID
   *          The UUID for the published element.
   * @return The URI of the published element with the variables replaced.
   * @throws WorkflowOperationException
   *           Thrown if the URI is malformed after replacing the variables.
   */
  public URI populateUrlWithVariables(String urlPattern, MediaPackage mp, String pubUUID)
          throws WorkflowOperationException {
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(EVENT_ID_TEMPLATE_KEY, mp.getIdentifier().compact());
    values.put(PUBLICATION_ID_TEMPLATE_KEY, pubUUID);
    String playerPath = securityService.getOrganization().getProperties().get(PLAYER_PROPERTY);
    values.put(PLAYER_PATH_TEMPLATE_KEY, playerPath);
    values.put(SERIES_ID_TEMPLATE_KEY, StringUtils.trimToEmpty(mp.getSeries()));
    String uriWithVariables = DocUtil.processTextTemplate("Replacing Variables in Publish URL", urlPattern, values);
    URI publicationURI;
    try {
      publicationURI = new URI(uriWithVariables);
    } catch (URISyntaxException e) {
      throw new WorkflowOperationException(String.format(
              "Unable to create URI from template '%s', replacement was: '%s'", urlPattern, uriWithVariables), e);
    }
    return publicationURI;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    RequireUtil.notNull(workflowInstance, "workflowInstance");

    final MediaPackage mp = workflowInstance.getMediaPackage();
    final WorkflowOperationInstance op = workflowInstance.getCurrentOperation();

    final String channelId = StringUtils.trimToEmpty(op.getConfiguration(CHANNEL_ID_KEY));
    if ("".equals(channelId)) {
      throw new WorkflowOperationException("Unable to publish this mediapackage as the configuration key "
              + CHANNEL_ID_KEY + " is missing. Unable to determine where to publish these elements.");
    }

    final String urlPattern = StringUtils.trimToEmpty(op.getConfiguration(URL_PATTERN));

    MimeType mimetype = null;
    String mimetypeString = StringUtils.trimToEmpty(op.getConfiguration(MIME_TYPE));
    if (!"".equals(mimetypeString)) {
      try {
        mimetype = MimeTypes.parseMimeType(mimetypeString);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException("Unable to parse the provided configuration for " + MIME_TYPE, e);
      }
    }

    final boolean withPublishedElements = Boolean.parseBoolean(StringUtils.trimToEmpty(op
            .getConfiguration(WITH_PUBLISHED_ELEMENTS)));

    if (mp.getPublications().length > 0) {
      final String rePublishStrategy = StringUtils.trimToEmpty(op.getConfiguration(STRATEGY));

      switch (rePublishStrategy) {

        case ("fail"):
          //fail is a dummy function for further distribution strategies 
          fail(mp);
          break;
        default:
          retract(mp, channelId);
      }
    }

    final String[] sourceFlavors = StringUtils.split(StringUtils.trimToEmpty(op.getConfiguration(SOURCE_FLAVORS)), ",");
    final String[] sourceTags = StringUtils.split(StringUtils.trimToEmpty(op.getConfiguration(SOURCE_TAGS)), ",");

    String publicationUUID = UUID.randomUUID().toString();
    Publication publication = PublicationImpl.publication(publicationUUID, channelId, null, null);

    // Configure the element selector
    final SimpleElementSelector selector = new SimpleElementSelector();
    for (String flavor : sourceFlavors) {
      selector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceTags) {
      selector.addTag(tag);
    }

    if (sourceFlavors.length > 0 || sourceTags.length > 0) {
      if (!withPublishedElements) {
        Map<Job, MediaPackageElement> jobs = new HashMap<>();
        for (final MediaPackageElement element : selector.select(mp, false)) {
          logger.info("Start publishing element '{}' of media package '{}' to publication channel '{}'", new Object[] {
                  element, mp, channelId });
          try {
            final Job job = distributionService.distribute(channelId, mp, element.getIdentifier(), true);
            jobs.put(job, element);
            logger.debug("Distribution job '{}' for element '{}' of media package '{}' created.", new Object[] { job,
                    element, mp });
          } catch (DistributionException | MediaPackageException e) {
            logger.error("Creating the distribution job for element '{}' of media package '{}' failed: {}",
                    new Object[] { element, mp, getStackTrace(e) });
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

        MediaPackageElement element = null;
        for (Entry<Job, MediaPackageElement> job : jobs.entrySet()) {
          try {
            element = MediaPackageElementParser.getFromXml(job.getKey().getPayload());
          } catch (MediaPackageException e) {
            logger.error("Job '{}' returned payload ({}) that could not be parsed to media package element: {}",
                    new Object[] { job, job.getKey().getPayload(), ExceptionUtils.getStackTrace(e) });
            throw new WorkflowOperationException(e);
          }
          // Make sure the mediapackage is prompted to create a new identifier for this element
          element.setIdentifier(null);
          PublicationImpl.addElementToPublication(publication, element);
        }
      } else {
        List<MediaPackageElement> publishedElements = new ArrayList<MediaPackageElement>();
        for (Publication alreadyPublished : mp.getPublications()) {
          publishedElements.addAll(Arrays.asList(alreadyPublished.getAttachments()));
          publishedElements.addAll(Arrays.asList(alreadyPublished.getCatalogs()));
          publishedElements.addAll(Arrays.asList(alreadyPublished.getTracks()));
        }
        for (MediaPackageElement element : selector.select(publishedElements, false)) {
          PublicationImpl.addElementToPublication(publication, element);
        }
      }
    }
    if (!"".equals(urlPattern)) {
      publication.setURI(populateUrlWithVariables(urlPattern, mp, publicationUUID));
    }
    if (mimetype != null) {
      publication.setMimeType(mimetype);
    }
    mp.add(publication);
    return createResult(mp, Action.CONTINUE);
  }

  /**
   * Removes publication for distributionChannel
   *
   * @param mp Mediapackage
   * @param channelId Publication-Channel
   * @throws org.opencastproject.workflow.api.WorkflowOperationException
   */
  private void retract(MediaPackage mp, String channelId) throws WorkflowOperationException {
    Map<Job, MediaPackageElement> jobs = new HashMap<>();
    for (final Publication publicationElement : mp.getPublications()) {
      if (channelId.equals(publicationElement.getChannel())) {
        logger.info("Start retracting element '{}' of media package '{}' from publication channel '{}'", new Object[]{
          publicationElement, mp, channelId});
        try {
          final Job retractjob = distributionService.retract(channelId, mp, publicationElement.getChannel());
          jobs.put(retractjob, publicationElement);
          logger.debug("Retracting job '{}' for element '{}' of media package '{}' created.", new Object[]{retractjob,
            publicationElement, mp});
        } catch (DistributionException e) {
          logger.error("Creating the retracting job for element '{}' of media package '{}' failed: {}",
                  new Object[]{publicationElement, mp, getStackTrace(e)});
          throw new WorkflowOperationException(e);
        }
      }
    }
    // Wait until all retraction jobs have returned
    if (!waitForStatus(jobs.keySet().toArray(new Job[jobs.keySet().size()])).isSuccess()) {
      throw new WorkflowOperationException("One of the retraction jobs did not complete successfully");
    }
  }
/**
 * Dummy function for further publication strategies
 * @param mp
 * @throws WorkflowOperationException 
 */
  private void fail(MediaPackage mp) throws WorkflowOperationException {
    logger.error("There is already a Published Media, fail Stragy for Mediapackage {}", mp.getIdentifier());
    throw new WorkflowOperationException("There is already a Published Media, fail Stragy for Mediapackage ");
  }
}
