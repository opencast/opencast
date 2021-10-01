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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.opencastproject.systems.OpencastConstants.SERVER_URL_PROPERTY;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Strings.toBool;
import static org.opencastproject.util.data.functions.Strings.trimToNone;
import static org.opencastproject.workflow.handler.distribution.EngagePublicationChannel.CHANNEL_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The workflow definition for handling "engage publication" operations
 */
public class PublishEngageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishEngageWorkflowOperationHandler.class);

  /** Configuration properties id */
  private static final String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";
  private static final String STREAMING_PUBLISH_PROPERTY = "org.opencastproject.publish.streaming.formats";

  /** Workflow configuration option keys */
  private static final String DOWNLOAD_SOURCE_FLAVORS = "download-source-flavors";
  private static final String DOWNLOAD_TARGET_SUBFLAVOR = "download-target-subflavor";
  private static final String DOWNLOAD_SOURCE_TAGS = "download-source-tags";
  private static final String DOWNLOAD_TARGET_TAGS = "download-target-tags";
  private static final String STREAMING_SOURCE_TAGS = "streaming-source-tags";
  private static final String STREAMING_TARGET_TAGS = "streaming-target-tags";
  private static final String STREAMING_SOURCE_FLAVORS = "streaming-source-flavors";
  private static final String STREAMING_TARGET_SUBFLAVOR = "streaming-target-subflavor";
  private static final String CHECK_AVAILABILITY = "check-availability";
  private static final String STRATEGY = "strategy";
  private static final String MERGE_FORCE_FLAVORS = "merge-force-flavors";

  private static final String MERGE_FORCE_FLAVORS_DEFAULT = "dublincore/*,security/*";

  /** Path the REST endpoint which will re-direct users to the currently configured video player **/
  static final String PLAYER_PATH = "/play/";

  /** The streaming distribution service */
  private StreamingDistributionService streamingDistributionService = null;

  /** The download distribution service */
  private DownloadDistributionService downloadDistributionService = null;

  /** The search service */
  private SearchService searchService = null;

  /** The server url */
  private URL serverUrl;

  private OrganizationDirectoryService organizationDirectoryService = null;

  /** Which streaming formats should be published automatically */
  private List<String> publishedStreamingFormats = null;

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

  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /** Supported streaming formats */
  private static final Set<TrackImpl.StreamingProtocol> STREAMING_FORMATS = new HashSet<>(Arrays.asList(
          TrackImpl.StreamingProtocol.RTMP,
          TrackImpl.StreamingProtocol.RTMPE,
          TrackImpl.StreamingProtocol.HLS,
          TrackImpl.StreamingProtocol.DASH,
          TrackImpl.StreamingProtocol.HDS,
          TrackImpl.StreamingProtocol.SMOOTH));

  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    BundleContext bundleContext = cc.getBundleContext();

    // Get configuration
    serverUrl = UrlSupport.url(bundleContext.getProperty(SERVER_URL_PROPERTY));
    publishedStreamingFormats = Arrays.asList(Optional.ofNullable(StringUtils.split(
            bundleContext.getProperty(STREAMING_PUBLISH_PROPERTY), ",")).orElse(new String[0]));
  }

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running engage publication workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance op = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    String downloadSourceTags = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_SOURCE_TAGS));
    String downloadTargetTags = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_TARGET_TAGS));
    String downloadSourceFlavors = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_SOURCE_FLAVORS));
    String downloadTargetSubflavor = StringUtils.trimToNull(op.getConfiguration(DOWNLOAD_TARGET_SUBFLAVOR));
    String streamingSourceTags = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_SOURCE_TAGS));
    String streamingTargetTags = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_TARGET_TAGS));
    String streamingSourceFlavors = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_SOURCE_FLAVORS));
    String streamingTargetSubflavor = StringUtils.trimToNull(op.getConfiguration(STREAMING_TARGET_SUBFLAVOR));
    String republishStrategy = StringUtils.trimToEmpty(op.getConfiguration(STRATEGY));
    String mergeForceFlavorsStr = StringUtils.trimToEmpty(
            StringUtils.defaultString(op.getConfiguration(MERGE_FORCE_FLAVORS), MERGE_FORCE_FLAVORS_DEFAULT));

    boolean checkAvailability = option(op.getConfiguration(CHECK_AVAILABILITY)).bind(trimToNone).map(toBool)
            .getOrElse(true);

    String[] sourceDownloadTags = StringUtils.split(downloadSourceTags, ",");
    String[] targetDownloadTags = StringUtils.split(downloadTargetTags, ",");
    String[] sourceDownloadFlavors = StringUtils.split(downloadSourceFlavors, ",");
    String[] sourceStreamingTags = StringUtils.split(streamingSourceTags, ",");
    String[] targetStreamingTags = StringUtils.split(streamingTargetTags, ",");
    String[] sourceStreamingFlavors = StringUtils.split(streamingSourceFlavors, ",");

    if (sourceDownloadTags.length == 0 && sourceDownloadFlavors.length == 0 && sourceStreamingTags.length == 0
            && sourceStreamingFlavors.length == 0) {
      logger.warn("No tags or flavors have been specified, so nothing will be published to the "
          + "engage publication channel");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Parse forced flavors
    List<MediaPackageElementFlavor> mergeForceFlavors = Arrays.stream(StringUtils.split(mergeForceFlavorsStr, ", "))
            .map(MediaPackageElementFlavor::parseFlavor).collect(Collectors.toList());

    // Parse the download target flavor
    MediaPackageElementFlavor downloadSubflavor = null;
    if (downloadTargetSubflavor != null) {
      try {
        downloadSubflavor = MediaPackageElementFlavor.parseFlavor(downloadTargetSubflavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(e);
      }
    }

    // Parse the streaming target flavor
    MediaPackageElementFlavor streamingSubflavor = null;
    if (streamingTargetSubflavor != null) {
      try {
        streamingSubflavor = MediaPackageElementFlavor.parseFlavor(streamingTargetSubflavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(e);
      }
    }

    // Configure the download element selector
    SimpleElementSelector downloadElementSelector = new SimpleElementSelector();
    for (String flavor : sourceDownloadFlavors) {
      downloadElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceDownloadTags) {
      downloadElementSelector.addTag(tag);
    }

    // Configure the streaming element selector
    SimpleElementSelector streamingElementSelector = new SimpleElementSelector();
    for (String flavor : sourceStreamingFlavors) {
      streamingElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceStreamingTags) {
      streamingElementSelector.addTag(tag);
    }

    // Select the appropriate elements for download and streaming
    Collection<MediaPackageElement> downloadElements = downloadElementSelector.select(mediaPackage, false);
    Collection<MediaPackageElement> streamingElements = streamingElementSelector.select(mediaPackage, false);

    try {
      Set<String> downloadElementIds = new HashSet<String>();
      Set<String> streamingElementIds = new HashSet<String>();

      // Look for elements matching the tag
      for (MediaPackageElement elem : downloadElements) {
        downloadElementIds.add(elem.getIdentifier());
      }
      for (MediaPackageElement elem : streamingElements) {
        streamingElementIds.add(elem.getIdentifier());
      }

      removePublicationElement(mediaPackage);
      switch (republishStrategy) {
        case ("merge"):
          // nothing to do here. other publication strategies can be added to this list later on
          break;
        default:
          retractFromEngage(mediaPackage);
      }

      List<Job> jobs = new ArrayList<Job>();
      //distribute Elements
      try {
        if (downloadElementIds.size() > 0) {
          Job job = downloadDistributionService.distribute(
              CHANNEL_ID, mediaPackage, downloadElementIds, checkAvailability);
          if (job != null) {
            jobs.add(job);
          }
        }

        if (streamingDistributionService != null && streamingDistributionService.publishToStreaming()) {
          for (String elementId : streamingElementIds) {
            Job job = streamingDistributionService.distribute(CHANNEL_ID, mediaPackage, elementId);
            if (job != null) {
              jobs.add(job);
            }
          }
        }
      } catch (DistributionException e) {
        throw new WorkflowOperationException(e);
      }

      if (jobs.size() < 1) {
        logger.info("No mediapackage element was found for distribution to engage");
        return createResult(mediaPackage, Action.CONTINUE);
      }

      // Wait until all distribution jobs have returned
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("One of the distribution jobs did not complete successfully");
      }

      logger.debug("Distribute of mediapackage {} completed", mediaPackage);

      String engageUrlString = null;
      try {
        MediaPackage mediaPackageForSearch = getMediaPackageForSearchIndex(mediaPackage, jobs, downloadSubflavor,
                targetDownloadTags, downloadElementIds, streamingSubflavor, streamingElementIds, targetStreamingTags);

        // MH-10216, check if only merging into existing mediapackage
        removePublicationElement(mediaPackage);
        switch (republishStrategy) {
          case ("merge"):
            // merge() returns merged mediapackage or null mediaPackage is not published
            mediaPackageForSearch = merge(mediaPackageForSearch, mergeForceFlavors);
            if (mediaPackageForSearch == null) {
              logger.info("Skipping republish for {} since it is not currently published",
                  mediaPackage.getIdentifier().toString());
              return createResult(mediaPackage, Action.SKIP);
            }
            break;
          default:
          // nothing to do here
        }

        // Check that the media package meets the criteria for publication
        if (isBlank(mediaPackageForSearch.getTitle())) {
          throw new WorkflowOperationException("Media package does not meet publication criteria: Missing title");
        }
        if (!mediaPackageForSearch.hasTracks()) {
          throw new WorkflowOperationException("Media package does not meet publication criteria: No tracks selected");
        }

        logger.info("Publishing media package {} to search index", mediaPackageForSearch);

        URL engageBaseUrl;
        Organization organization = organizationDirectoryService.getOrganization(workflowInstance.getOrganizationId());
        engageUrlString = StringUtils.trimToNull(organization.getProperties().get(ENGAGE_URL_PROPERTY));
        if (engageUrlString != null) {
          engageBaseUrl = new URL(engageUrlString);
        } else {
          engageBaseUrl = serverUrl;
          logger.info(
              "Using 'server.url' as a fallback for the non-existing organization level key '{}' "
                  + "for the publication url",
              ENGAGE_URL_PROPERTY);
        }

        // create the publication URI (used by Admin UI for event details link)
        URI engageUri = this.createEngageUri(engageBaseUrl.toURI(), mediaPackage);

        // Create new distribution element
        Publication publicationElement = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_ID,
                engageUri, MimeTypes.parseMimeType("text/html"));
        mediaPackage.add(publicationElement);

        // create publication URI for streaming
        if (streamingDistributionService != null
            && streamingDistributionService.publishToStreaming()
            && !publishedStreamingFormats.isEmpty()) {
          for (Track track : mediaPackageForSearch.getTracks()) {
            String mimeType = track.getMimeType().toString();
            if (isStreamingFormat(track) && (publishedStreamingFormats.contains(mimeType)
                    || publishedStreamingFormats.contains("*"))) {
              publicationElement.addTrack(track);
            }
          }
          for (Attachment attachment : mediaPackageForSearch.getAttachments()) {
            publicationElement.addAttachment(attachment);
          }
          for (Catalog catalog : mediaPackageForSearch.getCatalogs()) {
            publicationElement.addCatalog(catalog);
          }
        }

        // Adding media package to the search index
        Job publishJob = null;
        try {
          publishJob = searchService.add(mediaPackageForSearch);
          if (!waitForStatus(publishJob).isSuccess()) {
            throw new WorkflowOperationException("Mediapackage " + mediaPackageForSearch.getIdentifier()
                    + " could not be published");
          }
        } catch (SearchException e) {
          throw new WorkflowOperationException("Error publishing media package", e);
        } catch (MediaPackageException e) {
          throw new WorkflowOperationException("Error parsing media package", e);
        }

        logger.debug("Publishing of mediapackage {} completed", mediaPackage);
        return createResult(mediaPackage, Action.CONTINUE);
      } catch (MalformedURLException e) {
        logger.error("{} is malformed: {}", ENGAGE_URL_PROPERTY, engageUrlString);
        throw new WorkflowOperationException(e);
      } catch (Throwable t) {
        if (t instanceof WorkflowOperationException) {
          throw (WorkflowOperationException) t;
        } else {
          throw new WorkflowOperationException(t);
        }
      }
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
  }

  /**
   * Local utility to assemble player path for this class
   *
   * @param engageUri
   * @param mp
   * @return the assembled player URI for this mediapackage
   */
  URI createEngageUri(URI engageUri, MediaPackage mp) {
    return URIUtils.resolve(engageUri, PLAYER_PATH + mp.getIdentifier().toString());
  }

  /**
   * Returns a mediapackage that only contains elements that are marked for distribution.
   *
   * @param current
   *          the current mediapackage
   * @param jobs
   *          the distribution jobs
   * @param downloadSubflavor
   *          flavor to be applied to elements distributed to download
   * @param downloadTargetTags
   *          tags to be applied to elements distributed to downloads
   * @param downloadElementIds
   *          identifiers for elements that have been distributed to downloads
   * @param streamingSubflavor
   *          flavor to be applied to elements distributed to streaming
   * @param streamingElementIds
   *          identifiers for elements that have been distributed to streaming
   * @param streamingTargetTags
   *          tags to be applied to elements distributed to streaming
   * @return the new mediapackage
   */
  protected MediaPackage getMediaPackageForSearchIndex(MediaPackage current, List<Job> jobs,
          MediaPackageElementFlavor downloadSubflavor, String[] downloadTargetTags, Set<String> downloadElementIds,
          MediaPackageElementFlavor streamingSubflavor, Set<String> streamingElementIds, String[] streamingTargetTags)
          throws MediaPackageException, NotFoundException, ServiceRegistryException, WorkflowOperationException {
    MediaPackage mp = (MediaPackage) current.clone();

    // All the jobs have passed, let's update the mediapackage with references to the distributed elements
    List<String> elementsToPublish = new ArrayList<String>();
    Map<String, String> distributedElementIds = new HashMap<String, String>();

    for (Job entry : jobs) {
      Job job = serviceRegistry.getJob(entry.getId());

      // If there is no payload, then the item has not been distributed.
      if (job.getPayload() == null) {
        continue;
      }

      List <MediaPackageElement> distributedElements = null;
      try {
        distributedElements = (List <MediaPackageElement>) MediaPackageElementParser.getArrayFromXml(job.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }

      // If the job finished successfully, but returned no new element, the channel simply doesn't support this
      // kind of element. So we just keep on looping.
      if (distributedElements == null || distributedElements.size() < 1) {
        continue;
      }

      for (MediaPackageElement distributedElement : distributedElements) {

        String sourceElementId = distributedElement.getIdentifier();
        if (sourceElementId != null) {
          MediaPackageElement sourceElement = mp.getElementById(sourceElementId);

          // Make sure the mediapackage is prompted to create a new identifier for this element
          distributedElement.setIdentifier(null);
          if (sourceElement != null) {
            // Adjust the flavor and tags for downloadable elements
            if (downloadElementIds.contains(sourceElementId)) {
              if (downloadSubflavor != null) {
                MediaPackageElementFlavor flavor = sourceElement.getFlavor();
                if (flavor != null) {
                  MediaPackageElementFlavor newFlavor = new MediaPackageElementFlavor(flavor.getType(),
                          downloadSubflavor.getSubtype());
                  distributedElement.setFlavor(newFlavor);
                }
              }
            }
            // Adjust the flavor and tags for streaming elements
            else if (streamingElementIds.contains(sourceElementId)) {
              if (streamingSubflavor != null && streamingElementIds.contains(sourceElementId)) {
                MediaPackageElementFlavor flavor = sourceElement.getFlavor();
                if (flavor != null) {
                  MediaPackageElementFlavor newFlavor = new MediaPackageElementFlavor(flavor.getType(),
                          streamingSubflavor.getSubtype());
                  distributedElement.setFlavor(newFlavor);
                }
              }
            }
            // Copy references from the source elements to the distributed elements
            MediaPackageReference ref = sourceElement.getReference();
            if (ref != null && mp.getElementByReference(ref) != null) {
              MediaPackageReference newReference = (MediaPackageReference) ref.clone();
              distributedElement.setReference(newReference);
            }
          }
        }

        if (isStreamingFormat(distributedElement)) {
          applyTags(distributedElement, streamingTargetTags);
        } else {
          applyTags(distributedElement, downloadTargetTags);
        }

        // Add the new element to the mediapackage
        mp.add(distributedElement);
        elementsToPublish.add(distributedElement.getIdentifier());
        distributedElementIds.put(sourceElementId, distributedElement.getIdentifier());
      }
    }

    // Mark everything that is set for removal
    List<MediaPackageElement> removals = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : mp.getElements()) {
      if (!elementsToPublish.contains(element.getIdentifier())) {
        removals.add(element);
      }
    }

    // Translate references to the distributed artifacts
    for (MediaPackageElement element : mp.getElements()) {

      if (removals.contains(element)) {
        continue;
      }

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference == null) {
        continue;
      }

      // See if the element has been distributed
      String distributedElementId = distributedElementIds.get(reference.getIdentifier());
      if (distributedElementId == null) {
        continue;
      }

      MediaPackageReference translatedReference
          = new MediaPackageReferenceImpl(mp.getElementById(distributedElementId));
      if (reference.getProperties() != null) {
        translatedReference.getProperties().putAll(reference.getProperties());
      }

      // Set the new reference
      element.setReference(translatedReference);

    }

    // Remove everything we don't want to add to publish
    for (MediaPackageElement element : removals) {
      mp.remove(element);
    }
    return mp;
  }

  /**
   * Checks if the MediaPackage track transport protocol is a streaming format protocol
   * @param element The MediaPackageElement to analyze
   * @return true if it is a TrackImpl and has a streaming protocol as transport
   */
  private boolean isStreamingFormat(MediaPackageElement element) {
    return element instanceof TrackImpl
            && STREAMING_FORMATS.contains(((TrackImpl) element).getTransport());
  }

  /**
   * Adds Tags to a MediaPackageElement
   * @param element the element that needs the tags
   * @param tags the list of tags to apply
   */
  private void applyTags(MediaPackageElement element, String[] tags) {
    for (String tag : tags) {
      element.addTag(tag);
    }
  }

  protected MediaPackage getDistributedMediapackage(String mediaPackageID) throws WorkflowOperationException {
    MediaPackage mediaPackage = null;
    SearchQuery query = new SearchQuery().withId(mediaPackageID);
    query.includeEpisodes(true);
    query.includeSeries(false);
    SearchResult result = searchService.getByQuery(query);
    if (result.size() == 0) {
      logger.info("The search service doesn't know mediapackage {}.", mediaPackageID);
      return mediaPackage; // i.e. null
    } else if (result.size() > 1) {
      logger.warn("More than one mediapackage with id {} returned from search service", mediaPackageID);
      throw new WorkflowOperationException("More than one mediapackage with id " + mediaPackageID + " found");
    } else {
      // else, merge the new with the existing (new elements will overwrite existing elements)
      mediaPackage = result.getItems()[0].getMediaPackage();
    }
    return mediaPackage;
  }


  /**
   * MH-10216, method copied from the original RepublishWorkflowOperationHandler
   * Merges mediapackage with published mediapackage.
   *
   * @param mediaPackageForSearch
   * @return merged mediapackage or null if a published medipackage was not found
   * @throws WorkflowOperationException
   */
  protected MediaPackage merge(MediaPackage mediaPackageForSearch, List<MediaPackageElementFlavor> forceFlavors)
          throws WorkflowOperationException {
    return mergePackages(mediaPackageForSearch,
            getDistributedMediapackage(mediaPackageForSearch.toString()),
            forceFlavors);
  }

  /**
   * MH-10216, Copied from the original RepublishWorkflowOperationHandler
   *
   * Merges the updated mediapackage with the one that is currently published in a way where the updated elements
   * replace existing ones in the published mediapackage based on their flavor.
   * <p>
   * If <code>publishedMp</code> is <code>null</code>, this method returns the updated mediapackage without any
   * modifications.
   *
   * @param updatedMp
   *          the updated media package
   * @param publishedMp
   *          the mediapackage that is currently published
   * @return the merged mediapackage
   */
  protected MediaPackage mergePackages(MediaPackage updatedMp, MediaPackage publishedMp,
          List<MediaPackageElementFlavor> forceFlavors) {
    if (publishedMp == null) {
      return updatedMp;
    }

    MediaPackage mergedMediaPackage = (MediaPackage) updatedMp.clone();
    for (MediaPackageElement element : publishedMp.elements()) {
      String type = element.getElementType().toString().toLowerCase();
      if (updatedMp.getElementsByFlavor(element.getFlavor()).length == 0) {
        if (forceFlavors.stream().anyMatch((f) -> element.getFlavor().matches(f))) {
          logger.info("Forcing removal of {} {} due to the absence of a new element with flavor {}",
                  type, element.getIdentifier(), element.getFlavor().toString());
          continue;
        }
        logger.info("Merging {} '{}' into the updated mediapackage", type, element.getIdentifier());
        mergedMediaPackage.add((MediaPackageElement) element.clone());
      } else {
        logger.info("Overwriting existing {} '{}' with '{}' in the updated mediapackage",
                type, element.getIdentifier(), updatedMp.getElementsByFlavor(element.getFlavor())[0].getIdentifier());

      }
    }

    return mergedMediaPackage;
  }

  private void removePublicationElement(MediaPackage mediaPackage) {
    for (Publication publicationElement : mediaPackage.getPublications()) {
      if (CHANNEL_ID.equals(publicationElement.getChannel())) {
        mediaPackage.remove(publicationElement);
      }
    }
  }

  /**
 * Removes every Publication for Searchindex from Mediapackage
 * Removes Mediapackage from Searchindex
   * @param mediaPackage Mediapackage
   * @throws WorkflowOperationException
   */
  private void retractFromEngage(MediaPackage mediaPackage) throws WorkflowOperationException {
    List<Job> jobs = new ArrayList<Job>();
    Set<String> elementIds = new HashSet<String>();
    try {
      MediaPackage distributedMediaPackage = getDistributedMediapackage(mediaPackage.toString());
      if (distributedMediaPackage != null) {

        for (MediaPackageElement element : distributedMediaPackage.getElements()) {
          elementIds.add(element.getIdentifier());
        }
        //bulk retraction
        if (elementIds.size() > 0) {
          Job  retractDownloadDistributionJob
              = downloadDistributionService.retract(CHANNEL_ID, distributedMediaPackage, elementIds);
          if (retractDownloadDistributionJob != null) {
            jobs.add(retractDownloadDistributionJob);
          }
        }

        if (streamingDistributionService.publishToStreaming()) {
          for (MediaPackageElement element : distributedMediaPackage.getElements()) {
            Job retractStreamingJob
                = streamingDistributionService.retract(CHANNEL_ID, distributedMediaPackage, element.getIdentifier());
            if (retractStreamingJob != null) {
              jobs.add(retractStreamingJob);
            }
          }
        }

        Job deleteSearchJob = null;
        logger.info("Retracting already published Elements for Mediapackage: {}",
            mediaPackage.getIdentifier().toString());
        deleteSearchJob = searchService.delete(mediaPackage.getIdentifier().toString());
        if (deleteSearchJob != null) {
          jobs.add(deleteSearchJob);
        }
      }
      // Wait until all retraction jobs have returned
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("One of the retraction jobs did not complete successfully");
      }
    } catch (DistributionException e) {
      throw new WorkflowOperationException(e);
    } catch (SearchException e) {
      throw new WorkflowOperationException("Error retracting media package", e);
    } catch (UnauthorizedException | NotFoundException ex) {
      logger.error("Retraction failed of Mediapackage: { }", mediaPackage.getIdentifier().toString(), ex);
    }
  }
}
