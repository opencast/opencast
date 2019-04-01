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

import java.util.Hashtable

import javax.xml.bind.annotation.adapters.XmlAdapter

/**
 * Adapter class for JaxB to represent a Hashtable
 *
 */
class HashtableAdapter : XmlAdapter<Array<HashEntry>, Hashtable<String, String>>() {

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter.marshal
     */
    @Throws(Exception::class)
    override fun marshal(myHashtable: Hashtable<String, String>): Array<HashEntry> {
        val keys = myHashtable.keys.toTypedArray<String>()
        val meta = arrayOfNulls<HashEntry>(keys.size)
        for (i in keys.indices)
            meta[i] = HashEntry(keys[i], myHashtable[keys[i]])
        return meta
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter.unmarshal
     */
    @Throws(Exception::class)
    override fun unmarshal(data: Array<HashEntry>): Hashtable<String, String> {
        val myHashtable = Hashtable<String, String>()
        for (i in data.indices) {
            myHashtable[data[i].key] = data[i].value
        }
        return myHashtable
    }
}
