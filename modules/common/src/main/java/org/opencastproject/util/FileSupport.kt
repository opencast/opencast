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

package org.opencastproject.util

import java.lang.String.format
import java.nio.file.Files.createLink
import java.nio.file.Files.deleteIfExists
import java.nio.file.Files.exists
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.Objects.requireNonNull

import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

/** Utility class, dealing with files.  */
object FileSupport {

    /** Only files will be deleted, the directory structure remains untouched.  */
    val DELETE_FILES = 0

    /** Delete everything including the root directory.  */
    val DELETE_ROOT = 1

    /** Delete all content but keep the root directory.  */
    val DELETE_CONTENT = 2

    /** Name of the java environment variable for the temp directory  */
    private val IO_TMPDIR = "java.io.tmpdir"

    /** Work directory  */
    private var tmpDir: File? = null

    /** Logging facility provided by log4j  */
    private val logger = LoggerFactory.getLogger(FileSupport::class.java!!)

    /**
     * Returns the webapp's temporary work directory.
     *
     * @return the temp directory
     */
    /**
     * Sets the webapp's temporary directory. Make sure that directory exists and has write permissions turned on.
     *
     * @param tmpDir
     * the new temporary directory
     * @throws IllegalArgumentException
     * if the file object doesn't represent a directory
     * @throws IllegalStateException
     * if the directory is write protected
     */
    var tempDirectory: File?
        get() {
            if (tmpDir == null) {
                tempDirectory = File(System.getProperty(IO_TMPDIR))
            }
            return tmpDir
        }
        @Throws(IllegalArgumentException::class, IllegalStateException::class)
        set(tmpDir) {
            if (tmpDir == null || !tmpDir.isDirectory)
                throw IllegalArgumentException(tmpDir!!.toString() + " is not a directory")
            if (!tmpDir.canWrite())
                throw IllegalStateException("$tmpDir is not writable")
            FileSupport.tmpDir = tmpDir
        }

    /**
     * Moves the specified file or directory from `sourceLocation` to `targetDirectory`. If
     * `targetDirectory` does not exist, it will be created.
     *
     * @param sourceLocation
     * the source file or directory
     * @param targetDirectory
     * the target directory
     * @return the moved file
     */
    @Throws(IOException::class)
    fun move(sourceLocation: File, targetDirectory: File): File {
        if (!targetDirectory.isDirectory)
            throw IllegalArgumentException("Target location must be a directory")

        if (!targetDirectory.exists())
            targetDirectory.mkdirs()

        val targetFile = File(targetDirectory, sourceLocation.name)
        if (sourceLocation.renameTo(targetFile))
            return targetFile

        // Rename doesn't work, so use copy / delete instead
        copy(sourceLocation, targetDirectory)
        delete(sourceLocation, true)
        return targetFile
    }

    /**
     * Copies the specified file from `sourceLocation` to `targetLocation` and returns a reference
     * to the newly created file or directory.
     *
     *
     * If `targetLocation` is an existing directory, then the source file or directory will be copied into this
     * directory, otherwise the source file will be copied to the file identified by `targetLocation`.
     *
     *
     * Note that existing files and directories will be overwritten.
     *
     *
     * Also note that if `targetLocation` is a directory than the directory itself, not only its content is
     * copied.
     *
     * @param sourceLocation
     * the source file or directory
     * @param targetLocation
     * the directory to copy the source file or directory to
     * @return the created copy
     * @throws IOException
     * if copying of the file or directory failed
     */
    @Throws(IOException::class)
    fun copy(sourceLocation: File, targetLocation: File): File {
        return copy(sourceLocation, targetLocation, true)
    }

