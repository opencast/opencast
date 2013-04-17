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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Strings.toBool;
import static org.opencastproject.util.data.functions.Strings.trimToNone;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The workflow definition for handling "engage publication" operations
 */
public class PublishEngageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishEngageWorkflowOperationHandler.class);

  /** Configuration properties id */
  private static final String SERVER_URL_PROPERTY = "org.opencastproject.server.url";
  private static final String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";
  private static final String STREAMING_URL_PROPERTY = "org.opencastproject.streaming.url";

  /** The channel name */
  public static final String CHANNEL_NAME = "engage-player";

  /** Workflow configuration option keys */
  private static final String DOWNLOAD_FLAVORS = "download-flavors";
  private static final String DOWNLOAD_TAGS = "download-tags";
  private static final String STREAMING_TAGS = "streaming-tags";
  private static final String STREAMING_FLAVORS = "streaming-flavors";
  private static final String CHECK_AVAILABILITY = "check-availability";

  /** The streaming distribution service */
  private DistributionService streamingDistributionService = null;

  /** The download distribution service */
  private DownloadDistributionService downloadDistributionService = null;

  /** The search service */
  private SearchService searchService = null;

  /** The base url to engage */
  private URL engageBaseUrl;

  /** Whether to distribute to streaming server */
  private boolean distributeStreaming = false;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param streamingDistributionService
   *          the streaming distribution service
   */
  protected void setStreamingDistributionService(DistributionService streamingDistributionService) {
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

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(DOWNLOAD_FLAVORS,
            "Distribute any mediapackage elements with one of these (comma separated) flavors to download");
    CONFIG_OPTIONS.put(DOWNLOAD_TAGS,
            "Distribute any mediapackage elements with one of these (comma separated) tags to download.");
    CONFIG_OPTIONS.put(STREAMING_FLAVORS,
            "Distribute any mediapackage elements with one of these (comma separated) flavors to streaming");
    CONFIG_OPTIONS.put(STREAMING_TAGS,
            "Distribute any mediapackage elements with one of these (comma separated) tags to streaming.");
    CONFIG_OPTIONS.put(CHECK_AVAILABILITY,
            "( true | false ) defaults to true. Check if the distributed download artifact is available at its URL");
  }

  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    BundleContext bundleContext = cc.getBundleContext();

    // Get engage UI url
    try {
      String engageBaseUrlStr = bundleContext.getProperty(ENGAGE_URL_PROPERTY);
      if (StringUtils.isNotBlank(engageBaseUrlStr)) {
        engageBaseUrl = new URL(engageBaseUrlStr);
      } else {
        engageBaseUrl = new URL(bundleContext.getProperty(SERVER_URL_PROPERTY));
      }
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }

    if (StringUtils.isNotBlank(bundleContext.getProperty(STREAMING_URL_PROPERTY)))
      distributeStreaming = true;

    logger.debug("Default engage server url is {}", engageBaseUrl);
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running engage publication workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Check which tags have been configured
    String downloadTags = StringUtils.trimToEmpty(workflowInstance.getCurrentOperation()
            .getConfiguration(DOWNLOAD_TAGS));
    String downloadFlavors = StringUtils.trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(
            DOWNLOAD_FLAVORS));
    String streamingTags = StringUtils.trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(
            STREAMING_TAGS));
    String streamingFlavors = StringUtils.trimToEmpty(workflowInstance.getCurrentOperation().getConfiguration(
            STREAMING_FLAVORS));
    boolean checkAvailability = option(workflowInstance.getCurrentOperation().getConfiguration(CHECK_AVAILABILITY))
            .bind(trimToNone).map(toBool).getOrElse(true);

    String[] sourceDownloadTags = StringUtils.split(downloadTags, ",");
    String[] sourceDownloadFlavors = StringUtils.split(downloadFlavors, ",");
    String[] sourceStreamingTags = StringUtils.split(streamingTags, ",");
    String[] sourceStreamingFlavors = StringUtils.split(streamingFlavors, ",");

    if (sourceDownloadTags.length == 0 && sourceDownloadFlavors.length == 0 && sourceStreamingTags.length == 0
            && sourceStreamingFlavors.length == 0) {
      logger.warn("No tags or flavors have been specified, so nothing will be published to the engage");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    SimpleElementSelector downloadElementSelector = new SimpleElementSelector();
    for (String flavor : sourceDownloadFlavors) {
      downloadElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceDownloadTags) {
      downloadElementSelector.addTag(tag);
    }

    SimpleElementSelector streamingElementSelector = new SimpleElementSelector();
    for (String flavor : sourceStreamingFlavors) {
      streamingElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceStreamingTags) {
      streamingElementSelector.addTag(tag);
    }

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

      // Also distribute the security configuration
      // -----
      // This was removed in the meantime by a fix for MH-8515, but could now be used again.
      // -----
      Attachment[] securityAttachments = mediaPackage.getAttachments(MediaPackageElements.XACML_POLICY);
      if (securityAttachments != null && securityAttachments.length > 0) {
        for (Attachment a : securityAttachments) {
          downloadElementIds.add(a.getIdentifier());
          streamingElementIds.add(a.getIdentifier());
        }
      }

      List<Job> jobs = new ArrayList<Job>();
      try {
        for (String elementId : downloadElementIds) {
          Job job = downloadDistributionService.distribute(mediaPackage, elementId, checkAvailability);
          if (job == null)
            continue;
          jobs.add(job);
        }
        if (distributeStreaming) {
          for (String elementId : streamingElementIds) {
            Job job = streamingDistributionService.distribute(mediaPackage, elementId);
            if (job == null)
              continue;
            jobs.add(job);
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
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess())
        throw new WorkflowOperationException("One of the distribution jobs did not complete successfully");

      // Create new distribution element
      URI engageUri = URIUtils.resolve(engageBaseUrl.toURI(), "/engage/ui/watch.html?id="
              + mediaPackage.getIdentifier().compact());
      Publication publicationElement = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_NAME,
              engageUri, MimeTypes.parseMimeType("text/html"));
      mediaPackage.add(publicationElement);

      logger.debug("Distribute operation completed");

      try {
        MediaPackage mediaPackageForSearch = getMediaPackageForSearchIndex(mediaPackage, jobs);
        if (!isPublishable(mediaPackageForSearch))
          throw new WorkflowOperationException("Media package does not meet criteria for publication");

        logger.info("Publishing media package {} to search index", mediaPackageForSearch);

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

        logger.debug("Publish operation complete");
        return createResult(mediaPackage, Action.CONTINUE);
      } catch (Throwable t) {
        if (t instanceof WorkflowOperationException)
          throw (WorkflowOperationException) t;
        else
          throw new WorkflowOperationException(t);
      }
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
  }

  protected MediaPackage getMediaPackageForSearchIndex(MediaPackage current, List<Job> jobs)
          throws MediaPackageException, NotFoundException, ServiceRegistryException, WorkflowOperationException {
    MediaPackage mp = (MediaPackage) current.clone();

    List<String> elementsForPublish = new ArrayList<String>();
    // All the jobs have passed, let's update the mediapackage with references to the distributed elements
    for (Job entry : jobs) {
      Job job = serviceRegistry.getJob(entry.getId());
      String elementId = job.getArguments().get(1);
      MediaPackageElement element = mp.getElementById(elementId);

      // If there is no payload, then the item has not been distributed.
      if (job.getPayload() == null)
        continue;

      MediaPackageElement newElement = null;
      try {
        newElement = MediaPackageElementParser.getFromXml(job.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }
      // If the job finished successfully, but returned no new element, the channel simply doesn't support this
      // kind of element. So we just keep on looping.
      if (newElement == null)
        continue;

      newElement.setIdentifier(null);
      MediaPackageReference ref = element.getReference();
      if (ref != null && mp.getElementByReference(ref) != null) {
        newElement.setReference((MediaPackageReference) ref.clone());
        mp.add(newElement);
      } else {
        mp.addDerived(newElement, element);
        if (ref != null) {
          Map<String, String> props = ref.getProperties();
          newElement.getReference().getProperties().putAll(props);
        }
      }

      elementsForPublish.add(newElement.getIdentifier());
    }

    // Mark everything that is set for removal
    List<MediaPackageElement> removals = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : mp.getElements()) {
      if (!elementsForPublish.contains(element.getIdentifier())) {
        removals.add(element);
      }
    }

    // Fix references and flavors
    for (MediaPackageElement element : mp.getElements()) {

      if (removals.contains(element))
        continue;

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference != null) {
        Map<String, String> referenceProperties = reference.getProperties();
        MediaPackageElement referencedElement = mp.getElementByReference(reference);

        // if we are distributing the referenced element, everything is fine. Otherwise...
        if (referencedElement != null && removals.contains(referencedElement)) {

          // Follow the references until we find a flavor
          MediaPackageElement parent = null;
          while ((parent = current.getElementByReference(reference)) != null) {
            if (parent.getFlavor() != null && element.getFlavor() == null) {
              element.setFlavor(parent.getFlavor());
            }
            if (parent.getReference() == null)
              break;
            reference = parent.getReference();
          }

          // Done. Let's cut the path but keep references to the mediapackage itself
          if (reference != null && reference.getType().equals(MediaPackageReference.TYPE_MEDIAPACKAGE))
            element.setReference(reference);
          else if (reference != null && (referenceProperties == null || referenceProperties.size() == 0))
            element.clearReference();
          else {
            // Ok, there is more to that reference than just pointing at an element. Let's keep the original,
            // you never know.
            removals.remove(referencedElement);
            referencedElement.setURI(null);
            referencedElement.setChecksum(null);
          }
        }
      }
    }

    // Remove everything we don't want to add to publish
    for (MediaPackageElement element : removals) {
      mp.remove(element);
    }
    return mp;
  }

  /** Media package must meet these criteria in order to be published. */
  private boolean isPublishable(MediaPackage mp) {
    boolean hasTitle = !isBlank(mp.getTitle());
    if (!hasTitle)
      logger.warn("Media package does not meet criteria for publication: There is no title");

    boolean hasTracks = mp.hasTracks();
    if (!hasTracks)
      logger.warn("Media package does not meet criteria for publication: There are no tracks");

    return hasTitle && hasTracks;
  }

}
