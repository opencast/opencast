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

package org.opencastproject.message.broker.api.comments

import org.opencastproject.message.broker.api.MessageItem

import java.io.Serializable

/**
 * [Serializable] class that represents all of the possible messages sent through a comment queue.
 */
class CommentItem
/**
 * Constructor to build an Update [CommentItem]
 */
(override val id: String, private val hasComments: Boolean, private val hasOpenComments: Boolean, private val needsCutting: Boolean) : MessageItem, Serializable {
    val type: Type

    enum class Type {
        Update
    }

    init {
        this.type = Type.Update
    }

    fun getEventId(): String {
        return id
    }

    fun hasComments(): Boolean {
        return hasComments
    }

    fun hasOpenComments(): Boolean {
        return hasOpenComments
    }

    fun needsCutting(): Boolean {
        return needsCutting
    }

    companion object {

        private const val serialVersionUID = -3946543513879987169L

        val COMMENT_QUEUE_PREFIX = "COMMENT."

        val COMMENT_QUEUE = COMMENT_QUEUE_PREFIX + "QUEUE"

        /**
         * @param eventId
         * The event to update
         * @param hasComments
         * Whether the event has comments
         * @param hasOpenComments
         * Whether the event has open comments
         * @param needsCutting
         * Whether the event has an open comment that it needs cutting
         * @return Builds a [CommentItem] for updating a comment.
         */
        fun update(eventId: String, hasComments: Boolean, hasOpenComments: Boolean, needsCutting: Boolean): CommentItem {
            return CommentItem(eventId, hasComments, hasOpenComments, needsCutting)
        }
    }

}
