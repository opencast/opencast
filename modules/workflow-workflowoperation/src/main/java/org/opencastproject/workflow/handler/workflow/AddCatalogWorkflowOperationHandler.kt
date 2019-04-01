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
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.util.MimeType
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Arrays
import java.util.UUID

/**
 * This WorkflowOperationHandler adds an configurable catalog to the MediaPackage.
 * It supports the following workflow configuration keys:
 * catalog-path the path of the catalog to add;
 * catalog-flavor the flavor of the catalog, used to identify catalogs of the same type;
 * catalog-name name of the catalog in the workspace;
 * catalog-tags list of comma seperated catalog tags;
 * catalog-type-collision-behavior the action to perform, if an catalog of the same flavor already exists,
 * three options are supported: 'skip' the adding of the catalog, 'fail' the workflow operation or 'keep'
 * the new catalog, resulting in two or more catalogs of the same type coexisting
 */
class AddCatalogWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The workspace, where the catalog files are put.  */
    private var workspace: Workspace? = null

    /** enum used to specify the behavior on detecting a catalog type collision  */
    private enum class CatalogTypeCollisionBehavior {
        SKIP, KEEP, FAIL
    }

    /**
     * Sets the workspace to use.
     *
     * @param workspace
     * the workspace
     */
    fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    @Throws(WorkflowOperationException::class)
    override fun start(wInst: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        // get Workflow configuration
        val catalogName = getConfig(wInst, CFG_KEY_CATALOG_NAME)
        val catalogPath = getConfig(wInst, CFG_KEY_CATALOG_PATH)
        val catalogTags = getConfig(wInst, CFG_KEY_CATALOG_TAGS, "")
        val collBehavior = parseCollisionBehavior(
                getConfig(wInst, CFG_KEY_CATALOG_TYPE_COLLISION_BEHAVIOR))
        var catalogFlavor: MediaPackageElementFlavor? = null
        try {
            catalogFlavor = MediaPackageElementFlavor.parseFlavor(
                    getConfig(wInst, CFG_KEY_CATALOG_FLAVOR))
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException("Unknown flavor")
        }

        val mp = wInst.mediaPackage

        // if CatalogType is already part of the MediaPackage handle special cases
        if (doesCatalogFlavorExist(catalogFlavor, mp.catalogs)) {
            if (collBehavior == CatalogTypeCollisionBehavior.FAIL) {
                throw WorkflowOperationException("Catalog Type already exists and 'fail' was specified")
            } else if (collBehavior == CatalogTypeCollisionBehavior.SKIP) {
                // don't add the Catalog
                return createResult(mp, Action.CONTINUE)
            }
        }

        // 'upload Catalog' to workspace
        val catalogFile = File(catalogPath)
        val catalogId = UUID.randomUUID().toString()
        var catalogURI: URI? = null
        try {
            FileUtils.openInputStream(catalogFile).use { catalogInputStream ->
                catalogURI = workspace!!.put(mp.identifier.toString(), catalogId,
                        catalogName, catalogInputStream)
            }
        } catch (e: IOException) {
            throw WorkflowOperationException(e)
        }

        // add Catalog to MediaPackage (and set Properties)
        val mpe = mp.add(catalogURI!!, MediaPackageElement.Type.Catalog, catalogFlavor!!)
        mpe.identifier = catalogId
        mpe.mimeType = CATALOG_MIME_TYPE
        for (tag in asList(catalogTags)) {
            mpe.addTag(tag)
        }

        return createResult(mp, Action.CONTINUE)
    }

    /**
     * Checks whether the catalogFlavor exists in the array of catalogs
     *
     * @param catalogFlavor
     * @param catalogs
     * @return true, if the catalogFlavor exists in the array of catalogs, else false
     */
    private fun doesCatalogFlavorExist(catalogFlavor: MediaPackageElementFlavor?, catalogs: Array<Catalog>): Boolean {
        return Arrays.asList(*catalogs).stream()
                .anyMatch { cat -> catalogFlavor!!.matches(cat.flavor) }
    }

    /**
     * Parses the rawBehavior String into an CatalogTypeCollisionBehavior.
     * Throws an WorkflowOperationException if the String couldn't be parsed.
     *
     * @param rawBehavior
     * @return
     * @throws WorkflowOperationException
     */
    @Throws(WorkflowOperationException::class)
    private fun parseCollisionBehavior(rawBehavior: String): CatalogTypeCollisionBehavior {
        try {
            return CatalogTypeCollisionBehavior.valueOf(rawBehavior.toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw WorkflowOperationException("Workflowoperation configured incorrectly, the configuration '"
                    + CFG_KEY_CATALOG_TYPE_COLLISION_BEHAVIOR
                    + "' only accepts 'skip', 'keep', 'fail'")
        }

    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory
                .getLogger(AddCatalogWorkflowOperationHandler::class.java)

        /** config key for the catalog name  */
        private val CFG_KEY_CATALOG_NAME = "catalog-name"
        /** config key for the catalog flavor  */
        private val CFG_KEY_CATALOG_FLAVOR = "catalog-flavor"
        /** config key which locates the catalog on the filesystem  */
        private val CFG_KEY_CATALOG_PATH = "catalog-path"
        /** config key for the catalog tags  */
        private val CFG_KEY_CATALOG_TAGS = "catalog-tags"
        /** config key which defines the behavior if a catalog of the same flavor already exists  */
        private val CFG_KEY_CATALOG_TYPE_COLLISION_BEHAVIOR = "catalog-type-collision-behavior"

        /** The mimetype of the added catalogs  */
        private val CATALOG_MIME_TYPE = MimeType.mimeType("text", "xml")
    }
}
