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

package org.opencastproject.workspace.impl

import org.opencastproject.workspace.api.Workspace

import org.quartz.Job
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.Trigger
import org.quartz.TriggerUtils
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Date

/** Clear outdated workspace files [Workspace].  */
class WorkspaceCleaner(val workspace: Workspace, private val schedulerPeriod: Int, val maxAge: Int) {

    private var quartz: org.quartz.Scheduler?

    init {

        // Continue only if we have a sensible period value
        if (schedulerPeriod <= 0) {
            logger.debug("No scheduler initialized due to invalid scheduling period ({})", schedulerPeriod)
            quartz = null
            return
        }

        try {
            quartz = StdSchedulerFactory().scheduler
            quartz!!.start()
            // create and set the job. To actually run it call schedule(..)
            val job = JobDetail(JOB_NAME, JOB_GROUP, Runner::class.java)
            job.setDurability(false)
            job.setVolatility(true)
            job.jobDataMap[JOB_PARAM_PARENT] = this
            quartz!!.addJob(job, true)
        } catch (e: org.quartz.SchedulerException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Set the schedule and start or restart the scheduler.
     */
    fun schedule() {
        if (quartz == null || schedulerPeriod <= 0) {
            logger.debug("Cancel scheduling of workspace cleaner due to invalid scheduling period")
            return
        }
        logger.debug("Scheduling workspace cleaner to run every {} seconds.", schedulerPeriod)
        try {
            val trigger = TriggerUtils.makeSecondlyTrigger(schedulerPeriod)
            trigger.startTime = Date()
            trigger.name = TRIGGER_NAME
            trigger.group = TRIGGER_GROUP
            trigger.jobName = JOB_NAME
            trigger.jobGroup = JOB_GROUP
            if (quartz!!.getTriggersOfJob(JOB_NAME, JOB_GROUP).size == 0) {
                quartz!!.scheduleJob(trigger)
            } else {
                quartz!!.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger)
            }
        } catch (e: Exception) {
            logger.error("Error scheduling Quartz job", e)
        }

    }

    /** Shutdown the scheduler.  */
    fun shutdown() {
        try {
            quartz!!.shutdown()
        } catch (ignore: org.quartz.SchedulerException) {
        }

    }

    // just to make sure Quartz is being shut down...
    @Throws(Throwable::class)
    protected fun finalize() {
        super.finalize()
        shutdown()
    }

    // --

    /** Quartz work horse.  */
    class Runner : Job {

        @Throws(JobExecutionException::class)
        override fun execute(jobExecutionContext: JobExecutionContext) {
            logger.debug("Start workspace cleaner")
            try {
                execute(jobExecutionContext.jobDetail.jobDataMap[JOB_PARAM_PARENT] as WorkspaceCleaner)
            } catch (e: Exception) {
                throw JobExecutionException("An error occurred while cleaning workspace", e)
            }

            logger.debug("Finished workspace cleaner")
        }

        private fun execute(workspaceCleaner: WorkspaceCleaner) {
            workspaceCleaner.workspace.cleanup(workspaceCleaner.maxAge)
        }

    }

    companion object {

        /** Log facility  */
        private val logger = LoggerFactory.getLogger(WorkspaceCleaner::class.java)

        private val JOB_NAME = "mh-workspace-cleaner-job"
        private val JOB_GROUP = "mh-workspace-cleaner-job-group"
        private val TRIGGER_NAME = "mh-workspace-cleaner-trigger"
        private val TRIGGER_GROUP = "mh-workspace-cleaner-trigger-group"
        private val JOB_PARAM_PARENT = "parent"
    }

}
