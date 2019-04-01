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

import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.util.UUID

/**
 * `PathSupport` is a helper class to deal with filesystem paths.
 */
object PathSupport {

    /** The logging facility  */
    private val logger = LoggerFactory.getLogger(PathSupport::class.java!!)

    /**
     * Returns the filename translated into a version that can safely be used as part of a file system path.
     *
     * @param fileName
     * The file name
     * @return the safe version
     */
    fun toSafeName(fileName: String): String {
        val urlExtension = FilenameUtils.getExtension(fileName)
        val baseName = FilenameUtils.getBaseName(fileName)
        val safeBaseName = baseName.replace("\\W".toRegex(), "_") // TODO -- ensure that this filename is safe on all platforms
        val safeString: String
        if ("" == urlExtension) {
            safeString = safeBaseName
        } else {
            safeString = safeBaseName + "." + urlExtension
        }
        if (safeString.length < 255)
            return safeString
        var random = UUID.randomUUID().toString()
        if ("" != urlExtension) {
            random = "$random.$urlExtension"
        }
        logger.info("using '{}' to represent url '{}', which is too long to store as a filename", random, fileName)
        return random
    }

    /**
     * Concatenates the two urls with respect to leading and trailing slashes.
     *
     * @return the concatenated url of the two arguments
     */
    @Deprecated("          Use Java's native <pre>Paths.get(String, …).toFile()</pre> instead")
    fun concat(prefix: String?, suffix: String?): String {
        var prefix = prefix
        var suffix = suffix
        if (prefix == null)
            throw IllegalArgumentException("Argument prefix is null")
        if (suffix == null)
            throw IllegalArgumentException("Argument suffix is null")

        prefix = adjustSeparator(prefix)
        suffix = adjustSeparator(suffix)
        prefix = removeDoubleSeparator(prefix)
        suffix = removeDoubleSeparator(suffix)

        if (!prefix.endsWith(File.separator) && !suffix.startsWith(File.separator))
            prefix += File.separator
        if (prefix.endsWith(File.separator) && suffix.startsWith(File.separator))
            suffix = suffix.substring(1)

        prefix += suffix
        return prefix
    }

    /**
     * Concatenates the path elements with respect to leading and trailing slashes.
     *
     * @param parts
     * the parts to concat
     * @return the concatenated path
     */
    @Deprecated("          Use Java's native <pre>Paths.get(String, …).toFile()</pre> instead")
    fun concat(parts: Array<String>?): String {
        if (parts == null)
            throw IllegalArgumentException("Argument parts is null")
        if (parts.size == 0)
            throw IllegalArgumentException("Array parts is empty")
        var path = removeDoubleSeparator(adjustSeparator(parts[0]))
        for (i in 1 until parts.size) {
            path = concat(path, removeDoubleSeparator(adjustSeparator(parts[i])))
        }
        return path
    }

    @Deprecated("")
    fun path(vararg parts: String): String {
        return concat(parts)
    }

    /**
     * Checks that the path only contains the system path separator. If not, wrong ones are replaced.
     */
    private fun adjustSeparator(path: String): String {
        var sp = File.separator
        if ("\\" == sp)
            sp = "\\\\"
        return path.replace("/".toRegex(), sp)
    }

    /**
     * Removes any occurrence of double file separators and replaces it with a single one.
     *
     * @param path
     * the path to check
     * @return the corrected path
     */
    @Deprecated("          This implements built-in Java functionality. Use instead:\n" +
            "               <ul>\n" +
            "                 <li><pre>Paths.get(\"/a/b//c\")</pre></li>\n" +
            "                 <li><pre>new File(\"/a/b//c\")</pre></li>\n" +
            "               </ul>")
    private fun removeDoubleSeparator(path: String): String {
        var path = path
        var index = 0
        val s = File.separator + File.separatorChar
        while ((index = path.indexOf(s, index)) != -1) {
            path = path.substring(0, index) + path.substring(index + 1)
        }
        return path
    }

}
/**
 * This class should not be instantiated, since it only provides static utility methods.
 */
