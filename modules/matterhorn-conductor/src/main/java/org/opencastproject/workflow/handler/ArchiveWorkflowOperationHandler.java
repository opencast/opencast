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

import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation for storing a media package in the episode service.
 */
public class ArchiveWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Logger logger = LoggerFactory.getLogger(ArchiveWorkflowOperationHandler.class);

  /** The episode service */
  private EpisodeService episodeService = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS
            .put("source-tags", "Archive any mediapackage elements with one of these (whitespace separated) tags");
    CONFIG_OPTIONS.put("source-flavors", "The \"flavor\" of the track to use as a source input");
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
   * @param episodeService
   *          an instance of the search service
   */
  protected void setEpisodeService(EpisodeService episodeService) {
    this.episodeService = episodeService;
  }

  protected MediaPackage getMediaPackageForArchival(MediaPackage current, List<String> tags, String[] sourceFlavors)
          throws MediaPackageException {
    MediaPackage mp = (MediaPackage) current.clone();

    Collection<MediaPackageElement> keep;

    if (tags.isEmpty() && sourceFlavors.length < 1) {
      keep = Arrays.asList(current.getElementsByTags(tags));
    } else {
      SimpleElementSelector simpleElementSelector = new SimpleElementSelector();
      for (String flavor : sourceFlavors) {
        simpleElementSelector.addFlavor(flavor);
      }
      for (String tag : tags) {
        simpleElementSelector.addTag(tag);
      }
      keep = simpleElementSelector.select(current, false);
    }

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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(WorkflowInstance, JobContext)
   */
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    MediaPackage mediaPackageFromWorkflow = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    String tags = StringUtils.trimToNull(currentOperation.getConfiguration("source-tags"));
    String sourceFlavorsString = StringUtils.trimToEmpty(currentOperation.getConfiguration("source-flavors"));

    String[] sourceFlavors = StringUtils.split(sourceFlavorsString, ",");
    if (sourceFlavors.length < 1 && tags == null)
      logger.debug("No source tags have been specified, so everything will be added to the archive index");

    List<String> tagSet = new ArrayList<String>();
    // If a set of tags has been specified, use it
    if (tags != null)
      tagSet = asList(tags);

    try {
      MediaPackage mediaPackageForArchive = getMediaPackageForArchival(mediaPackageFromWorkflow, tagSet, sourceFlavors);
      if (mediaPackageForArchive == null)
        createResult(mediaPackageForArchive, Action.CONTINUE);

      logger.info("Archiving media package {}", mediaPackageForArchive);
      // adding media package to the episode service
      episodeService.add(mediaPackageForArchive);
      logger.debug("Archive operation complete");
      return createResult(mediaPackageFromWorkflow, Action.CONTINUE);
    } catch (Throwable t) {
      throw new WorkflowOperationException(t);
    }
  }

}
