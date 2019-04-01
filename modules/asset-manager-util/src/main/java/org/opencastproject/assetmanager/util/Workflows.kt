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

import com.entwinemedia.fn.Stream.`$`
import org.opencastproject.assetmanager.api.fn.Enrichments.enrich

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.ASelectQuery
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.workflow.api.ConfiguredWorkflow
import org.opencastproject.workflow.api.WorkflowDatabaseException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowParsingException
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Utility class to apply workflows to episodes. Removed 'final class' so that we can mock it for
 * unit tests.
 */
class Workflows(private val am: AssetManager, private val ws: Workspace, private val wfs: WorkflowService) {

    // CHECKSTYLE:OFF
    private val getMediapackage = object : Fn<Snapshot, MediaPackage>() {
        override fun apply(snapshot: Snapshot): MediaPackage {
            return snapshot.mediaPackage
        }
    }
    // CHECKSTYLE:ON

    private val findLatest = object : Fn<String, Iterable<Snapshot>>() {
        override fun apply(mpId: String): Iterable<Snapshot> {
            val q = am.createQuery()
            return enrich(q.select(q.snapshot()).where(q.mediaPackageId(mpId).and(q.version().isLatest)).run()).getSnapshots()
        }
    }

    /**
     * Apply a workflow to each episode contained in the result set of a select query.
     */
    fun applyWorkflow(q: ASelectQuery, wf: ConfiguredWorkflow): Stream<WorkflowInstance> {
        return enrich(q.run()).getSnapshots().map(getMediapackage).bind(applyWorkflow(wf))
    }

    /**
     * Apply a workflow to the latest version of each media package.
     */
    fun applyWorkflowToLatestVersion(mpIds: Iterable<String>, wf: ConfiguredWorkflow): Stream<WorkflowInstance> {
        return `$`(mpIds).bind(findLatest).map(getMediapackage).bind(applyWorkflow(wf))
    }

    /**
     * Apply a workflow to a media package. The function returns some workflow instance if the
     * workflow could be started successfully, none otherwise.
     */
    fun applyWorkflow(wf: ConfiguredWorkflow): Fn<MediaPackage, Opt<WorkflowInstance>> {
        return object : Fn<MediaPackage, Opt<WorkflowInstance>>() {
            override fun apply(mp: MediaPackage): Opt<WorkflowInstance> {
                try {
                    return Opt.some(wfs.start(wf.workflowDefinition, mp, wf.parameters))
                } catch (e: WorkflowDatabaseException) {
                    logger.error("Cannot start workflow on media package " + mp.identifier.toString(), e)
                    return Opt.none()
                } catch (e: WorkflowParsingException) {
                    logger.error("Cannot start workflow on media package " + mp.identifier.toString(), e)
                    return Opt.none()
                }

            }
        }
    }

    companion object {
        /** Log facility  */
        private val logger = LoggerFactory.getLogger(Workflows::class.java)

        private val ASSETS_COLLECTION_ID = "assets"
    }
}
