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

package org.opencastproject.workingfilerepository.impl

import org.opencastproject.rest.RestConstants
import org.opencastproject.security.api.SecurityService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.Checksum
import org.opencastproject.util.FileSupport
import org.opencastproject.util.Log
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.PathSupport
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
import org.opencastproject.util.jmx.JmxUtil
import org.opencastproject.workingfilerepository.api.PathMappable
import org.opencastproject.workingfilerepository.api.WorkingFileRepository
import org.opencastproject.workingfilerepository.jmx.WorkingFileRepositoryBean

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Date

import javax.management.ObjectInstance

/**
 * A very simple (read: inadequate) implementation that stores all files under a root directory using the media package
 * ID as a subdirectory and the media package element ID as the file name.
 */
open class WorkingFileRepositoryImpl : WorkingFileRepository, PathMappable {

    /** The JMX working file repository bean  */
    private val workingFileRepositoryBean = WorkingFileRepositoryBean(this)

    /** The JMX bean object instance  */
    private var registeredMXBean: ObjectInstance? = null

    /** The remote service manager  */
    protected var remoteServiceManager: ServiceRegistry

    /** The root directory for storing files  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.PathMappable.getPathPrefix
     */
    override var pathPrefix: String? = null

    /** The Base URL for this server  */
    var serverUrl: String? = null

    /** The URL path for the services provided by the working file repository  */
    var servicePath: String? = null

