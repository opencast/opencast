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

package org.opencastproject.workflow.remote

import org.apache.http.HttpStatus.SC_CONFLICT
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_OK
import org.apache.http.HttpStatus.SC_PRECONDITION_FAILED
import org.apache.http.HttpStatus.SC_UNAUTHORIZED

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.SolrUtils
import org.opencastproject.workflow.api.WorkflowDatabaseException
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowException
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowListener
import org.opencastproject.workflow.api.WorkflowParser
import org.opencastproject.workflow.api.WorkflowQuery
import org.opencastproject.workflow.api.WorkflowQuery.QueryTerm
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workflow.api.WorkflowSet
import org.opencastproject.workflow.api.WorkflowStatistics

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.ParseException
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Collections
import kotlin.collections.Map.Entry

/**
 * An implementation of the workflow service that communicates with a remote workflow service via HTTP.
 */
class WorkflowServiceRemoteImpl : RemoteBase(WorkflowService.JOB_TYPE), WorkflowService {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.getWorkflowDefinitionById
     */
    @Throws(WorkflowDatabaseException::class, NotFoundException::class)
    override fun getWorkflowDefinitionById(id: String): WorkflowDefinition {
        val get = HttpGet("/definition/$id.xml")
        val response = getResponse(get, SC_NOT_FOUND, SC_OK)
        try {
            if (response != null) {
                return if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Workflow definition $id does not exist.")
                } else {
                    WorkflowParser.parseWorkflowDefinition(response.entity.content)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowDatabaseException(e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to connect to a remote workflow service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.getWorkflowById
     */
    @Throws(WorkflowDatabaseException::class, NotFoundException::class)
    override fun getWorkflowById(id: Long): WorkflowInstance {
        val get = HttpGet("/instance/$id.xml")
        val response = getResponse(get, SC_NOT_FOUND, SC_OK)
        try {
            if (response != null) {
                return if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Workflow instance $id does not exist.")
                } else {
                    WorkflowParser.parseWorkflowInstance(response.entity.content)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowDatabaseException(e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to connect to a remote workflow service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService
     * .getWorkflowInstances
     */
    @Throws(WorkflowDatabaseException::class)
    override fun getWorkflowInstances(query: WorkflowQuery): WorkflowSet {
        val queryStringParams = ArrayList<NameValuePair>()

        if (query.text != null)
            queryStringParams.add(BasicNameValuePair("q", query.text))

        if (query.states != null) {
            for (stateQueryTerm in query.states) {
                val value = if (stateQueryTerm.isInclude) stateQueryTerm.value else "-" + stateQueryTerm.value
                queryStringParams.add(BasicNameValuePair("state", value))
            }
        }

        if (query.currentOperations != null) {
            for (opQueryTerm in query.currentOperations) {
                val value = if (opQueryTerm.isInclude) opQueryTerm.value else "-" + opQueryTerm.value
                queryStringParams.add(BasicNameValuePair("op", value))
            }
        }

        if (query.seriesId != null)
            queryStringParams.add(BasicNameValuePair("seriesId", query.seriesId))

        if (query.seriesTitle != null)
            queryStringParams.add(BasicNameValuePair("seriesTitle", query.seriesTitle))

        if (query.mediaPackageId != null)
            queryStringParams.add(BasicNameValuePair("mp", query.mediaPackageId))

        if (query.workflowDefinitionId != null)
            queryStringParams.add(BasicNameValuePair("workflowdefinition", query.workflowDefinitionId))

        if (query.fromDate != null)
            queryStringParams.add(BasicNameValuePair("fromdate", SolrUtils.serializeDate(query.fromDate)))

        if (query.toDate != null)
            queryStringParams.add(BasicNameValuePair("todate", SolrUtils.serializeDate(query.toDate)))

        if (query.creator != null)
            queryStringParams.add(BasicNameValuePair("creator", query.creator))

        if (query.contributor != null)
            queryStringParams.add(BasicNameValuePair("contributor", query.contributor))

        if (query.language != null)
            queryStringParams.add(BasicNameValuePair("language", query.language))

        if (query.license != null)
            queryStringParams.add(BasicNameValuePair("license", query.license))

        if (query.title != null)
            queryStringParams.add(BasicNameValuePair("title", query.title))

        if (query.subject != null)
            queryStringParams.add(BasicNameValuePair("subject", query.subject))

        if (query.sort != null) {
            var sort = query.sort.toString()
            if (!query.isSortAscending) {
                sort += "_DESC"
            }
            queryStringParams.add(BasicNameValuePair("sort", sort))
        }

        if (query.startPage > 0)
            queryStringParams.add(BasicNameValuePair("startPage", java.lang.Long.toString(query.startPage)))

        if (query.count > 0)
            queryStringParams.add(BasicNameValuePair("count", java.lang.Long.toString(query.count)))

        val url = StringBuilder()
        url.append("/instances.xml")
        if (queryStringParams.size > 0)
            url.append("?" + URLEncodedUtils.format(queryStringParams, "UTF-8"))

        val get = HttpGet(url.toString())
        val response = getResponse(get)
        try {
            if (response != null)
                return WorkflowParser.parseWorkflowSet(response.entity.content)
        } catch (e: Exception) {
            throw WorkflowDatabaseException(e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Workflow instances can not be loaded from a remote workflow service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.getWorkflowInstancesForAdministrativeRead
     */
    @Throws(WorkflowDatabaseException::class, UnauthorizedException::class)
    override fun getWorkflowInstancesForAdministrativeRead(q: WorkflowQuery): WorkflowSet {
        return getWorkflowInstances(q)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.getStatistics
     */
    @Throws(WorkflowDatabaseException::class)
    override fun getStatistics(): WorkflowStatistics {
        val get = HttpGet("/statistics.xml")
        val response = getResponse(get)
        try {
            if (response != null)
                return WorkflowParser.parseWorkflowStatistics(response.entity.content)
        } catch (e: Exception) {
            throw WorkflowDatabaseException("Unable to load workflow statistics", e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to connect to a remote workflow service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.start
     */
    @Throws(WorkflowDatabaseException::class)
    override fun start(workflowDefinition: WorkflowDefinition, mediaPackage: MediaPackage,
                       properties: Map<String, String>): WorkflowInstance {
        try {
            return start(workflowDefinition, mediaPackage, null, properties)
        } catch (e: NotFoundException) {
            throw IllegalStateException("A null parent workflow id should never result in a not found exception ", e)
        }

    }

    /**
     * Converts a Map<String></String>, String> to s key=value\n string, suitable for the properties form parameter expected by the
     * workflow rest endpoint.
     *
     * @param props
     * The map of strings
     * @return the string representation
     */
    private fun mapToString(props: Map<String, String>): String {
        val sb = StringBuilder()
        for ((key, value) in props) {
            sb.append(key)
            sb.append("=")
            sb.append(value)
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.start
     */
    @Throws(WorkflowDatabaseException::class, NotFoundException::class)
    override fun start(workflowDefinition: WorkflowDefinition?, mediaPackage: MediaPackage,
                       parentWorkflowId: Long?, properties: Map<String, String>?): WorkflowInstance {
        val post = HttpPost("/start")
        try {
            val params = ArrayList<BasicNameValuePair>()
            if (workflowDefinition != null)
                params.add(BasicNameValuePair("definition", WorkflowParser.toXml(workflowDefinition)))
            params.add(BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)))
            if (parentWorkflowId != null)
                params.add(BasicNameValuePair("parent", parentWorkflowId.toString()))
            if (properties != null)
                params.add(BasicNameValuePair("properties", mapToString(properties)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw IllegalStateException("Unable to assemble a remote workflow request", e)
        }

        val response = getResponse(post, SC_NOT_FOUND, SC_OK)
        try {
            if (response != null) {
                return if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Workflow instance $parentWorkflowId does not exist.")
                } else {
                    WorkflowParser.parseWorkflowInstance(response.entity.content)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowDatabaseException("Unable to build a workflow from xml", e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to start a remote workflow. The http response code was unexpected.")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.start
     */
    @Throws(WorkflowDatabaseException::class)
    override fun start(workflowDefinition: WorkflowDefinition, mediaPackage: MediaPackage): WorkflowInstance {
        try {
            return start(workflowDefinition, mediaPackage, null, null)
        } catch (e: NotFoundException) {
            throw IllegalStateException("A null parent workflow id should never result in a not found exception ", e)
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.countWorkflowInstances
     */
    @Throws(WorkflowDatabaseException::class)
    override fun countWorkflowInstances(): Long {
        return countWorkflowInstances(null, null)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.countWorkflowInstances
     */
    @Throws(WorkflowDatabaseException::class)
    override fun countWorkflowInstances(state: WorkflowState?, operation: String?): Long {
        val queryStringParams = ArrayList<NameValuePair>()
        if (state != null)
            queryStringParams.add(BasicNameValuePair("state", state.toString()))
        if (operation != null)
            queryStringParams.add(BasicNameValuePair("operation", operation))

        val url = StringBuilder("/count")
        if (queryStringParams.size > 0) {
            url.append("?")
            url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"))
        }

        val get = HttpGet(url.toString())
        val response = getResponse(get)
        try {
            if (response != null) {
                var body: String? = null
                try {
                    body = EntityUtils.toString(response.entity)
                    return java.lang.Long.parseLong(body!!)
                } catch (e: NumberFormatException) {
                    throw WorkflowDatabaseException("Unable to parse the response body as a long: " + body!!)
                }

            }
        } catch (e: ParseException) {
            throw WorkflowDatabaseException("Unable to parse the response body")
        } catch (e: IOException) {
            throw WorkflowDatabaseException("Unable to parse the response body")
        } finally {
            closeConnection(response)
        }

        throw WorkflowDatabaseException("Unable to count workflow instances")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.stop
     */
    @Throws(WorkflowDatabaseException::class, NotFoundException::class)
    override fun stop(workflowInstanceId: Long): WorkflowInstance {
        val post = HttpPost("/stop")
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("id", java.lang.Long.toString(workflowInstanceId)))
        try {
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Unable to assemble a remote workflow service request", e)
        }

        val response = getResponse(post, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (response.statusLine.statusCode == SC_NOT_FOUND) {
                    throw NotFoundException("Workflow instance with id='$workflowInstanceId' not found")
                } else {
                    logger.info("Workflow '{}' stopped", workflowInstanceId)
                    return WorkflowParser.parseWorkflowInstance(response.entity.content)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowDatabaseException(e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to stop workflow instance $workflowInstanceId")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.suspend
     */
    @Throws(WorkflowDatabaseException::class, NotFoundException::class)
    override fun suspend(workflowInstanceId: Long): WorkflowInstance {
        val post = HttpPost("/suspend")
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("id", java.lang.Long.toString(workflowInstanceId)))
        try {
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Unable to assemble a remote workflow service request", e)
        }

        val response = getResponse(post, SC_OK, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (response.statusLine.statusCode == SC_NOT_FOUND) {
                    throw NotFoundException("Workflow instance with id='$workflowInstanceId' not found")
                } else {
                    logger.info("Workflow '{}' suspended", workflowInstanceId)
                    return WorkflowParser.parseWorkflowInstance(response.entity.content)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowDatabaseException(e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to suspend workflow instance $workflowInstanceId")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.resume
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, WorkflowException::class, IllegalStateException::class)
    override fun resume(workflowInstanceId: Long): WorkflowInstance {
        return resume(workflowInstanceId, null)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.resume
     */
    @Throws(NotFoundException::class, UnauthorizedException::class, WorkflowException::class, IllegalStateException::class)
    override fun resume(workflowInstanceId: Long, properties: Map<String, String>?): WorkflowInstance {
        val post = HttpPost("/resume")
        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("id", java.lang.Long.toString(workflowInstanceId)))
        if (properties != null)
            params.add(BasicNameValuePair("properties", mapToString(properties)))
        post.entity = UrlEncodedFormEntity(params, StandardCharsets.UTF_8)
        val response = getResponse(post, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED, SC_CONFLICT)
        try {
            if (response != null) {
                if (response.statusLine.statusCode == SC_NOT_FOUND) {
                    throw NotFoundException("Workflow instance with id='$workflowInstanceId' not found")
                } else if (response.statusLine.statusCode == SC_UNAUTHORIZED) {
                    throw UnauthorizedException("You do not have permission to resume")
                } else if (response.statusLine.statusCode == SC_CONFLICT) {
                    throw IllegalStateException("Can not resume a workflow where the current state is not in paused")
                } else {
                    logger.info("Workflow '{}' resumed", workflowInstanceId)
                    return WorkflowParser.parseWorkflowInstance(response.entity.content)
                }
            }
        } catch (e: NotFoundException) {
            throw e
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw WorkflowException(e)
        } finally {
            closeConnection(response)
        }
        throw WorkflowException("Unable to resume workflow instance $workflowInstanceId")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.update
     */
    @Throws(WorkflowDatabaseException::class)
    override fun update(workflowInstance: WorkflowInstance) {
        val post = HttpPost("/update")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("workflow", WorkflowParser.toXml(workflowInstance)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Unable to assemble a remote workflow service request", e)
        } catch (e: Exception) {
            throw IllegalStateException("unable to serialize workflow instance to xml")
        }

        val response = getResponse(post, SC_NO_CONTENT)
        try {
            if (response != null) {
                logger.info("Workflow '{}' updated", workflowInstance)
                return
            }
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to update workflow instance " + workflowInstance.id)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.remove
     */
    @Throws(WorkflowDatabaseException::class, NotFoundException::class)
    override fun remove(workflowInstanceId: Long) {
        val delete = HttpDelete("/remove/" + java.lang.Long.toString(workflowInstanceId))
        val response = getResponse(delete, SC_NO_CONTENT, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Workflow id not found: $workflowInstanceId")
                } else {
                    logger.info("Workflow '{}' removed", workflowInstanceId)
                    return
                }
            }
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to remove workflow instance $workflowInstanceId")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.listAvailableWorkflowDefinitions
     */
    @Throws(WorkflowDatabaseException::class)
    override fun listAvailableWorkflowDefinitions(): List<WorkflowDefinition> {
        val get = HttpGet("/definitions.xml")
        val response = getResponse(get)
        try {
            if (response != null) {
                val list = WorkflowParser.parseWorkflowDefinitions(response.entity.content)
                Collections.sort(list) //sorts by title
                return list
            }
        } catch (e: Exception) {
            throw IllegalStateException("Unable to parse workflow definitions")
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException(
                "Unable to read the registered workflow definitions from the remote workflow service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.registerWorkflowDefinition
     */
    @Throws(WorkflowDatabaseException::class)
    override fun registerWorkflowDefinition(workflow: WorkflowDefinition) {
        val put = HttpPut("/definition")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("workflowDefinition", WorkflowParser.toXml(workflow)))
            put.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Unable to assemble a remote workflow service request", e)
        } catch (e: Exception) {
            throw IllegalStateException("unable to serialize workflow definition to xml")
        }

        val response = getResponse(put, SC_CREATED, SC_PRECONDITION_FAILED)
        try {
            if (response != null) {
                if (SC_PRECONDITION_FAILED == response.statusLine.statusCode) {
                    throw IllegalStateException("A workflow definition with ID '" + workflow.id
                            + "' is already registered.")
                } else {
                    logger.info("Workflow definition '{}' registered", workflow)
                    return
                }
            }
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to register workflow definition " + workflow.id)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.unregisterWorkflowDefinition
     */
    @Throws(NotFoundException::class, WorkflowDatabaseException::class)
    override fun unregisterWorkflowDefinition(workflowDefinitionId: String) {
        val delete = HttpDelete("/definition/$workflowDefinitionId")
        val response = getResponse(delete, SC_NO_CONTENT, SC_NOT_FOUND)
        try {
            if (response != null) {
                if (SC_NOT_FOUND == response.statusLine.statusCode) {
                    throw NotFoundException("Workflow definition '$workflowDefinitionId' not found.")
                } else {
                    logger.info("Workflow definition '{}' unregistered", workflowDefinitionId)
                    return
                }
            }
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to delete workflow definition '$workflowDefinitionId'")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.addWorkflowListener
     */
    override fun addWorkflowListener(listener: WorkflowListener) {
        throw UnsupportedOperationException("Adding workflow listeners to a remote workflow service is not supported")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workflow.api.WorkflowService.removeWorkflowListener
     */
    override fun removeWorkflowListener(listener: WorkflowListener) {
        throw UnsupportedOperationException(
                "Removing workflow listeners from a remote workflow service is not supported")
    }

    @Throws(WorkflowDatabaseException::class, UnauthorizedException::class)
    override fun cleanupWorkflowInstances(lifetime: Int, state: WorkflowState?) {
        val post = HttpPost("/cleanup")

        val params = ArrayList<BasicNameValuePair>()
        params.add(BasicNameValuePair("lifetime", lifetime.toString()))
        if (state != null)
            params.add(BasicNameValuePair("state", state.toString()))
        try {
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Unable to assemble a remote workflow service request", e)
        }

        val response = getResponse(post, SC_OK, HttpStatus.SC_UNAUTHORIZED)
        try {
            if (response != null) {
                if (HttpStatus.SC_UNAUTHORIZED == response.statusLine.statusCode) {
                    throw UnauthorizedException("You do not have permission to cleanup")
                } else {
                    logger.info("Successful request to workflow cleanup endpoint")
                    return
                }
            }
        } finally {
            closeConnection(response)
        }
        throw WorkflowDatabaseException("Unable to successfully request the workflow cleanup endpoint")
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(WorkflowServiceRemoteImpl::class.java)
    }
}
