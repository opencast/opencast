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

package org.opencastproject.engage.theodul.api

/**
 * Interface for the EngagePluginRegistration
 * Describes and engage plugin and stores its values
 */
interface EngagePluginRegistration {

    /**
     * Returns the description of the engage plugin
     * @return description
     */
    val description: String

    /**
     * Returns the ID of the engage plugin
     * @return ID
     */
    val id: Int

    /**
     * Returns the name of the engage plugin
     * @return name
     */
    val name: String

    /**
     * Returns the REST path of the engage plugin
     * @return REST path
     */
    val restPath: String

    /**
     * Returns the static resource path of the engage plugin
     * @return static resource path
     */
    val staticPath: String

    /**
     * Returns whether this engage plugin has a REST endpoint
     * @return true if this plugin has a REST endpoint
     */
    fun hasRestEndpoint(): Boolean

    /**
     * Returns whether this engage plugin has a static resource path
     * @return true if this plugin has a static resource path, false else
     */
    fun hasStaticResources(): Boolean

}
