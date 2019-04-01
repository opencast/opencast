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

package org.opencastproject.message.broker.api.workflow

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser
import org.opencastproject.workflow.api.WorkflowInstance

import org.xml.sax.SAXException

import java.io.IOException
import java.io.Serializable

import javax.xml.parsers.ParserConfigurationException

/**
 * [Serializable] class that represents all of the possible messages sent through a WorkflowService queue.
 */
class WorkflowItem : MessageItem, Serializable {

    override val id: String
    val workflowDefinitionId: String?
    val workflowInstanceId: Long
    private val episodeDublincoreCatalog: String?
    private val mediaPackage: String?
    private val state: String?
    val accessControlListJSON: String?

    val type: Type

    enum class Type {
        DeleteInstance, UpdateInstance
    }

    /**
     * Constructor to build an update workflow instance [WorkflowItem].
     *
     * @param workflowInstance
     * The workflow instance to update.
     */
    constructor(workflowInstance: WorkflowInstance, dublincoreXml: String, accessControlList: AccessControlList) {
        this.id = workflowInstance.mediaPackage.identifier.compact()
        this.workflowDefinitionId = workflowInstance.template
        this.workflowInstanceId = workflowInstance.id
        this.episodeDublincoreCatalog = dublincoreXml
        this.mediaPackage = MediaPackageParser.getAsXml(workflowInstance.mediaPackage)
        this.state = workflowInstance.state.toString()
        this.accessControlListJSON = AccessControlParser.toJsonSilent(accessControlList)
        this.type = Type.UpdateInstance
    }

    /**
     * Constructor to build a delete workflow [WorkflowItem].
     *
     * @param workflowInstanceId
     * The id of the workflow instance to delete.
     * @param workflowInstance
     * The workflow instance to update.
     */
    constructor(workflowInstanceId: Long, workflowInstance: WorkflowInstance) {
        // We just need the media package id and workflow id
        this.id = workflowInstance.mediaPackage.identifier.compact()
        this.workflowInstanceId = workflowInstanceId
        this.workflowDefinitionId = null
        this.episodeDublincoreCatalog = null
        this.mediaPackage = null
        this.state = null
        this.accessControlListJSON = null
        this.type = Type.DeleteInstance
    }

    fun getEpisodeDublincoreCatalog(): DublinCoreCatalog? {
        if (episodeDublincoreCatalog == null) {
            return null
        }
        try {
            return DublinCoreXmlFormat.read(episodeDublincoreCatalog)
        } catch (e: IOException) {
            throw IllegalStateException("Unable to parse dublincore catalog", e)
        } catch (e: ParserConfigurationException) {
            throw IllegalStateException("Unable to parse dublincore catalog", e)
        } catch (e: SAXException) {
            throw IllegalStateException("Unable to parse dublincore catalog", e)
        }

    }

    fun getMediaPackage(): MediaPackage? {
        if (mediaPackage == null) {
            return null
        }
        try {
            return MediaPackageParser.getFromXml(mediaPackage)
        } catch (e: MediaPackageException) {
            throw IllegalStateException("Could not parse media package XML", e)
        }

    }

    fun getState(): WorkflowInstance.WorkflowState {
        return WorkflowInstance.WorkflowState.valueOf(state)
    }

    companion object {

        private const val serialVersionUID = -202811055899495045L

        val WORKFLOW_QUEUE_PREFIX = "WORKFLOW."

        val WORKFLOW_QUEUE = WORKFLOW_QUEUE_PREFIX + "QUEUE"

        /**
         * @param workflowInstance
         * The workflow instance to update.
         * @param dublincoreXml
         * The episode dublincore catalog used for metadata updates
         * @return Builds [WorkflowItem] for updating a workflow instance.
         */
        fun updateInstance(workflowInstance: WorkflowInstance, dublincoreXml: String,
                           accessControlList: AccessControlList): WorkflowItem {
            return WorkflowItem(workflowInstance, dublincoreXml, accessControlList)
        }

        /**
         * @param workflowInstanceId
         * The unique id of the workflow instance to delete.
         * @param workflowInstance
         * The workflow instance to delete.
         * @return Builds [WorkflowItem] for deleting a workflow instance.
         */
        fun deleteInstance(workflowInstanceId: Long, workflowInstance: WorkflowInstance): WorkflowItem {
            return WorkflowItem(workflowInstanceId, workflowInstance)
        }
    }
}
