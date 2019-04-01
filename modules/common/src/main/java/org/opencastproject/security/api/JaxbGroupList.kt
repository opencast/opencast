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

import java.util.ArrayList

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A wrapper for group collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "groups", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "groups", namespace = "http://org.opencastproject.security")
class JaxbGroupList {

    /** A list of groups.  */
    @XmlElement(name = "group")
    protected var groups: MutableList<JaxbGroup> = ArrayList()

    constructor() {}

    constructor(group: JaxbGroup) {
        groups.add(group)
    }

    constructor(groups: MutableCollection<JaxbGroup>) {
        for (group in groups)
            groups.add(group)
    }

    /**
     * @return the groups
     */
    fun getGroups(): List<JaxbGroup> {
        return groups
    }

    /**
     * @param roles
     * the roles to set
     */
    fun setRoles(roles: MutableList<JaxbGroup>) {
        this.groups = roles
    }

    fun add(group: Group) {
        if (group is JaxbGroup) {
            groups.add(group)
        } else {
            groups.add(JaxbGroup.fromGroup(group))
        }
    }

}
