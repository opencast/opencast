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

import com.entwinemedia.fn.data.Opt.none
import com.entwinemedia.fn.data.Opt.nul
import com.entwinemedia.fn.data.Opt.some
import org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR
import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.commons.lang3.exception.ExceptionUtils.getMessage
import org.opencastproject.util.FileSupport.link
import org.opencastproject.util.IoSupport.file
import org.opencastproject.util.PathSupport.path
import org.opencastproject.util.data.functions.Strings.trimToNone

import org.opencastproject.assetmanager.api.Version
import org.opencastproject.assetmanager.impl.storage.AssetStore
import org.opencastproject.assetmanager.impl.storage.AssetStoreException
import org.opencastproject.assetmanager.impl.storage.DeletionSelector
import org.opencastproject.assetmanager.impl.storage.Source
import org.opencastproject.assetmanager.impl.storage.StoragePath
import org.opencastproject.util.FileSupport
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Option
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI

abstract class AbstractFileSystemAssetStore : AssetStore {

    /** The store type e.g. filesystem (short-term), aws (long-term), other implementations  */
    protected var storeType: String? = null

    protected abstract val workspace: Workspace

    protected abstract val rootDirectory: String

    override val usedSpace: Option<Long>
        get() = Option.some(FileUtils.sizeOfDirectory(File(rootDirectory)))

    override val usableSpace: Option<Long>
        get() = Option.some(File(rootDirectory).usableSpace)

    override val totalSpace: Option<Long>
        get() = Option.some(File(rootDirectory).totalSpace)

    @Throws(AssetStoreException::class)
    override fun put(storagePath: StoragePath, source: Source) {
        // Retrieving the file from the workspace has the advantage that in most cases the file already exists in the local
        // working file repository. In the very few cases where the file is not in the working file repository,
        // this strategy leads to a minor overhead because the file not only gets downloaded and stored in the file system
        // but also a hard link needs to be created (or if that's not possible, a copy of the file.
        val origin = getUniqueFileFromWorkspace(source)
        val destination = createFile(storagePath, source)
        try {
            mkParent(destination)
            link(origin, destination)
        } catch (e: IOException) {
            logger.error("Error while linking/copying file {} to {}: {}", origin, destination, getMessage(e))
            throw AssetStoreException(e)
        } finally {
            if (origin != null) {
                FileUtils.deleteQuietly(origin)
            }
        }
    }

    private fun getUniqueFileFromWorkspace(source: Source): File? {
        try {
            return workspace.get(source.uri, true)
        } catch (e: NotFoundException) {
            logger.error("Source file '{}' does not exist", source.uri)
            throw AssetStoreException(e)
        } catch (e: IOException) {
            logger.error("Error while getting file '{}' from workspace: {}", source.uri, getMessage(e))
            throw AssetStoreException(e)
        }

    }

    @Throws(AssetStoreException::class)
    override fun copy(from: StoragePath, to: StoragePath): Boolean {
        return findStoragePathFile(from).map(object : Fn<File, Boolean>() {
            override fun apply(f: File): Boolean? {
                val t = createFile(to, f)
                mkParent(t)
                logger.debug("Copying {} to {}", f.absolutePath, t.absolutePath)
                try {
                    link(f, t, true)
                } catch (e: IOException) {
                    logger.error("Error copying archive file {} to {}", f, t)
                    throw AssetStoreException(e)
                }

                return true
            }
        }).getOr(false)
    }

    @Throws(AssetStoreException::class)
    override fun get(path: StoragePath): Opt<InputStream> {
        return findStoragePathFile(path).map(object : Fn<File, InputStream>() {
            override fun apply(file: File): InputStream {
                try {
                    return FileInputStream(file)
                } catch (e: FileNotFoundException) {
                    logger.error("Error getting archive file {}", file)
                    throw AssetStoreException(e)
                }

            }
        })
    }

    @Throws(AssetStoreException::class)
    override fun contains(path: StoragePath): Boolean {
        return findStoragePathFile(path).isSome
    }

    @Throws(AssetStoreException::class)
    override fun delete(sel: DeletionSelector): Boolean {
        val dir = getDeletionSelectorDir(sel)
        try {
            FileUtils.deleteDirectory(dir)
            // also delete the media package directory if all versions have been deleted
            FileSupport.deleteHierarchyIfEmpty(file(path(rootDirectory, sel.organizationId)), dir.parentFile)
            return true
        } catch (e: IOException) {
            logger.error("Error deleting directory from archive {}", dir)
            throw AssetStoreException(e)
        }

    }

    /**
     * Returns the directory file from a deletion selector
     *
     * @param sel
     * the deletion selector
     * @return the directory file
     */
    private fun getDeletionSelectorDir(sel: DeletionSelector): File {
        val basePath = path(rootDirectory, sel.organizationId, sel.mediaPackageId)
        for (v in sel.version)
            return file(basePath, v.toString())
        return file(basePath)
    }

    /** Create all parent directories of a file.  */
    private fun mkParent(f: File) {
        mkDirs(f.parentFile)
    }

    /** Create this directory and all of its parents.  */
    protected fun mkDirs(d: File?) {
        if (d != null && !d.exists() && !d.mkdirs()) {
            val msg = "Cannot create directory $d"
            logger.error(msg)
            throw AssetStoreException(msg)
        }
    }

    /** Return the extension of a file.  */
    private fun extension(f: File): Opt<String> {
        return trimToNone(getExtension(f.absolutePath)).toOpt()
    }

    /** Return the extension of a URI, i.e. the extension of its path.  */
    private fun extension(uri: URI): Opt<String> {
        try {
            return trimToNone(getExtension(uri.toURL().path)).toOpt()
        } catch (e: MalformedURLException) {
            throw Error(e)
        }

    }

    /** Create a file from a storage path and the extension of file `f`.  */
    private fun createFile(p: StoragePath, f: File): File {
        return createFile(p, extension(f))
    }

    /** Create a file from a storage path and the extension of the URI of `s`.  */
    private fun createFile(p: StoragePath, s: Source): File {
        return createFile(p, extension(s.uri))
    }

    /** Create a file from a storage path and an optional extension.  */
    private fun createFile(p: StoragePath, extension: Opt<String>): File {
        return file(
                rootDirectory,
                p.organizationId,
                p.mediaPackageId,
                p.version.toString(),
                if (extension.isSome)
                    p.mediaPackageElementId + EXTENSION_SEPARATOR + extension.get()
                else
                    p
                            .mediaPackageElementId)
    }

    /**
     * Returns a file [Option] from a storage path if one is found or an empty [Option]
     *
     * @param storagePath
     * the storage path
     * @return the file [Option]
     */
    private fun findStoragePathFile(storagePath: StoragePath): Opt<File> {
        val filter = FilenameFilter { dir, name -> FilenameUtils.getBaseName(name) == storagePath.mediaPackageElementId }
        val containerDir = createFile(storagePath, Opt.none(String::class.java)).parentFile
        return nul(containerDir.listFiles(filter)).bind(object : Fn<Array<File>, Opt<File>>() {
            override fun apply(files: Array<File>): Opt<File> {
                when (files.size) {
                    0 -> return none()
                    1 -> return some(files[0])
                    else -> throw AssetStoreException("Storage path " + files[0].parent
                            + "contains multiple files with the same element id!: " + storagePath.mediaPackageElementId)
                }
            }
        })
    }

    override fun getStoreType(): String? {
        return storeType
    }

    companion object {
        /** Log facility  */
        private val logger = LoggerFactory.getLogger(AbstractFileSystemAssetStore::class.java)
    }

}
