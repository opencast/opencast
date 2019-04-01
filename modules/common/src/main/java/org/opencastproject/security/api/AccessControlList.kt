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

import java.util.AbstractMap.SimpleEntry
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A list of [AccessControlEntry]s.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "acl", namespace = "http://org.opencastproject.security", propOrder = ["entries"])
@XmlRootElement(name = "acl", namespace = "http://org.opencastproject.security")
class AccessControlList {

    /** The list of access control entries  */
    /**
     * @return the entries
     */
    @XmlElement(name = "ace")
    var entries: List<AccessControlEntry>? = null
        private set

    /**
     * No-arg constructor needed by JAXB
     */
    constructor() {
        this.entries = ArrayList()
    }

    constructor(vararg entries: AccessControlEntry) {
        this.entries = ArrayList(Arrays.asList(*entries))
    }

    constructor(entries: List<AccessControlEntry>) {
        this.entries = ArrayList(entries)
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return entries!!.toString()
    }

    /**
     * Merge this access control list with another one based on roles specified within. In case both lists specify
     * rules for a specific role, the set of rules from the access control list passed as argument to this method will
     * take precedence over the internal set of rules.
     *
     * Example:
     * <pre>
     * ROLE_USER1   ROLE_USER2   ROLE_USER3
     * read  write  read  write  read  write
     * this     ok    ok     ok    ok
     * argument              ok           ok
     * result   ok    ok     ok           ok
    </pre> *
     *
     * @param acl
     * Access control list to merge with
     * @return Merged access control list
     */
    fun merge(acl: AccessControlList): AccessControlList {
        val roles = HashSet<String>()
        val newEntries = ArrayList(acl.entries!!)
        // Get list of new roles
        for (entry in newEntries) {
            roles.add(entry.role)
        }
        // Apply old rules if no new rules for a role exist
        for (entry in this.entries!!) {
            if (!roles.contains(entry.role)) {
                newEntries.add(entry)
            }
        }
        this.entries = newEntries
        return this
    }

    /**
     * Merge this access control list with another one based on actions specified within. In case both lists specify
     * rules for a specific action, the rules from the access control list passed as argument to this method will take
     * precedence over the internal rules.
     *
     * Example:
     * <pre>
     * ROLE_USER1   ROLE_USER2   ROLE_USER3
     * read  write  read  write  read  write
     * this     ok    ok     ok    ok
     * argument              ok           ok
     * result   ok    ok     ok    ok     ok
    </pre> *
     *
     * @param acl
     * Access control list to merge with
     * @return Merged access control list
     */
    fun mergeActions(acl: AccessControlList): AccessControlList {
        val rules = HashMap<SimpleEntry<String, String>, AccessControlEntry>()
        var key: SimpleEntry<String, String>
        for (entry in this.entries!!) {
            key = SimpleEntry(entry.role, entry.action)
            rules[key] = entry
        }
        for (entry in acl.entries!!) {
            key = SimpleEntry(entry.role, entry.action)
            rules[key] = entry
        }
        this.entries = ArrayList(rules.values)
        return this
    }

}
