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

package org.opencastproject.util

import kotlin.collections.Map.Entry

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * JaxB implementation of the entry of a Hashtable, so that the element can be serialized in the intendet way The Entry
 * now looks &lt;item key="key"&gt;&lt;value&gt;value&lt;/value&gt;&lt;/item&gt;
 *
 */
@XmlType(name = "hash-entry", namespace = "http://util.opencastproject.org")
@XmlRootElement(name = "hash-entry", namespace = "http://util.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
class HashEntry : Entry<String, String> {

    @XmlAttribute(name = "key")
    protected var key: String

    @XmlElement
    protected var value: String

    constructor() {}

    constructor(key: String, value: String) {
        this.key = key
        this.value = value
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Map.Entry.getKey
     */
    override fun getKey(): String {
        return key
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Map.Entry.getValue
     */
    override fun getValue(): String {
        return value
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.Map.Entry.setValue
     */
    override fun setValue(value: String): String {
        this.value = value
        return this.value
    }

}
