/*
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

package org.opencastproject.workflow.api;

import static java.lang.String.format;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base implementation for an operation handler, which implements a simple start operation that returns a
 * {@link WorkflowOperationResult} with the current mediapackage and {@link Action#CONTINUE}.
 */
public abstract class AbstractWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractWorkflowOperationHandler.class);

  /** The ID of this operation handler */
  protected String id = null;

  /** The description of what this handler actually does */
  protected String description = null;

  /** Optional service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** Optional workspace */
  protected Workspace workspace = null;

  /** The JobBarrier polling interval */
  private long jobBarrierPollingInterval = JobBarrier.DEFAULT_POLLING_INTERVAL;

  /** Config for Tag Parsing operation */
  protected enum Configuration {
    none, one, atLeastOne, many
  };

  public static final String TARGET_FLAVORS = "target-flavors";
  public static final String TARGET_FLAVOR = "target-flavor";
  public static final String TARGET_TAGS = "target-tags";
  public static final String TARGET_TAG = "target-tag";
  public static final String SOURCE_FLAVORS = "source-flavors";
  public static final String SOURCE_FLAVOR = "source-flavor";
  public static final String SOURCE_TAG = "source-tag";
  public static final String SOURCE_TAGS = "source-tags";

  /**
   * Activates this component with its properties once all of the collaborating services have been set
   *
   * @param cc
   *          The component's context, containing the properties used for configuration
   */
  @Activate
  protected void activate(ComponentContext cc) {
    this.id = (String) cc.getProperties().get(WorkflowService.WORKFLOW_OPERATION_PROPERTY);
    this.description = (String) cc.getProperties().get(Constants.SERVICE_DESCRIPTION);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public abstract WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#skip(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    return createResult(Action.SKIP);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#destroy(
   *      org.opencastproject.workflow.api.WorkflowInstance, JobContext)
   */
  @Override
  public void destroy(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
  }

  /**
   * Converts a comma separated string into a set of values. Useful for converting operation configuration strings into
   * multi-valued sets.
   *
   * @param elements
   *          The comma space separated string
   * @return the set of values
   */
  protected List<String> asList(String elements) {
    elements = StringUtils.trimToEmpty(elements);
    List<String> list = new ArrayList<>();
    for (String s : StringUtils.split(elements, ",")) {
      if (StringUtils.trimToNull(s) != null) {
        list.add(s.trim());
      }
    }
    return list;
  }

  protected MediaPackageElement createDerivedMediaPackageElementFrom(MediaPackageElement source)
          throws NotFoundException, IOException {
    MediaPackageElement derived = (MediaPackageElement) source.clone();
    derived.generateIdentifier();
    derived.referTo(source);
    // copy file
    derived.setURI(workspace.put(
        source.getMediaPackage().getIdentifier().toString(),
        derived.getIdentifier(),
        getFileNameFromElements(source, derived),
        workspace.read(source.getURI())
    ));
    return derived;
  }

  /**
   * Generates a filename using the base name of a source element and the extension of a derived element.
   *
   * @param source
   *          the source media package element
   * @param derived
   *          the derived media package element
   * @return the filename
   */
  protected String getFileNameFromElements(MediaPackageElement source, MediaPackageElement derived) {
    String fileName = FilenameUtils.getBaseName(source.getURI().getPath());
    String fileExtension = FilenameUtils.getExtension(derived.getURI().getPath());
    return fileName + "." + fileExtension;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getId()
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Creates a result for the execution of this workflow operation handler.
   *
   * @param action
   *          the action to take
   * @return the result
   */
  protected WorkflowOperationResult createResult(Action action) {
    return createResult(null, null, action, 0);
  }

  /**
   * Creates a result for the execution of this workflow operation handler.
   *
   * @param mediaPackage
   *          the modified mediapackage
   * @param action
   *          the action to take
   * @return the result
   */
  protected WorkflowOperationResult createResult(MediaPackage mediaPackage, Action action) {
    return createResult(mediaPackage, null, action, 0);
  }

  /**
   * Creates a result for the execution of this workflow operation handler.
   * <p>
   * Since there is no way for the workflow service to determine the queuing time (e. g. waiting on services), it needs
   * to be provided by the handler.
   *
   * @param mediaPackage
   *          the modified mediapackage
   * @param action
   *          the action to take
   * @param timeInQueue
   *          the amount of time this handle spent waiting for services
   * @return the result
   */
  protected WorkflowOperationResult createResult(MediaPackage mediaPackage, Action action, long timeInQueue) {
    return createResult(mediaPackage, null, action, timeInQueue);
  }

  /**
   * Creates a result for the execution of this workflow operation handler.
   * <p>
   * Since there is no way for the workflow service to determine the queuing time (e. g. waiting on services), it needs
   * to be provided by the handler.
   *
   * @param mediaPackage
   *          the modified mediapackage
   * @param properties
   *          the properties to add to the workflow instance
   * @param action
   *          the action to take
   * @param timeInQueue
   *          the amount of time this handle spent waiting for services
   * @return the result
   */
  protected WorkflowOperationResult createResult(MediaPackage mediaPackage, Map<String, String> properties,
          Action action, long timeInQueue) {
    return new WorkflowOperationResultImpl(mediaPackage, properties, action, timeInQueue);
  }

  /**
   * Sets the service registry. This method is here as a convenience for developers that need the registry to do job
   * waiting.
   *
   * @param serviceRegistry
   *          the service registry
   */
  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Waits until all of the jobs have reached either one of these statuses:
   * <ul>
   * <li>{@link Job.Status#FINISHED}</li>
   * <li>{@link Job.Status#FAILED}</li>
   * <li>{@link Job.Status#DELETED}</li>
   * </ul>
   * After that, the method returns with the actual outcomes of the jobs.
   *
   * @param jobs
   *          the jobs
   * @return the jobs and their outcomes
   * @throws IllegalStateException
   *           if the service registry has not been set
   * @throws IllegalArgumentException
   *           if the jobs collecion is either <code>null</code> or empty
   */
  protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
    return waitForStatus(0, jobs);
  }

  /**
   * Waits until all of the jobs have reached either one of these statuses:
   * <ul>
   * <li>{@link Job.Status#FINISHED}</li>
   * <li>{@link Job.Status#FAILED}</li>
   * <li>{@link Job.Status#DELETED}</li>
   * </ul>
   * After that, the method returns with the actual outcomes of the jobs.
   *
   * @param timeout
   *          the maximum amount of time in milliseconds to wait
   * @param jobs
   *          the jobs
   * @return the jobs and their outcomes
   * @throws IllegalStateException
   *           if the service registry has not been set
   * @throws IllegalArgumentException
   *           if the jobs collection is either <code>null</code> or empty
   */
  protected JobBarrier.Result waitForStatus(long timeout, Job... jobs) throws IllegalStateException,
          IllegalArgumentException {
    if (serviceRegistry == null) {
      throw new IllegalStateException("Can't wait for job status without providing a service registry first");
    }
    JobBarrier barrier = new JobBarrier(null, serviceRegistry, jobBarrierPollingInterval, jobs);
    return barrier.waitForJobs(timeout);
  }

  /**
   * Get a mandatory configuration key. Values are returned trimmed.
   *
   * @throws WorkflowOperationException
   *         if the configuration key is either missing or empty
   */
  protected String getConfig(WorkflowInstance wi, String key) throws WorkflowOperationException {
    return getConfig(wi.getCurrentOperation(), key);
  }

  /**
   * Get a configuration key. Values are returned trimmed.
   *
   * @param w
   *        WorkflowInstance with current operation
   * @param key
   *        Configuration key to check for
   * @param defaultValue
   *        Value to return if key does not exists
   */
  protected String getConfig(WorkflowInstance w, String key, String defaultValue) {
    Optional<String> cfgOpt = getOptConfig(w.getCurrentOperation(), key);
    if (cfgOpt.isPresent()) {
      return cfgOpt.get();
    }
    return defaultValue;
  }

  /**
   * Get a mandatory configuration key. Values are returned trimmed.
   *
   * @throws WorkflowOperationException
   *         if the configuration key is either missing or empty
   */
  protected String getConfig(WorkflowOperationInstance woi, String key) throws WorkflowOperationException {
    Optional<String> cfgOpt = getOptConfig(woi, key);
    if (cfgOpt.isPresent()) {
      return cfgOpt.get();
    }
    throw new WorkflowOperationException(format("Configuration key '%s' is either missing or empty", key));
  }

  /**
   * Get an optional configuration key. Values are returned trimmed.
   */
  protected Optional<String> getOptConfig(WorkflowInstance wi, String key) {
    return getOptConfig(wi.getCurrentOperation(), key);
  }

  /**
   * Get an optional configuration key. Values are returned trimmed.
   */
  protected Optional<String> getOptConfig(WorkflowOperationInstance woi, String key) {
    return Optional.ofNullable(woi.getConfiguration(key))
        .map(String::trim)
        .filter(s -> !s.isEmpty());
  }

  /**
   * Uses one {@code configuration} for both source tags and flavors, in case the WOH does not care whether the source
   * entities are identified by tags or flavors.
   *
   * @see AbstractWorkflowOperationHandler#getTagsAndFlavors(WorkflowInstance, Configuration, Configuration,
   *      Configuration, Configuration)
   */
  protected ConfiguredTagsAndFlavors getTagsAndFlavors(WorkflowInstance workflow, Configuration srcTagsAndFlavors,
      Configuration targetTags, Configuration targetFlavors) throws WorkflowOperationException {
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(
        workflow,
        Configuration.many,
        Configuration.many,
        targetTags,
        targetFlavors);

    switch(srcTagsAndFlavors) {
      case none:
        if (!tagsAndFlavors.getSrcFlavors().isEmpty() || !tagsAndFlavors.getSrcTags().isEmpty()) {
          throw new WorkflowOperationException("No source tags or flavors may be set.");
        }
        break;
      case one:
        if (tagsAndFlavors.getSrcFlavors().size() + tagsAndFlavors.getSrcTags().size() != 1) {
          throw new WorkflowOperationException("Exactly one source tag or flavor must be set.");
        }
        break;
      case atLeastOne:
        if (tagsAndFlavors.getSrcFlavors().size() + tagsAndFlavors.getSrcTags().size() < 1) {
          throw new WorkflowOperationException("At least one source tag or flavor must be set.");
        }
        break;
      case many:
        break;
      default:
        throw new WorkflowOperationException("Couldn't process srcTagsAndFlavors configuration option!");
    }

    return tagsAndFlavors;
  }

  /**
   * Returns a ConfiguredTagsAndFlavors instance, which includes all specified source/target tags and flavors if they
   * are valid.
   * Lists can be empty, if no values were specified! This is to enable WOHs to individually check if a given tag/flavor
   * was set.
   * This also means that you should use Configuration.many as parameter, if a tag/flavor is optional.
   * None: Must not be specified
   * One: Exactly one must be specified
   * AtLeastOne: At least one must be specified
   * Many: An arbitrary amount may be specified
   * @param srcTags none, one, atLeastOne or many
   * @param srcFlavors none, one, atLeastOne or many
   * @param targetFlavors none, one, atLeastOne or many
   * @param targetTags none, one, atLeastOne or many
   * @return ConfiguredTagsAndFlavors object including lists for the configured tags/flavors
   */
  protected ConfiguredTagsAndFlavors getTagsAndFlavors(WorkflowInstance workflow, Configuration srcTags,
      Configuration srcFlavors, Configuration targetTags, Configuration targetFlavors)
          throws WorkflowOperationException {
    WorkflowOperationInstance operation = workflow.getCurrentOperation();
    ConfiguredTagsAndFlavors tagsAndFlavors = new ConfiguredTagsAndFlavors();
    MediaPackageElementFlavor flavor;

    List<String> srcTagList = new ArrayList<>();
    switch(srcTags) {
      case none:
        break;
      case one:
        String srcTag = StringUtils.trimToNull(operation.getConfiguration(SOURCE_TAG));
        if (srcTag == null) {
          throw new WorkflowOperationException("Configuration key '" + SOURCE_TAG + "' must be set");
        }
        srcTagList.add(srcTag);
        break;
      case atLeastOne:
        srcTagList = getTags(operation, SOURCE_TAGS, SOURCE_TAG);
        if (srcTagList.isEmpty()) {
          throw new WorkflowOperationException("Configuration key '" + SOURCE_TAGS + "' or '" + SOURCE_TAG
              + "' must be set");
        }
        break;
      case many:
        srcTagList = getTags(operation, SOURCE_TAGS, SOURCE_TAG);
        break;
      default:
        throw new WorkflowOperationException("Couldn't process srcTags configuration option!");
    }
    tagsAndFlavors.setSrcTags(srcTagList);

    List<MediaPackageElementFlavor> srcFlavorList = new ArrayList<>();
    switch(srcFlavors) {
      case none:
        break;
      case one:
        String singleSourceFlavor = StringUtils.trimToNull(operation.getConfiguration(SOURCE_FLAVOR));
        if (singleSourceFlavor == null) {
          throw new WorkflowOperationException("Configuration key '" + SOURCE_FLAVOR + "' must be set");
        }
        try {
          flavor = MediaPackageElementFlavor.parseFlavor(singleSourceFlavor);
        } catch (IllegalArgumentException e) {
          throw new WorkflowOperationException(singleSourceFlavor + " is not a valid flavor!");
        }
        srcFlavorList.add(flavor);
        break;
      case atLeastOne:
        srcFlavorList = getFlavors(operation, SOURCE_FLAVORS, SOURCE_FLAVOR);
        if (srcFlavorList.isEmpty()) {
          throw new WorkflowOperationException("Configuration key '" + SOURCE_FLAVORS + "' or '" + SOURCE_FLAVOR
              + "' must be set");
        }
        break;
      case many:
        srcFlavorList = getFlavors(operation, SOURCE_FLAVORS, SOURCE_FLAVOR);
        break;
      default:
        throw new WorkflowOperationException("Couldn't process srcFlavors configuration option!");
    }
    tagsAndFlavors.setSrcFlavors(srcFlavorList);

    ConfiguredTagsAndFlavors.TargetTags targetTagMap = new ConfiguredTagsAndFlavors.TargetTags();
    List<String> targetTagList = new ArrayList<>();
    switch(targetTags) {
      case none:
        break;
      case one:
        String targetTag = StringUtils.trimToNull(operation.getConfiguration(TARGET_TAG));
        if (targetTag == null) {
          throw new WorkflowOperationException("Configuration key '" + TARGET_TAG + "' must be set");
        }
        targetTagMap = parseTargetTagsByType(List.of(targetTag));
        break;
      case atLeastOne:
        targetTagList = getTags(operation, TARGET_TAGS, TARGET_TAG);
        if (targetTagList.isEmpty()) {
          throw new WorkflowOperationException("Configuration key '" + TARGET_TAGS + "' or '" + TARGET_TAG
              + "' must be set");
        }
        targetTagMap = parseTargetTagsByType(targetTagList);
        break;
      case many:
        targetTagList = getTags(operation, TARGET_TAGS, TARGET_TAG);
        targetTagMap = parseTargetTagsByType(targetTagList);
        break;
      default:
        throw new WorkflowOperationException("Couldn't process target-tag configuration option!");
    }
    tagsAndFlavors.setTargetTags(targetTagMap);

    List<MediaPackageElementFlavor> targetFlavorList = new ArrayList<>();
    switch(targetFlavors) {
      case none:
        break;
      case one:
        String singleTargetFlavor = StringUtils.trimToNull(operation.getConfiguration(TARGET_FLAVOR));
        if (singleTargetFlavor == null) {
          throw new WorkflowOperationException("Configuration key '" + TARGET_FLAVOR + "' must be set");
        }
        try {
          flavor = MediaPackageElementFlavor.parseFlavor(singleTargetFlavor);
        } catch (IllegalArgumentException e) {
          throw new WorkflowOperationException(singleTargetFlavor + " is not a valid flavor!");
        }
        targetFlavorList.add(flavor);
        break;
      case atLeastOne:
        targetFlavorList = getFlavors(operation, TARGET_FLAVORS, TARGET_FLAVOR);
        if (targetTagList.isEmpty()) {
          throw new WorkflowOperationException("Configuration key '" + TARGET_FLAVORS + "' or '" + TARGET_FLAVOR
              + "' must be set");
        }
        break;
      case many:
        targetFlavorList = getFlavors(operation, TARGET_FLAVORS, TARGET_FLAVOR);
        break;
      default:
        throw new WorkflowOperationException("Couldn't process targetFlavors configuration option!");
    }
    tagsAndFlavors.setTargetFlavors(targetFlavorList);
    return tagsAndFlavors;
  }

  private ConfiguredTagsAndFlavors.TargetTags parseTargetTagsByType(List<String> tags) {
    final String plus = "+";
    final String minus = "-";
    List<String> overrideTags = new ArrayList();
    List<String> addTags = new ArrayList();
    List<String> removeTags = new ArrayList();

    for (String targetTag : tags) {
      if (!StringUtils.startsWithAny(targetTag, plus, minus)) {
        if (addTags.size() > 0
            || removeTags.size() > 0) {
          logger.warn("You may not mix override tags and tag changes. "
              + "The list of override tags so far is {}. "
              + "The tag {} is not prefixed with '{}' or '{}'.", overrideTags, targetTag, plus, minus);
        }
        overrideTags.add(targetTag);
      } else if (StringUtils.startsWith(targetTag, plus)) {
        addTags.add(StringUtils.substring(targetTag, 1));
      } else if (StringUtils.startsWith(targetTag, minus)) {
        removeTags.add(StringUtils.substring(targetTag, 1));
      }
    }

    return new ConfiguredTagsAndFlavors.TargetTags(overrideTags, addTags, removeTags);
  }

  /**
   * Helper function that applies target tags to the given element, based on the type(s) of the tag(s)
   * @param targetTags The target tags to apply to the element
   * @param element The element the target tags are applied to
   * @return The element with the applied target tags
   */
  protected <T extends MediaPackageElement> T applyTargetTagsToElement(
      ConfiguredTagsAndFlavors.TargetTags targetTags,
      T element
  ) {
    // set tags on target element
    List<String> overrideTags = targetTags.getOverrideTags();
    List<String> addTags = targetTags.getAddTags();
    List<String> removeTags = targetTags.getRemoveTags();
    if (overrideTags.size() > 0) {
      element.clearTags();
      for (String tag : overrideTags) {
        element.addTag(tag);
      }
    } else {
      for (String tag : removeTags) {
        element.removeTag(tag);
      }
      for (String tag : addTags) {
        element.addTag(tag);
      }
    }

    return element;
  }

  private List<String> getTags(WorkflowOperationInstance operation, String multipleTagsKey, String singleTagKey) {
    List<String> tagList = asList(StringUtils.trimToNull(operation.getConfiguration(multipleTagsKey)));
    String singleTag = StringUtils.trimToNull(operation.getConfiguration(singleTagKey));
    if (tagList.isEmpty() && singleTag != null) {
      tagList.add(singleTag);
    }
    return tagList;
  }

  private List<MediaPackageElementFlavor> getFlavors(WorkflowOperationInstance operation, String multipleFlavorsKey,
      String singleFlavorKey) throws WorkflowOperationException {
    List<MediaPackageElementFlavor> flavorList = new ArrayList<>();
    List<String> singleFlavorString = asList(StringUtils.trimToNull(operation.getConfiguration(multipleFlavorsKey)));
    String singleSourceFlavor = StringUtils.trimToNull(operation.getConfiguration(singleFlavorKey));
    if (singleFlavorString.isEmpty() && singleSourceFlavor != null) {
      singleFlavorString.add(singleSourceFlavor);
    }
    for (String elem : singleFlavorString) {
      try {
        MediaPackageElementFlavor flavor = MediaPackageElementFlavor.parseFlavor(elem);
        flavorList.add(flavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(elem + " is not a valid flavor!");
      }
    }
    return flavorList;
  }

  /**
   * Set the @link org.opencastproject.job.api.JobBarrier polling interval.
   * <p>
   * While waiting for other jobs to finish, the barrier will poll the status of these jobs until they are finished. To
   * reduce load on the system, the polling is done only every x milliseconds. This interval defines the sleep time
   * between these polls.
   * <p>
   * If most cases you want to leave this at its default value. It will make sense, though, to adjust this time if you
   * know that your job will be exceptionally short. An example of this might be the unit tests where other jobs are
   * usually mocked. But this setting is not limited to tests and may be a sensible options for other jobs as well.
   *
   * @param interval the time in miliseconds between two polling operations
   *
   * @see org.opencastproject.job.api.JobBarrier#DEFAULT_POLLING_INTERVAL
   */
  public void setJobBarrierPollingInterval(long interval) {
    this.jobBarrierPollingInterval = interval;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : super.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WorkflowOperationHandler) {
      if (id != null) {
        return id.equals(((WorkflowOperationHandler) obj).getId());
      } else {
        return this == obj;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getId();
  }
}
