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

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.mediapackage.selector.SimpleFlavorPrioritySelector;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "distribute" operations
 */
public class DistributeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DistributeWorkflowOperationHandler.class);

  /** The distribution service */
  private DistributionService distributionService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param distributionService
   *          the distribution service
   */
  protected void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-tags",
            "Distribute any mediapackage elements with one of these (comma separated) tags.  If a source-tag "
                    + "starts with a '-', mediapackage elements with this tag will be excluded from distribution.");
    CONFIG_OPTIONS.put("target-tags",
            "Apply these (comma separated) tags to any mediapackage elements produced as a result of distribution");
    CONFIG_OPTIONS.put("source-flavors",
            "Apply these (comma separated) flavors to any mediapackage elements produced as a result of distribution");
    CONFIG_OPTIONS
            .put("source-priority-flavors",
                    "Apply these (comma separated) priority flavors to any mediapackage elements produced as a result of distribution");
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
    logger.debug("Running distribution workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Check which tags have been configured
    String sourceTags = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration("source-tags"));
    String targetTags = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration("target-tags"));
    String sourceFlavors = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(
            "source-flavors"));
    String sourcePriorityFlavors = StringUtils.trimToNull(workflowInstance.getCurrentOperation().getConfiguration(
            "source-priority-flavors"));

    AbstractMediaPackageElementSelector<MediaPackageElement> elementSelector;

    if (sourcePriorityFlavors != null) {
      if (sourceFlavors != null || sourceTags != null)
        throw new IllegalArgumentException(
                "Source-flavors or source-tags can not be used in case of source-priority-flavors property");

      elementSelector = new SimpleFlavorPrioritySelector();
      for (String flavor : asList(sourcePriorityFlavors)) {
        elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
      }
    } else {

      if (sourceTags == null && sourceFlavors == null) {
        logger.warn("No tags or flavors have been specified");
        return createResult(mediaPackage, Action.CONTINUE);
      }
      elementSelector = new SimpleElementSelector();

      if (sourceFlavors != null) {
        for (String flavor : asList(sourceFlavors)) {
          elementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
        }
      }
      if (sourceTags != null) {
        for (String tag : asList(sourceTags)) {
          elementSelector.addTag(tag);
        }
      }
    }

    try {

      Set<String> elementIds = new HashSet<String>();

      // Look for elements matching the tag
      Collection<MediaPackageElement> elements = elementSelector.select(mediaPackage, false);
      for (MediaPackageElement elem : elements) {
        elementIds.add(elem.getIdentifier());
      }

      // Also distribute all of the metadata catalogs
      for (Catalog c : mediaPackage.getCatalogs())
        elementIds.add(c.getIdentifier());

      // Also distribute the security configuration
      // -----
      // Stop distributing the security config for now since no one actually uses it.
      // This is done as a fix for MH-8515. I leave the code in place since it should
      // be reactivated as soon as this issues has been resolved cleanly. Please see
      // the ticket for further information [cedriessen]
      // -----
      // Attachment[] securityAttachments = mediaPackage.getAttachments(MediaPackageElements.XACML_POLICY);
      // if (securityAttachments != null && securityAttachments.length > 0) {
      // for (Attachment a : securityAttachments) {
      // elementIds.add(a.getIdentifier());
      // }
      // }

      // Finally, push the elements to the distribution channel
      List<String> targetTagList = asList(targetTags);

      Map<String, Job> jobs = new HashMap<String, Job>(elementIds.size());
      try {
        for (String elementId : elementIds) {
          Job job = distributionService.distribute(mediaPackage, elementId);
          if (job == null)
            continue;
          jobs.put(elementId, job);
        }
      } catch (DistributionException e) {
        throw new WorkflowOperationException(e);
      }

      // Wait until all distribution jobs have returned
      if (!waitForStatus(jobs.values().toArray(new Job[jobs.size()])).isSuccess()) {
        throw new WorkflowOperationException("One of the distribution jobs did not complete successfully");
      }

      // All the jobs have passed, let's update the mediapackage with references to the distributed elements
      for (Map.Entry<String, Job> entry : jobs.entrySet()) {
        String elementId = entry.getKey();
        Job job = serviceRegistry.getJob(entry.getValue().getId());
        MediaPackageElement element = mediaPackage.getElementById(elementId);

        // If there is no payload, then the item has not been distributed.
        if (job.getPayload() == null) {
          continue;
        }

        MediaPackageElement newElement = null;
        try {
          newElement = MediaPackageElementParser.getFromXml(job.getPayload());
        } catch (MediaPackageException e) {
          throw new WorkflowOperationException(e);
        }
        // If the job finished successfully, but returned no new element, the channel simply doesn't support this
        // kind of element. So we just keep on looping.
        if (newElement == null) {
          continue;
        }
        newElement.setIdentifier(null);
        MediaPackageReference ref = element.getReference();
        if (ref != null && mediaPackage.getElementByReference(ref) != null) {
          newElement.setReference((MediaPackageReference) ref.clone());
          mediaPackage.add(newElement);
        } else {
          mediaPackage.addDerived(newElement, element);
          if (ref != null) {
            Map<String, String> props = ref.getProperties();
            newElement.getReference().getProperties().putAll(props);
          }
        }

        for (String tag : targetTagList) {
          if (StringUtils.trimToNull(tag) == null)
            continue;
          newElement.addTag(tag);
        }
      }

      logger.debug("Distribute operation completed");
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
