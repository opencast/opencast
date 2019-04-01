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
package org.opencastproject.assetmanager.storage.impl.fs

import org.opencastproject.util.IoSupport.file

import org.opencastproject.assetmanager.impl.storage.AssetStore
import org.opencastproject.util.PathSupport
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException

class OsgiFileSystemAssetStore : AbstractFileSystemAssetStore() {

    /** The root directory for storing files  */
    protected override var rootDirectory: String? = null
        private set

    /** The workspace  */
    /**
     * OSGi DI.
     */
    protected override var workspace: Workspace? = null
        set

    /**
     * Service activator, called via declarative services configuration.
     *
     * @param cc
     * the component context
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun activate(cc: ComponentContext) {
        storeType = cc.properties.get(AssetStore.STORE_TYPE_PROPERTY) as String
        logger.info("{} is: {}", AssetStore.STORE_TYPE_PROPERTY, storeType)

        rootDirectory = StringUtils.trimToNull(cc.bundleContext.getProperty(CONFIG_STORE_ROOT_DIR))
        if (rootDirectory == null) {
            val storageDir = StringUtils.trimToNull(cc.bundleContext.getProperty(CFG_OPT_STORAGE_DIR))
                    ?: throw IllegalArgumentException("Storage directory must be set")
            rootDirectory = PathSupport.concat(storageDir, DEFAULT_STORE_DIRECTORY)
        }
        mkDirs(file(rootDirectory))
        logger.info("Start asset manager files system store at " + rootDirectory!!)
    }

    companion object {
        /** Log facility  */
        private val logger = LoggerFactory.getLogger(OsgiFileSystemAssetStore::class.java)

        /** Configuration key for the default Opencast storage directory. A value is optional.  */
        val CFG_OPT_STORAGE_DIR = "org.opencastproject.storage.dir"

        /**
         * The default store directory name.
         * Will be used in conjunction with [.CFG_OPT_STORAGE_DIR] if [.CFG_OPT_STORAGE_DIR] is not set.
         */
        private val DEFAULT_STORE_DIRECTORY = "archive"

        /** Configuration key for the archive root directory.  */
        val CONFIG_STORE_ROOT_DIR = "org.opencastproject.episode.rootdir"
    }
}
