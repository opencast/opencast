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

package org.opencastproject.solr.internal

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.core.CoreContainer

/**
 * Wrapper around the embedded solr server class providing the ability to cleanly shut down the server instance.
 */
class EmbeddedSolrServerWrapper
/**
 * Creates a new wrapped instance of the
 *
 * @param coreContainer
 * the core container
 * @param coreName
 * name of the core
 */
(coreContainer: CoreContainer, coreName: String) : EmbeddedSolrServer(coreContainer, coreName) {

    /** Reference to the solr core  */
    private val coreContainer: CoreContainer? = null

    init {
        this.coreContainer = coreContainer
    }

    /**
     * Shuts down the embedded solr server by forwarding the shutdown command to the [CoreContainer].
     */
    fun shutdown() {
        if (coreContainer != null)
            coreContainer!!.shutdown()
    }

    companion object {

        /** Serial version UID  */
        private val serialVersionUID = 6563713572181761065L
    }

}
