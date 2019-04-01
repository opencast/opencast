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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.FileNotFoundException
import java.io.IOException
import java.util.Vector
import java.util.zip.Deflater

import de.schlichtherle.io.ArchiveDetector
import de.schlichtherle.io.ArchiveException
import de.schlichtherle.io.ArchiveWarningException
import de.schlichtherle.io.DefaultArchiveDetector
import de.schlichtherle.io.File
import de.schlichtherle.io.archive.zip.ZipDriver

/*
 * WARNING:
 * Some archivers, such as file-roller in Ubuntu, seem not to be able to uncompress zip archives containg files with special characters.
 * The pure zip standard uses the CP437 encoding which CAN'T represent special characters, but applications have implemented their own methods
 * to overcome this inconvenience. TrueZip also. However, it seems file-roller (and probably others) doesn't "understand" how to restore the
 * original filenames and shows strange (and un-readable) characters in the unzipped filenames
 * However, the inverse process (unzipping zip archives made with file-roller and containing files with special characters) seems to work fine
 *
 * N.B. By "special characters" I mean those not present in the original ASCII specification, such as accents, special letters, etc
 *
 * ruben.perez
 */

/**
 * Provides static methods for compressing and extracting zip files using zip64 extensions when necessary.
 */
object ZipUtil {

    private val logger = LoggerFactory.getLogger(ZipUtil::class.java!!)

    val BEST_SPEED = Deflater.BEST_SPEED
    val BEST_COMPRESSION = Deflater.BEST_COMPRESSION
    val DEFAULT_COMPRESSION = Deflater.DEFAULT_COMPRESSION
    val NO_COMPRESSION = Deflater.NO_COMPRESSION

    /**
     * Utility class to ease the process of umounting a zip file
     *
     * @param zipFile
     * The file to umount
     * @throws IOException
     * If some problem occurs on unmounting
     */
    @Throws(IOException::class)
    private fun umount(zipFile: File) {
        try {
            File.umount(zipFile)
        } catch (awe: ArchiveWarningException) {
            logger.warn("Umounting {} threw the following warning: {}", zipFile.canonicalPath, awe.message)
        } catch (ae: ArchiveException) {
            logger.error("Unable to umount zip file: {}", zipFile.canonicalPath)
            throw IOException("Unable to umount zip file: " + zipFile.canonicalPath, ae)
        }

    }

    /** */
    /* SERVICE CLASSES - The two following classes are the ones actually doing the job */
    /** */

