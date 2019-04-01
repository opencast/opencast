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

package org.opencastproject.workspace.impl

import java.lang.String.format
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_OK
import org.opencastproject.util.EqualsUtil.ne
import org.opencastproject.util.IoSupport.locked
import org.opencastproject.util.PathSupport.path
import org.opencastproject.util.RequireUtil.notNull
import org.opencastproject.util.data.Arrays.cons
import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.Prelude.sleep
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.assetmanager.util.AssetPathUtils
import org.opencastproject.mediapackage.identifier.Id
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.util.FileSupport
import org.opencastproject.util.HttpUtil
import org.opencastproject.util.IoSupport
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.PathSupport
import org.opencastproject.util.data.Effect
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.data.functions.Misc
import org.opencastproject.util.jmx.JmxUtil
import org.opencastproject.workingfilerepository.api.PathMappable
import org.opencastproject.workingfilerepository.api.WorkingFileRepository
import org.opencastproject.workspace.api.Workspace
import org.opencastproject.workspace.impl.jmx.WorkspaceBean

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.TeeInputStream
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Date
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

import javax.management.ObjectInstance
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.UriBuilder

/**
 * Implements a simple cache for remote URIs. Delegates methods to [WorkingFileRepository] wherever possible.
 *
 *
 * Note that if you are running the workspace on the same machine as the singleton working file repository, you can save
 * a lot of space if you configure both root directories onto the same volume (that is, if your file system supports
 * hard links).
 *
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
class WorkspaceImpl : Workspace {

    /** The JMX workspace bean  */
    private val workspaceBean = WorkspaceBean(this)

    /** The JMX bean object instance  */
    private var registeredMXBean: ObjectInstance? = null

    private val lock = Any()

    /** The workspace root directory  */
    private var wsRoot: String? = null

    /** If true, hardlinking can be done between working file repository and workspace  */
    private var linkingEnabled = false

    private var trustedHttpClient: TrustedHttpClient? = null

    private var securityService: SecurityService? = null

    /** The working file repository  */
    private var wfr: WorkingFileRepository? = null

    /** The path mappable  */
    private var pathMappable: PathMappable? = null

    private val staticCollections = CopyOnWriteArraySet<String>()

    private val waitForResourceFlag = false

    /** the asset manager directory if locally available  */
    private var assetManagerPath: String? = null

    /** The workspce cleaner  */
    private var workspaceCleaner: WorkspaceCleaner? = null

    override val totalSpace: Option<Long>
        get() = some(File(wsRoot!!).totalSpace)

    override val usableSpace: Option<Long>
        get() = some(File(wsRoot!!).usableSpace)

    override val usedSpace: Option<Long>
        get() = some(FileUtils.sizeOfDirectory(File(wsRoot!!)))

    override val baseUri: URI
        get() = wfr!!.baseUri

    constructor() {}

    /**
     * Creates a workspace implementation which is located at the given root directory.
     *
     *
     * Note that if you are running the workspace on the same machine as the singleton working file repository, you can
     * save a lot of space if you configure both root directories onto the same volume (that is, if your file system
     * supports hard links).
     *
     * @param rootDirectory
     * the repository root directory
     */
    constructor(rootDirectory: String, waitForResource: Boolean) {
        this.wsRoot = rootDirectory
        this.waitForResourceFlag = waitForResource
    }

    /**
     * Check is a property exists in a given bundle context.
     *
     * @param cc
     * the OSGi component context
     * @param prop
     * property to check for.
     */
    private fun ensureContextProp(cc: ComponentContext?, prop: String): Boolean {
        return cc != null && cc.bundleContext.getProperty(prop) != null
    }

    /**
     * OSGi service activation callback.
     *
     * @param cc
     * the OSGi component context
     */
    fun activate(cc: ComponentContext) {
        if (this.wsRoot == null) {
            if (ensureContextProp(cc, WORKSPACE_DIR_KEY)) {
                // use rootDir from CONFIG
                this.wsRoot = cc.bundleContext.getProperty(WORKSPACE_DIR_KEY)
                logger.info("CONFIG " + WORKSPACE_DIR_KEY + ": " + this.wsRoot)
            } else if (ensureContextProp(cc, STORAGE_DIR_KEY)) {
                // create rootDir by adding "workspace" to the default data directory
                this.wsRoot = PathSupport.concat(cc.bundleContext.getProperty(STORAGE_DIR_KEY), "workspace")
                logger.warn("CONFIG " + WORKSPACE_DIR_KEY + " is missing: falling back to " + this.wsRoot)
            } else {
                throw IllegalStateException("Configuration '$WORKSPACE_DIR_KEY' is missing")
            }
        }

        // Create the root directory
        val f = File(this.wsRoot!!)
        if (!f.exists()) {
            try {
                FileUtils.forceMkdir(f)
            } catch (e: Exception) {
                throw IllegalStateException("Could not create workspace directory.", e)
            }

        }

        // Test whether hard linking between working file repository and workspace is possible
        if (pathMappable != null) {
            val wfrRoot = pathMappable!!.pathPrefix
            val srcFile = File(wfrRoot, ".linktest")
            try {
                FileUtils.touch(srcFile)
            } catch (e: IOException) {
                throw IllegalStateException("The working file repository seems read-only", e)
            }

            // Create a unique target file
            val targetFile: File
            try {
                targetFile = File.createTempFile(".linktest.", ".tmp", File(wsRoot!!))
                targetFile.delete()
            } catch (e: IOException) {
                throw IllegalStateException("The workspace seems read-only", e)
            }

            // Test hard linking
            linkingEnabled = FileSupport.supportsLinking(srcFile, targetFile)

            // Clean up
            FileUtils.deleteQuietly(targetFile)

            if (linkingEnabled)
                logger.info("Hard links between the working file repository and the workspace enabled")
            else {
                logger.warn("Hard links between the working file repository and the workspace are not possible")
                logger.warn("This will increase the overall amount of disk space used")
            }
        }

        // Set up the garbage collection timer
        var garbageCollectionPeriodInSeconds = -1
        if (ensureContextProp(cc, WORKSPACE_CLEANUP_PERIOD_KEY)) {
            val period = cc.bundleContext.getProperty(WORKSPACE_CLEANUP_PERIOD_KEY)
            try {
                garbageCollectionPeriodInSeconds = Integer.parseInt(period)
            } catch (e: NumberFormatException) {
                logger.warn("Invalid configuration for workspace garbage collection period ({}={})",
                        WORKSPACE_CLEANUP_PERIOD_KEY, period)
                garbageCollectionPeriodInSeconds = -1
            }

        }

        // Activate garbage collection
        var maxAgeInSeconds = -1
        if (ensureContextProp(cc, WORKSPACE_CLEANUP_MAX_AGE_KEY)) {
            val age = cc.bundleContext.getProperty(WORKSPACE_CLEANUP_MAX_AGE_KEY)
            try {
                maxAgeInSeconds = Integer.parseInt(age)
            } catch (e: NumberFormatException) {
                logger.warn("Invalid configuration for workspace garbage collection max age ({}={})",
                        WORKSPACE_CLEANUP_MAX_AGE_KEY, age)
                maxAgeInSeconds = -1
            }

        }

        registeredMXBean = JmxUtil.registerMXBean(workspaceBean, JMX_WORKSPACE_TYPE)

        // Start cleanup scheduler if we have sensible cleanup values:
        if (garbageCollectionPeriodInSeconds > 0) {
            workspaceCleaner = WorkspaceCleaner(this, garbageCollectionPeriodInSeconds, maxAgeInSeconds)
            workspaceCleaner!!.schedule()
        }

        // Initialize the list of static collections
        // TODO MH-12440 replace with a different mechanism that doesn't hardcode collection names
        staticCollections.add("archive")
        staticCollections.add("captions")
        staticCollections.add("composer")
        staticCollections.add("composite")
        staticCollections.add("coverimage")
        staticCollections.add("executor")
        staticCollections.add("inbox")
        staticCollections.add("ocrtext")
        staticCollections.add("sox")
        staticCollections.add("uploaded")
        staticCollections.add("videoeditor")
        staticCollections.add("videosegments")
        staticCollections.add("waveform")

        // Check if we can read from the asset manager locally to avoid downloading files via HTTP
        assetManagerPath = AssetPathUtils.getAssetManagerPath(cc)
    }

    /** Callback from OSGi on service deactivation.  */
    fun deactivate() {
        JmxUtil.unregisterMXBean(registeredMXBean!!)
        if (workspaceCleaner != null) {
            workspaceCleaner!!.shutdown()
        }
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun get(uri: URI): File {
        return get(uri, false)
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun get(uri: URI, uniqueFilename: Boolean): File {
        var inWs = toWorkspaceFile(uri)

        if (uniqueFilename) {
            inWs = File(FilenameUtils.removeExtension(inWs.absolutePath) + '-'.toString() + UUID.randomUUID() + '.'.toString()
                    + FilenameUtils.getExtension(inWs.name))
            logger.debug("Created unique filename: {}", inWs)
        }

        if (pathMappable != null && StringUtils.isNotBlank(pathMappable!!.pathPrefix)
                && StringUtils.isNotBlank(pathMappable!!.urlPrefix)) {
            if (uri.toString().startsWith(pathMappable!!.urlPrefix)) {
                val localPath = uri.toString().substring(pathMappable!!.urlPrefix.length)
                val wfrCopy = workingFileRepositoryFile(localPath)
                // does the file exist and is it up to date?
                logger.trace("Looking up {} at {}", uri.toString(), wfrCopy.absolutePath)
                if (wfrCopy.isFile) {
                    val workspaceFileLastModified = if (inWs.isFile) inWs.lastModified() else 0L
                    // if the file exists in the workspace, but is older than the wfr copy, replace it
                    if (workspaceFileLastModified < wfrCopy.lastModified()) {
                        logger.debug("Replacing {} with an updated version from the file repository", inWs.absolutePath)
                        locked(inWs, copyOrLink(wfrCopy))
                    } else {
                        logger.debug("{} is up to date", inWs)
                    }
                    logger.debug("Getting {} directly from working file repository root at {}", uri, inWs)
                    return File(inWs.absolutePath)
                } else {
                    logger.warn("The working file repository and workspace paths don't match. Looking up {} at {} failed",
                            uri.toString(), wfrCopy.absolutePath)
                }
            }
        }

        // Check if we can get the files directly from the asset manager
        val asset = AssetPathUtils.getLocalFile(assetManagerPath, securityService!!.organization.id, uri)
        if (asset != null) {
            logger.debug("Copy local file {} from asset manager to workspace", asset)
            Files.copy(asset.toPath(), inWs.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return File(inWs.absolutePath)
        }

        // do HTTP transfer
        return locked(inWs, downloadIfNecessary(uri))
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun read(uri: URI): InputStream {

        // Check if we can get the file from the working file repository directly
        if (pathMappable != null) {
            if (uri.toString().startsWith(pathMappable!!.urlPrefix)) {
                val localPath = uri.toString().substring(pathMappable!!.urlPrefix.length)
                val wfrCopy = workingFileRepositoryFile(localPath)
                // does the file exist?
                logger.trace("Looking up {} at {} for read", uri, wfrCopy)
                if (wfrCopy.isFile) {
                    logger.debug("Getting {} directly from working file repository root at {} for read", uri, wfrCopy)
                    return FileInputStream(wfrCopy)
                }
                logger.warn("The working file repository URI and paths don't match. Looking up {} at {} failed", uri, wfrCopy)
            }
        }

        // Check if we can get the files directly from the asset manager
        val asset = AssetPathUtils.getLocalFile(assetManagerPath, securityService!!.organization.id, uri)
        return asset?.let { FileInputStream(it) } ?: DeleteOnCloseFileInputStream(get(uri, true))

        // fall back to get() which should download the file into local workspace if necessary
    }

    /** Copy or link `src` to `dst`.  */
    @Throws(IOException::class)
    private fun copyOrLink(src: File, dst: File) {
        if (linkingEnabled) {
            FileUtils.deleteQuietly(dst)
            FileSupport.link(src, dst)
        } else {
            FileSupport.copy(src, dst)
        }
    }

    /** [.copyOrLink] as an effect. `src -> dst -> ()`  */
    private fun copyOrLink(src: File): Effect<File> {
        return object : Effect.X<File>() {
            @Throws(IOException::class)
            override fun xrun(dst: File) {
                copyOrLink(src, dst)
            }
        }
    }

    /**
     * Handle the HTTP response.
     *
     * @return either a token to initiate a follow-up request or a file or none if the requested URI cannot be found
     * @throws IOException
     * in case of any IO related issues
     */
    @Throws(IOException::class)
    private fun handleDownloadResponse(response: HttpResponse, src: URI, dst: File): Either<String, Option<File>> {
        val url = src.toString()
        val status = response.statusLine.statusCode
        when (status) {
            HttpServletResponse.SC_NOT_FOUND -> return right(none(File::class.java))
            HttpServletResponse.SC_NOT_MODIFIED -> {
                logger.debug("{} has not been modified.", url)
                return right(some(dst))
            }
            HttpServletResponse.SC_ACCEPTED -> {
                logger.debug("{} is not ready, try again later.", url)
                return left(response.getHeaders("token")[0].value)
            }
            HttpServletResponse.SC_OK -> {
                logger.debug("Downloading {} to {}", url, dst.absolutePath)
                return right(some(downloadTo(response, dst)))
            }
            else -> {
                logger.warn("Received unexpected response status {} while trying to download from {}", status, url)
                FileUtils.deleteQuietly(dst)
                return right(none(File::class.java))
            }
        }
    }

    /**
     * [.handleDownloadResponse] as a function.
     * `(URI, dst_file) -> HttpResponse -> Either token (Option File)`
     */
    private fun handleDownloadResponse(src: URI, dst: File): Function<HttpResponse, Either<String, Option<File>>> {
        return object : Function.X<HttpResponse, Either<String, Option<File>>>() {
            @Throws(Exception::class)
            public override fun xapply(response: HttpResponse): Either<String, Option<File>> {
                return handleDownloadResponse(response, src, dst)
            }
        }
    }

    /** Create a get request to the given URI.  */
    @Throws(IOException::class)
    private fun createGetRequest(src: URI, dst: File, vararg params: Tuple<String, String>): HttpGet {
        try {
            val builder = URIBuilder(src.toString())
            for (a in params) {
                builder.setParameter(a.a, a.b)
            }
            val get = HttpGet(builder.build())
            // if the destination file already exists add the If-None-Match header
            if (dst.isFile && dst.length() > 0) {
                get.setHeader("If-None-Match", md5(dst))
            }
            return get
        } catch (e: URISyntaxException) {
            throw IOException(e)
        }

    }

    /**
     * Download content of `uri` to file `dst` only if necessary, i.e. either the file does not yet
     * exist in the workspace or a newer version is available at `uri`.
     *
     * @return the file
     */
    @Throws(IOException::class, NotFoundException::class)
    private fun downloadIfNecessary(src: URI, dst: File): File {
        var get = createGetRequest(src, dst)
        while (true) {
            // run the http request and handle its response
            val result = trustedHttpClient!!
                    .runner<Either<String, Option<File>>>(get).run(handleDownloadResponse(src, dst))
            // handle to result of response processing
            // right: there's an expected result
            for (a in result.right()) {
                // right: either a file could be found or not
                for (ff in a.right()) {
                    for (f in ff) {
                        return f
                    }
                    FileUtils.deleteQuietly(dst)
                    // none
                    throw NotFoundException()
                }
                // left: file will be ready later
                for (token in a.left()) {
                    get = createGetRequest(src, dst, tuple("token", token))
                    sleep(60000)
                }
            }
            // left: an exception occurred
            for (e in result.left()) {
                logger.warn(format("Could not copy %s to %s: %s", src.toString(), dst.absolutePath, e.message))
                FileUtils.deleteQuietly(dst)
                throw NotFoundException(e)
            }
        }
    }

    /**
     * [.downloadIfNecessary] as a function.
     * `src_uri -> dst_file -> dst_file`
     */
    private fun downloadIfNecessary(src: URI): Function<File, File> {
        return object : Function.X<File, File>() {
            @Throws(Exception::class)
            public override fun xapply(dst: File): File {
                return downloadIfNecessary(src, dst)
            }
        }
    }

    /**
     * Returns the md5 of a file
     *
     * @param file
     * the source file
     * @return the md5 hash
     * @throws IOException
     * if the file cannot be accessed
     * @throws IllegalArgumentException
     * if `file` is `null`
     * @throws IllegalStateException
     * if `file` does not exist or is not a regular file
     */
    @Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
    protected fun md5(file: File?): String {
        if (file == null)
            throw IllegalArgumentException("File must not be null")
        if (!file.isFile)
            throw IllegalArgumentException("File " + file.absolutePath + " can not be read")

        FileInputStream(file).use { `in` -> return DigestUtils.md5Hex(`in`) }
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun delete(uri: URI) {

        val uriPath = uri.toString()
        val uriElements = uriPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var collectionId: String? = null
        var isMediaPackage = false

        logger.trace("delete {}", uriPath)

        if (uriPath.startsWith(wfr!!.baseUri.toString())) {
            if (uriPath.indexOf(WorkingFileRepository.COLLECTION_PATH_PREFIX) > 0) {
                if (uriElements.size > 2) {
                    collectionId = uriElements[uriElements.size - 2]
                    val filename = uriElements[uriElements.size - 1]
                    wfr!!.deleteFromCollection(collectionId, filename)
                }
            } else if (uriPath.indexOf(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX) > 0) {
                isMediaPackage = true
                if (uriElements.size >= 3) {
                    val mediaPackageId = uriElements[uriElements.size - 3]
                    val elementId = uriElements[uriElements.size - 2]
                    wfr!!.delete(mediaPackageId, elementId)
                }
            }
        }

        // Remove the file and optionally its parent directory if empty
        val f = toWorkspaceFile(uri)
        if (f.isFile) {
            synchronized(lock) {
                val mpElementDir = f.parentFile
                FileUtils.forceDelete(f)

                // Remove containing folder if a mediapackage element or a not a static collection
                if (isMediaPackage || !isStaticCollection(collectionId))
                    FileSupport.delete(mpElementDir)

                // Also delete mediapackage itself when empty
                if (isMediaPackage)
                    FileSupport.delete(mpElementDir.parentFile)
            }
        }

        // wait for WFR
        waitForResource(uri, HttpServletResponse.SC_NOT_FOUND, "File %s does not disappear in WFR")
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun delete(mediaPackageID: String, mediaPackageElementID: String) {
        // delete locally
        val f = workspaceFile(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID)
        FileUtils.deleteQuietly(f)
        FileSupport.delete(f.parentFile)
        // delete in WFR
        wfr!!.delete(mediaPackageID, mediaPackageElementID)
        // todo check in WFR
    }

    @Throws(IOException::class)
    override fun put(mediaPackageID: String, mediaPackageElementID: String, fileName: String, `in`: InputStream): URI {
        val safeFileName = PathSupport.toSafeName(fileName)
        val uri = wfr!!.getURI(mediaPackageID, mediaPackageElementID, fileName)
        notNull(`in`, "in")

        // Determine the target location in the workspace
        var workspaceFile: File? = null
        synchronized(lock) {
            workspaceFile = toWorkspaceFile(uri)
            FileUtils.touch(workspaceFile!!)
        }

        // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
        if (linkingEnabled) {
            // The WFR stores an md5 hash along with the file, so we need to use the API and not try to write (link) the file
            // there ourselves
            wfr!!.put(mediaPackageID, mediaPackageElementID, fileName, `in`)
            val workingFileRepoDirectory = workingFileRepositoryFile(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
                    mediaPackageID, mediaPackageElementID)
            val workingFileRepoCopy = File(workingFileRepoDirectory, safeFileName)
            FileSupport.link(workingFileRepoCopy, workspaceFile!!, true)
        } else {
            FileOutputStream(workspaceFile!!).use { out -> TeeInputStream(`in`, out, true).use { tee -> wfr!!.put(mediaPackageID, mediaPackageElementID, fileName, tee) } }
        }
        // wait until the file appears on the WFR node
        waitForResource(uri, HttpServletResponse.SC_OK, "File %s does not appear in WFR")
        return uri
    }

    @Throws(IOException::class)
    override fun putInCollection(collectionId: String, fileName: String, `in`: InputStream): URI {
        val safeFileName = PathSupport.toSafeName(fileName)
        val uri = wfr!!.getCollectionURI(collectionId, fileName)

        // Determine the target location in the workspace
        var tee: InputStream? = null
        var tempFile: File? = null
        var out: FileOutputStream? = null
        try {
            synchronized(lock) {
                tempFile = toWorkspaceFile(uri)
                FileUtils.touch(tempFile!!)
                out = FileOutputStream(tempFile!!)
            }

            // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
            if (linkingEnabled) {
                tee = `in`
                wfr!!.putInCollection(collectionId, fileName, tee)
                FileUtils.forceMkdir(tempFile!!.parentFile)
                val workingFileRepoDirectory = workingFileRepositoryFile(WorkingFileRepository.COLLECTION_PATH_PREFIX,
                        collectionId)
                val workingFileRepoCopy = File(workingFileRepoDirectory, safeFileName)
                FileSupport.link(workingFileRepoCopy, tempFile!!, true)
            } else {
                tee = TeeInputStream(`in`, out, true)
                wfr!!.putInCollection(collectionId, fileName, tee)
            }
        } catch (e: IOException) {
            FileUtils.deleteQuietly(tempFile)
            throw e
        } finally {
            IoSupport.closeQuietly(tee)
            IoSupport.closeQuietly(out)
        }
        waitForResource(uri, HttpServletResponse.SC_OK, "File %s does not appear in WFR")
        return uri
    }

    override fun getURI(mediaPackageID: String, mediaPackageElementID: String): URI {
        return wfr!!.getURI(mediaPackageID, mediaPackageElementID)
    }

    override fun getURI(mediaPackageID: String, mediaPackageElementID: String, filename: String): URI {
        return wfr!!.getURI(mediaPackageID, mediaPackageElementID, filename)
    }

    override fun getCollectionURI(collectionID: String, fileName: String): URI {
        return wfr!!.getCollectionURI(collectionID, fileName)
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun copyTo(collectionURI: URI, toMediaPackage: String, toMediaPackageElement: String, toFileName: String): URI {
        val path = collectionURI.toString()
        val filename = FilenameUtils.getName(path)
        val collection = getCollection(collectionURI)

        // Copy the local file
        val original = toWorkspaceFile(collectionURI)
        if (original.isFile) {
            val copyURI = wfr!!.getURI(toMediaPackage, toMediaPackageElement, filename)
            val copy = toWorkspaceFile(copyURI)
            FileUtils.forceMkdir(copy.parentFile)
            FileSupport.link(original, copy)
        }

        // Tell working file repository
        val wfrUri = wfr!!.copyTo(collection, filename, toMediaPackage, toMediaPackageElement, toFileName)
        // wait for WFR
        waitForResource(wfrUri, SC_OK, "File %s does not appear in WFR")
        return wfrUri
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun moveTo(collectionURI: URI, toMediaPackage: String, toMediaPackageElement: String, toFileName: String): URI {
        val path = collectionURI.toString()
        val filename = FilenameUtils.getName(path)
        val collection = getCollection(collectionURI)
        logger.debug("Moving {} from {} to {}/{}", filename, collection, toMediaPackage, toMediaPackageElement)
        // move locally
        val original = toWorkspaceFile(collectionURI)
        if (original.isFile) {
            val copyURI = wfr!!.getURI(toMediaPackage, toMediaPackageElement, toFileName)
            val copy = toWorkspaceFile(copyURI)
            FileUtils.forceMkdir(copy.parentFile)
            FileUtils.deleteQuietly(copy)
            FileUtils.moveFile(original, copy)
            if (!isStaticCollection(collection))
                FileSupport.delete(original.parentFile)
        }
        // move in WFR
        val wfrUri = wfr!!.moveTo(collection, filename, toMediaPackage, toMediaPackageElement, toFileName)
        // wait for WFR
        waitForResource(wfrUri, SC_OK, "File %s does not appear in WFR")
        return wfrUri
    }

    @Throws(NotFoundException::class)
    override fun getCollectionContents(collectionId: String): Array<URI> {
        return wfr!!.getCollectionContents(collectionId)
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun deleteFromCollection(collectionId: String, fileName: String, removeCollection: Boolean) {
        // local delete
        val f = workspaceFile(WorkingFileRepository.COLLECTION_PATH_PREFIX, collectionId,
                PathSupport.toSafeName(fileName))
        FileUtils.deleteQuietly(f)
        if (removeCollection) {
            FileSupport.delete(f.parentFile)
        }
        // delete in WFR
        try {
            wfr!!.deleteFromCollection(collectionId, fileName, removeCollection)
        } catch (e: IllegalArgumentException) {
            throw NotFoundException(e)
        }

        // wait for WFR
        waitForResource(wfr!!.getCollectionURI(collectionId, fileName), SC_NOT_FOUND, "File %s does not disappear in WFR")
    }

    @Throws(NotFoundException::class, IOException::class)
    override fun deleteFromCollection(collectionId: String, fileName: String) {
        deleteFromCollection(collectionId, fileName, false)
    }

    /**
     * Transforms a URI into a workspace File. If the file comes from the working file repository, the path in the
     * workspace mirrors that of the repository. If the file comes from another source, directories are created for each
     * segment of the URL. Sub-directories may be created as needed.
     *
     * @param uri
     * the uri
     * @return the local file representation
     */
    internal fun toWorkspaceFile(uri: URI): File {
        // MH-11497: Fix for compatibility with stream security: the query parameters are deleted.
        // TODO Refactor this class to use the URI class and methods instead of String for handling URIs
        val uriString = UriBuilder.fromUri(uri).replaceQuery(null).build().toString()
        val wfrPrefix = wfr!!.baseUri.toString()
        var serverPath = FilenameUtils.getPath(uriString)
        if (uriString.startsWith(wfrPrefix)) {
            serverPath = serverPath.substring(wfrPrefix.length)
        } else {
            serverPath = serverPath.replace(":/*".toRegex(), "_")
        }
        val wsDirectoryPath = PathSupport.concat(wsRoot, serverPath)
        val wsDirectory = File(wsDirectoryPath)
        wsDirectory.mkdirs()

        var safeFileName = PathSupport.toSafeName(FilenameUtils.getName(uriString))
        if (StringUtils.isBlank(safeFileName))
            safeFileName = UNKNOWN_FILENAME
        return File(wsDirectory, safeFileName)
    }

    /** Return a file object pointing into the workspace.  */
    private fun workspaceFile(vararg path: String): File {
        return File(path(cons(String::class.java, wsRoot, path)))
    }

    /** Return a file object pointing into the working file repository.  */
    private fun workingFileRepositoryFile(vararg path: String): File {
        return File(path(cons(String::class.java, pathMappable!!.pathPrefix, path)))
    }

    /**
     * Returns the working file repository collection.
     *
     *
     *
     * <pre>
     * http://localhost:8080/files/collection/&lt;collection&gt;/ -> &lt;collection&gt;
    </pre> *
     *
     * @param uri
     * the working file repository collection uri
     * @return the collection name
     */
    private fun getCollection(uri: URI): String {
        val path = uri.toString()
        if (path.indexOf(WorkingFileRepository.COLLECTION_PATH_PREFIX) < 0)
            throw IllegalArgumentException("$uri must point to a working file repository collection")

        var collection = FilenameUtils.getPath(path)
        if (collection.endsWith("/"))
            collection = collection.substring(0, collection.length - 1)
        collection = collection.substring(collection.lastIndexOf("/"))
        collection = collection.substring(collection.lastIndexOf("/") + 1, collection.length)
        return collection
    }

    private fun isStaticCollection(collection: String?): Boolean {
        return staticCollections.contains(collection)
    }

    fun setRepository(repo: WorkingFileRepository) {
        this.wfr = repo
        if (repo is PathMappable) {
            this.pathMappable = repo
            logger.info("Mapping workspace to working file repository using {}", pathMappable!!.pathPrefix)
        }
    }

    fun setTrustedHttpClient(trustedHttpClient: TrustedHttpClient) {
        this.trustedHttpClient = trustedHttpClient
    }

    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    @Throws(IOException::class)
    private fun waitForResource(uri: URI, expectedStatus: Int, errorMsg: String) {
        if (waitForResourceFlag) {
            HttpUtil.waitForResource(trustedHttpClient!!, uri, expectedStatus, TIMEOUT, INTERVAL)
                    .fold(Misc.chuck(), object : Effect.X<Int>() {
                        @Throws(Exception::class)
                        override fun xrun(status: Int?) {
                            if (ne(status, expectedStatus)) {
                                val msg = format(errorMsg, uri.toString())
                                logger.warn(msg)
                                throw IOException(msg)
                            }
                        }
                    })
        }
    }

    override fun cleanup(maxAgeInSeconds: Int) {
        // Cancel cleanup if we do not have a valid setting for the maximum file age
        if (maxAgeInSeconds < 0) {
            logger.debug("Canceling cleanup of workspace due to maxAge ({}) <= 0", maxAgeInSeconds)
            return
        }

        // Warn if time is very short since this operation is dangerous and *should* only be a fallback for if stuff
        // remained in the workspace due to some errors. If we have a very short maxAge, we may delete file which are
        // currently being processed. The warn value is 2 days:
        if (maxAgeInSeconds < 60 * 60 * 24 * 2) {
            logger.warn("The max age for the workspace cleaner is dangerously low. Please consider increasing the value to " + "avoid deleting data in use by running workflows.")
        }

        // Get workspace root directly
        val workspaceDirectory = File(wsRoot!!)
        logger.info("Starting cleanup of workspace at {}", workspaceDirectory)

        val now = Date().time
        for (file in FileUtils.listFiles(workspaceDirectory, null, true)) {
            val fileLastModified = file.lastModified()
            // Ensure file/dir is older than maxAge
            val fileAgeInSeconds = (now - fileLastModified) / 1000
            if (fileLastModified == 0L || fileAgeInSeconds < maxAgeInSeconds) {
                logger.debug("File age ({}) < max age ({}) or unknown: Skipping {} ", fileAgeInSeconds, maxAgeInSeconds, file)
                continue
            }

            // Delete old files
            if (FileUtils.deleteQuietly(file)) {
                logger.info("Deleted {}", file)
            } else {
                logger.warn("Could not delete {}", file)
            }
        }
        logger.info("Finished cleanup of workspace")
    }

    @Throws(IOException::class)
    override fun cleanup(mediaPackageId: Id) {
        cleanup(mediaPackageId, false)
    }

    @Throws(IOException::class)
    override fun cleanup(mediaPackageId: Id, filesOnly: Boolean) {
        val mediaPackageDir = workspaceFile(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, mediaPackageId.toString())

        if (filesOnly) {
            logger.debug("Clean workspace media package directory {} (files only)", mediaPackageDir)
            FileSupport.delete(mediaPackageDir, FileSupport.DELETE_FILES)
        } else {
            logger.debug("Clean workspace media package directory {}", mediaPackageDir)
            FileUtils.deleteDirectory(mediaPackageDir)
        }
    }

    override fun rootDirectory(): String {
        return wsRoot
    }

    private inner class DeleteOnCloseFileInputStream @Throws(FileNotFoundException::class)
    internal constructor(private var file: File?) : FileInputStream(file) {

        @Throws(IOException::class)
        override fun close() {
            try {
                super.close()
            } finally {
                if (file != null) {
                    logger.debug("Cleaning up {}", file)
                    file!!.delete()
                    file = null
                }
            }
        }
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(WorkspaceImpl::class.java)

        /** Configuration key for the workspace root directory  */
        val WORKSPACE_DIR_KEY = "org.opencastproject.workspace.rootdir"
        /** Configuration key for the storage directory  */
        val STORAGE_DIR_KEY = "org.opencastproject.storage.dir"
        /** Configuration key for garbage collection period.  */
        val WORKSPACE_CLEANUP_PERIOD_KEY = "org.opencastproject.workspace.cleanup.period"
        /** Configuration key for garbage collection max age.  */
        val WORKSPACE_CLEANUP_MAX_AGE_KEY = "org.opencastproject.workspace.cleanup.max.age"

        /** Workspace JMX type  */
        private val JMX_WORKSPACE_TYPE = "Workspace"

        /** Unknown file name string  */
        private val UNKNOWN_FILENAME = "unknown"

        /**
         * Download content of an HTTP response to a file.
         *
         * @return the destination file
         */
        @Throws(IOException::class)
        private fun downloadTo(response: HttpResponse, dst: File): File {
            // ignore return value
            dst.createNewFile()
            response.entity.content.use { `in` -> FileOutputStream(dst).use { out -> IOUtils.copyLarge(`in`, out) } }
            return dst
        }

        private val TIMEOUT = 2L * 60L * 1000L
        private val INTERVAL = 1000L
    }
}
