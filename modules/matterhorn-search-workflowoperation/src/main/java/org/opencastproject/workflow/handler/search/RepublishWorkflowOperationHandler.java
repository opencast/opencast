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
package org.opencastproject.workflow.handler.search;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for handling "republish" operations
 */
public class RepublishWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(RepublishWorkflowOperationHandler.class);

  /** The search service */
  private SearchService searchService = null;

  /** The configuration options */
  private static final String OPT_SOURCE_FLAVORS = "source-flavors";
  private static final String OPT_SOURCE_TAGS = "source-tags";
  private static final String OPT_MERGE = "merge";
  private static final String OPT_EXISTING_ONLY = "existing-only";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(OPT_SOURCE_FLAVORS, "Republish any mediapackage elements with one of these flavors");
    CONFIG_OPTIONS.put(OPT_SOURCE_TAGS, "Republish only mediapackage elements that are tagged with one of these tags");
    CONFIG_OPTIONS.put(OPT_MERGE, "Merge with existing published data");
    CONFIG_OPTIONS.put(OPT_EXISTING_ONLY, "Republish only if it can be merged with or replace existing published data");
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
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    Id mId = mediaPackage.getIdentifier();

    // The flavors of the elements that are to be published
    Set<MediaPackageElementFlavor> flavors = new HashSet<MediaPackageElementFlavor>();

    // Check which flavors have been configured
    String configuredFlavors = workflowInstance.getCurrentOperation().getConfiguration(OPT_SOURCE_FLAVORS);
    if (StringUtils.trimToNull(configuredFlavors) == null) {
      logger.warn("No flavors have been specified, republishing all media package elements");
      for (MediaPackageElement e : mediaPackage.elements()) {
        flavors.add(e.getFlavor());
      }
    } else {
      for (String flavor : asList(configuredFlavors)) {
        MediaPackageElementFlavor f = MediaPackageElementFlavor.parseFlavor(flavor);
        flavors.add(f);
      }
    }

    // Get the configured tags
    String configuredTags = workflowInstance.getCurrentOperation().getConfiguration(OPT_SOURCE_TAGS);
    List<String> tags = asList(configuredTags);

    // Merge or replace?
    boolean merge = Boolean.parseBoolean(workflowInstance.getCurrentOperation().getConfiguration(OPT_MERGE));
    boolean onlyExisting = Boolean.parseBoolean(workflowInstance.getCurrentOperation().getConfiguration(OPT_EXISTING_ONLY));

    // Apply tags and flavors to the current mediapackage
    MediaPackage filteredMediaPackage = null;
    try {
      filteredMediaPackage = filterMediaPackage(mediaPackage, flavors, tags);
    } catch (MediaPackageException e) {
      throw new WorkflowOperationException("Error filtering media package", e);
    }

    // If merge, load current mediapackage from search service
    MediaPackage publishedMediaPackage = null;
    if (merge || onlyExisting) {
      SearchQuery query = new SearchQuery().withId(mId.toString());
      SearchResult result = searchService.getByQuery(query);
      if (result.size() == 0) {
        logger.info("The search service doesn't know mediapackage {}", mId);
        if (onlyExisting) {
          logger.info("Skipping republish for {} since it is not currently published", mId);
          return createResult(mediaPackage, Action.SKIP);
        }
        publishedMediaPackage = filteredMediaPackage;
      } else if (result.size() > 1) {
        logger.warn("More than one mediapackage with id {} returned from search service", mId);
        throw new WorkflowOperationException("More than one mediapackage with id " + mId + " found");
      } else {
        publishedMediaPackage = merge(filteredMediaPackage, result.getItems()[0].getMediaPackage());
      }
    } else {
      publishedMediaPackage = filteredMediaPackage;
    }

    // Does the mediapackage have a title and track?
    if (!isPublishable(publishedMediaPackage)) {
      throw new WorkflowOperationException("Media package does not meet criteria for publication");
    }

    // Publish the media package to the search index
    try {

      logger.info("Publishing media package {} to search index", publishedMediaPackage);
      Job publishJob = null;
      try {
        publishJob = searchService.add(publishedMediaPackage);
        if (!waitForStatus(publishJob).isSuccess()) {
          throw new WorkflowOperationException("Mediapackage " + mediaPackage + " could not be republished");
        }
      } catch (SearchException e) {
        throw new WorkflowOperationException("Error republishing media package", e);
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException("Error parsing media package", e);
      }

      logger.info("Completed republish operation on {}", mediaPackage.getIdentifier());
      return createResult(mediaPackage, Action.CONTINUE);

    } catch (Throwable t) {
      if (t instanceof WorkflowOperationException)
        throw (WorkflowOperationException) t;
      else
        throw new WorkflowOperationException(t);
    }
  }

  /**
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
  static MediaPackage merge(MediaPackage updatedMp, MediaPackage publishedMp) {
    if (publishedMp == null)
      return updatedMp;

    final MediaPackage mergedMp = MediaPackageSupport.copy(publishedMp);
    for (final MediaPackageElement updatedElement : updatedMp.elements()) {
      for (final MediaPackageElementFlavor flavor : Option.option(updatedElement.getFlavor())) {
        for (final MediaPackageElement outdated : mergedMp.getElementsByFlavor(flavor)) {
          mergedMp.remove(outdated);
        }
        logger.info(format("Update %s of type %s", updatedElement.getIdentifier(), updatedElement.getElementType()));
        mergedMp.add(updatedElement);
      }
    }
    return mergedMp;
  }

  /**
   * Creates a clone of the mediapackage and removes those elements that do not match the flavor and tags filter
   * criteria.
   *
   * @param mediaPackage
   *          the media package
   * @param flavors
   *          the flavors
   * @param tags
   *          the tags
   * @return the filtered media package
   */
  protected MediaPackage filterMediaPackage(MediaPackage mediaPackage, Set<MediaPackageElementFlavor> flavors,
          List<String> tags) throws MediaPackageException {
    MediaPackage filteredMediaPackage = (MediaPackage) mediaPackage.clone();

    // The list of elements to keep
    List<MediaPackageElement> keep = new ArrayList<MediaPackageElement>();

    // Filter by flavor
    if (flavors.size() > 0) {
      logger.debug("Filtering elements based on flavors");
      for (MediaPackageElementFlavor flavor : flavors) {
        keep.addAll(Arrays.asList(mediaPackage.getElementsByFlavor(flavor)));
      }
    }

    // Keep those elements that have been identified in the tags
    if (tags.size() > 0) {
      logger.debug("Filtering elements based on tags");
      if (keep.size() > 0) {
        keep.retainAll(Arrays.asList(mediaPackage.getElementsByTags(tags)));
      } else {
        keep.addAll(Arrays.asList(mediaPackage.getElementsByTags(tags)));
      }
    }

    // If no filter has been supplied, take all elements
    if (flavors.size() == 0 && tags.size() == 0)
      keep.addAll(Arrays.asList(mediaPackage.getElements()));

    // Fix references and flavors
    for (MediaPackageElement element : filteredMediaPackage.getElements()) {

      if (!keep.contains(element)) {
        logger.info("Removing {} '{}' from mediapackage '{}'", new String[] {
                element.getElementType().toString().toLowerCase(), element.getIdentifier(),
                filteredMediaPackage.getIdentifier().toString() });
        filteredMediaPackage.remove(element);
        continue;
      }

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference != null) {
        Map<String, String> referenceProperties = reference.getProperties();
        MediaPackageElement referencedElement = filteredMediaPackage.getElementByReference(reference);

        // if we are distributing the referenced element, everything is fine. Otherwise...
        if (referencedElement != null && !keep.contains(referencedElement.getIdentifier())) {

          // Follow the references until we find a flavor
          MediaPackageElement parent = null;
          while ((parent = mediaPackage.getElementByReference(reference)) != null) {
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
            referencedElement.setURI(null);
            referencedElement.setChecksum(null);
          }
        }
      }
    }

    return filteredMediaPackage;
  }

  /**
   * Media package must have a title and contain tracks in order to be published.
   *
   * @param mp
   *          the media package
   * @return <code>true</code> if the mediapackage can be published
   */
  protected boolean isPublishable(MediaPackage mp) {
    return !isBlank(mp.getTitle()) && mp.hasTracks();
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

}
