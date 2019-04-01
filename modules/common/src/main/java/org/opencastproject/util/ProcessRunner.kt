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

import com.entwinemedia.fn.Stream.`$`

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.Pred
import com.entwinemedia.fn.Prelude
import com.entwinemedia.fn.data.ListBuilder
import com.entwinemedia.fn.data.ListBuilders
import com.entwinemedia.fn.fns.Booleans

import java.io.IOException
import java.util.HashMap

/**
 * Helper class to run an external process on the host system and to read its STDOUT and STDERR streams.
 */
object ProcessRunner {
    val IGNORE = Booleans.yes<String>()

    val TO_CONSOLE: Pred<String> = object : Pred<String>() {
        override fun apply(s: String): Boolean {
            return true
        }
    }

    private val l = ListBuilders.looseImmutableArray
    private val NO_ENV = HashMap<String, String>()

    @Throws(IOException::class)
    fun run(info: ProcessInfo, stdout: Fn<String, Boolean>, stderr: Fn<String, Boolean>): Int {
        val pb = ProcessBuilder(info.commandLine).redirectErrorStream(info.isRedirectErrorStream)
        pb.environment().putAll(info.environment)
        // create stream consumer runnables
        val consumeOut = StreamConsumer(stdout)
        val consumeError = StreamConsumer(stderr)
        // create threads and run them
        val consumeOutThread = Thread(consumeOut)
        val consumeErrorThread = Thread(consumeError)
        consumeOutThread.start()
        consumeErrorThread.start()
        // Wait for the consumer threads to run to be able to immediately consume output.
        // This is important to avoid deadlocks or blocks of the process.
        // From the java doc for {@link Process}:
        //   "Because some native platforms only provide limited buffer size for standard input and output streams,
        //    failure to promptly write the input stream or read the output stream of the subprocess may cause the
        //    subprocess to block, and even deadlock."
        consumeOut.waitUntilRunning()
        consumeError.waitUntilRunning()
        val p = pb.start()
        consumeOut.consume(p.inputStream)
        consumeError.consume(p.errorStream)
        // wait until the streams have been fully consumed
        consumeOut.waitUntilFinished()
        consumeError.waitUntilFinished()
        // wait and exit
        try {
            return p.waitFor()
        } catch (e: InterruptedException) {
            return Prelude.chuck(e)
        }

    }

    fun mk(commandLine: String): ProcessInfo {
        return ProcessInfo(mkCommandLine(commandLine), NO_ENV, false)
    }

    fun mk(commandLine: String, environment: Map<String, String>, redirectErrorStream: Boolean): ProcessInfo {
        return ProcessInfo(mkCommandLine(commandLine), environment, redirectErrorStream)
    }

    fun mk(command: String, options: String): ProcessInfo {
        return ProcessInfo(mkCommandLine(command, options), NO_ENV, false)
    }

    fun mk(command: String, options: Array<String>): ProcessInfo {
        return ProcessInfo(`$`(command).append(`$`(*options)).toList(), NO_ENV, false)
    }

    fun mk(commandLine: Array<String>): ProcessInfo {
        return ProcessInfo(l.mk(*commandLine), NO_ENV, false)
    }

    fun mk(commandLine: String, redirectErrorStream: Boolean): ProcessInfo {
        return ProcessInfo(mkCommandLine(commandLine), NO_ENV, redirectErrorStream)
    }

    fun mk(commandLine: Array<String>, redirectErrorStream: Boolean): ProcessInfo {
        return ProcessInfo(l.mk(*commandLine), NO_ENV, redirectErrorStream)
    }

    fun mk(command: String, options: String, redirectErrorStream: Boolean): ProcessInfo {
        return ProcessInfo(mkCommandLine(command, options), NO_ENV, redirectErrorStream)
    }

    private fun mkCommandLine(command: String): List<String> {
        return l.mk(*command.split("\\s+".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
    }

    private fun mkCommandLine(command: String, options: String): List<String> {
        return `$`(command).append(mkCommandLine(options)).toList()
    }

    class ProcessInfo(val commandLine: List<String>,
                      val environment: Map<String, String>,
                      val isRedirectErrorStream: Boolean)
}
