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

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter

/**
 * Helper class to handle Runtime.exec() output.
 *
 */
@Deprecated("use {@link StreamConsumer} instead")
open class StreamHelper
/**
 * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
 * to the given buffer and also redirected to the provided output stream.
 *
 * @param inputStream
 * the input stream to read from
 * @param redirect
 * a stream to also redirect the captured output to
 * @param processLogger
 * the logger to append to
 * @param contentBuffer
 * the buffer to write the captured output to
 */
@JvmOverloads constructor(
        /** The input stream  */
        private val inputStream: InputStream,
        /** The output stream  */
        private val outputStream: OutputStream? = null, processLogger: Logger? = null, contentBuffer: StringBuffer? = null) : Thread() {

    /** The content buffer  */
    var contentBuffer: StringBuffer? = null

    /** the output writer  */
    protected var writer: PrintWriter? = null

    /** Append messages to this logger  */
    protected var processLogger: Logger? = null

    /** True to keep reading the streams  */
    protected var keepReading = true

    /**
     * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
     * to the given buffer.
     *
     * @param inputStream
     * the input stream to read from
     * @param contentBuffer
     * the buffer to write the captured output to
     */
    constructor(inputStream: InputStream, contentBuffer: StringBuffer) : this(inputStream, null, null, contentBuffer) {}

    /**
     * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
     * to the given buffer.
     *
     * @param inputStream
     * the input stream to read from
     * @param processLogger
     * the logger to append to
     * @param contentBuffer
     * the buffer to write the captured output to
     */
    constructor(inputStream: InputStream, processLogger: Logger, contentBuffer: StringBuffer) : this(inputStream, null, processLogger, contentBuffer) {}

    /**
     * Creates a new stream helper and immediately starts capturing output from the given stream. Output will be captured
     * to the given buffer and also redirected to the provided output stream.
     *
     * @param inputStream
     * the input stream to read from
     * @param redirect
     * a stream to also redirect the captured output to
     * @param contentBuffer
     * the buffer to write the captured output to
     */
    constructor(inputStream: InputStream, redirect: OutputStream, contentBuffer: StringBuffer) : this(inputStream, redirect, null, contentBuffer) {}

    init {
        this.processLogger = processLogger
        this.contentBuffer = contentBuffer
        start()
    }

    /**
     * Tells the stream helper to stop reading and exit from the main loop, it then waits for the thread to die.
     *
     * @see Thread.join
     * @throws InterruptedException
     * if the thread is interrupted while waiting for the main loop to come to an end
     */
    @Throws(InterruptedException::class)
    fun stopReading() {
        keepReading = false
        this.join()
    }

    /**
     * Thread run
     */
    override fun run() {
        var reader: BufferedReader? = null
        try {
            if (outputStream != null) {
                writer = PrintWriter(outputStream)
            }
            reader = BufferedReader(InputStreamReader(inputStream))
            // Whether any content has been read
            var foundContent = false
            // Keep reading either until there is nothing more to read from or we are told to stop waiting
            while (keepReading || foundContent) {
                while (!reader.ready()) {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        logger.debug("Closing process stream")
                        return
                    }

                    if (!keepReading && !reader.ready()) {
                        return
                    }
                }
                val line = reader.readLine()
                append(line)
                log(line)
                foundContent = true
            }
            if (writer != null)
                writer!!.flush()
        } catch (e: IOException) {
            if (keepReading) {
                logger.error("Error reading process stream: {}", e.message, e)
            }
        } catch (t: Throwable) {
            logger.debug("Unknown error while reading from process input: {}", t.message)
        } finally {
            IoSupport.closeQuietly(reader)
            IoSupport.closeQuietly(writer)
        }
    }

    /**
     * This method will write any output from the stream to the the content buffer and the logger.
     *
     * @param output
     * the stream output
     */
    protected open fun append(output: String) {
        // Process stream redirects
        if (writer != null) {
            writer!!.println(output)
        }

        // Fill the content buffer, if one has been assigned
        if (contentBuffer != null) {
            contentBuffer!!.append(output.trim { it <= ' ' })
            contentBuffer!!.append('\n')
        }

        // Append output to logger?
    }

    /**
     * If a logger has been specified, the output is written to the logger using the defined log level.
     *
     * @param output
     * the stream output
     */
    protected fun log(output: String) {
        if (processLogger != null) {
            processLogger!!.info(output)
        }
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(StreamHelper::class.java!!)
    }
}
/**
 * Creates a new stream helper and immediately starts capturing output from the given stream.
 *
 * @param inputStream
 * the input stream
 */
