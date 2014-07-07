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

import org.opencastproject.job.api.JobContext;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.StaticResource;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;

/**
 * Abstract base implementation for a resumable operation handler, which implements a simple operations for starting an
 * operation, returning a {@link WorkflowOperationResult} with the current mediapackage and {@link Action#PAUSE} and
 * resuming an operation, returning a {@link WorkflowOperationResult} with the current mediapackage and
 * {@link Action#CONTINUE}.
 */
public class ResumableWorkflowOperationHandlerBase extends AbstractWorkflowOperationHandler implements
        ResumableWorkflowOperationHandler {

  /** OSGi component context, obtained during component activation */
  protected ComponentContext componentContext = null;

  /** Reference to the static resource service */
  protected ServiceRegistration staticResourceRegistration = null;

  /** The static resource representing the hold state ui */
  protected StaticResource staticResource = null;

  /** Title of this hold state */
  protected String holdActionTitle = null;

  private static final String DEFAULT_TITLE = "Action";

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext)
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    super.activate(componentContext);
  }

  /**
   * Callback from the OSGi environment on component shutdown. Make sure to call this super implementation when
   * overwriting this class.
   */
  public void deactivate() {
    if (staticResourceRegistration != null)
      staticResourceRegistration.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#getHoldStateUserInterfaceURL(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public String getHoldStateUserInterfaceURL(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    if (staticResource == null)
      return null;
    return staticResource.getDefaultUrl();
  }

  /**
   * Sets the title that is displayed on the hold state ui.
   *
   * @param title
   *          the title
   */
  protected void setHoldActionTitle(String title) {
    this.holdActionTitle = title;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#getHoldActionTitle()
   */
  public String getHoldActionTitle() {
    if (holdActionTitle == null) {
      return DEFAULT_TITLE;
    } else {
      return holdActionTitle;
    }
  }

  /**
   * Registers the resource identified by <code>resourcePath</code> as the ui to be displayed during hold.
   *
   * @param resourcePath
   *          the path to the resource
   * @return the URL that was created when registering the resource
   */
  protected String registerHoldStateUserInterface(final String resourcePath) {
    String alias = "/workflow/hold/" + getClass().getName().toLowerCase();
    if (resourcePath == null)
      throw new IllegalArgumentException("Classpath must not be null");
    String path = FilenameUtils.getPathNoEndSeparator(resourcePath);
    String welcomeFile = FilenameUtils.getName(resourcePath);
    staticResource = new StaticResource(getClass().getClassLoader(), path, alias, welcomeFile);
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("contextId", RestConstants.HTTP_CONTEXT_ID);
    props.put("alias", alias);
    staticResourceRegistration = componentContext.getBundleContext().registerService(Servlet.class.getName(),
            staticResource, props);
    return staticResource.getDefaultUrl();
  }

  /**
   * {@inheritDoc}
   *
   * This default implementation will put the workflow into the hold state.
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    return createResult(Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
          Map<String, String> properties) throws WorkflowOperationException {
    return createResult(Action.CONTINUE);
  }

  @Override
  public boolean isAlwaysPause() {
    return false;
  }
}
