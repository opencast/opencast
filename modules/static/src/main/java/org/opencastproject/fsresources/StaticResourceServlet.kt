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

package org.opencastproject.fsresources

import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.MimeTypes

import org.apache.commons.lang3.StringUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.StringTokenizer
import java.util.zip.CRC32

import javax.servlet.ServletException
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Serves static content from a configured path on the filesystem. In production systems, this should be replaced with
 * apache httpd or another web server optimized for serving static content.
 */
/**
 * No-arg constructor
 */
class StaticResourceServlet : HttpServlet() {

    /** The filesystem directory to serve files fro  */
    protected var distributionDirectory: String? = null

    /**
     * OSGI Activation callback
     *
     * @param cc
     * the component context
     */
    fun activate(cc: ComponentContext?) {
        if (cc != null) {
            val ccDistributionDirectory = cc.bundleContext.getProperty("org.opencastproject.download.directory")
            if (StringUtils.isNotEmpty(ccDistributionDirectory)) {
                this.distributionDirectory = ccDistributionDirectory
            } else {
                val storageDir = cc.bundleContext.getProperty("org.opencastproject.storage.dir")
                if (StringUtils.isNotEmpty(storageDir)) {
                    this.distributionDirectory = File(storageDir, "downloads").path
                }
            }
        }

        if (distributionDirectory == null) {
            throw ConfigurationException("Distribution directory not set")
        }
        logger.info("Serving static files from '{}'", distributionDirectory)
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.servlet.http.HttpServlet.doGet
     */
    @Throws(ServletException::class, IOException::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        logger.debug("Looking for static resource '{}'", req.requestURI)
        val path = req.pathInfo
        if (path == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN)
            return
        }

        var normalized: String? = path.trim { it <= ' ' }.replace("/+".toRegex(), "/").replace("\\.\\.".toRegex(), "")
        if (normalized != null && normalized.startsWith("/") && normalized.length > 1) {
            normalized = normalized.substring(1)
        }

        val f = File(distributionDirectory, normalized!!)
        var eTag: String? = null
        if (f.isFile && f.canRead()) {
            logger.debug("Serving static resource '{}'", f.absolutePath)
            eTag = computeEtag(f)
            if (eTag == req.getHeader("If-None-Match")) {
                resp.status = 304
                return
            }
            resp.setHeader("ETag", eTag)
            val contentType = MimeTypes.getMimeType(normalized)
            if (MimeTypes.DEFAULT_TYPE != contentType) {
                resp.contentType = contentType
            }
            resp.setHeader("Content-Length", java.lang.Long.toString(f.length()))
            resp.setDateHeader("Last-Modified", f.lastModified())

            resp.setHeader("Accept-Ranges", "bytes")
            val ranges = parseRange(req, resp, eTag, f.lastModified(), f.length())

            if ((ranges == null || ranges.isEmpty()) && req.getHeader("Range") == null || ranges === FULL_RANGE) {
                val e = copyRange(FileInputStream(f), resp.outputStream, 0, f.length())
                if (e != null) {
                    try {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                        return
                    } catch (e1: IOException) {
                        logger.warn("unable to send http 500 error: {}", e1)
                        return
                    } catch (e2: IllegalStateException) {
                        logger.trace("unable to send http 500 error. Client side was probably closed during file copy.", e2)
                        return
                    }

                }
            } else {
                if (ranges == null || ranges.isEmpty()) {
                    return
                }
                if (ranges.size == 1) {
                    val range = ranges[0]
                    resp.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length)
                    val length = range.end - range.start + 1
                    if (length < Integer.MAX_VALUE) {
                        resp.setContentLength(length.toInt())
                    } else {
                        // Set the content-length as String to be able to use a long
                        resp.setHeader("content-length", "" + length)
                    }
                    try {
                        resp.bufferSize = 2048
                    } catch (e: IllegalStateException) {
                        logger.debug(e.message, e)
                    }

                    resp.status = HttpServletResponse.SC_PARTIAL_CONTENT
                    val e = copyRange(FileInputStream(f), resp.outputStream, range.start, range.end)
                    if (e != null) {
                        try {
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                            return
                        } catch (e1: IOException) {
                            logger.warn("unable to send http 500 error: {}", e1)
                            return
                        } catch (e2: IllegalStateException) {
                            logger.trace("unable to send http 500 error. Client side was probably closed during file copy.", e2)
                            return
                        }

                    }
                } else {
                    resp.status = HttpServletResponse.SC_PARTIAL_CONTENT
                    resp.contentType = "multipart/byteranges; boundary=$mimeSeparation"
                    try {
                        resp.bufferSize = 2048
                    } catch (e: IllegalStateException) {
                        logger.debug(e.message, e)
                    }

                    copy(f, resp.outputStream, ranges.iterator(), contentType)
                }
            }
        } else {
            logger.debug("unable to find file '{}', returning HTTP 404")
            resp.sendError(HttpServletResponse.SC_NOT_FOUND)
        }
    }

    /**
     * Computes an etag for a file using the filename, last modified, and length of the file.
     *
     * @param file
     * the file
     * @return the etag
     */
    protected fun computeEtag(file: File): String {
        val crc = CRC32()
        crc.update(file.name.toByteArray())
        checksum(file.lastModified(), crc)
        checksum(file.length(), crc)
        return java.lang.Long.toString(crc.value)
    }

    @Throws(IOException::class)
    protected fun copy(f: File, out: ServletOutputStream, ranges: Iterator<Range>, contentType: String?) {
        var exception: IOException? = null
        while (exception == null && ranges.hasNext()) {
            val currentRange = ranges.next()
            // Writing MIME header.
            out.println()
            out.println("--$mimeSeparation")
            if (contentType != null) {
                out.println("Content-Type: $contentType")
            }
            out.println("Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/" + currentRange.length)
            out.println()

            // Printing content
            val `in` = FileInputStream(f)
            exception = copyRange(`in`, out, currentRange.start, currentRange.end)
            `in`.close()
        }
        out.println()
        out.print("--$mimeSeparation--")
        // Rethrow any exception that has occurred
        if (exception != null) {
            throw exception
        }
    }

    /**
     * Parse the range header.
     *
     * @param req
     * The servlet request we are processing
     * @param response
     * The servlet response we are creating
     * @return Vector of ranges
     */
    @Throws(IOException::class)
    protected fun parseRange(req: HttpServletRequest, response: HttpServletResponse, eTag: String,
                             lastModified: Long, fileLength: Long): ArrayList<Range>? {

        // Checking If-Range
        val headerValue = req.getHeader("If-Range")
        if (headerValue != null) {
            var headerValueTime = -1L
            try {
                headerValueTime = req.getDateHeader("If-Range")
            } catch (e: IllegalArgumentException) {
                logger.debug(e.message, e)
            }

            if (headerValueTime == -1L) {
                // If the ETag the client gave does not match the entity
                // etag, then the entire entity is returned.
                if (eTag != headerValue.trim { it <= ' ' }) {
                    return FULL_RANGE
                }
            } else {
                // If the timestamp of the entity the client got is older than
                // the last modification date of the entity, the entire entity
                // is returned.
                if (lastModified > headerValueTime + 1000) {
                    return FULL_RANGE
                }
            }
        }

        if (fileLength == 0L) {
            return null
        }

        // Retrieving the range header (if any is specified
        var rangeHeader: String? = req.getHeader("Range") ?: return null

// bytes is the only range unit supported (and I don't see the point
        // of adding new ones).
        if (!rangeHeader!!.startsWith("bytes")) {
            response.addHeader("Content-Range", "bytes */$fileLength")
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
            return null
        }

        rangeHeader = rangeHeader.substring(6)

        // Vector which will contain all the ranges which are successfully
        // parsed.
        val result = ArrayList<Range>()
        val commaTokenizer = StringTokenizer(rangeHeader, ",")

        // Parsing the range list
        while (commaTokenizer.hasMoreTokens()) {
            val rangeDefinition = commaTokenizer.nextToken().trim { it <= ' ' }
            val currentRange = Range()
            currentRange.length = fileLength
            val dashPos = rangeDefinition.indexOf('-')
            if (dashPos == -1) {
                response.addHeader("Content-Range", "bytes */$fileLength")
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
                return null
            }
            if (dashPos == 0) {
                try {
                    val offset = java.lang.Long.parseLong(rangeDefinition)
                    currentRange.start = fileLength + offset
                    currentRange.end = fileLength - 1
                } catch (e: NumberFormatException) {
                    response.addHeader("Content-Range", "bytes */$fileLength")
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
                    return null
                }

            } else {
                try {
                    currentRange.start = java.lang.Long.parseLong(rangeDefinition.substring(0, dashPos))
                    if (dashPos < rangeDefinition.length - 1) {
                        currentRange.end = java.lang.Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length))
                    } else {
                        currentRange.end = fileLength - 1
                    }
                } catch (e: NumberFormatException) {
                    response.addHeader("Content-Range", "bytes */$fileLength")
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
                    return null
                }

            }
            if (!currentRange.validate()) {
                response.addHeader("Content-Range", "bytes */$fileLength")
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
                return null
            }
            result.add(currentRange)
        }
        return result
    }

    /**
     * Copy the contents of the specified input stream to the specified output stream, and ensure that both streams are
     * closed before returning (even in the face of an exception).
     *
     * @param istream
     * The input stream to read from
     * @param ostream
     * The output stream to write to
     * @param start
     * Start of the range which will be copied
     * @param end
     * End of the range which will be copied
     * @return Exception which occurred during processing
     */
    fun copyRange(istream: InputStream, ostream: ServletOutputStream, start: Long, end: Long): IOException? {
        logger.debug("Serving bytes:{}-{}", start, end)
        try {
            istream.skip(start)
        } catch (e: IOException) {
            logger.trace("Cannot skip to input stream position {}. The user probably closed the client side.", start, e)
            return e
        }

        // MH-10447, fix for files of size 2048*C bytes
        var bytesToRead = end - start + 1
        val buffer = ByteArray(2048)
        var len = buffer.size
        try {
            len = bytesToRead.toInt() % buffer.size
            if (len > 0) {
                len = istream.read(buffer, 0, len)
                if (len > 0) {
                    // This test could actually be "if (len != -1)"
                    ostream.write(buffer, 0, len)
                    bytesToRead -= len.toLong()
                    if (bytesToRead == 0L)
                        return null
                } else
                    return null
            }

            len = istream.read(buffer)
            while (len > 0) {
                ostream.write(buffer, 0, len)
                bytesToRead -= len.toLong()
                if (bytesToRead < 1)
                    break
                len = istream.read(buffer)
            }
        } catch (e: IOException) {
            logger.trace("IOException after starting the byte copy, current length {}, buffer {}." + " The user probably closed the client side after the file started copying.",
                    len, buffer, e)
            return e
        }

        return null
    }

    protected inner class Range {

        var start: Long = 0
        var end: Long = 0
        var length: Long = 0

        /**
         * Validate range.
         */
        fun validate(): Boolean {
            if (end >= length) {
                end = length - 1
            }
            return start >= 0 && end >= 0 && start <= end && length > 0
        }

        fun recycle() {
            start = 0
            end = 0
            length = 0
        }
    }

    companion object {

        /** The serialization UID  */
        private val serialVersionUID = 1L
        /** Full range marker.  */
        private val FULL_RANGE: ArrayList<Range>
        /** The logger  */
        private val logger = LoggerFactory.getLogger(StaticResourceServlet::class.java)

        /** static initializer  */
        init {
            FULL_RANGE = ArrayList()
        }

        private fun checksum(l: Long, crc: CRC32) {
            var l = l
            for (i in 0..7) {
                crc.update((l and 0x000000ff).toInt())
                l = l shr 8
            }
        }

        /**
         * MIME multipart separation string
         */
        protected val mimeSeparation = "MATTERHORN_MIME_BOUNDARY"
    }
}
