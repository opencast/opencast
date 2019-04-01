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

import java.lang.String.format
import org.apache.http.HttpStatus.SC_ACCEPTED
import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_OK

import org.opencastproject.job.api.JobContext
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action

import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.ArrayList

/**
 * Workflow operation handler that will send HTTP POST or PUT requests to a specified address.
 *
 *
 * The request can contain a message type and a message body and automatically includes the workflow instance id. Should
 * the notification fail, a retry strategy is implemented.
 *
 *
 * Requests will be send using the POST method by default, PUT is a supported alternative method.
 *
 */
class HttpNotificationWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    /** The http client to use when connecting to remote servers  */
    protected var client: HttpClient? = null

    /**
     * {@inheritDoc}
     */
    @Throws(WorkflowOperationException::class)
    override fun start(workflowInstance: WorkflowInstance, context: JobContext): WorkflowOperationResult {
        logger.debug("Running HTTP notification workflow operation on workflow {}", workflowInstance.id)

        var maxRetry = DEFAULT_MAX_RETRY
        var timeout = DEFAULT_TIMEOUT

        // Required configuration
        val urlPath = getConfig(workflowInstance, OPT_URL_PATH)

        // Optional configuration
        val notificationSubject = getConfig(workflowInstance, OPT_NOTIFICATION_SUBJECT, null)
        val notificationMessage = getConfig(workflowInstance, OPT_NOTIFICATION_MESSAGE, null)
        val method = getConfig(workflowInstance, OPT_METHOD, "post")
        val maxRetryOpt = getConfig(workflowInstance, OPT_MAX_RETRY, null)
        val timeoutOpt = getConfig(workflowInstance, OPT_TIMEOUT, null)


        // If set, convert the timeout to milliseconds
        if (timeoutOpt != null) {
            timeout = Integer.parseInt(timeoutOpt) * 1000
        }

        // Is there a need to retry on failure?
        if (maxRetryOpt != null) {
            maxRetry = Integer.parseInt(maxRetryOpt)
        }

        // Figure out which request method to use
        var request: HttpEntityEnclosingRequestBase? = null
        if (StringUtils.equalsIgnoreCase("post", method)) {
            request = HttpPost(urlPath)
        } else if (StringUtils.equalsIgnoreCase("put", method)) {
            request = HttpPut(urlPath)
        } else {
            throw WorkflowOperationException("The configuration key '$OPT_METHOD' only supports 'post' and 'put'")
        }
        logger.debug("Request will be sent using the '{}' method", method)

        // Add event parameters as form parameters
        try {
            val params = ArrayList<BasicNameValuePair>()

            // Add the subject (if specified)
            if (notificationSubject != null) {
                params.add(BasicNameValuePair(HTTP_PARAM_SUBJECT, notificationSubject))
            }

            // Add the message (if specified)
            if (notificationMessage != null) {
                params.add(BasicNameValuePair(HTTP_PARAM_MESSAGE, notificationMessage))
            }

            // Add the workflow instance id
            params.add(BasicNameValuePair(HTTP_PARAM_WORKFLOW, java.lang.Long.toString(workflowInstance.id)))

            request.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw WorkflowOperationException(
                    "Error happened during the encoding of the event parameter as form parameter:", e)
        }

        // Execute the request
        if (!executeRequest(request, maxRetry, timeout, INITIAL_SLEEP_TIME)) {
            throw WorkflowOperationException(format("Notification could not be delivered to %s", urlPath))
        }

        return createResult(workflowInstance.mediaPackage, Action.CONTINUE)
    }

    /**
     * Execute the given notification request. If the target is not responding, retry as many time as the maxAttampts
     * parameter with in between each try a sleep time.
     *
     * @param request
     * The request to execute
     * @param maxAttempts
     * The number of attempts in case of error
     * @param timeout
     * The wait time in milliseconds at which a connection attempt will throw
     * @return true if the request has been executed successfully
     */
    private fun executeRequest(request: HttpUriRequest, maxAttempts: Int, timeout: Int, sleepTime: Int): Boolean {
        var maxAttempts = maxAttempts

        logger.debug(format("Executing notification request on target %s, %d attemps left", request.uri, maxAttempts))

        val config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout).build()
        val httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()

        val response: HttpResponse
        try {
            response = httpClient.execute(request)
        } catch (e: ClientProtocolException) {
            logger.error("Protocol error during execution of query on target {}: {}", request.uri, e.message)
            return false
        } catch (e: IOException) {
            logger.error("I/O error during execution of query on target {}: {}", request.uri, e.message)
            return false
        }

        val statusCode = response.getStatusLine().statusCode
        if (statusCode == SC_OK || statusCode == SC_NO_CONTENT || statusCode == SC_ACCEPTED) {
            logger.debug(format("Request successfully executed on target %s, status code: %d", request.uri, statusCode))
            return true
        } else if (maxAttempts > 1) {
            logger.debug(format("Request failed on target %s, status code: %d, will retry in %d seconds", request.uri,
                    statusCode, sleepTime / 1000))
            try {
                Thread.sleep(sleepTime.toLong())
                return executeRequest(request, --maxAttempts, timeout, sleepTime * SLEEP_SCALE_FACTOR)
            } catch (e: InterruptedException) {
                logger.error("Error during sleep time before new notification request try: {}", e.message)
                return false
            }

        } else {
            logger.warn("Request failed on target {}, status code: {}, no more attempt.", request.uri, statusCode)
            return false
        }
    }

    companion object {

        /** Configuration key for the target URL of the notification request  */
        val OPT_URL_PATH = "url"

        /** Configuration key for the notification event  */
        val OPT_NOTIFICATION_SUBJECT = "subject"

        /** Configuration key for the notification message  */
        val OPT_NOTIFICATION_MESSAGE = "message"

        /** Configuration key for the HTTP method to use (put or post)  */
        val OPT_METHOD = "method"

        /** Configuration key for the maximal attempts for the notification request  */
        val OPT_MAX_RETRY = "max-retry"

        /** Configuration key for the request timeout  */
        val OPT_TIMEOUT = "timeout"

        /** Name of the subject HTTP parameter  */
        val HTTP_PARAM_SUBJECT = "subject"

        /** Name of the message HTTP parameter  */
        val HTTP_PARAM_MESSAGE = "message"

        /** Name of the workflow instance id HTTP parameter  */
        val HTTP_PARAM_WORKFLOW = "workflowInstanceId"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(HttpNotificationWorkflowOperationHandler::class.java)

        /** Default value for the number of attempts for a request  */
        private val DEFAULT_MAX_RETRY = 5

        /** Default maximum wait time the client when trying to execute a request  */
        private val DEFAULT_TIMEOUT = 10 * 1000

        /** Default time between two request attempts  */
        val INITIAL_SLEEP_TIME = 10 * 1000

        /** The scale factor to the sleep time between two notification attempts  */
        val SLEEP_SCALE_FACTOR = 2
    }

}
