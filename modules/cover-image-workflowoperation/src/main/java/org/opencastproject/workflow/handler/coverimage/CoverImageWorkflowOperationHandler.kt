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

package org.opencastproject.workflow.handler.coverimage

import org.opencastproject.coverimage.CoverImageService
import org.opencastproject.metadata.api.StaticMetadataService
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.workspace.api.Workspace

/**
 * Implementation of [CoverImageWorkflowOperationHandlerBase] for usage in an OSGi context
 */
class CoverImageWorkflowOperationHandler : CoverImageWorkflowOperationHandlerBase() {

    /** The cover image service  */
    /**
     * OSGi callback to set the cover image service
     *
     * @param coverImageService
     * an instance of the cover image service
     */
    protected override var coverImageService: CoverImageService? = null

    /** The workspace service  */
    /**
     * OSGi callback to set the workspace service
     *
     * @param workspace
     * an instance of the workspace service
     */
    protected override var workspace: Workspace? = null

    /** Reference to the static metadata service  */
    /**
     * OSGi callback to set the static metadata service
     *
     * @param srv
     * an instance of the static metadata service
     */
    protected override var staticMetadataService: StaticMetadataService? = null

    /** The dublin core catalog service  */
    /**
     * OSGi callback to set the dublin core catalog service
     *
     * @param dcService
     * an instance of the dublin core catalog service
     */
    protected override var dublinCoreCatalogService: DublinCoreCatalogService? = null
}
