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

package org.opencastproject.dictionary.hunspell

import org.opencastproject.util.ReadinessIndicator.ARTIFACT

import org.opencastproject.dictionary.api.DictionaryService
import org.opencastproject.metadata.mpeg7.Textual
import org.opencastproject.metadata.mpeg7.TextualImpl
import org.opencastproject.util.ReadinessIndicator

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.Dictionary
import java.util.Hashtable
import java.util.LinkedList

/**
 * This dictionary service implementation passes the input text
 * to the hunspell spell checker and returns its results.
 */
class DictionaryServiceImpl : DictionaryService {

    /* The hunspell binary to execute */
    var binary = "hunspell"

    /* The regular command line options for filtering */
    var command = " -i utf-8 -d de_DE,en_GB,en_US -G"

    /**
     * OSGi callback on component activation.
     *
     * @param  ctx  the bundle context
     */
    @Throws(UnsupportedEncodingException::class)
    internal fun activate(ctx: BundleContext) {
        val properties = Hashtable<String, String>()
        properties[ARTIFACT] = "dictionary"
        ctx.registerService(ReadinessIndicator::class.java.name,
                ReadinessIndicator(), properties)

        /* Get hunspell binary from config file */
        var binary: String? = ctx.getProperty(HUNSPELL_BINARY_CONFIG_KEY) as String
        if (binary != null) {
            /* Fix special characters */
            binary = String(binary.toByteArray(charset("ISO-8859-1")), "UTF-8")
            logger.info("Setting hunspell binary to '{}'", binary)
            this.binary = binary
        }

        /* Get hunspell command line options from config file */
        var command: String? = ctx.getProperty(HUNSPELL_COMMAND_CONFIG_KEY) as String
        if (command != null) {
            /* Fix special characters */
            command = String(command.toByteArray(charset("ISO-8859-1")), "UTF-8")
            logger.info("Setting hunspell command line options to '{}'", command)
            this.command = command
        }
    }


    /**
     * Run hunspell with text as input.
     */
    @Throws(Throwable::class)
    fun runHunspell(text: String): LinkedList<String> {

        // create a new list of arguments for our process
        val commandLine = "$binary $command"
        val commandList = commandLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()


        var stdout: InputStream? = null
        var stderr: InputStream? = null
        var stdin: OutputStream? = null
        var p: Process? = null
        var bufr: BufferedReader? = null
        val words = LinkedList<String>()

        logger.info("Executing hunspell command '{}'", StringUtils.join(commandList, " "))
        p = ProcessBuilder(*commandList).start()
        stderr = p!!.errorStream
        stdout = p.inputStream
        stdin = p.outputStream

        /* Pipe text through hunspell for filtering */
        stdin!!.write(text.toByteArray(charset("UTF-8")))
        stdin.flush()
        stdin.close()

        /* Get output of hunspell */
        var line: String
        bufr = BufferedReader(InputStreamReader(stdout!!, "UTF-8"))
        while ((line = bufr.readLine()) != null) {
            words.add(line)
        }
        bufr.close()

        /* Get error messages */
        bufr = BufferedReader(InputStreamReader(stderr!!))
        while ((line = bufr.readLine()) != null) {
            logger.warn(line)
        }
        bufr.close()

        if (p.waitFor() != 0) {
            logger.error("Hunspell reported an error (Missing dictionaries?)")
            throw IllegalStateException("Hunspell returned error code")
        }

        return words
    }


    /**
     * Filter the text according to the rules defined by the dictionary
     * implementation used. This implementation will just let the whole text pass
     * through.
     *
     * @return filtered text
     */
    override fun cleanUpText(text: String): Textual {

        var words: LinkedList<String>? = null

        try {
            words = runHunspell(text)
        } catch (t: Throwable) {
            logger.error("Error executing hunspell")
            logger.error(t.message, t)
            return null
        }


        val result = StringUtils.join(words, " ")
        return if ("" == result) {
            null
        } else TextualImpl(result)
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(DictionaryServiceImpl::class.java)

        val HUNSPELL_BINARY_CONFIG_KEY = "org.opencastproject.dictionary.hunspell.binary"

        val HUNSPELL_COMMAND_CONFIG_KEY = "org.opencastproject.dictionary.hunspell.command"
    }

}
