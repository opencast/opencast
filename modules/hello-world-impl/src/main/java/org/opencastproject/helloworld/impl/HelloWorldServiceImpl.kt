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

package org.opencastproject.helloworld.impl

import org.opencastproject.helloworld.api.HelloWorldService

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A simple tutorial class to learn about Opencast Services
 */
class HelloWorldServiceImpl : HelloWorldService {

    override fun helloWorld(): String {
        logger.info("Hello World")
        return "Hello World"
    }

    override fun helloName(name: String): String {
        logger.info("Name is {}.", name)
        return if ("" == name) {
            "Hello!"
        } else "Hello $name!"
    }

    companion object {

        /** The module specific logger  */
        private val logger = LoggerFactory.getLogger(HelloWorldServiceImpl::class.java)
    }
}
