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

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.doc.DocUtil;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * WOH that distributes selected elements to an internal distribution channel and adds reflective publication elements
 * to the media package.
 */

public class ConfigurablePublishWorkflowOperationHandler extends ConfigurableWorkflowOperationHandlerBase {

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

  /** Workflow configuration options */
  static final String CHANNEL_ID_KEY = "channel-id";
  static final String MIME_TYPE = "mimetype";
  static final String SOURCE_TAGS = "source-tags";
  static final String SOURCE_FLAVORS = "source-flavors";
  static final String WITH_PUBLISHED_ELEMENTS = "with-published-elements";
  static final String CHECK_AVAILABILITY = "check-availability";
  static final String STRATEGY = "strategy";
  static final String MODE = "mode";

  /** Known values for mode **/
  static final String MODE_SINGLE = "single";
  static final String MODE_MIXED = "mixed";
  static final String MODE_BULK = "bulk";

  static final String[] KNOWN_MODES = { MODE_SINGLE, MODE_MIXED, MODE_BULK };

  static final String DEFAULT_MODE = MODE_BULK;

  /** The workflow configuration key for defining the url pattern. */
  static final String URL_PATTERN = "url-pattern";

  private SecurityService securityService;

  /**
   * The workspace service.
   */
  private Workspace workspace = null;

