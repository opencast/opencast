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
package org.opencastproject.workflow.handler.assetmanager

import org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageReference
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.selector.SimpleElementSelector
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays

/**
 * Workflow operation for taking a snapshot of a media package.
 *
 * @see AssetManager.takeSnapshot
 */
class AssetManagerSnapshotWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The asset manager.  */
    private var assetManager: AssetManager? = null

    /** OSGi DI  */
    fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    @Throws(WorkflowOperationException::class)
    override fun start(wi: WorkflowInstance, ctx: JobContext): WorkflowOperationResult {
        val mpWorkflow = wi.mediaPackage
        val currentOperation = wi.currentOperation

        // Check which tags have been configured
        val tags = StringUtils.trimToNull(currentOperation.getConfiguration("source-tags"))
        val sourceFlavorsString = StringUtils.trimToEmpty(currentOperation.getConfiguration("source-flavors"))

        val sourceFlavors = StringUtils.split(sourceFlavorsString, ",")
        if (sourceFlavors.size < 1 && tags == null)
            logger.debug("No source tags have been specified, so everything will be added to the AssetManager")

        val tagSet: List<String>
        // If a set of tags has been specified, use it
        if (tags != null) {
            tagSet = asList(tags)
        } else {
            tagSet = ArrayList()
        }

        try {
            val mpAssetManager = getMediaPackageForArchival(mpWorkflow, tagSet, sourceFlavors)
            if (mpAssetManager != null) {
                logger.info("Take snapshot of media package {}", mpAssetManager)
                // adding media package to the episode service
                assetManager!!.takeSnapshot(DEFAULT_OWNER, mpAssetManager)
                logger.debug("Snapshot operation complete")
                return createResult(mpWorkflow, Action.CONTINUE)
            } else {
                return createResult(mpWorkflow, Action.CONTINUE)
            }
        } catch (t: Throwable) {
            throw WorkflowOperationException(t)
        }

    }

    @Throws(MediaPackageException::class)
    protected fun getMediaPackageForArchival(current: MediaPackage, tags: List<String>, sourceFlavors: Array<String>): MediaPackage {
        val mp = current.clone() as MediaPackage

        val keep: MutableCollection<MediaPackageElement>

        if (tags.isEmpty() && sourceFlavors.size < 1) {
            keep = ArrayList(Arrays.asList(*current.getElementsByTags(tags)))
        } else {
            val simpleElementSelector = SimpleElementSelector()
            for (flavor in sourceFlavors) {
                simpleElementSelector.addFlavor(flavor)
            }
            for (tag in tags) {
                simpleElementSelector.addTag(tag)
            }
            keep = simpleElementSelector.select(current, false)
        }

        // Also archive the publication elements
        for (publication in current.publications) {
            keep.add(publication)
        }

        // Mark everything that is set for removal
        val removals = ArrayList<MediaPackageElement>()
        for (element in mp.elements) {
            if (!keep.contains(element)) {
                removals.add(element)
            }
        }

        // Fix references and flavors
        for (element in mp.elements) {

            if (removals.contains(element))
                continue

            // Is the element referencing anything?
            var reference: MediaPackageReference? = element.reference
            if (reference != null) {
                val referenceProperties = reference.properties
                val referencedElement = mp.getElementByReference(reference)

                // if we are distributing the referenced element, everything is fine. Otherwise...
                if (referencedElement != null && removals.contains(referencedElement)) {

                    // Follow the references until we find a flavor
                    var parent: MediaPackageElement
                    while ((parent = current.getElementByReference(reference!!)) != null) {
                        if (parent.flavor != null && element.flavor == null) {
                            element.flavor = parent.flavor
                        }
                        if (parent.reference == null) {
                            break
                        }
                        reference = parent.reference
                    }

                    // Done. Let's cut the path but keep references to the mediapackage itself
                    if (reference != null && reference.type == MediaPackageReference.TYPE_MEDIAPACKAGE)
                        element.reference = reference
                    else if (reference != null && (referenceProperties == null || referenceProperties.size == 0))
                        element.clearReference()
                    else {
                        // Ok, there is more to that reference than just pointing at an element. Let's keep the original,
                        // you never know.
                        removals.remove(referencedElement)
                        referencedElement.setURI(null)
                        referencedElement.checksum = null
                    }
                }
            }
        }

        // Remove everything we don't want to add to publish
        for (element in removals) {
            mp.remove(element)
        }
        return mp
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetManagerSnapshotWorkflowOperationHandler::class.java)
    }
}
