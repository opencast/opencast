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

package org.opencastproject.message.broker.api.acl

import org.opencastproject.message.broker.api.MessageItem

import java.io.Serializable

/**
 * [Serializable] class that represents all of the possible messages sent through an Acl queue.
 */
class AclItem : MessageItem, Serializable {

    override var id: String? = null
        private set
    val newAclName: String
    val type: Type

    enum class Type {
        Create, Update, Delete
    }

    /**
     * Constructor to build an Update [AclItem]
     */
    constructor(currentAclName: String, newAclName: String) {
        this.id = currentAclName
        this.newAclName = newAclName
        this.type = Type.Update
    }

    /**
     * Constructor to build a create or delete [AclItem].
     */
    constructor(currentAclName: String, type: Type) {
        this.id = currentAclName
        this.type = type
    }

    fun getCurrentAclName(): String? {
        return id
    }

    companion object {

        private const val serialVersionUID = -8329403993622629220L

        val ACL_QUEUE_PREFIX = "ACL."

        val ACL_QUEUE = ACL_QUEUE_PREFIX + "QUEUE"

        /**
         * @return Builds [AclItem] for deleting an acl.
         */
        fun create(currentAclName: String): AclItem {
            return AclItem(currentAclName, Type.Create)
        }

        /**
         * @param currentAclName
         * @param newAclName
         * @return Builds a [AclItem] for updating a mediapackage.
         */
        fun update(currentAclName: String, newAclName: String): AclItem {
            return AclItem(currentAclName, newAclName)
        }

        /**
         * @return Builds [AclItem] for deleting an acl.
         */
        fun delete(currentAclName: String): AclItem {
            return AclItem(currentAclName, Type.Delete)
        }
    }

}
