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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.opencastproject.util.IoSupport.withResource
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.FileSupport
import org.opencastproject.util.data.Effect
import org.opencastproject.util.data.Function

import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.util.UUID

class FileReadDeleteTest {

    private val start = Any()
    @Volatile
    private var totalRead: Long = 0
    private var expectedSize: Long = 0

    /** NIO based file reader.  */
    private val readNio = object : Function<FileInputStream, Function<Long, Long>>() {
        override fun apply(`in`: FileInputStream): Function<Long, Long> {
            val channel = `in`.channel
            val sink = Sink()
            //
            return object : Function.X<Long, Long>() {
                @Throws(Exception::class)
                override fun xapply(total: Long?): Long? {
                    return channel.transferTo(total!!, 1024, sink)
                }
            }
        }
    }

    /** Normal IO based file reader.  */
    private val readIo = object : Function<FileInputStream, Function<Long, Long>>() {
        override fun apply(`in`: FileInputStream): Function<Long, Long> {
            val buffer = ByteArray(1024)
            //
            return object : Function.X<Long, Long>() {
                @Throws(Exception::class)
                override fun xapply(total: Long?): Long? {
                    return `in`.read(buffer).toLong()
                }
            }
        }
    }

    @Before
    fun setUp() {
        totalRead = 0
        expectedSize = resourceAsFile(FILE).length()
    }

    @Test
    @Throws(Exception::class)
    fun testFileDeletionWhileReadingIo() {
        testFileDeletionWhileReading(readIo)
    }

    @Test
    @Throws(Exception::class)
    fun testFileDeletionWhileReadingNio() {
        testFileDeletionWhileReading(readNio)
    }

    @Throws(Exception::class)
    private fun testFileDeletionWhileReading(mkReader: Function<FileInputStream, Function<Long, Long>>) {
        val source = resourceAsFile(FILE)
        val work = FileSupport.copy(source, File(source.parentFile, UUID.randomUUID().toString() + ".tmp"))
        assertTrue("Work file could not be created", work.exists())
        try {
            val readerThread = Thread(mkRunnable(work, mkReader))
            synchronized(start) {
                readerThread.start()
                // wait for reader
                start.wait()
            }
            assertEquals("Reader already finished", 0, totalRead)
            assertTrue("File could not be deleted", work.delete())
            assertFalse("File still exists", work.exists())
            logger.debug("Work file deleted")
            // wait for reader to complete
            readerThread.join()
            assertEquals("File not completely read", expectedSize, totalRead)
        } finally {
            // cleanup
            FileSupport.delete(work)
            assertFalse(work.exists())
        }
    }

    /** Create a runnable containing a reader created from a `readerMaker`.  */
    private fun mkRunnable(file: File, readerMaker: Function<FileInputStream, Function<Long, Long>>): Runnable {
        return Runnable {
            try {
                withResource(FileInputStream(file), mkReaderFrom(readerMaker))
            } catch (e: FileNotFoundException) {
                chuck(e)
            }
        }
    }

    /**
     * Create a read effect from a `readerMaker` function.
     * This read effect will be used to consume a file input stream.
     */
    private fun mkReaderFrom(readerMaker: Function<FileInputStream, Function<Long, Long>>): Effect<FileInputStream> {
        return object : Effect<FileInputStream>() {
            public override fun run(`in`: FileInputStream) {
                logger.debug("Start reading")
                var total = 0L
                var read: Long
                val readFile = readerMaker.apply(`in`)
                try {
                    while ((read = readFile.apply(total)) > 0) {
                        total = total + read
                        logger.debug("Read $total")
                        if (total > 3000) {
                            synchronized(start) {
                                start.notifyAll()
                            }
                        }
                        Thread.sleep(100)
                    }
                    totalRead = total
                    logger.debug("File completely read $total")
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun resourceAsFile(resource: String): File {
        try {
            return File(this.javaClass.getResource(resource).toURI())
        } catch (e: URISyntaxException) {
            return chuck(e)
        }

    }

    private class Sink : WritableByteChannel {
        private var closed = false

        @Throws(IOException::class)
        override fun write(byteBuffer: ByteBuffer): Int {
            return byteBuffer.limit()
        }

        override fun isOpen(): Boolean {
            return !closed
        }

        @Throws(IOException::class)
        override fun close() {
            closed = true
        }
    }

    companion object {
        private val FILE = "/opencast_header.gif"

        private val logger = LoggerFactory.getLogger(FileReadDeleteTest::class.java)
    }
}
