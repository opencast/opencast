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
package org.opencastproject.security.urlsigning.provider.impl

import org.opencastproject.security.urlsigning.WowzaResourceStrategyImpl
import org.opencastproject.urlsigning.common.ResourceStrategy

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WowzaUrlSigningProvider : AbstractUrlSigningProvider() {
    /** The Wowza resource strategy to use to convert from the base url to a resource url.  */
    override val resourceStrategy: ResourceStrategy = WowzaResourceStrategyImpl()

    override fun getLogger(): Logger {
        return logger
    }

    override fun toString(): String {
        return "Wowza URL Signing Provider"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WowzaUrlSigningProvider::class.java)
    }

}