    /**
     * Copies the specified `sourceLocation` to `targetLocation` and returns a reference to the
     * newly created file or directory.
     *
     *
     * If `targetLocation` is an existing directory, then the source file or directory will be copied into this
     * directory, otherwise the source file will be copied to the file identified by `targetLocation`.
     *
     *
     * If `overwrite` is set to `false`, this method throws an [IOException] if the target
     * file already exists.
     *
     *
     * Note that if `targetLocation` is a directory than the directory itself, not only its content is copied.
     *
     * @param sourceFile
     * the source file or directory
     * @param targetFile
     * the directory to copy the source file or directory to
     * @param overwrite
     * `true` to overwrite existing files
     * @return the created copy
     * @throws IOException
     * if copying of the file or directory failed
     */
    @Throws(IOException::class)
    fun copy(sourceFile: File, targetFile: File, overwrite: Boolean): File {

        // This variable is used when the channel copy files, and stores the maximum size of the file parts copied from
        // source to target
        val chunk = 1024 * 1024 * 512 // 512 MB

        // This variable is used when the cannel copy fails completely, as the size of the memory buffer used to copy the
        // data from one stream to the other.
        val bufferSize = 1024 * 1024 // 1 MB

        val dest = determineDestination(targetFile, sourceFile, overwrite)

        // We are copying a directory
        if (sourceFile.isDirectory) {
            if (!dest.exists()) {
                dest.mkdirs()
            }
            val children = sourceFile.listFiles()
            for (child in children!!) {
                copy(child, dest, overwrite)
            }
        } else {
            // If dest is not an "absolute file", getParentFile may return null, even if there *is* a parent file.
            // That's why "getAbsoluteFile" is used here
            dest.absoluteFile.parentFile.mkdirs()
            if (dest.exists())
                delete(dest)

            var sourceChannel: FileChannel? = null
            var targetChannel: FileChannel? = null
            var sourceStream: FileInputStream? = null
            var targetStream: FileOutputStream? = null
            var size: Long = 0

            try {
                sourceStream = FileInputStream(sourceFile)
                targetStream = FileOutputStream(dest)
                try {
                    sourceChannel = sourceStream.channel
                    targetChannel = targetStream.channel
                    size = targetChannel!!.transferFrom(sourceChannel, 0, sourceChannel!!.size())
                } catch (ioe: IOException) {
                    logger.warn("Got IOException using Channels for copying.")
                } finally {
                    // This has to be in "finally", because in 64-bit machines the channel copy may fail to copy the whole file
                    // without causing a exception
                    if (sourceChannel != null && targetChannel != null && size < sourceFile.length()) {
                        // Failing back to using FileChannels *but* with chunks and not altogether
                        logger.info("Trying to copy the file in chunks using Channels")
                        while (size < sourceFile.length())
                            size += targetChannel.transferFrom(sourceChannel, size, chunk.toLong())
                    }
                }
            } catch (ioe: IOException) {
                if (sourceStream != null && targetStream != null && size < sourceFile.length()) {
                    logger.warn("Got IOException using Channels for copying in chunks. Trying to use stream copy instead...")
                    var copied = 0
                    val buffer = ByteArray(bufferSize)
                    while ((copied = sourceStream.read(buffer, 0, buffer.size)) != -1)
                        targetStream.write(buffer, 0, copied)
                } else
                    throw ioe
            } finally {
                sourceChannel?.close()
                sourceStream?.close()
                targetChannel?.close()
                targetStream?.close()
            }

            if (sourceFile.length() != dest.length()) {
                logger.warn("Source $sourceFile and target $dest do not have the same length")
                // TOOD: Why would this happen?
                // throw new IOException("Source " + sourceLocation + " and target " +
                // dest + " do not have the same length");
            }
        }// We are copying a file
        return dest
    }

