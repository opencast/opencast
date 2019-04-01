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

import java.util.HashMap

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * JAXB annotated implementation of the job context.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "jobcontext", namespace = "http://job.opencastproject.org")
@XmlRootElement(name = "jobcontext", namespace = "http://job.opencastproject.org")
class JaxbJobContext : JobContext {

    /** The context identifier, which is equal to the root job identifier  */
    /**
     * Gets the identifier for this context.
     *
     * @return the context identifier
     */
    /**
     * Sets the context identifier.
     *
     * @param id
     * the context id to set the id
     */
    @get:XmlAttribute(name = "id")
    override var id: Long? = null

    /** The properties  */
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.opencastproject.job.api.JobContext#getParentJobId()
    // */
    // @XmlElement(name = "parent")
    // @Override
    // public Long getParentJobId() {
    // return parentJobId;
    // }
    //
    // /**
    // * Sets the parent job id.
    // *
    // * @param id
    // * the parent job id
    // */
    // public void setParentJobId(Long id) {
    // this.parentJobId = id;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.opencastproject.job.api.JobContext#getUserId()
    // */
    // @XmlElement(name = "user")
    // @Override
    // public String getUserId() {
    // return userId;
    // }
    //
    // /**
    // * Sets the user id.
    // *
    // * @param userId
    // * the user id
    // */
    // public void setUserId(String userId) {
    // this.userId = userId;
    // }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.JobContext.getProperties
     */
    @get:XmlElement(name = "properties")
    override var properties: HashMap<String, String>? = null
        protected set

    // protected Long parentJobId = null;
    //
    // protected String userId = null;
    //

    /** The default no arg constructor needed by JAXB  */
    constructor() {
        this.properties = HashMap()
    }

    /**
     * Constructs a jaxb job context from another context.
     *
     * @param jobContext
     * the template
     */
    constructor(jobContext: JobContext) {
        this.id = jobContext.id
        this.properties = HashMap()
        // TODO NULL CHECK
        if (jobContext.properties != null) {
            this.properties!!.putAll(jobContext.properties)
        }
    }

    /**
     * Sets the context properties.
     *
     * @param properties
     * the properties
     */
    fun setProperties(properties: Map<String, String>?) {
        this.properties!!.clear()
        if (properties != null) {
            this.properties!!.putAll(properties)
        }
    }

}
