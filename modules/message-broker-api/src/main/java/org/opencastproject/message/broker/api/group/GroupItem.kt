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

package org.opencastproject.message.broker.api.group

import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.security.api.Group
import org.opencastproject.security.api.GroupParser
import org.opencastproject.security.api.JaxbGroup

import java.io.IOException
import java.io.Serializable

/**
 * [Serializable] class that represents all of the possible messages sent through an Acl queue.
 */
class GroupItem : MessageItem, Serializable {

    override val id: String
    private val group: String?
    val type: Type

    enum class Type {
        Update, Delete
    }

    /**
     * Constructor to build a Create or Update [GroupItem]
     */
    constructor(group: JaxbGroup, type: Type) {
        this.id = group.groupId
        try {
            this.group = GroupParser.I.toXml(group)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }

        this.type = Type.Update
    }

    /**
     * Constructor to build a delete [GroupItem].
     */
    constructor(groupId: String, type: Type) {
        this.id = groupId
        this.group = null
        this.type = type
    }

    fun getGroupId(): String {
        return id
    }

    fun getGroup(): Group? {
        try {
            return if (group == null) null else GroupParser.I.parseGroupFromXml(group)
        } catch (e: Exception) {
            throw IllegalStateException()
        }

    }

    companion object {

        private const val serialVersionUID = 6332696075634123068L

        val GROUP_QUEUE_PREFIX = "GROUP."

        val GROUP_QUEUE = GROUP_QUEUE_PREFIX + "QUEUE"

        /**
         * Builds a [GroupItem] for creating or updating a Group.
         *
         * @param group
         * The group
         * @return A new [GroupItem] with the correct information to update or create it.
         */
        fun update(group: JaxbGroup): GroupItem {
            return GroupItem(group, Type.Update)
        }

        /**
         * @return Builds [GroupItem] for deleting a group.
         */
        fun delete(groupId: String): GroupItem {
            return GroupItem(groupId, Type.Delete)
        }
    }

}
