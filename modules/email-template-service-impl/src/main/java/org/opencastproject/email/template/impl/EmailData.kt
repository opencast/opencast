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
package org.opencastproject.email.template.impl

import org.opencastproject.job.api.Incident
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.util.doc.DocData
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance

import java.util.HashMap
import java.util.LinkedHashMap

/**
 * Holds data to be displayed in an email message. The following data will be available: mediaPackage, workflow,
 * workflowConfig: workflow configuration as key-value pairs, catalogs: hash of catalogs whose key is the catalog flavor
 * sub-type e.g. "series", "episode", failedOperation: the last operation marked as "failOnError" that failed.
 *
 */
class EmailData
/**
 * Create the base data object for populating email fields.
 *
 * @param name
 * a name for this object
 * @param workflow
 * workflow instance
 * @param catalogs
 * hash map of media package catalogs
 * @param failed
 * workflow operation that caused the workflow to fail
 * @param incidents
 * incidents
 */
(name: String, private val workflow: WorkflowInstance, // The hash map below looks like this:
        // "episode" --> { "creator": "John Harvard", "type": "L05", "isPartOf": "20140224038"... }
        // "series" --> { "creator": "Harvard", "identifier": "20140224038"... }
 private val catalogs: HashMap<String, HashMap<String, String>>,
 private val failed: WorkflowOperationInstance, private val incidents: List<Incident>) : DocData(name, null, null) {
    private val workflowConfig: MutableMap<String, String>
    private val mediaPackage: MediaPackage

    init {
        this.workflowConfig = HashMap()
        for (key in workflow.configurationKeys) {
            workflowConfig[key] = workflow.getConfiguration(key)
        }
        this.mediaPackage = workflow.mediaPackage
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    override fun toString(): String {
        return ("EmailDOC:name=" + meta["name"] + ", notes=" + notes + ", workflow id=" + workflow.id
                + ", media package id=" + mediaPackage.identifier)
    }

    @Throws(IllegalStateException::class)
    override fun toMap(): Map<String, Any> {
        val m = LinkedHashMap<String, Any>()
        m["meta"] = meta
        m["notes"] = notes
        m["mediaPackage"] = mediaPackage
        m["workflow"] = workflow
        m["workflowConfig"] = workflowConfig
        m["catalogs"] = catalogs
        m["failedOperation"] = failed // Will be null if no errors
        m["incident"] = incidents // Will be null if no incidents
        return m
    }

}
