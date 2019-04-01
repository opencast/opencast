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

import org.opencastproject.util.jaxb.JaxbParser

import java.io.ByteArrayInputStream
import java.io.IOException

/** JAXB parser for JAXB DTOs of [Group].  */
class GroupParser private constructor(contextPath: String) : JaxbParser(contextPath) {

    @Throws(IOException::class)
    fun parseGroupFromXml(xml: String): JaxbGroup {
        return unmarshal(JaxbGroup::class.java, ByteArrayInputStream(xml.toByteArray()))
    }

    @Throws(IOException::class)
    fun parseGroupListFromXml(xml: String): JaxbGroupList {
        return unmarshal(JaxbGroupList::class.java, ByteArrayInputStream(xml.toByteArray()))
    }

    @Throws(IOException::class)
    fun toXml(group: JaxbGroup): String {
        return marshal(group)
    }

    @Throws(IOException::class)
    fun toXml(groupList: JaxbGroupList): String {
        return marshal(groupList)
    }

    companion object {

        /** Instance of GroupParser  */
        val I = GroupParser("org.opencastproject.security.api")
    }

}
