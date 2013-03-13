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

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for handling "publish" operations
 */
public class PublishWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(PublishWorkflowOperationHandler.class);

  /** The search service */
  private SearchService searchService = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS
            .put("source-tags", "Publish any mediapackage elements with one of these (whitespace separated) tags");
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
   * Callback for declarative services configuration that will introduce us to the search service. Implementation
   * assumes that the reference is configured as being static.
   * 
   * @param searchService
   *          an instance of the search service
   */
  protected void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  protected MediaPackage getMediaPackageForSearchIndex(MediaPackage current, List<String> tags)
          throws MediaPackageException {
    MediaPackage mp = (MediaPackage) current.clone();

    List<MediaPackageElement> keep = new ArrayList<MediaPackageElement>();

    // Keep those elements that have been identified in the tags
    keep.addAll(Arrays.asList(current.getElementsByTags(tags)));

    // Explicitly keep all security policies
    keep.addAll(Arrays.asList(mp.getAttachments(MediaPackageElements.XACML_POLICY)));

    // Mark everything that is set for removal
    List<MediaPackageElement> removals = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : mp.getElements()) {
      if (!keep.contains(element)) {
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackageFromWorkflow = workflowInstance.getMediaPackage();

    // Check which tags have been configured
    String tags = workflowInstance.getCurrentOperation().getConfiguration("source-tags");
    if (StringUtils.trimToNull(tags) == null) {
      logger.warn("No source tags have been specified, so nothing will be added to the search index");
      return createResult(mediaPackageFromWorkflow, Action.CONTINUE);
    }

    List<String> tagSet = asList(tags);

    try {
      MediaPackage mediaPackageForSearch = getMediaPackageForSearchIndex(mediaPackageFromWorkflow, tagSet);
      if (!isPublishable(mediaPackageForSearch)) {
        throw new WorkflowOperationException("Media package does not meet criteria for publication");
      }
      logger.info("Publishing media package {} to search index", mediaPackageForSearch);

      // adding media package to the search index
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
      return createResult(mediaPackageFromWorkflow, Action.CONTINUE);
    } catch (Throwable t) {
      if (t instanceof WorkflowOperationException)
        throw (WorkflowOperationException) t;
      else
        throw new WorkflowOperationException(t);
    }
  }

}
