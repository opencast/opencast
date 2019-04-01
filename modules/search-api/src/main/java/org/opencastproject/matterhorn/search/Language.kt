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


package org.opencastproject.matterhorn.search

import java.io.Serializable
import java.util.Locale

/**
 * A `Language` consists of a language identifier, e.g. `de` to identify the German language, and
 * of the language description in the various supported languages. There is also a connection to the associate
 * `Locale`.
 *
 * @see Locale
 */
interface Language : Serializable, Comparable<Language> {

    /**
     * Returns the locale that is associated with the language.
     *
     * @return the locale
     */
    val locale: Locale

    /**
     * Returns the name of this language in its own language, e.g
     *
     *  * `English` for English
     *  * `Deutsch` for German
     *  * `Français` for French
     *
     *
     * @return the language name in its own language
     */
    val description: String

    /**
     * Returns the language's identifier, which corresponds to the locales name for this language.
     *
     * @return the language identifier
     */
    val identifier: String

    /**
     * Returns the name of this language in the specified language, e.g given that `language` was
     * `German`, this method would return:
     *
     *  * `Englisch` for English
     *  * `Deutsch` for German
     *  * `Französisch` for French
     *
     *
     * @param language
     * the language version of this language
     * @return the language name in the specified language
     */
    fun getDescription(language: Language): String

}
