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
package org.opencastproject.coverimage.impl.xsl

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.WordUtils

/** Helper class to use within the XSLT transformation  */
object XsltHelper {

    private val LINE_SEPARATOR = "<newline>"

    /**
     * Splits a string into several lines, depending on the maximum allowed characters per line and returns the desired
     * line.
     *
     * @param text
     * Text to split up
     * @param maxChars
     * maximum allowed characters per line
     * @param line
     * line number to return (starting by 1)
     * @param isLastLine
     * whether this is the last line used to represent the string. If so, the string will be abbreviated using
     * ellipsis in case the text cannot be represented by the given number of lines
     * @return the line or null if the given text is null, the line number is less than 1 or if the desired line does not
     * exist
     */
    fun split(text: String?, maxChars: Int, line: Int, isLastLine: Boolean): String? {
        if (text == null || line < 1) {
            return null
        }

        if (maxChars > text.length && line == 1 || maxChars == 0) {
            return text
        }

        val textWithSeparator = WordUtils.wrap(text, maxChars, LINE_SEPARATOR, true)
        val lines = textWithSeparator.split(LINE_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (lines.size >= line) {
            if (isLastLine && lines.size > line && maxChars > 3) {
                /* Abbreviate lines using ellipsis. Because of word wrapping, the line length can be less than maxChars.
           That case is covered by first appending the ellipsis and then applying abbreviate */
                val lastLine = lines[line - 1] + "..."
                return StringUtils.abbreviate(lastLine, maxChars)
            } else {
                return lines[line - 1]
            }
        } else {
            return null
        }
    }
}// Utility classes should not have a public or default constructor.
