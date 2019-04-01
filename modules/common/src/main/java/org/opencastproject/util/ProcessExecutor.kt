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

import org.opencastproject.util.data.Collections.map

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

/**
 * Helper class to execute processes on the host system and outside of the java vm. Since there are problems with
 * reading stdin, stdout and stderr that need to be taken into account when running on various platforms, this helper
 * class is used to deal with those.
 *
 * A generic Exception should be used to indicate what types of checked exceptions might be thrown from this process.
 *
 */
@Deprecated("use {@link ProcessRunner} instead")
class ProcessExecutor<T : Exception> {

    private val redirectErrorStream: Boolean
    private val commandLine: Array<String>
    private val environment: Map<String, String>

    protected constructor(commandLine: String, environment: Map<String, String>, redirectErrorStream: Boolean) {
        this.commandLine = commandLine.split("\\s+".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        this.environment = environment
        this.redirectErrorStream = redirectErrorStream
    }

    protected constructor(command: String, options: String) {
        this.commandLine = mkCommandLine(command, options)
        this.environment = map()
        this.redirectErrorStream = false
    }

    protected constructor(command: String, options: Array<String>) {
        val commandLineList = ArrayList<String>()
        commandLineList.add(command)
        commandLineList.addAll(Arrays.asList(*options))
        this.commandLine = commandLineList.toTypedArray<String>()
        this.environment = map()
        this.redirectErrorStream = false
    }

    protected constructor(commandLine: Array<String>) {
        this.commandLine = commandLine
        this.environment = map()
        this.redirectErrorStream = false
    }

    protected constructor(commandLine: String, redirectErrorStream: Boolean) {
        this.commandLine = commandLine.split("\\s+".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        this.redirectErrorStream = redirectErrorStream
        this.environment = map()
    }

    protected constructor(commandLine: Array<String>, redirectErrorStream: Boolean) {
        this.commandLine = commandLine
        this.redirectErrorStream = redirectErrorStream
        this.environment = map()
    }

    protected constructor(command: String, options: String, redirectErrorStream: Boolean) {
        this.commandLine = mkCommandLine(command, options)
        this.redirectErrorStream = redirectErrorStream
        this.environment = map()
    }

    // --

    @Throws(ProcessExcecutorException::class)
    fun execute() {
        var process: Process? = null
        var errorStreamHelper: StreamHelper? = null
        var inputStreamHelper: StreamHelper? = null
        try {
            // no special working directory is set which means the working directory of the
            // current java process is used.
            val pbuilder = ProcessBuilder(*commandLine)
            pbuilder.redirectErrorStream(redirectErrorStream)
            pbuilder.environment().putAll(environment)
            process = pbuilder.start()
            // consume stdin (the process's stdout)
            // Quoting the java doc for {@link Process}:
            //   "Because some native platforms only provide limited buffer size for standard input and output streams,
            //    failure to promptly write the input stream or read the output stream of the subprocess may cause the
            //    subprocess to block, and even deadlock."
            // Since it's more likely that a process writes massive amounts to stdout it should be made
            // sure to read from it as fast as possible.
            // todo use a thread pool to avoid the delay of starting a new thread. Reading late from a
            //   process's stream is a serious issue which can cause the application to crash or hang.
            inputStreamHelper = object : StreamHelper(process!!.inputStream) {
                override fun append(output: String) {
                    onStdout(output)
                }
            }
            // consume stderr if it is not redirected and merged into stdin
            if (!redirectErrorStream) {
                errorStreamHelper = object : StreamHelper(process.errorStream) {
                    override fun append(output: String) {
                        onStderr(output)
                    }
                }
            }
            // wait until streams have been emptied otherwise relevant information may get lost
            inputStreamHelper.join()
            errorStreamHelper?.join()
            // wait for the process to exit
            process.waitFor()
            val exitCode = process.exitValue()
            // allow subclasses to react to the process result
            onProcessFinished(exitCode)
        } catch (t: Throwable) {
            var msg: String? = null
            if (errorStreamHelper != null && errorStreamHelper.contentBuffer != null) {
                msg = errorStreamHelper.contentBuffer!!.toString()
            }
            // TODO: What if the error stream has been redirected? Can we still get the error messgae?
            throw ProcessExcecutorException(msg, t)
        } finally {
            IoSupport.closeQuietly(process)
        }
    }

    /**
     * A line of output has been read from the processe's stderr. Subclasses should override this method in order to deal
     * with process output.
     *
     * @param line
     * the line from `stderr`
     */
    protected fun onStderr(line: String) {
        logger.warn(line)
    }

    /**
     * A line of output has been read from the processe's stdout. Subclasses should override this method in order to deal
     * with process output.
     *
     * @param line
     * the line from `stdout`
     */
    protected fun onStdout(line: String) {
        logger.debug(line)
    }

    @Throws(T::class)
    protected fun onProcessFinished(exitCode: Int) {
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ProcessExecutor<*>::class.java!!)

        private fun mkCommandLine(command: String, options: String): Array<String> {
            val commandLineList = ArrayList<String>()
            commandLineList.add(command)
            Collections.addAll(commandLineList, *options.split("\\s+".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
            return commandLineList.toTypedArray<String>()
        }
    }
}
