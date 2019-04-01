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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.workflow

import org.opencastproject.job.api.JobContext
import org.opencastproject.rest.RestConstants
import org.opencastproject.rest.StaticResource
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.io.FilenameUtils
import org.osgi.framework.ServiceRegistration
import org.osgi.service.component.ComponentContext

import java.util.Dictionary
import java.util.Hashtable

import javax.servlet.Servlet

/**
 * Abstract base implementation for a resumable operation handler, which implements a simple operations for starting an
 * operation, returning a [WorkflowOperationResult] with the current mediapackage and [Action.PAUSE] and
 * resuming an operation, returning a [WorkflowOperationResult] with the current mediapackage and
 * [Action.CONTINUE].
 */
open class ResumableWorkflowOperationHandlerBase : AbstractWorkflowOperationHandler(), ResumableWorkflowOperationHandler {

    /** OSGi component context, obtained during component activation  */
    protected var componentContext: ComponentContext? = null

    /** Reference to the static resource service  */
    protected var staticResourceRegistration: ServiceRegistration<*>? = null

    /** The static resource representing the hold state ui  */
    protected var staticResource: StaticResource? = null

    /** Title of this hold state  */
    protected var holdActionTitle: String? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.activate
     */
    public override fun activate(componentContext: ComponentContext) {
        this.componentContext = componentContext
        super.activate(componentContext)
    }

    /**
     * Callback from the OSGi environment on component shutdown. Make sure to call this super implementation when
     * overwriting this class.
     */
    fun deactivate() {
        if (staticResourceRegistration != null)
            staticResourceRegistration!!.unregister()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler.getHoldStateUserInterfaceURL
     */
    @Throws(WorkflowOperationException::class)
    override fun getHoldStateUserInterfaceURL(workflowInstance: WorkflowInstance): String? {
        return if (staticResource == null) null else staticResource!!.defaultUrl
    }

    /**
     * Sets the title that is displayed on the hold state ui.
     *
     * @param title
     * the title
     */
    protected fun setHoldActionTitle(title: String) {
        this.holdActionTitle = title
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler.getHoldActionTitle
     */
    override fun getHoldActionTitle(): String {
        return if (holdActionTitle == null) {
            DEFAULT_TITLE
        } else {
            holdActionTitle
        }
    }

    /**
     * Registers the resource identified by `resourcePath` as the ui to be displayed during hold.
     *
     * @param resourcePath
     * the path to the resource
     * @return the URL that was created when registering the resource
     */
    protected fun registerHoldStateUserInterface(resourcePath: String?): String? {
        val alias = "/workflow/hold/" + javaClass.name.toLowerCase()
        if (resourcePath == null)
            throw IllegalArgumentException("Classpath must not be null")
        val path = FilenameUtils.getPathNoEndSeparator(resourcePath)
        val welcomeFile = FilenameUtils.getName(resourcePath)
        staticResource = StaticResource(javaClass.classLoader, path, alias, welcomeFile)
        val props = Hashtable<String, String>()
        props["httpContext.id"] = RestConstants.HTTP_CONTEXT_ID
        props["alias"] = alias
        staticResourceRegistration = componentContext!!.bundleContext.registerService(Servlet::class.java.name,
                staticResource, props)
        return staticResource!!.defaultUrl
    }

    /**
     * {@inheritDoc}
     *
     * This default implementation will put the workflow into the hold state.
     *
     * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        return createResult(Action.PAUSE)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler.resume
     */
    @Throws(WorkflowOperationException::class)
    override fun resume(workflowInstance: WorkflowInstance, context: JobContext,
                        properties: Map<String, String>): WorkflowOperationResult {
        return createResult(Action.CONTINUE)
    }

    override fun isAlwaysPause(): Boolean {
        return false
    }

    companion object {

        private val DEFAULT_TITLE = "Action"
    }
}
