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

package org.opencastproject.adminui.util

import java.util.regex.Matcher
import java.util.regex.Pattern

class CompositeLanguageCodeParser(compositedLanguageCode: String) {
    private val matcher: Matcher

    val isComposite: Boolean
        get() = matcher.matches()

    val simpleLanguage: String
        get() = if (matcher.matches()) {
            matcher.group(1)
        } else ""

    init {
        matcher = COMPOSITE_LANGUAGE_PATTERN.matcher(compositedLanguageCode)
    }

    companion object {

        /**
         * Some language strings are composed of language code and country, this pattern is the basis for finding the language
         * part.
         */
        val COMPOSITE_LANGUAGE_NAME = "(.*)[_|-].*"

        private val COMPOSITE_LANGUAGE_PATTERN = Pattern.compile(COMPOSITE_LANGUAGE_NAME)
    }

}