    /**
     * Compresses source files into a zip archive
     *
     * @param sourceFiles
     * A [java.io.File] array with the files to include in the root of the archive
     * @param destination
     * A [java.io.File] descriptor to the location where the zip file should be created
     * @param recursive
     * Indicate whether or not recursively zipping nested directories
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun zip(sourceFiles: Array<java.io.File>?, destination: java.io.File?, recursive: Boolean = false, level: Int = DEFAULT_COMPRESSION): java.io.File {
        var level = level

        if (sourceFiles == null) {
            logger.error("The array with files to zip cannot be null")
            throw IllegalArgumentException("The array with files to zip cannot be null")
        }

        if (sourceFiles.size <= 0) {
            logger.error("The array with files to zip cannot be empty")
            throw IllegalArgumentException("The array with files to zip cannot be empty")
        }

        if (destination == null) {
            logger.error("The destination file cannot be null")
            throw IllegalArgumentException("The destination file cannot be null")
        }

        if (destination.exists()) {
            logger.error("The destination file {} already exists", destination.canonicalPath)
            throw IllegalArgumentException("The destination file already exists")
        }

        if (level < -1) {
            logger.warn("Compression level cannot be less than 0 (or -1 for default)")
            logger.warn("Reverting to default...")
            level = -1
        } else if (level > 9) {
            logger.warn("Compression level cannot be greater than 9")
            logger.warn("Reverting to default...")
            level = -1
        }

        // Limits the compression support to ZIP only and sets the compression level
        val zd = ZipDriver(level)
        val ad = DefaultArchiveDetector(ArchiveDetector.NULL, "zip", zd)
        val zipFile: File
        try {
            zipFile = File(destination.canonicalFile, ad)
        } catch (ioe: IOException) {
            logger.error("Unable to create the zip file: {}", destination.absolutePath)
            throw IOException("Unable to create the zip file: {}" + destination.absolutePath, ioe)
        }

        try {
            if (!zipFile.isArchive) {
                logger.error("The destination file does not represent a valid zip archive (.zip extension is required)")
                zipFile.deleteAll()
                throw IllegalArgumentException(
                        "The destination file does not represent a valid zip archive (.zip extension is required)")
            }

            if (!zipFile.mkdirs())
                throw IOException("Couldn't create the destination file")

            for (f in sourceFiles) {

                if (f == null) {
                    logger.error("Null inputfile in array")
                    zipFile.deleteAll()
                    throw IllegalArgumentException("Null inputfile in array")
                }

                logger.debug("Attempting to zip file {}...", f.absolutePath)

                // TrueZip manual says that (archiveC|copy)All(From|To) methods work with either directories or regular files
                // Therefore, one could do zipFile.archiveCopyAllFrom(f), where f is a regular file, and it would work. Well, it
                // DOESN'T
                // This is why we have to tell if a file is a regular file or a directory BEFORE copying it with the appropriate
                // method
                var success = false
                if (f.exists()) {
                    if (!f.isDirectory || recursive) {
                        success = File(zipFile, f.name).copyAllFrom(f)
                        if (success)
                            logger.debug("File {} zipped successfuly", f.absolutePath)
                        else {
                            logger.error("File {} not zipped", f.absolutePath)
                            zipFile.deleteAll()
                            throw IOException("Failed to zip one of the input files: " + f.absolutePath)
                        }
                    }
                } else {
                    logger.error("Input file {} doesn't exist", f.absolutePath)
                    zipFile.deleteAll()
                    throw FileNotFoundException("One of the input files does not exist: " + f.absolutePath)
                }
            }
        } catch (e: IOException) {
            throw e
        } finally {
            umount(zipFile)
        }

        return destination
    }

    /**
     * Extracts a zip file to a directory.
     *
     * @param zipFile
     * A [String] with the path to the source zip archive
     * @param destination
     * A [String] with the location where the zip archive will be extracted. If this destination directory
     * does not exist, it will be created.
     * @throws IOException
     * if the zip file cannot be read, the destination directory cannot be created or the extraction is not
     * successful
     */
    @Throws(IOException::class)
    fun unzip(zipFile: java.io.File?, destination: java.io.File?) {

        val success: Boolean

        if (zipFile == null) {
            logger.error("The zip file cannot be null")
            throw IllegalArgumentException("The zip file must be set")
        }

        if (!zipFile.exists()) {
            logger.error("The zip file does not exist: {}", zipFile.canonicalPath)
            throw FileNotFoundException("The zip file does not exist: " + zipFile.canonicalPath)
        }

        if (destination == null) {
            logger.error("The destination file cannot be null")
            throw IllegalArgumentException("Destination file cannot be null")
        }

        // FIXME Commented out for 3rd party compatibility. See comment in the zip method above -ruben.perez
        // File f = new File(zipFile.getCanonicalFile(), new DefaultArchiveDetector(ArchiveDetector.NULL, "zip", new
        // ZipDriver("utf-8")));
        val f: File
        try {
            f = File(zipFile.canonicalFile)
        } catch (ioe: IOException) {
            logger.error("Unable to create the zip file: {}", destination.absolutePath)
            throw IOException("Unable to create the zip file: {}" + destination.absolutePath, ioe)
        }

        try {
            if (f.isArchive && f.isDirectory) {
                if (destination.exists()) {
                    if (!destination.isDirectory) {
                        logger.error("Destination file must be a directory")
                        throw IllegalArgumentException("Destination file must be a directory")
                    }
                }

                try {
                    destination.mkdirs()
                } catch (e: SecurityException) {
                    logger.error("Cannot create destination directory: {}", e.message)
                    throw IOException("Cannot create destination directory", e)
                }

                success = f.copyAllTo(destination)

                if (success)
                    logger.debug("File {} unzipped successfully", zipFile.canonicalPath)
                else {
                    logger.warn("File {} was not correctly unzipped", zipFile.canonicalPath)
                    throw IOException("File " + zipFile.canonicalPath + " was not correctly unzipped")
                }
            } else {
                logger.error("The input file is not a valid zip file")
                throw IllegalArgumentException("The input file is not a valid zip file")
            }
        } catch (e: IOException) {
            throw e
        } finally {
            umount(f)
        }

    }