    /**
     * Copies recursively the *content* of the specified `sourceDirectory` to
     * `targetDirectory`.
     *
     *
     * If `overwrite` is set to `false`, this method throws an [IOException] if the target
     * file already exists.
     *
     * @param sourceDirectory
     * the source directory
     * @param targetDirectory
     * the target directory to copy the content of the source directory to
     * @param overwrite
     * `true` to overwrite existing files
     * @throws IOException
     * if copying fails
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun copyContent(sourceDirectory: File?, targetDirectory: File?, overwrite: Boolean = false) {
        if (sourceDirectory == null)
            throw IllegalArgumentException("Source directory must not by null")
        if (!sourceDirectory.isDirectory)
            throw IllegalArgumentException(sourceDirectory.absolutePath + " is not a directory")
        if (targetDirectory == null)
            throw IllegalArgumentException("Target directory must not by null")
        if (!targetDirectory.isDirectory)
            throw IllegalArgumentException(targetDirectory.absolutePath + " is not a directory")

        for (content in sourceDirectory.listFiles()!!) {
            copy(content, targetDirectory, overwrite)
        }
    }

    /**
     * Links the specified file or directory from `sourceLocation` to `targetLocation`. If
     * `targetLocation` does not exist, it will be created.
     *
     *
     * If this fails (because linking is not supported on the current filesystem, then a copy is made.
     *
     * If `overwrite` is set to `false`, this method throws an [IOException] if the target
     * file already exists.
     *
     * @param sourceLocation
     * the source file or directory
     * @param targetLocation
     * the targetLocation
     * @param overwrite
     * `true` to overwrite existing files
     * @return the created link
     * @throws IOException
     * if linking of the file or directory failed
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun link(sourceLocation: File, targetLocation: File, overwrite: Boolean = false): File {
        val sourcePath = requireNonNull(sourceLocation).toPath()
        val targetPath = requireNonNull(targetLocation).toPath()

        if (exists(sourcePath)) {
            if (overwrite) {
                deleteIfExists(targetPath)
            } else {
                if (exists(targetPath)) {
                    throw IOException(format("There is already a file/directory at %s", targetPath))
                }
            }

            try {
                createLink(targetPath, sourcePath)
            } catch (e: UnsupportedOperationException) {
                logger.debug("Copy file because creating hard-links is not supported by the current file system: {}",
                        ExceptionUtils.getMessage(e))
                Files.copy(sourcePath, targetPath)
            } catch (e: IOException) {
                logger.debug("Copy file because creating a hard-link at '{}' for existing file '{}' did not work: {}",
                        targetPath, sourcePath, ExceptionUtils.getStackTrace(e))
                if (overwrite) {
                    Files.copy(sourcePath, targetPath, REPLACE_EXISTING)
                } else {
                    Files.copy(sourcePath, targetPath)
                }
            }

        } else {
            throw IOException(format("No file/directory found at %s", sourcePath))
        }

        return targetPath.toFile()
    }

    /**
     * Returns `true` if the operating system as well as the disk layout support creating a hard link from
     * `src` to `dest`. Note that this implementation requires two files rather than directories and
     * will overwrite any existing file that might already be present at the destination.
     *
     * @param sourceLocation
     * the source file
     * @param targetLocation
     * the target file
     * @return `true` if the link was created, `false` otherwhise
     */
    fun supportsLinking(sourceLocation: File, targetLocation: File): Boolean {
        val sourcePath = requireNonNull(sourceLocation).toPath()
        val targetPath = requireNonNull(targetLocation).toPath()

        if (!exists(sourcePath))
            throw IllegalArgumentException(format("Source %s does not exist", sourcePath))

        logger.debug("Creating hard link from {} to {}", sourcePath, targetPath)
        try {
            deleteIfExists(targetPath)
            createLink(targetPath, sourcePath)
        } catch (e: Exception) {
            logger.debug("Unable to create a link from {} to {}: {}", sourcePath, targetPath, e)
            return false
        }

        return true
    }

    @Throws(IOException::class)
    private fun determineDestination(targetLocation: File, sourceLocation: File, overwrite: Boolean): File {
        var dest: File? = null

        // Source location exists
        if (sourceLocation.exists()) {
            // Is the source file/directory readable
            if (sourceLocation.canRead()) {
                // If a directory...
                if (targetLocation.isDirectory) {
                    // Create a destination file within it, with the same name of the source target
                    dest = File(targetLocation, sourceLocation.name)
                } else {
                    // targetLocation is either a normal file or doesn't exist
                    dest = targetLocation
                }

                // Source and target locations can not be the same
                if (sourceLocation == dest) {
                    throw IOException("Source and target locations must be different")
                }

                // Search the first existing parent of the target file, to check if it can be written
                // getParentFile can return null even though there *is* a parent file, if the file is not absolute
                // That's the reason why getAbsoluteFile is used here
                var iter: File? = dest.absoluteFile
                while (iter != null) {
                    if (iter.exists()) {
                        if (iter.canWrite()) {
                            break
                        } else {
                            throw IOException("Destination " + dest + "cannot be written/modified")
                        }
                    }
                    iter = iter.parentFile
                }

                // Check the target file can be overwritten
                if (dest.exists() && !dest.isDirectory && !overwrite) {
                    throw IOException("Destination $dest already exists")
                }

            } else {
                throw IOException("$sourceLocation cannot be read")
            }
        } else {
            throw IOException("Source $sourceLocation does not exist")
        }

        return dest
    }

    /**
     * Delete all directories from `start` up to directory `limit` if they are empty. Directory
     * `limit` is *exclusive* and will not be deleted.
     *
     * @return true if the *complete* hierarchy has been deleted. false in any other case.
     */
    fun deleteHierarchyIfEmpty(limit: File, start: File): Boolean {
        return (limit.isDirectory
                && start.isDirectory
                && (isEqual(limit, start) || isParent(limit, start) && start.list()!!.size == 0 && start.delete() && deleteHierarchyIfEmpty(
                limit, start.parentFile)))
    }

    /** Compare two files by their canonical paths.  */
    fun isEqual(a: File, b: File): Boolean {
        try {
            return a.canonicalPath == b.canonicalPath
        } catch (e: IOException) {
            return false
        }

    }

    /**
     * Check if `a` is a parent of `b`. This can only be the case if `a` is a directory
     * and a sub path of `b`. `isParent(a, a) == true`.
     */
    fun isParent(a: File, b: File): Boolean {
        try {
            val aCanonical = a.canonicalPath
            val bCanonical = b.canonicalPath
            return aCanonical != bCanonical && bCanonical.startsWith(aCanonical)
        } catch (e: IOException) {
            return false
        }

    }

