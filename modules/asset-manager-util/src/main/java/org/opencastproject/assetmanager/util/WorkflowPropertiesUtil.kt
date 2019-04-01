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
package org.opencastproject.assetmanager.util

import org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER
import org.opencastproject.systems.OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Property
import org.opencastproject.assetmanager.api.PropertyId
import org.opencastproject.assetmanager.api.Value
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.ARecord
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.mediapackage.MediaPackage
import java.util.Collections
import java.util.HashMap

/**
 * Utility class to store and retrieve Workflow Properties (which are stored in specially prefixed Asset Manager
 * properties)
 */
object WorkflowPropertiesUtil {

    /**
     * Retrieve latest properties for a set of event ids
     * @param assetManager The Asset Manager to use
     * @param eventIds Collection of event IDs (can be a set, but doesn't have to be)
     * @return A map mapping event IDs to key value pairs (which are themselves maps) representing the properties
     */
    fun getLatestWorkflowPropertiesForEvents(assetManager: AssetManager,
                                             eventIds: Collection<String>): Map<String, Map<String, String>> {
        val query = assetManager.createQuery()
        val result = query.select(query.snapshot(), query.propertiesOf(WORKFLOW_PROPERTIES_NAMESPACE))
                .where(query.mediaPackageIds(*eventIds.toTypedArray()).and(query.version().isLatest)).run()
        val workflowProperties = HashMap<String, Map<String, String>>(eventIds.size)
        for (record in result.records.toList()) {
            val recordProps = record.properties.toList()
            val eventMap = HashMap<String, String>(recordProps.size)
            for (property in recordProps) {
                eventMap[property.id.name] = property.value[Value.STRING]
            }
            val eventId = record.mediaPackageId
            workflowProperties[eventId] = eventMap
        }
        return workflowProperties
    }

    /**
     * Retrieve the latest properties for a single media package
     * @param assetManager The Asset Manager to use
     * @param mediaPackageId The media package to query
     * @return A list of properties represented by a Map
     */
    fun getLatestWorkflowProperties(assetManager: AssetManager,
                                    mediaPackageId: String): Map<String, String> {
        val query = assetManager.createQuery()
        val queryResults = query.select(query.snapshot(), query.propertiesOf(WORKFLOW_PROPERTIES_NAMESPACE))
                .where(query.mediaPackageId(mediaPackageId).and(query.version().isLatest)).run()
                .records.toList()
        val workflowParameters = HashMap<String, String>(0)
        if (!queryResults.isEmpty()) {
            for (property in queryResults[0].properties) {
                workflowParameters[property.id.name] = property.value[Value.STRING]
            }
        }
        return workflowParameters
    }

    /**
     * Store selected properties for a media package
     * @param assetManager The Asset Manager to use
     * @param mediaPackage The media package to store properties relative to
     * @param properties A list of properties represented by a Map
     */
    fun storeProperties(assetManager: AssetManager, mediaPackage: MediaPackage,
                        properties: Map<String, String>) {

        // Properties can only be created if a snapshot exists. Hence, we create a snapshot if there is none right now.
        if (!assetManager.snapshotExists(mediaPackage.identifier.toString())) {
            assetManager.takeSnapshot(DEFAULT_OWNER, mediaPackage)
        }

        // Store all properties
        for ((key, value) in properties) {
            val propertyId = PropertyId
                    .mk(mediaPackage.identifier.compact(), WORKFLOW_PROPERTIES_NAMESPACE, key)
            val property = Property.mk(propertyId, Value.mk(value))
            assetManager.setProperty(property)
        }
    }

    fun storeProperty(assetManager: AssetManager, mediaPackage: MediaPackage,
                      name: String, value: String) {
        storeProperties(assetManager, mediaPackage, Collections.singletonMap(name, value))
    }
}
