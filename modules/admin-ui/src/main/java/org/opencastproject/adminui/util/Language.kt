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

import org.opencastproject.adminui.api.LanguageService

import java.util.Locale

/**
 * Represents a language with its properties.
 *
 * @author ademasi
 */
class Language(
        /**
         * @return the code
         */
        /**
         * @param code
         * the code to set
         */
        var code: String?) {
    /**
     * @return the displayName
     */
    /**
     * @param displayName
     * the displayName to set
     */
    var displayName: String? = null
    /**
     * @return the locale
     */
    var locale: Locale? = null
        private set

    init {
        this.displayName = LanguageFileUtil.getDisplayLanguageFromLanguageCode(code)

        val localCodes = code.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (localCodes.size > 1)
            this.locale = Locale(localCodes[0], localCodes[1])
        else
            this.locale = Locale(localCodes[0])
    }

    /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (code == null) 0 else code!!.hashCode()
        result = prime * result + if (displayName == null) 0 else displayName!!.hashCode()
        return result
    }

    /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as Language?
        if (code == null) {
            if (other!!.code != null)
                return false
        } else if (code != other!!.code)
            return false
        if (displayName == null) {
            if (other.displayName != null)
                return false
        } else if (displayName != other.displayName)
            return false
        return true
    }

    override fun toString(): String {
        return String.format("code: %s / displayName: %s", code, displayName)
    }

    companion object {

        fun defaultLanguage(): Language {
            return Language(LanguageService.DEFAULT_LANGUAGE)
        }
    }
}