    /**
     * Compresses source files into a zip archive (no recursive)
     *
     * @param sourceFiles
     * A [String] array with the file names to be included in the root of the archive
     * @param destination
     * A [String] with the path name of the resulting zip file
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    fun zip(sourceFiles: Array<String>, destination: String, level: Int): java.io.File {
        return zip(sourceFiles, destination, false, level)
    }

    /**
     * Compresses source files into a zip archive
     *
     * @param sourceFiles
     * A [String] array with the file names to be included in the root of the archive
     * @param destination
     * A [String] with the path name of the resulting zip file
     * @param recursive
     * Indicate whether or not recursively zipping nested directories
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun zip(sourceFiles: Array<String>?, destination: String?, recursive: Boolean = false, level: Int = DEFAULT_COMPRESSION): java.io.File {

        if (sourceFiles == null) {
            logger.error("The input String array cannot be null")
            throw IllegalArgumentException("The input String array cannot be null")
        }

        if (destination == null) {
            logger.error("Destination file cannot be null")
            throw IllegalArgumentException("Destination file cannot be null")
        }

        if ("" == destination) {
            logger.error("Destination file name must be set")
            throw IllegalArgumentException("Destination file name must be set")
        }

        val files = Vector<java.io.File>()
        for (name in sourceFiles) {
            if (name == null) {
                logger.error("One of the input file names is null")
                throw IllegalArgumentException("One of the input file names is null")
            } else if ("" == name) {
                logger.error("One of the input file names is blank")
                throw IllegalArgumentException("One of the input file names is blank")
            }
            files.add(java.io.File(name))
        }

        return zip(files.toTypedArray<File>(), java.io.File(destination), recursive, level)

    }

    /**
     * Compresses source files into a zip archive (no recursive)
     *
     * @param sourceFiles
     * A [String] array with the file names to be included in the root of the archive
     * @param destination
     * A [java.io.File] with the path name of the resulting zip file
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    fun zip(sourceFiles: Array<String>, destination: java.io.File, level: Int): java.io.File {
        return zip(sourceFiles, destination, false, level)
    }

    /**
     * Compresses source files into a zip archive
     *
     * @param sourceFiles
     * A [String] array with the file names to be included in the root of the archive
     * @param destination
     * A [java.io.File] with the path name of the resulting zip file
     * @param recursive
     * Indicate whether or not recursively zipping nested directories
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun zip(sourceFiles: Array<String>?, destination: java.io.File, recursive: Boolean = false, level: Int = DEFAULT_COMPRESSION): java.io.File {

        if (sourceFiles == null) {
            logger.error("The input String array cannot be null")
            throw IllegalArgumentException("The input String array cannot be null")
        }

        val files = Vector<java.io.File>()
        for (name in sourceFiles) {
            if (name == null) {
                logger.error("One of the input file names is null")
                throw IllegalArgumentException("One of the input file names is null")
            } else if ("" == name) {
                logger.error("One of the input file names is blank")
                throw IllegalArgumentException("One of the input file names is blank")
            }
            files.add(java.io.File(name))
        }

        return zip(files.toTypedArray<File>(), destination, recursive, level)

    }

    /**
     * Compresses source files into a zip archive (no recursive)
     *
     * @param sourceFiles
     * A [java.io.File] array with the file names to be included in the root of the archive
     * @param destination
     * A [String] with the path name of the resulting zip file
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    fun zip(sourceFiles: Array<java.io.File>, destination: String, level: Int): java.io.File {
        return zip(sourceFiles, destination, false, level)
    }

    /**
     * Compresses source files into a zip archive
     *
     * @param sourceFiles
     * A [java.io.File] array with the file names to be included in the root of the archive
     * @param destination
     * A [String] with the path name of the resulting zip file
     * @param recursive
     * Indicate whether or not recursively zipping nested directories
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    @JvmOverloads
    fun zip(sourceFiles: Array<java.io.File>, destination: String?, recursive: Boolean = false, level: Int = DEFAULT_COMPRESSION): java.io.File {

        if (destination == null) {
            logger.error("Destination file cannot be null")
            throw IllegalArgumentException("Destination file cannot be null")
        }

        if ("" == destination) {
            logger.error("Destination file name must be set")
            throw IllegalArgumentException("Destination file name must be set")
        }

        return zip(sourceFiles, java.io.File(destination), recursive, level)

    }

    /**
     * Compresses source files into a zip archive (no recursive)
     *
     * @param sourceFiles
     * A [java.io.File] array with the file names to be included in the root of the archive
     * @param destination
     * A [java.io.File] with the path name of the resulting zip file
     * @param level
     * The zip algorithm compression level. Ranges between 0 (no compression) and 9 (max. compression)
     * @return A [java.io.File] descriptor of the zip archive file
     * @throws IOException
     * If the zip file can not be created, or the input files names can not be correctly parsed
     */
    @Throws(IOException::class)
    fun zip(sourceFiles: Array<java.io.File>, destination: java.io.File, level: Int): java.io.File {
        return zip(sourceFiles, destination, false, level)
    }