    /**
     * Like [.delete] but does not throw any IO exceptions.
     * In case of an IOException it will only be logged at warning level and the method returns false.
     */
    @JvmOverloads
    fun deleteQuietly(f: File, recurse: Boolean = false): Boolean {
        try {
            return delete(f, recurse)
        } catch (e: IOException) {
            logger.warn("Cannot delete " + f.absolutePath + " because of IOException"
                    + if (e.message != null) " " + e.message else "")
            return false
        }

    }

    /**
     * Deletes the specified file and returns `true` if the file was deleted.
     *
     *
     * In the case that `f` references a directory, it will only be deleted if it doesn't contain other files
     * or directories, unless `recurse` is set to `true`.
     *
     *
     * @param f
     * the file or directory
     * @param recurse
     * `true` to do a recursive deletes for directories
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun delete(f: File?, recurse: Boolean = false): Boolean {
        if (f == null)
            return false
        if (!f.exists())
            return false
        if (f.isDirectory) {
            val children = f.list() ?: throw IOException("Cannot list content of directory " + f.absolutePath)
            if (children != null) {
                if (children.size > 0 && !recurse)
                    return false
                for (child in children) {
                    delete(File(f, child), true)
                }
            } else {
                logger.debug("Unexpected null listing files in {}", f.absolutePath)
            }
        }
        return f.delete()
    }

    /**
     * Deletes the content of directory `dir` and, if specified, the directory itself. If `dir` is a
     * normal file it will always be deleted.
     *
     * @return true everthing was deleted, false otherwise
     */
    fun delete(dir: File, mode: Int): Boolean {
        if (dir.isDirectory) {
            var ok = delete(dir.listFiles()!!, mode != DELETE_FILES)
            if (mode == DELETE_ROOT) {
                ok = ok and dir.delete()
            }
            return ok
        } else {
            return dir.delete()
        }
    }

    /**
     * Deletes the content of directory `dir` and, if specified, the directory itself. If `dir` is a
     * normal file it will be deleted always.
     */
    private fun delete(files: Array<File>, deleteDir: Boolean): Boolean {
        var ok = true
        for (f in files) {
            if (f.isDirectory) {
                delete(f.listFiles()!!, deleteDir)
                if (deleteDir) {
                    ok = ok and f.delete()
                }
            } else {
                ok = ok and f.delete()
            }
        }
        return ok
    }

    /**
     * Returns a directory `subdir` inside the webapp's temporary work directory.
     *
     * @param subdir
     * name of the subdirectory
     * @return the ready to use temp directory
     */
    fun getTempDirectory(subdir: String): File {
        val tmp = File(tempDirectory, subdir)
        if (!tmp.exists())
            tmp.mkdirs()
        if (!tmp.isDirectory)
            throw IllegalStateException("$tmp is not a directory!")
        if (!tmp.canRead())
            throw IllegalStateException("Temp directory $tmp is not readable!")
        if (!tmp.canWrite())
            throw IllegalStateException("Temp directory $tmp is not writable!")
        return tmp
    }

    /**
     * Returns true if both canonical file paths are equal.
     *
     * @param a
     * the first file or null
     * @param b
     * the second file or null
     */
    fun equals(a: File?, b: File?): Boolean {
        try {
            return a != null && b != null && a.canonicalPath == b.canonicalPath
        } catch (e: IOException) {
            return false
        }

    }

}
/** Disable construction of this utility class  */
/**
 * Copies recursively the *content* of the specified `sourceDirectory` to
 * `targetDirectory`.
 *
 *
 * Note that existing files and directories will be overwritten.
 *
 * @param sourceDirectory
 * the source directory
 * @param targetDirectory
 * the target directory to copy the content of the source directory to
 * @throws IOException
 * if copying fails
 */
/**
 * Links the specified file or directory from `sourceLocation` to `targetLocation`. If
 * `targetLocation` does not exist, it will be created, if the target file already exists, an
 * [IOException] will be thrown.
 *
 *
 * If this fails (because linking is not supported on the current filesystem, then a copy is made.
 *
 *
 * @param sourceLocation
 * the source file or directory
 * @param targetLocation
 * the targetLocation
 * @return the created link
 * @throws IOException
 * if linking of the file or directory failed
 */
/**
 * Deletes the specified file and returns `true` if the file was deleted.
 *
 *
 * If `f` is a directory, it will only be deleted if it doesn't contain any other files or directories. To
 * do a recursive delete, you may use [.delete].
 *
 * @param f
 * the file or directory
 * @see .delete
 */
/**
 * Like [.delete] but does not throw any IO exceptions.
 * In case of an IOException it will only be logged at warning level and the method returns false.
 */
