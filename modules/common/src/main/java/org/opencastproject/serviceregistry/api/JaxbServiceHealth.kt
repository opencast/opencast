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

package org.opencastproject.serviceregistry.api

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * A wrapper for service health info.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "health", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "health", namespace = "http://serviceregistry.opencastproject.org")
class JaxbServiceHealth {

    @XmlElement(name = "healthy")
    protected var healthy: Int? = 0

    @XmlElement(name = "warning")
    protected var warning: Int? = 0

    @XmlElement(name = "error")
    protected var error: Int? = 0

    constructor() {}

    constructor(healthy: Int?, warning: Int?, error: Int?) {
        this.healthy = healthy
        this.warning = warning
        this.error = error
    }
}

