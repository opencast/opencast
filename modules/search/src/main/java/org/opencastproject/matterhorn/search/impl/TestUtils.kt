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


package org.opencastproject.matterhorn.search.impl

/**
 * Utility class containing a few helper methods.
 */
object TestUtils {

    /** Name of the property to indicate an ongoing unit or integration test  */
    private val TEST_PROPERTY = "matterhorn.test"

    /**
     * Returns `true` if a test is currently going on.
     *
     * @return `true` if the current code is being executed as a test
     */
    val isTest: Boolean
        get() = "true".equals(System.getProperty(TEST_PROPERTY), ignoreCase = true)

    /**
     * Enables testing by setting a system property. This method is used to add test specific code to production
     * implementations while using a consistent methodology to determine testing status.
     *
     *
     * Use [.isTest] to determine whether testing has been turned on.
     */
    fun startTesting() {
        System.setProperty(TEST_PROPERTY, java.lang.Boolean.TRUE.toString())
    }

}
/**
 * This utility class is not intended to be instantiated.
 */// Nothing to do