  /** OSGi DI */
  void setDownloadDistributionService(DownloadDistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /** OSGi DI */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  protected DownloadDistributionService getDistributionService() {
    assert (distributionService != null);
    return distributionService;
  }

  private void addMetadataCatalogsToTemplateData(Map<String, Object> values, MediaPackage mp) {
    if (! mp.hasCatalogs()) {
      logger.info("No metdata catalogs found.");
      return;
    }
    if (values == null) values = new HashMap<>();

    for (Catalog catalog : mp.getCatalogs()) {
      if (catalog.getFlavor() != null) {
        String mainflavor = catalog.getFlavor().getType();
        String subflavor = catalog.getFlavor().getSubtype();
        Map<String, Object> metadata = new HashMap<>();
        try {
          DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(workspace, catalog);
          for (Map.Entry<EName, List<DublinCoreValue>> entry : dc.getValues().entrySet()) {
            String value = entry.getValue().get(0).getValue();
            metadata.put(entry.getKey().getLocalName(), value);
          }
          if (!values.containsKey(mainflavor)) {
            values.put(mainflavor, new HashMap<>());
          }
          ((Map<String, Object>)values.get(mainflavor)).put(subflavor, metadata);
        } catch (Exception e) {
          logger.error("Catalog from URL %s with flavor %s could not be loaded", catalog.getURI(),
                  mainflavor + "/" + subflavor);
        }
      }
    }
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
    Map<String, Object> values = new HashMap<>();
    values.put(EVENT_ID_TEMPLATE_KEY, mp.getIdentifier().compact());
    values.put(PUBLICATION_ID_TEMPLATE_KEY, pubUUID);
    String playerPath = securityService.getOrganization().getProperties().get(PLAYER_PROPERTY);
    values.put(PLAYER_PATH_TEMPLATE_KEY, playerPath);
    values.put(SERIES_ID_TEMPLATE_KEY, StringUtils.trimToEmpty(mp.getSeries()));
    addMetadataCatalogsToTemplateData(values, mp);
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

    final boolean withPublishedElements = BooleanUtils
            .toBoolean(StringUtils.trimToEmpty(op.getConfiguration(WITH_PUBLISHED_ELEMENTS)));

    boolean checkAvailability = BooleanUtils
            .toBoolean(StringUtils.trimToEmpty(op.getConfiguration(CHECK_AVAILABILITY)));

    if (getPublications(mp, channelId).size() > 0) {
      final String rePublishStrategy = StringUtils.trimToEmpty(op.getConfiguration(STRATEGY));

      switch (rePublishStrategy) {

        case ("fail"):
          // fail is a dummy function for further distribution strategies
          fail(mp);
          break;
        case ("merge"):
          // nothing to do here. other publication strategies can be added to this list later on
          break;
        default:
          retract(mp, channelId);
      }
    }

    String mode = StringUtils.trimToEmpty(op.getConfiguration(MODE));
    if ("".equals(mode)) {
      mode = DEFAULT_MODE;
    } else if (!ArrayUtils.contains(KNOWN_MODES, mode)) {
      logger.error("Unknown value for configuration key mode: '{}'", mode);
      throw new IllegalArgumentException("Unknown value for configuration key mode");
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
        Set<MediaPackageElement> elements = distribute(selector.select(mp, false), mp, channelId, mode,
                checkAvailability);
        if (elements.size() > 0) {
          for (MediaPackageElement element : elements) {
            // Make sure the mediapackage is prompted to create a new identifier for this element
            element.setIdentifier(null);
            PublicationImpl.addElementToPublication(publication, element);
          }
        } else {
          logger.info("No element found for distribution in media package '{}'", mp);
          return createResult(mp, Action.CONTINUE);
        }
      } else {
        List<MediaPackageElement> publishedElements = new ArrayList<>();
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

    try {
      workspace.cleanup(mp.getIdentifier());
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }
    return createResult(mp, Action.CONTINUE);
  }

  private Set<MediaPackageElement> distribute(Collection<MediaPackageElement> elements, MediaPackage mediapackage,
          String channelId, String mode, boolean checkAvailability) throws WorkflowOperationException {

    Set<MediaPackageElement> result = new HashSet<>();

    Set<String> bulkElementIds = new HashSet<>();
    Set<String> singleElementIds = new HashSet<>();

    for (MediaPackageElement element : elements) {
      if (MODE_BULK.equals(mode)
              || (MODE_MIXED.equals(mode) && (element.getElementType() != MediaPackageElement.Type.Track))) {
        bulkElementIds.add(element.getIdentifier());
      } else {
        singleElementIds.add(element.getIdentifier());
      }
    }

    Set<Job> jobs = new HashSet<>();
    if (bulkElementIds.size() > 0) {
      logger.info("Start bulk publishing of {} elements of media package '{}' to publication channel '{}'",
              bulkElementIds.size(), mediapackage, channelId);
      try {
        Job job = distributionService.distribute(channelId, mediapackage, bulkElementIds, checkAvailability);
        jobs.add(job);
      } catch (DistributionException | MediaPackageException e) {
        logger.error("Creating the distribution job for {} elements of media package '{}' failed",
                bulkElementIds.size(), mediapackage, e);
        throw new WorkflowOperationException(e);
      }
    }
    if (singleElementIds.size() > 0) {
      logger.info("Start single publishing of {} elements of media package '{}' to publication channel '{}'",
              singleElementIds.size(), mediapackage, channelId);
      for (String elementId : singleElementIds) {
        try {
          Job job = distributionService.distribute(channelId, mediapackage, elementId, checkAvailability);
          jobs.add(job);
        } catch (DistributionException | MediaPackageException e) {
          logger.error("Creating the distribution job for element '{}' of media package '{}' failed", elementId,
                  mediapackage, e);
          throw new WorkflowOperationException(e);
        }
      }
    }

    if (jobs.size() > 0) {
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("At least one of the distribution jobs did not complete successfully");
      }
      for (Job job : jobs) {
        try {
          List<? extends MediaPackageElement> elems = MediaPackageElementParser.getArrayFromXml(job.getPayload());
          result.addAll(elems);
        } catch (MediaPackageException e) {
          logger.error("Job '{}' returned payload ({}) that could not be parsed to media package elements", job,
                  job.getPayload(), e);
          throw new WorkflowOperationException(e);
        }
      }
      logger.info("Published {} elements of media package {} to publication channel {}",
              bulkElementIds.size() + singleElementIds.size(), mediapackage, channelId);
    }
    return result;
  }

  /**
   * Dummy function for further publication strategies
   *
   * @param mp
   * @throws WorkflowOperationException
   */
  private void fail(MediaPackage mp) throws WorkflowOperationException {
    logger.error("There is already a Published Media, fail Stragy for Mediapackage {}", mp.getIdentifier());
    throw new WorkflowOperationException("There is already a Published Media, fail Stragy for Mediapackage ");
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
