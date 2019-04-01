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

import com.entwinemedia.fn.Prelude.chuck

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Fx
import com.entwinemedia.fn.Unit

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch

/**
 * A StreamConsumer helps to asynchronously consume a text input stream line by line.
 * The consumer guarantees the closing of the stream.
 */
class StreamConsumer
/**
 * Create a new stream consumer.
 *
 * @param consumer
 * a predicate function that may stop reading further lines by returning `false`
 */
(private val consumer: Fn<String, Boolean>) : Runnable {
    private val running = CountDownLatch(1)
    private val ready = CountDownLatch(1)
    private val finished = CountDownLatch(1)

    private var stopped = false
    private var stream: InputStream? = null
    private var reader: BufferedReader? = null

    private val consumeBuffered = object : Fx<BufferedReader>() {
        override fun apply(reader: BufferedReader) {
            var line: String
            try {
                while ((line = reader.readLine()) != null) {
                    if (!consumer.apply(line)) {
                        stopConsuming()
                    }
                }
            } catch (e: IOException) {
                if (!stopped) {
                    chuck<Any>(e)
                }
            }

        }
    }.toFn()

    override fun run() {
        try {
            running.countDown()
            ready.await()
            // also save a reference to the reader to able to close it in stopReading
            // otherwise the read loop may continue reading from the buffer
            reader = BufferedReader(InputStreamReader(stream!!))
            IoSupport.withResource<Unit, BufferedReader>(reader, consumeBuffered)
            finished.countDown()
        } catch (e: InterruptedException) {
            chuck<Any>(e)
        }

    }

    /** Wait for the executing thread to run.  */
    fun waitUntilRunning() {
        try {
            running.await()
        } catch (e: InterruptedException) {
            chuck<Any>(e)
        }

    }

    /** Wait until the stream has been fully consumed.  */
    fun waitUntilFinished() {
        try {
            finished.await()
        } catch (e: InterruptedException) {
            chuck<Any>(e)
        }

    }

    /** Forcibly stop consuming the stream.  */
    fun stopConsuming() {
        if (stream != null) {
            stopped = true
            IoSupport.closeQuietly(stream)
            IoSupport.closeQuietly(reader)
        }
    }

    /** Start consuming `stream`. It is guaranteed that the stream gets closed.  */
    fun consume(stream: InputStream) {
        waitUntilRunning()
        this.stream = stream
        ready.countDown()
    }
}
