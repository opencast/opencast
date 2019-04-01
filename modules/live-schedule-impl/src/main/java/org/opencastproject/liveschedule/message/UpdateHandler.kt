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
package org.opencastproject.liveschedule.message

import org.opencastproject.liveschedule.api.LiveScheduleService
import org.opencastproject.message.broker.api.MessageItem

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class UpdateHandler(destinationId: String) {

    /** Services  */
    protected var liveScheduleService: LiveScheduleService

    var destinationId: String
        protected set

    fun activate(cc: ComponentContext) {
        logger.info("Activating {}", this.javaClass.name)
    }

    init {
        this.destinationId = destinationId
    }

    abstract fun execute(message: MessageItem)

    // === Set by OSGI begin
    fun setLiveScheduleService(service: LiveScheduleService) {
        this.liveScheduleService = service
    }

    companion object {

        protected val PUBLISH_LIVE_PROPERTY = "publishLive"

        private val logger = LoggerFactory.getLogger(SchedulerUpdateHandler::class.java)
    }
    // === Set by OSGI end

}
