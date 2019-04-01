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

package org.opencastproject.job.api

import org.opencastproject.util.jaxb.JaxbParser

import java.io.IOException
import java.io.InputStream

/** JAXB parser for JAXB DTOs of [Incident].  */
class IncidentParser private constructor(contextPath: String) : JaxbParser(contextPath) {

    @Throws(IOException::class)
    fun parseDigestFromXml(xml: InputStream): JaxbIncidentDigestList {
        return unmarshal(JaxbIncidentDigestList::class.java, xml)
    }

    @Throws(IOException::class)
    fun parseIncidentFromXml(xml: InputStream): JaxbIncident {
        return unmarshal(JaxbIncident::class.java, xml)
    }

    @Throws(IOException::class)
    fun parseIncidentsFromXml(xml: InputStream): JaxbIncidentList {
        return unmarshal(JaxbIncidentList::class.java, xml)
    }

    @Throws(IOException::class)
    fun parseIncidentTreeFromXml(xml: InputStream): JaxbIncidentTree {
        return unmarshal(JaxbIncidentTree::class.java, xml)
    }

    @Throws(IOException::class)
    fun toXml(incident: JaxbIncident): String {
        return marshal(incident)
    }

    @Throws(IOException::class)
    fun toXml(tree: JaxbIncidentTree): String {
        return marshal(tree)
    }

    companion object {
        /** Instance of IncidentParser  */
        val I = IncidentParser("org.opencastproject.job.api:org.opencastproject.serviceregistry.api")
    }
}
