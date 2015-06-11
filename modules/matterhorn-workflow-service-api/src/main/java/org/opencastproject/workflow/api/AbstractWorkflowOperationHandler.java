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

package org.opencastproject.workflow.api;

import static java.lang.String.format;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Misc.chuck;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Abstract base implementation for an operation handler, which implements a simple start operation that returns a
 * {@link WorkflowOperationResult} with the current mediapackage and {@link Action#CONTINUE}.
 */
public abstract class AbstractWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The ID of this operation handler */
  protected String id = null;

  /** The description of what this handler actually does */
  protected String description = null;

  /** The configuration options for this operation handler */
  protected SortedMap<String, String> options = new TreeMap<String, String>();

  /** Optional service registry */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * Activates this component with its properties once all of the collaborating services have been set
   *
   * @param cc
   *          The component's context, containing the properties used for configuration
   */
  protected void activate(ComponentContext cc) {
    this.id = (String) cc.getProperties().get(WorkflowService.WORKFLOW_OPERATION_PROPERTY);
    this.description = (String) cc.getProperties().get(Constants.SERVICE_DESCRIPTION);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public abstract WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException;

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#skip(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    return createResult(Action.SKIP);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#destroy(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public void destroy(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return options;
  }

  /**
   * Adds a configuration option to the list of possible configuration options.
   *
   * @param name
   *          the option name
   * @param description
   *          the option description
   */
  public void addConfigurationOption(String name, String description) {
    options.put(name, description);
  }

  /**
   * Removes the configuration option from the list of possible configuration options.
   *
   * @param name
   *          the option name
   */
  public void removeConfigurationOption(String name) {
    options.remove(name);
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
    elements = StringUtils.trimToNull(elements);
    List<String> list = new ArrayList<String>();
    if (elements != null) {
      for (String s : StringUtils.split(elements, ",")) {
        if (StringUtils.trimToNull(s) != null) {
          list.add(s.trim());
        }
      }
    }
    return list;
  }

  /** {@link #asList(String)} as a function. */
  protected Function<String, List<String>> asList = new Function<String, List<String>>() {
    @Override public List<String> apply(String s) {
      return asList(s);
    }
  };

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
   * <p>
   * Since there is no way for the workflow service to determine the queuing time (e. g. waiting on services), it needs
   * to be provided by the handler.
   *
   * @param action
   *          the action to take
   * @param timeInQueue
   *          the amount of time this handle spent waiting for services
   * @return the result
   */
  protected WorkflowOperationResult createResult(Action action, long timeInQueue) {
    return createResult(null, null, action, timeInQueue);
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
   *          the maximum amount of time in miliseconds to wait
   * @param jobs
   *          the jobs
   * @return the jobs and their outcomes
   * @throws IllegalStateException
   *           if the service registry has not been set
   * @throws IllegalArgumentException
   *           if the jobs collecion is either <code>null</code> or empty
   */
  protected JobBarrier.Result waitForStatus(long timeout, Job... jobs) throws IllegalStateException,
          IllegalArgumentException {
    if (serviceRegistry == null)
      throw new IllegalStateException("Can't wait for job status without providing a service registry first");
    JobBarrier barrier = new JobBarrier(serviceRegistry, jobs);
    return barrier.waitForJobs(timeout);
  }

  /**
   * Get a configuration option.
   *
   * @deprecated use {@link #getConfig(WorkflowInstance, String)} or {@link #getOptConfig(org.opencastproject.workflow.api.WorkflowInstance, String)}
   */
  protected Option<String> getCfg(WorkflowInstance wi, String key) {
    return option(wi.getCurrentOperation().getConfiguration(key));
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
   * Get a mandatory configuration key. Values are returned trimmed.
   *
   * @throws WorkflowOperationException
   *         if the configuration key is either missing or empty
   */
  protected String getConfig(WorkflowOperationInstance woi, String key) throws WorkflowOperationException {
    for (final String cfg : getOptConfig(woi, key)) {
      return cfg;
    }
    throw new WorkflowOperationException(format("Configuration key '%s' is either missing or empty", key));
  }

  /**
   * Get an optional configuration key. Values are returned trimmed.
   */
  protected Opt<String> getOptConfig(WorkflowInstance wi, String key) {
    return getOptConfig(wi.getCurrentOperation(), key);
  }

  /**
   * Get an optional configuration key. Values are returned trimmed.
   */
  protected Opt<String> getOptConfig(WorkflowOperationInstance woi, String key) {
    return Opt.nul(woi.getConfiguration(key)).flatMap(Strings.trimToNone);
  }

  /**
   * Create an error function.
   * <p/>
   * Example usage: <code>getCfg(wi, "key").getOrElse(this.&lt;String&gt;cfgKeyMissing("key"))</code>
   *
   * @see #getCfg(WorkflowInstance, String)
   * @deprecated see {@link #getCfg(WorkflowInstance, String)} for details
   */
  protected <A> Function0<A> cfgKeyMissing(final String key) {
    return new Function0<A>() {
      @Override public A apply() {
        return chuck(new WorkflowOperationException(key + " is missing or malformed"));
      }
    };
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
      if (id != null)
        return id.equals(((WorkflowOperationHandler) obj).getId());
      else
        return this == obj;
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
