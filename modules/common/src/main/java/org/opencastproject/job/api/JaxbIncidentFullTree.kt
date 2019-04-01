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

import org.opencastproject.util.data.Monadics.mlist

import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.IncidentServiceException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Function
import java.util.Locale

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentFullTree", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "incidentFullTree", namespace = "http://job.opencastproject.org")
class JaxbIncidentFullTree {
    @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
    private val incidents: List<JaxbIncidentFull>

    @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_TREE)
    private val descendants: List<JaxbIncidentFullTree>

    /** Constructor for JAXB  */
    constructor() {}

    @Throws(IncidentServiceException::class, NotFoundException::class)
    constructor(svc: IncidentService, locale: Locale, tree: IncidentTree) {
        this.incidents = mlist(tree.incidents).map(JaxbIncidentFull.mkFn(svc, locale)).value()
        this.descendants = mlist(tree.descendants).map(mkFn(svc, locale)).value()
    }

    companion object {

        fun mkFn(svc: IncidentService, locale: Locale): Function<IncidentTree, JaxbIncidentFullTree> {
            return object : Function.X<IncidentTree, JaxbIncidentFullTree>() {
                @Throws(Exception::class)
                public override fun xapply(tree: IncidentTree): JaxbIncidentFullTree {
                    return JaxbIncidentFullTree(svc, locale, tree)
                }
            }
        }
    }
}
