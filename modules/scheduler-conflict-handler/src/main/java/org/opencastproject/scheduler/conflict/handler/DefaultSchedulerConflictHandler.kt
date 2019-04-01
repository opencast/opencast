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
package org.opencastproject.scheduler.conflict.handler

import org.opencastproject.scheduler.api.ConflictResolution.Strategy.OLD
import org.opencastproject.util.OsgiUtil.getOptCfg

import org.opencastproject.scheduler.api.ConflictHandler
import org.opencastproject.scheduler.api.ConflictResolution
import org.opencastproject.scheduler.api.ConflictResolution.Strategy
import org.opencastproject.scheduler.api.SchedulerEvent
import org.opencastproject.scheduler.impl.ConflictResolutionImpl

import com.entwinemedia.fn.Fn

import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Dictionary

/**
 * Default implementation of a scheduler conflict handler
 */
class DefaultSchedulerConflictHandler : ConflictHandler, ManagedService {

    private var strategy = Strategy.OLD

    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        strategy = getOptCfg(properties, CFG_KEY_HANDLER).toOpt().map(toStrategy).getOr(OLD)
        logger.info("Updated scheduler conflict handler configuration to {}", strategy)
    }

    override fun handleConflict(newEvent: SchedulerEvent, oldEvent: SchedulerEvent): ConflictResolution {
        when (strategy) {
            OLD -> return ConflictResolutionImpl(Strategy.OLD, oldEvent)
            ConflictResolution.Strategy.NEW -> return ConflictResolutionImpl(Strategy.NEW, newEvent)
            else -> throw IllegalStateException("No strategy found for $strategy")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DefaultSchedulerConflictHandler::class.java)

        private val CFG_KEY_HANDLER = "handler"

        private val toStrategy = object : Fn<String, Strategy>() {
            override fun apply(strategy: String): Strategy {
                if (Strategy.OLD.toString().equals(strategy, ignoreCase = true)) {
                    return Strategy.OLD
                } else if (Strategy.NEW.toString().equals(strategy, ignoreCase = true)) {
                    return Strategy.NEW
                } else {
                    logger.warn("No configuration option for {} exists. Use default old", strategy)
                    return Strategy.OLD
                }
            }

        }
    }

}