    /** The security service to get current organization from  */
    protected var securityService: SecurityService

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getTotalSpace
     */
    override val totalSpace: Option<Long>
        get() {
            val f = File(pathPrefix!!)
            return Option.some(f.totalSpace)
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getUsableSpace
     */
    override val usableSpace: Option<Long>
        get() {
            val f = File(pathPrefix!!)
            return Option.some(f.usableSpace)
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getUsedSpace
     */
    override val usedSpace: Option<Long>
        get() = Option.some(FileUtils.sizeOfDirectory(File(pathPrefix!!)))

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getDiskSpace
     */
    override val diskSpace: String
        get() {
            val usable = Math.round((usableSpace.get() / 1024 / 1024 / 1024).toFloat())
            val total = Math.round((totalSpace.get() / 1024 / 1024 / 1024).toFloat())
            val percent = Math.round(100.0 * usableSpace.get() / (1 + totalSpace.get()))
            return "Usable space $usable Gb out of $total Gb ($percent%)"
        }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.PathMappable.getUrlPrefix
     */
    override val urlPrefix: String
        get() = baseUri.toString()

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getBaseUri
     */
    override val baseUri: URI
        get() {
            if (securityService.organization != null) {
                val orgProps = securityService.organization.properties
                if (orgProps != null && orgProps.containsKey(OpencastConstants.WFR_URL_ORG_PROPERTY)) {
                    try {
                        return URI(UrlSupport.concat(orgProps[OpencastConstants.WFR_URL_ORG_PROPERTY], servicePath))
                    } catch (ex: URISyntaxException) {
                        logger.warn("Organization working file repository URL not set, fallback to server URL")
                    }

                }
            }

            return URI.create(UrlSupport.concat(serverUrl, servicePath))
        }

    /**
     * Activate the component
     */
    @Throws(IOException::class)
    open fun activate(cc: ComponentContext) {
        if (pathPrefix != null)
            return  // If the root directory was set, respect that setting

        // server url
        serverUrl = cc.bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)
        if (StringUtils.isBlank(serverUrl))
            throw IllegalStateException("Server URL must be set")

        // working file repository 'facade' configuration
        servicePath = cc.properties.get(RestConstants.SERVICE_PATH_PROPERTY) as String

        // root directory
        pathPrefix = StringUtils.trimToNull(cc.bundleContext.getProperty("org.opencastproject.file.repo.path"))
        if (pathPrefix == null) {
            val storageDir = cc.bundleContext.getProperty("org.opencastproject.storage.dir")
                    ?: throw IllegalStateException("Storage directory must be set")
            pathPrefix = storageDir + File.separator + "files"
        }

        try {
            createRootDirectory()
        } catch (e: IOException) {
            logger.error("Unable to create the working file repository root directory at {}", pathPrefix)
            throw e
        }

        registeredMXBean = JmxUtil.registerMXBean(workingFileRepositoryBean, JMX_WORKING_FILE_REPOSITORY_TYPE)

        logger.info(diskSpace)
    }

    /**
     * Callback from OSGi on service deactivation.
     */
    fun deactivate() {
        JmxUtil.unregisterMXBean(registeredMXBean!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.delete
     */
    @Throws(IOException::class)
    override fun delete(mediaPackageID: String, mediaPackageElementID: String): Boolean {
        val f: File
        try {
            f = getFile(mediaPackageID, mediaPackageElementID)

            val parentDirectory = f.parentFile
            logger.debug("Attempting to delete {}", parentDirectory.absolutePath)
            FileUtils.forceDelete(parentDirectory)
            val parentsParentDirectory = parentDirectory.parentFile
            if (parentsParentDirectory.isDirectory && parentsParentDirectory.list()!!.size == 0)
                FileUtils.forceDelete(parentDirectory.parentFile)
            return true
        } catch (e: NotFoundException) {
            log.info("Unable to delete non existing media package element {}@{}", mediaPackageElementID, mediaPackageID)
            return false
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.get
     */
    @Throws(NotFoundException::class, IOException::class)
    override fun get(mediaPackageID: String, mediaPackageElementID: String): InputStream {
        val f = getFile(mediaPackageID, mediaPackageElementID)
        logger.debug("Attempting to read file {}", f.absolutePath)
        return FileInputStream(f)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getCollectionURI
     */
    override fun getCollectionURI(collectionID: String, fileName: String): URI {
        try {
            return URI(baseUri + COLLECTION_PATH_PREFIX + collectionID + "/" + PathSupport.toSafeName(fileName))
        } catch (e: URISyntaxException) {
            throw IllegalStateException("Unable to create valid uri from $collectionID and $fileName")
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getURI
     */
    override fun getURI(mediaPackageID: String, mediaPackageElementID: String): URI {
        return getURI(mediaPackageID, mediaPackageElementID, null!!)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getURI
     */
    override fun getURI(mediaPackageID: String, mediaPackageElementID: String, fileName: String): URI {
        var fileName = fileName
        var uri = UrlSupport.concat(*arrayOf(baseUri.toString(), MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID))
        if (fileName == null) {
            val existingDirectory = getElementDirectory(mediaPackageID, mediaPackageElementID)
            if (existingDirectory.isDirectory) {
                val files = existingDirectory.listFiles()
                var md5Exists = false
                for (f in files!!) {
                    if (f.name.endsWith(MD5_EXTENSION)) {
                        md5Exists = true
                    } else {
                        fileName = f.name
                    }
                }
                if (md5Exists && fileName != null) {
                    uri = UrlSupport.concat(uri, PathSupport.toSafeName(fileName))
                }
            }
        } else {
            uri = UrlSupport.concat(uri, PathSupport.toSafeName(fileName))
        }
        try {
            return URI(uri)
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException(e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.put
     */
    @Throws(IOException::class)
    override fun put(mediaPackageID: String, mediaPackageElementID: String, filename: String, `in`: InputStream): URI {
        checkPathSafe(mediaPackageID)
        checkPathSafe(mediaPackageElementID)
        val dir = getElementDirectory(mediaPackageID, mediaPackageElementID)

        var filesToDelete: Array<File>? = null

        if (dir.exists()) {
            filesToDelete = dir.listFiles()
        } else {
            logger.debug("Attempting to create a new directory at {}", dir.absolutePath)
            FileUtils.forceMkdir(dir)
        }

        // Destination files
        val f = File(dir, PathSupport.toSafeName(filename))
        val md5File = getMd5File(f)

        // Temporary files while adding
        var fTmp: File? = null
        var md5FileTmp: File? = null

        if (f.exists()) {
            logger.debug("Updating file {}", f.absolutePath)
        } else {
            logger.debug("Adding file {}", f.absolutePath)
        }

        var out: FileOutputStream? = null
        try {

            fTmp = File.createTempFile(f.name, ".tmp", dir)
            md5FileTmp = File.createTempFile(md5File.name, ".tmp", dir)

            logger.trace("Writing to new temporary file {}", fTmp!!.absolutePath)

            out = FileOutputStream(fTmp)

            // Wrap the input stream and copy the input stream to the file
            var messageDigest: MessageDigest? = null
            var dis: DigestInputStream? = null
            try {
                messageDigest = MessageDigest.getInstance("MD5")
                dis = DigestInputStream(`in`, messageDigest)
                IOUtils.copy(dis, out)
            } catch (e1: NoSuchAlgorithmException) {
                logger.error("Unable to create md5 message digest")
            }

            // Store the hash
            val md5 = Checksum.convertToHex(dis!!.messageDigest.digest())
            try {
                FileUtils.writeStringToFile(md5FileTmp!!, md5)
            } catch (e: IOException) {
                FileUtils.deleteQuietly(md5FileTmp)
                throw e
            } finally {
                IOUtils.closeQuietly(dis)
            }

        } catch (e: IOException) {
            IOUtils.closeQuietly(out)
            FileUtils.deleteQuietly(dir)
            throw e
        } finally {
            IOUtils.closeQuietly(out)
            IOUtils.closeQuietly(`in`)
        }

        // Rename temporary files to the final version atomically
        try {
            Files.move(md5FileTmp!!.toPath(), md5File.toPath(), StandardCopyOption.ATOMIC_MOVE)
            Files.move(fTmp!!.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            logger.trace("Atomic move not supported by this filesystem: using replace instead")
            Files.move(md5FileTmp!!.toPath(), md5File.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.move(fTmp!!.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        // Clean up any other files
        if (filesToDelete != null && filesToDelete.size > 0) {
            for (fileToDelete in filesToDelete) {
                if (fileToDelete != f && fileToDelete != md5File) {
                    logger.trace("delete {}", fileToDelete.absolutePath)
                    if (!fileToDelete.delete()) {
                        throw IllegalStateException("Unable to delete file: " + fileToDelete.absolutePath)
                    }
                }
            }
        }

        return getURI(mediaPackageID, mediaPackageElementID, filename)
    }

    /**
     * Creates a file containing the md5 hash for the contents of a source file.
     *
     * @param f
     * the source file containing the data to hash
     * @throws IOException
     * if the hash cannot be created
     */
    @Throws(IOException::class)
    protected fun createMd5(f: File): File {
        var md5In: FileInputStream? = null
        var md5File: File? = null
        try {
            md5In = FileInputStream(f)
            val md5 = DigestUtils.md5Hex(md5In)
            IOUtils.closeQuietly(md5In)
            md5File = getMd5File(f)
            FileUtils.writeStringToFile(md5File, md5)
            return md5File
        } catch (e: IOException) {
            FileUtils.deleteQuietly(md5File)
            throw e
        } finally {
            IOUtils.closeQuietly(md5In)
        }
    }

    /**
     * Creates a file containing the md5 hash for the contents of a source file.
     *
     * @param is
     * the input stream containing the data to hash
     * @throws IOException
     * if the hash cannot be created
     */
    @Throws(IOException::class)
    protected fun createMd5(`is`: InputStream): String {
        val md5File: File? = null
        try {
            return DigestUtils.md5Hex(`is`)
        } catch (e: IOException) {
            FileUtils.deleteQuietly(md5File)
            throw e
        } finally {
            IOUtils.closeQuietly(`is`)
        }
    }

    /**
     * Gets the file handle for an md5 associated with a content file. Calling this method and obtaining a File handle is
     * not a guarantee that the md5 file exists.
     *
     * @param f
     * The source file
     * @return The md5 file
     */
    private fun getMd5File(f: File): File {
        return File(f.parent, f.name + MD5_EXTENSION)
    }

    /**
     * Gets the file handle for a source file from its md5 file.
     *
     * @param md5File
     * The md5 file
     * @return The source file
     */
    protected fun getSourceFile(md5File: File): File {
        return File(md5File.parent, md5File.name.substring(0, md5File.name.length - 4))
    }

    protected fun checkPathSafe(id: String?) {
        if (id == null)
            throw NullPointerException("IDs can not be null")
        if (id.indexOf("..") > -1 || id.indexOf(File.separator) > -1) {
            throw IllegalArgumentException("Invalid media package, element ID, or file name")
        }
    }

    /**
     * Returns the file to the media package element.
     *
     * @param mediaPackageID
     * the media package identifier
     * @param mediaPackageElementID
     * the media package element identifier
     * @return the file or `null` if no such element exists
     * @throws IllegalStateException
     * if more than one matching elements were found
     * @throws NotFoundException
     * if the file cannot be found in the Working File Repository
     */
    @Throws(IllegalStateException::class, NotFoundException::class)
    protected fun getFile(mediaPackageID: String, mediaPackageElementID: String): File {
        checkPathSafe(mediaPackageID)
        checkPathSafe(mediaPackageElementID)
        val directory = getElementDirectory(mediaPackageID, mediaPackageElementID)

        val md5Files = directory.listFiles(MD5_FINAME_FILTER)
        if (md5Files == null) {
            logger.debug("Element directory {} does not exist", directory)
            throw NotFoundException("Element directory $directory does not exist")
        } else if (md5Files.size == 0) {
            logger.debug("There are no complete files in the element directory {}", directory.absolutePath)
            throw NotFoundException("There are no complete files in the element directory " + directory.absolutePath)
        } else if (md5Files.size == 1) {
            val f = getSourceFile(md5Files[0])
            return if (f.exists())
                f
            else
                throw NotFoundException("Unable to locate $f in the working file repository")
        } else {
            logger.error("Integrity error: Element directory {} contains more than one element", mediaPackageID + "/"
                    + mediaPackageElementID)
            throw IllegalStateException("Directory " + mediaPackageID + "/" + mediaPackageElementID
                    + "does not contain exactly one element")
        }
    }

    /**
     * Returns the file from the given collection.
     *
     * @param collectionId
     * the collection identifier
     * @param fileName
     * the file name
     * @return the file
     * @throws NotFoundException
     * if either the collection or the file don't exist
     */
    @Throws(NotFoundException::class, IllegalArgumentException::class)
    protected fun getFileFromCollection(collectionId: String, fileName: String): File {
        checkPathSafe(collectionId)

        var directory: File? = null
        try {
            directory = getCollectionDirectory(collectionId, false)
            if (directory == null) {
                //getCollectionDirectory returns null on a non-existant directory which is not being created...
                directory = File(PathSupport.concat(arrayOf<String>(pathPrefix, COLLECTION_PATH_PREFIX, collectionId)))
                throw NotFoundException(directory.absolutePath)
            }
        } catch (e: IOException) {
            // can be ignored, since we don't want the directory to be created, so it will never happen
        }

        val sourceFile = File(directory, PathSupport.toSafeName(fileName))
        val md5File = getMd5File(sourceFile)
        if (!sourceFile.exists())
            throw NotFoundException(sourceFile.absolutePath)
        if (!md5File.exists())
            throw NotFoundException(md5File.absolutePath)
        return sourceFile
    }

    private fun getElementDirectory(mediaPackageID: String, mediaPackageElementID: String): File {
        return File(PathSupport.concat(arrayOf<String>(pathPrefix, MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID)))
    }

    /**
     * Returns a `File` reference to collection. If the collection does not exist, the method either returns
     * `null` or creates it, depending on the parameter `create`.
     *
     * @param collectionId
     * the collection identifier
     * @param create
     * whether to create a collection directory if it does not exist
     * @return the collection directory or `null` if it does not exist and should not be created
     * @throws IOException
     * if creating a non-existing directory fails
     */
    @Throws(IOException::class)
    private fun getCollectionDirectory(collectionId: String, create: Boolean): File? {
        val collectionDir = File(
                PathSupport.concat(arrayOf<String>(pathPrefix, COLLECTION_PATH_PREFIX, collectionId)))
        if (!collectionDir.exists()) {
            if (!create)
                return null
            try {
                FileUtils.forceMkdir(collectionDir)
                logger.debug("Created collection directory $collectionId")
            } catch (e: IOException) {
                // We check again to see if it already exists because this collection dir may live on a shared disk.
                // Synchronizing does not help because the other instance is not in the same JVM.
                if (!collectionDir.exists()) {
                    throw IllegalStateException("Can not create collection directory$collectionDir")
                }
            }

        }
        return collectionDir
    }

    @Throws(IOException::class)
    internal fun createRootDirectory() {
        val f = File(pathPrefix!!)
        if (!f.exists())
            FileUtils.forceMkdir(f)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getCollectionSize
     */
    @Throws(NotFoundException::class)
    override fun getCollectionSize(id: String): Long {
        var collectionDir: File? = null
        try {
            collectionDir = getCollectionDirectory(id, false)
            if (collectionDir == null || !collectionDir.canRead())
                throw NotFoundException("Can not find collection $id")
        } catch (e: IOException) {
            // can be ignored, since we don't want the directory to be created, so it will never happen
        }

        val files = collectionDir!!.listFiles(MD5_FINAME_FILTER)
                ?: throw IllegalArgumentException("Collection $id is not a directory")
        return files.size.toLong()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getFromCollection
     */
    @Throws(NotFoundException::class, IOException::class)
    override fun getFromCollection(collectionId: String, fileName: String): InputStream {
        val f = getFileFromCollection(collectionId, fileName)
        if (f == null || !f.isFile) {
            throw NotFoundException("Unable to locate $f in the working file repository")
        }
        logger.debug("Attempting to read file {}", f.absolutePath)
        return FileInputStream(f)
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     * if the hash can't be created
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.putInCollection
     */
    @Throws(IOException::class)
    override fun putInCollection(collectionId: String, fileName: String, `in`: InputStream): URI {
        checkPathSafe(collectionId)
        checkPathSafe(fileName)
        val f = File(PathSupport.concat(arrayOf<String>(pathPrefix, COLLECTION_PATH_PREFIX, collectionId, PathSupport.toSafeName(fileName))))
        logger.debug("Attempting to write a file to {}", f.absolutePath)
        var out: FileOutputStream? = null
        try {
            if (!f.exists()) {
                logger.debug("Attempting to create a new file at {}", f.absolutePath)
                val collectionDirectory = getCollectionDirectory(collectionId, true)
                if (!collectionDirectory!!.exists()) {
                    logger.debug("Attempting to create a new directory at {}", collectionDirectory.absolutePath)
                    FileUtils.forceMkdir(collectionDirectory)
                }
                f.createNewFile()
            } else {
                logger.debug("Attempting to overwrite the file at {}", f.absolutePath)
            }
            out = FileOutputStream(f)

            // Wrap the input stream and copy the input stream to the file
            var messageDigest: MessageDigest? = null
            var dis: DigestInputStream? = null
            try {
                messageDigest = MessageDigest.getInstance("MD5")
                dis = DigestInputStream(`in`, messageDigest)
                IOUtils.copy(dis, out)
            } catch (e1: NoSuchAlgorithmException) {
                logger.error("Unable to create md5 message digest")
            }

            // Store the hash
            val md5 = Checksum.convertToHex(dis!!.messageDigest.digest())
            var md5File: File? = null
            try {
                md5File = getMd5File(f)
                FileUtils.writeStringToFile(md5File, md5)
            } catch (e: IOException) {
                FileUtils.deleteQuietly(md5File)
                throw e
            } finally {
                IOUtils.closeQuietly(dis)
            }

        } catch (e: IOException) {
            FileUtils.deleteQuietly(f)
            throw e
        } finally {
            IOUtils.closeQuietly(out)
            IOUtils.closeQuietly(`in`)
        }
        return getCollectionURI(collectionId, fileName)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.copyTo
     */
    @Throws(NotFoundException::class, IOException::class)
    override fun copyTo(fromCollection: String, fromFileName: String, toMediaPackage: String, toMediaPackageElement: String,
                        toFileName: String): URI {
        val source = getFileFromCollection(fromCollection, fromFileName)
                ?: throw IllegalArgumentException("Source file $fromCollection/$fromFileName does not exist")
        val destDir = getElementDirectory(toMediaPackage, toMediaPackageElement)
        if (!destDir.exists()) {
            // we needed to create the directory, but couldn't
            try {
                FileUtils.forceMkdir(destDir)
            } catch (e: IOException) {
                throw IllegalStateException("could not create mediapackage/element directory '" + destDir.absolutePath
                        + "' : " + e)
            }

        }
        val destFile: File
        try {
            destFile = File(destDir, PathSupport.toSafeName(toFileName))
            FileSupport.link(source, destFile)
            createMd5(destFile)
        } catch (e: Exception) {
            FileUtils.deleteDirectory(destDir)
        }

        return getURI(toMediaPackage, toMediaPackageElement, toFileName)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.moveTo
     */
    @Throws(NotFoundException::class, IOException::class)
    override fun moveTo(fromCollection: String, fromFileName: String, toMediaPackage: String, toMediaPackageElement: String,
                        toFileName: String): URI {
        val source = getFileFromCollection(fromCollection, fromFileName)
        val sourceMd5 = getMd5File(source)
        val destDir = getElementDirectory(toMediaPackage, toMediaPackageElement)

        logger.debug("Moving {} from {} to {}/{}", *arrayOf(fromFileName, fromCollection, toMediaPackage, toMediaPackageElement))
        if (!destDir.exists()) {
            // we needed to create the directory, but couldn't
            try {
                FileUtils.forceMkdir(destDir)
            } catch (e: IOException) {
                throw IllegalStateException("could not create mediapackage/element directory '" + destDir.absolutePath
                        + "' : " + e)
            }

        }

        var dest: File? = null
        try {
            dest = getFile(toMediaPackage, toMediaPackageElement)
            logger.debug("Removing existing file from target location at {}", dest)
            delete(toMediaPackage, toMediaPackageElement)
        } catch (e: NotFoundException) {
            dest = File(getElementDirectory(toMediaPackage, toMediaPackageElement), PathSupport.toSafeName(toFileName))
        }

        try {
            FileUtils.moveFile(source, dest!!)
            FileUtils.moveFile(sourceMd5, getMd5File(dest))
        } catch (e: IOException) {
            FileUtils.deleteDirectory(destDir)
            throw IllegalStateException("unable to copy file$e")
        }

        return getURI(toMediaPackage, toMediaPackageElement, dest.name)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.deleteFromCollection
     */
    @Throws(IOException::class)
    override fun deleteFromCollection(collectionId: String, fileName: String, removeCollection: Boolean): Boolean {
        var f: File? = null
        try {
            f = getFileFromCollection(collectionId, fileName)
        } catch (e: NotFoundException) {
            logger.trace("File {}/{} does not exist", collectionId, fileName)
            return false
        }

        val md5File = getMd5File(f)

        if (!f.isFile)
            throw IllegalStateException("$f is not a regular file")
        if (!md5File.isFile)
            throw IllegalStateException("$md5File is not a regular file")
        if (!md5File.delete())
            throw IOException("MD5 hash $md5File cannot be deleted")
        if (!f.delete())
            throw IOException("$f cannot be deleted")

        if (removeCollection) {
            val parentDirectory = f.parentFile
            if (parentDirectory.isDirectory && parentDirectory.list()!!.size == 0) {
                logger.debug("Attempting to delete empty collection directory {}", parentDirectory.absolutePath)
                try {
                    FileUtils.forceDelete(parentDirectory)
                } catch (e: IOException) {
                    logger.warn("Unable to delete empty collection directory {}", parentDirectory.absolutePath)
                    return false
                }

            }
        }
        return true
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.deleteFromCollection
     */
    @Throws(IOException::class)
    override fun deleteFromCollection(collectionId: String, fileName: String): Boolean {
        return deleteFromCollection(collectionId, fileName, false)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.getCollectionContents
     */
    @Throws(NotFoundException::class)
    override fun getCollectionContents(collectionId: String): Array<URI> {
        var collectionDir: File? = null
        try {
            collectionDir = getCollectionDirectory(collectionId, false)
            if (collectionDir == null)
                throw NotFoundException(collectionId)
        } catch (e: IOException) {
            // We are not asking for the collection to be created, so this exception is never thrown
        }

        val files = collectionDir!!.listFiles(MD5_FINAME_FILTER)
        val uris = arrayOfNulls<URI>(files!!.size)
        for (i in files.indices) {
            try {
                uris[i] = URI(baseUri + COLLECTION_PATH_PREFIX + collectionId + "/"
                        + PathSupport.toSafeName(getSourceFile(files[i]).name))
            } catch (e: URISyntaxException) {
                throw IllegalStateException("Invalid URI for " + files[i])
            }

        }

        return uris
    }

    /**
     * Returns the md5 hash value for the given mediapackage element.
     *
     * @throws NotFoundException
     * if the media package element does not exist
     */
    @Throws(IOException::class, IllegalStateException::class, NotFoundException::class)
    internal fun getMediaPackageElementDigest(mediaPackageID: String, mediaPackageElementID: String): String {
        val f = getFile(mediaPackageID, mediaPackageElementID)
                ?: throw NotFoundException("$mediaPackageID/$mediaPackageElementID")
        return getFileDigest(f)
    }

    /**
     * Returns the md5 hash value for the given collection element.
     *
     * @throws NotFoundException
     * if the collection element does not exist
     */
    @Throws(IOException::class, NotFoundException::class)
    internal fun getCollectionElementDigest(collectionId: String, fileName: String): String {
        return getFileDigest(getFileFromCollection(collectionId, fileName))
    }

    /**
     * Returns the md5 of a file
     *
     * @param file
     * the source file
     * @return the md5 hash
     */
    @Throws(IOException::class)
    private fun getFileDigest(file: File?): String {
        if (file == null)
            throw IllegalArgumentException("File must not be null")
        if (!file.exists() || !file.isFile)
            throw IllegalArgumentException("File " + file.absolutePath + " can not be read")

        // Check if there is a precalculated md5 hash
        val md5HashFile = getMd5File(file)
        if (file.exists()) {
            logger.trace("Reading precalculated hash for {} from {}", file, md5HashFile.name)
            return FileUtils.readFileToString(md5HashFile, "utf-8")
        }

        // Calculate the md5 hash
        var `in`: InputStream? = null
        var md5: String? = null
        try {
            `in` = FileInputStream(file)
            md5 = DigestUtils.md5Hex(`in`)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        // Write the md5 hash to disk for later reference
        try {
            FileUtils.writeStringToFile(md5HashFile, md5, "utf-8")
        } catch (e: IOException) {
            logger.warn("Error storing cached md5 checksum at {}", md5HashFile)
            throw e
        }

        return md5
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository.cleanupOldFilesFromCollection
     */
    @Throws(IOException::class)
    override fun cleanupOldFilesFromCollection(collectionId: String, days: Long): Boolean {
        val colDir = getCollectionDirectory(collectionId, false)
        // Collection doesn't exist?
        if (colDir == null) {
            logger.trace("Collection {} does not exist", collectionId)
            return false
        }

        logger.info("Cleaning up files older than {} days from collection {}", days, collectionId)

        if (!colDir.isDirectory)
            throw IllegalStateException("$colDir is not a directory")

        val referenceTime = System.currentTimeMillis() - days * 24 * 3600 * 1000
        for (f in colDir.listFiles()!!) {
            val lastModified = f.lastModified()
            logger.trace("{} last modified: {}, reference date: {}",
                    f.name, Date(lastModified), Date(referenceTime))
            if (lastModified <= referenceTime) {
                // Delete file
                deleteFromCollection(collectionId, f.name)
                logger.info("Cleaned up file {} from collection {}", f.name, collectionId)
            }
        }

        return true
    }

    /**
     * Sets the remote service manager.
     *
     * @param remoteServiceManager
     */
    fun setRemoteServiceManager(remoteServiceManager: ServiceRegistry) {
        this.remoteServiceManager = remoteServiceManager
    }

    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    companion object {
        /** The logger  */
        private val logger = LoggerFactory.getLogger(WorkingFileRepositoryImpl::class.java)
        private val log = Log(logger)

        /** The extension we use for the md5 hash calculated from the file contents  */
        val MD5_EXTENSION = ".md5"

        /** The filename filter matching .md5 files  */
        private val MD5_FINAME_FILTER = FilenameFilter { dir, name -> name.endsWith(MD5_EXTENSION) }

        /** Working file repository JMX type  */
        private val JMX_WORKING_FILE_REPOSITORY_TYPE = "WorkingFileRepository"
    }

}
