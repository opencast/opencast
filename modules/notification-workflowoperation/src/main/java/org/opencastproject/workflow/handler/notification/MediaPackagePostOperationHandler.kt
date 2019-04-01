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

package org.opencastproject.workflow.handler.notification

import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchService
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URL
import java.util.ArrayList

/**
 * Workflow Operation for POSTing a MediaPackage via HTTP
 */
class MediaPackagePostOperationHandler : AbstractWorkflowOperationHandler() {

    /** search service  */
    private var searchService: SearchService? = null

    fun setSearchService(searchService: SearchService) {
        this.searchService = searchService
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowOperationHandler.start
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {

        // get configuration
        val currentOperation = workflowInstance.currentOperation
        val config = Configuration(currentOperation)

        val workflowMP = workflowInstance.mediaPackage
        var mp = workflowMP

        /* Check if we need to replace the Mediapackage we got with the published
     * Mediapackage from the Search Service */
        if (config.mpFromSearch()) {
            val searchQuery = SearchQuery()
            searchQuery.withId(mp.identifier.toString())
            val result = searchService!!.getByQuery(searchQuery)
            if (result.size() != 1L) {
                throw WorkflowOperationException("Received multiple results for identifier"
                        + "\"" + mp.identifier.toString() + "\" from search service. ")
            }
            logger.info("Getting Mediapackage from Search Service")
            mp = result.items[0].mediaPackage
        }

        logger.info("Submitting \"" + mp.title + "\" (" + mp.identifier.toString() + ") as "
                + config.format.name + " to " + config.url!!.toString())

        try {
            // serialize MediaPackage to target format
            val mpStr: String
            if (config.format == Configuration.Format.JSON) {
                mpStr = MediaPackageParser.getAsJSON(mp)
            } else {
                mpStr = MediaPackageParser.getAsXml(mp)
            }

            // Log mediapackge
            if (config.debug()) {
                logger.info(mpStr)
            }

            // constrcut message body
            val data = ArrayList<NameValuePair>()
            data.add(BasicNameValuePair("mediapackage", mpStr))
            data.addAll(config.getAdditionalFields())

            // construct POST
            val post = HttpPost(config.url)
            post.entity = UrlEncodedFormEntity(data, config.encoding)

            // execute POST
            val client = DefaultHttpClient()

            // Handle authentication
            if (config.authenticate()) {
                val targetUrl = config.url!!.toURL()
                client.credentialsProvider.setCredentials(
                        AuthScope(targetUrl.host, targetUrl.port),
                        config.credentials)
            }

            val response = client.execute(post)

            // throw Exception if target host did not return 200
            val status = response.statusLine.statusCode
            if (status >= 200 && status < 300) {
                if (config.debug()) {
                    logger.info("Successfully submitted \"" + mp.title
                            + "\" (" + mp.identifier.toString() + ") to " + config.url!!.toString()
                            + ": " + status)
                }
            } else if (status == 418) {
                logger.warn("Submitted \"" + mp.title + "\" ("
                        + mp.identifier.toString() + ") to " + config.url!!.toString()
                        + ": The target claims to be a teapot. "
                        + "The Reason for this is probably an insane programmer.")
            } else {
                throw WorkflowOperationException("Faild to submit \"" + mp.title
                        + "\" (" + mp.identifier.toString() + "), " + config.url!!.toString()
                        + " answered with: " + Integer.toString(status))
            }
        } catch (e: Exception) {
            if (e is WorkflowOperationException) {
                throw e
            } else {
                logger.error("Submitting mediapackage failed: {}", e.toString())
                throw WorkflowOperationException(e)
            }
        }

        return createResult(workflowMP, Action.CONTINUE)
    }

    // <editor-fold defaultstate="collapsed" desc="Inner class that wraps around this WorkflowOperations Configuration">
    private class Configuration @Throws(WorkflowOperationException::class)
    internal constructor(operation: WorkflowOperationInstance) {

        // Configuration values
        var url: URI? = null
            private set
        var format = Format.XML
            private set
        var encoding = "UTF-8"
            private set
        private var authenticate = false
        var credentials: UsernamePasswordCredentials? = null
            private set
        private val additionalFields = ArrayList<NameValuePair>()
        private var debug = false
        private var mpFromSearch = true

        enum class Format {
            XML, JSON
        }

        init {
            try {
                val keys = operation.configurationKeys

                // configure URL
                if (keys.contains(PROPERTY_URL)) {
                    url = URI(operation.getConfiguration(PROPERTY_URL))
                } else {
                    throw IllegalArgumentException("No target URL provided.")
                }

                // configure format
                if (keys.contains(PROPERTY_FORMAT)) {
                    format = Format.valueOf(operation.getConfiguration(PROPERTY_FORMAT).toUpperCase())
                }

                // configure message encoding
                if (keys.contains(PROPERTY_ENCODING)) {
                    encoding = operation.getConfiguration(PROPERTY_ENCODING)
                }

                // configure authentication
                if (keys.contains(PROPERTY_AUTH)) {
                    val auth = operation.getConfiguration(PROPERTY_AUTH).toUpperCase()
                    if ("NO" != auth && "FALSE" != auth) {
                        val username = operation.getConfiguration(PROPERTY_AUTHUSER)
                        val password = operation.getConfiguration(PROPERTY_AUTHPASSWD)
                        if (username == null || password == null) {
                            throw WorkflowOperationException("Username and Password must be provided for authentication!")
                        }
                        credentials = UsernamePasswordCredentials(username, password)
                        authenticate = true
                    }
                }

                // Configure debug mode
                if (keys.contains(PROPERTY_DEBUG)) {
                    val debugstr = operation.getConfiguration(PROPERTY_DEBUG).trim { it <= ' ' }.toUpperCase()
                    debug = "YES" == debugstr || "TRUE" == debugstr
                }

                // Configure debug mode
                if (keys.contains(PROPERTY_MEDIAPACKAGE_TYPE)) {
                    val cfgval = operation.getConfiguration(PROPERTY_MEDIAPACKAGE_TYPE).trim { it <= ' ' }.toUpperCase()
                    mpFromSearch = "SEARCH" == cfgval
                }

                // get additional form fields
                val iter = operation.configurationKeys.iterator()
                while (iter.hasNext()) {
                    val key = iter.next()
                    if (key.startsWith("+")) {
                        val value = operation.getConfiguration(key)
                        additionalFields.add(BasicNameValuePair(key.substring(1), value))
                    }
                }
            } catch (e: Exception) {
                throw WorkflowOperationException("Faild to configure operation instance.", e)
            }

        }

        fun authenticate(): Boolean {
            return authenticate
        }

        fun getAdditionalFields(): List<NameValuePair> {
            return additionalFields
        }

        fun debug(): Boolean {
            return debug
        }

        fun mpFromSearch(): Boolean {
            return mpFromSearch
        }

        companion object {

            // Key for the WorkflowOperation Configuration
            val PROPERTY_URL = "url"
            val PROPERTY_FORMAT = "format"
            val PROPERTY_ENCODING = "encoding"
            val PROPERTY_AUTH = "auth.enabled"
            val PROPERTY_AUTHUSER = "auth.username"
            val PROPERTY_AUTHPASSWD = "auth.password"
            val PROPERTY_DEBUG = "debug"
            val PROPERTY_MEDIAPACKAGE_TYPE = "mediapackage.type"
        }
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(MediaPackagePostOperationHandler::class.java)
    }

    // </editor-fold>
}
