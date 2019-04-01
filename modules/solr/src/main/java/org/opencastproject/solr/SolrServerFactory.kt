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

package org.opencastproject.solr

import org.opencastproject.solr.internal.EmbeddedSolrServerWrapper
import org.opencastproject.util.PathSupport

import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer
import org.apache.solr.client.solrj.impl.XMLResponseParser
import org.apache.solr.core.CoreContainer
import org.apache.solr.core.OpencastSolrConfig
import org.apache.solr.core.SolrConfig
import org.apache.solr.core.SolrCore
import org.apache.solr.schema.IndexSchema
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URL

/**
 * Factory class that will create clients to solr server instances that are either running inside the local virtual
 * machine or remotely.
 */
object SolrServerFactory {

    /** Log facility  */
    private val logger = LoggerFactory.getLogger(SolrServerFactory::class.java)

    /** Configuration key for default embedded solr location  */
    val PROP_GLOBAL_SOLR_DIR = "org.opencastproject.solr.dir"

    /** Default embedded solr location (relative to KARAF_DATA)  */
    val DEFAULT_GLOBAL_SOLR_DIR = "solr-indexes"

    /**
     * Constructor. Prepares solr connection.
     *
     * @param solrDir
     * The directory of the solr instance.
     * @param dataDir
     * The directory of the solr index data.
     */
    @Throws(SolrServerException::class)
    fun newEmbeddedInstance(solrDir: File, dataDir: File): SolrServer {
        try {
            val cc = CoreContainer(solrDir.absolutePath)
            val config = OpencastSolrConfig(solrDir.absolutePath, SolrConfig.DEFAULT_CONF_FILE, null)
            val schema = IndexSchema(config, "$solrDir/conf/schema.xml", null)
            val core0 = SolrCore(null, dataDir.absolutePath, config, schema, null)
            cc.register("core0", core0, false)
            return EmbeddedSolrServerWrapper(cc, "core0")
        } catch (e: Exception) {
            throw SolrServerException(e)
        }

    }

    /**
     * Constructor. Prepares solr connection.
     *
     * @param url
     * the connection url to the solr server
     */
    fun newRemoteInstance(url: URL): SolrServer {
        try {
            val server = CommonsHttpSolrServer(url)
            server.setSoTimeout(5000)
            server.setConnectionTimeout(5000)
            server.setDefaultMaxConnectionsPerHost(100)
            server.setMaxTotalConnections(100)
            server.setFollowRedirects(false) // defaults to false
            server.setAllowCompression(true)
            server.setMaxRetries(1) // defaults to 0. > 1 not recommended.
            server.parser = XMLResponseParser() // binary parser is used by default
            return server
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    /**
     * Shuts down a solr server or, in the case of a connection to a remote server, closes the connection.
     *
     * @param solrServer
     * the solr server instance
     */
    fun shutdown(solrServer: SolrServer) {
        if (solrServer is EmbeddedSolrServerWrapper) {
            solrServer.shutdown()
        } else {
            // TODO: there doesn't appear to be any mechanism to close a remote connection to a solr server.
        }
    }

    /**
     * Get location for embedded SOLR from configuration.
     *
     * @param cc
     * ComponentContext to get configuration from
     * @param indexKey
     * Configuration key for specific index
     * @param defaultDir
     * Default directory for specific index
     */
    @Throws(IllegalStateException::class)
    fun getEmbeddedDir(cc: ComponentContext, indexKey: String, defaultDir: String): String {
        /* Check if we have a specific key for this SOLR index */
        var solrDir = cc.bundleContext.getProperty(indexKey)
        if (StringUtils.isNotEmpty(solrDir)) {
            return solrDir
        }
        logger.debug("No explicit location configuration for embedded solr. Trying $\\{{}\\}/{}",
                PROP_GLOBAL_SOLR_DIR, defaultDir)

        /* Check if we have a globas SOLR directory configuration */
        solrDir = cc.bundleContext.getProperty(PROP_GLOBAL_SOLR_DIR)
        if (StringUtils.isNotEmpty(solrDir)) {
            return PathSupport.concat(solrDir, defaultDir)
        }
        logger.debug("No general location configuration for embedded solr. Trying $\\{karaf.data\\}/{}/{}",
                DEFAULT_GLOBAL_SOLR_DIR, defaultDir)
        solrDir = cc.bundleContext.getProperty("karaf.data")
        if (StringUtils.isNotEmpty(solrDir)) {
            solrDir = PathSupport.concat(solrDir, SolrServerFactory.DEFAULT_GLOBAL_SOLR_DIR)
            return PathSupport.concat(solrDir, defaultDir)
        }

        /* No config found. No fallback possible */
        throw IllegalStateException("Could not set SOLR root dir. All properties are empty.")
    }

}
/** Disallow construction of this utility class  */