    /**
     * Extracts a zip file to a directory.
     *
     * @param zipFile
     * A [String] with the path to the source zip archive
     * @param destination
     * A [String] with the location where the zip archive will be extracted. If this destination directory
     * does not exist, it will be created.
     * @throws IOException
     * if the zip file cannot be read, the destination directory cannot be created or the extraction is not
     * successful
     */
    @Throws(IOException::class)
    fun unzip(zipFile: String?, destination: String?) {

        if (zipFile == null) {
            logger.error("Input filename cannot be null")
            throw IllegalArgumentException("Input filename cannot be null")
        }

        if ("" == zipFile) {
            logger.error("Input filename cannot be empty")
            throw IllegalArgumentException("Input filename cannot be empty")
        }

        if (destination == null) {
            logger.error("Output filename cannot be null")
            throw IllegalArgumentException("Output filename cannot be null")
        }

        if ("" == destination) {
            logger.error("Output filename cannot be empty")
            throw IllegalArgumentException("Output filename cannot be empty")
        }

        unzip(java.io.File(zipFile), java.io.File(destination))

    }

    /**
     * Extracts a zip file to a directory.
     *
     * @param zipFile
     * A [java.io.File] with the path to the source zip archive
     * @param destination
     * A [String] with the location where the zip archive will be extracted.
     * @throws IOException
     * if the zip file cannot be read, the destination directory cannot be created or the extraction is not
     * successful
     */
    @Throws(IOException::class)
    fun unzip(zipFile: java.io.File, destination: String?) {

        if (destination == null) {
            logger.error("Output filename cannot be null")
            throw IllegalArgumentException("Output filename cannot be null")
        }

        if ("" == destination) {
            logger.error("Output filename cannot be empty")
            throw IllegalArgumentException("Output filename cannot be empty")
        }

        unzip(zipFile, java.io.File(destination))

    }

    /**
     * Extracts a zip file to a directory.
     *
     * @param zipFile
     * A [String] with the path to the source zip archive
     * @param destination
     * A [java.io.File] with the location where the zip archive will be extracted.
     * @throws IOException
     * if the zip file cannot be read, the destination directory cannot be created or the extraction is not
     * successful
     */
    @Throws(IOException::class)
    fun unzip(zipFile: String?, destination: java.io.File) {

        if (zipFile == null) {
            logger.error("Input filename cannot be null")
            throw IllegalArgumentException("Input filename cannot be null")
        }

        if ("" == zipFile) {
            logger.error("Input filename cannot be empty")
            throw IllegalArgumentException("Input filename cannot be empty")
        }

        unzip(java.io.File(zipFile), destination)

    }

}
/** Disable construction of this utility class  */
/*************************************************************************************  *//* "ALIASES" - For different types of input, but actually calling the previous methods */
/** */
/**
 * Compresses source files into a zip archive (no recursive, default compression)
 *
 * @param sourceFiles
 * A [String] array with the file names to be included in the root of the archive
 * @param destination
 * A [String] with the path name of the resulting zip file
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (default compression)
 *
 * @param sourceFiles
 * A [String] array with the file names to be included in the root of the archive
 * @param destination
 * A [String] with the path name of the resulting zip file
 * @param recursive
 * A [boolean] indicating whether or not recursively zipping nested directories
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (no recursive, default compression)
 *
 * @param sourceFiles
 * A [String] array with the file names to be included in the root of the archive
 * @param destination
 * A [java.io.File] with the path name of the resulting zip file
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (default compression)
 *
 * @param sourceFiles
 * A [String] array with the file names to be included in the root of the archive
 * @param destination
 * A [java.io.File] with the path name of the resulting zip file
 * @param recursive
 * Indicate whether or not recursively zipping nested directories
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (no recursive, default compression)
 *
 * @param sourceFiles
 * A [java.io.File] array with the file names to be included in the root of the archive
 * @param destination
 * A [String] with the path name of the resulting zip file
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (default compression)
 *
 * @param sourceFiles
 * A [java.io.File] array with the file names to be included in the root of the archive
 * @param destination
 * A [String] with the path name of the resulting zip file
 * @param recursive
 * Indicate whether or not recursively zipping nested directories
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (no recursive, default compression)
 *
 * @param sourceFiles
 * A [java.io.File] array with the file names to be included in the root of the archive
 * @param destination
 * A [java.io.File] with the path name of the resulting zip file
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
/**
 * Compresses source files into a zip archive (default compression)
 *
 * @param sourceFiles
 * A [java.io.File] array with the file names to be included in the root of the archive
 * @param destination
 * A [java.io.File] with the path name of the resulting zip file
 * @param recursive
 * Indicate whether or not recursively zipping nested directories
 * @return A [java.io.File] descriptor of the zip archive file
 * @throws IOException
 * If the zip file can not be created, or the input files names can not be correctly parsed
 */
