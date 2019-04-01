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

package org.opencastproject.workflow.handler.incident

import org.opencastproject.util.EnumSupport.parseEnum
import org.opencastproject.util.data.Option.option

import org.opencastproject.job.api.Incident.Severity
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobContext
import org.opencastproject.serviceregistry.api.NopService
import org.opencastproject.util.Log
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.data.functions.Strings
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

import java.util.Arrays
import java.util.stream.Collectors

class IncidentCreatorWorkflowOperationHandler : AbstractWorkflowOperationHandler() {

    private var nopService: NopService? = null

    @Throws(WorkflowOperationException::class)
    override fun start(wi: WorkflowInstance, ctx: JobContext): WorkflowOperationResult {
        val woi = wi.currentOperation
        val code = option(woi.getConfiguration(OPT_CODE)).bind(Strings.toInt).getOrElse(1)
        val severity = option(woi.getConfiguration(OPT_SEVERITY)).bind(parseEnum(Severity.FAILURE)).getOrElse(Severity.INFO)

        val details = Arrays.stream(ArrayUtils.nullToEmpty(
                StringUtils.split(woi.getConfiguration(OPT_DETAILS), ";")))
                .map { opt -> opt.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() }
                .filter { t -> t.size == 2 }
                .map<Any> { x -> Tuple.tuple(x[0], x[1]) }
                .collect<List<Tuple<String, String>>, Any>(Collectors.toList<Any>())
        val params = Arrays.stream(ArrayUtils.nullToEmpty(
                StringUtils.split(woi.getConfiguration(OPT_PARAMS), ";")))
                .map { opt -> opt.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() }
                .filter { t -> t.size == 2 }
                .collect<Map<String, String>, Any>(Collectors.toMap({ x -> x[0] }, { x -> x[1] }))
        log.info("Create nop job")
        val job = nopService!!.nop()
        log.info("Log a dummy incident with code %d", code)
        serviceRegistry.incident().record(job, severity, code, params, details)
        return if (!waitForStatus(job).isSuccess) {
            throw WorkflowOperationException("Job did not complete successfully")
        } else {
            createResult(WorkflowOperationResult.Action.CONTINUE)
        }
    }

    /** OSGi DI.  */
    fun setNopService(nopService: NopService) {
        this.nopService = nopService
    }

    companion object {
        private val log = Log.mk(IncidentCreatorWorkflowOperationHandler::class.java)

        private val OPT_CODE = "code"
        private val OPT_SEVERITY = "severity"
        private val OPT_DETAILS = "details"
        private val OPT_PARAMS = "params"
    }
}
