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

package org.opencastproject.message.broker.api.theme

import org.opencastproject.message.broker.api.MessageItem

import java.io.Serializable

/**
 * [Serializable] class that represents all of the possible messages sent through a ThemeService queue.
 */
class ThemeItem : MessageItem, Serializable {

    private val id: Long
    val theme: SerializableTheme?

    /** The type of the message being sent.  */
    val type: Type

    val themeId: Long?
        get() = id

    enum class Type {
        Update, Delete
    }

    /**
     * Constructor used to create or update a theme.
     *
     * @param theme
     * The theme details to update
     */
    constructor(theme: SerializableTheme) {
        this.id = theme.id
        this.theme = theme
        this.type = Type.Update
    }

    /**
     * Constructor to build a delete theme [ThemeItem].
     *
     * @param themeId
     */
    constructor(themeId: Long?) {
        this.id = themeId!!
        this.theme = null
        this.type = Type.Delete
    }

    override fun getId(): String {
        return java.lang.Long.toString(id)
    }

    companion object {

        private const val serialVersionUID = 3318918491810662792L

        val THEME_QUEUE_PREFIX = "THEME."

        val THEME_QUEUE = THEME_QUEUE_PREFIX + "QUEUE"

        /**
         * @param theme
         * The theme to update.
         * @return Builds [ThemeItem] for creating or updating a theme.
         */
        fun update(theme: SerializableTheme): ThemeItem {
            return ThemeItem(theme)
        }

        /**
         * @param themeId
         * The unique id of the theme to update.
         * @return Builds [ThemeItem] for deleting a theme.
         */
        fun delete(themeId: Long?): ThemeItem {
            return ThemeItem(themeId)
        }
    }

}
