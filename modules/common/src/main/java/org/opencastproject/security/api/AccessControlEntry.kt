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

package org.opencastproject.security.api

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A tuple of role, action, and whether the combination is to be allowed.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ace", namespace = "http://org.opencastproject.security", propOrder = ["action", "allow", "role"])
@XmlRootElement(name = "ace", namespace = "http://org.opencastproject.security")
class AccessControlEntry {

    /** The role  */
    /**
     * @return the role
     */
    val role: String? = null

    /** The action  */
    /**
     * @return the action
     */
    val action: String? = null

    /** Whether this role is allowed to take this action  */
    /**
     * @return the allow
     */
    val isAllow = false

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {}

    /**
     * Constructs an access control entry for a role, action, and allow tuple
     *
     * @param role
     * the role
     * @param action
     * the action
     * @param allow
     * Whether this role is allowed to take this action
     */
    constructor(role: String, action: String, allow: Boolean) {
        this.role = role
        this.action = action
        this.isAllow = allow
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj is AccessControlEntry) {
            val other = obj as AccessControlEntry?
            return this.isAllow == other!!.isAllow && this.role == other!!.role && this.action == other.action
        } else {
            return false
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return (role + action + java.lang.Boolean.toString(isAllow)).hashCode()
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        val sb = StringBuilder(role!!).append(" is ")
        if (!isAllow)
            sb.append("not ")
        sb.append("allowed to ")
        sb.append(action)
        return sb.toString()
    }

}
