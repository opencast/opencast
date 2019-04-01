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

package org.opencastproject.event.handler

import org.opencastproject.message.broker.api.BaseMessage
import org.opencastproject.message.broker.api.MessageReceiver
import org.opencastproject.message.broker.api.MessageSender
import org.opencastproject.message.broker.api.series.SeriesItem
import org.opencastproject.security.api.SecurityService

import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

/**
 * Very simple approach to serialize the work of all three dependend update handlers. Todo: Merge all handlers into one
 * to avoid unnecessary distribution updates etc.
 */
class ConductingSeriesUpdatedEventHandler {

    private var securityService: SecurityService? = null
    private var messageReceiver: MessageReceiver? = null

    private var assetManagerUpdatedEventHandler: AssetManagerUpdatedEventHandler? = null
    private var seriesUpdatedEventHandler: SeriesUpdatedEventHandler? = null
    private var workflowPermissionsUpdatedEventHandler: WorkflowPermissionsUpdatedEventHandler? = null

    // Use a single thread executor to ensure that only one update is handled at a time.
    // This is because Opencast lacks a distributed synchronization model on media packages and/or series.
    // Note that this measure only _reduces_ the chance of data corruption cause by concurrent modifications.
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    private var messageWatcher: MessageWatcher? = null

    fun activate(cc: ComponentContext) {
        logger.info("Activating {}", ConductingSeriesUpdatedEventHandler::class.java.name)
        messageWatcher = MessageWatcher()
        singleThreadExecutor.execute(messageWatcher!!)
    }

    fun deactivate(cc: ComponentContext) {
        logger.info("Deactivating {}", ConductingSeriesUpdatedEventHandler::class.java.name)
        if (messageWatcher != null)
            messageWatcher!!.stopListening()

        singleThreadExecutor.shutdown()
    }

    private inner class MessageWatcher : Runnable {

        private val logger = LoggerFactory.getLogger(MessageWatcher::class.java)

        private var listening = true
        private var future: FutureTask<Serializable>? = null
        private val executor = Executors.newSingleThreadExecutor()

        fun stopListening() {
            this.listening = false
            future!!.cancel(true)
        }

        override fun run() {
            logger.info("Starting to listen for series update messages")
            while (listening) {
                future = messageReceiver!!.receiveSerializable(QUEUE_ID, MessageSender.DestinationType.Queue)
                executor.execute(future!!)
                try {
                    val baseMessage = future!!.get() as BaseMessage
                    securityService!!.organization = baseMessage.organization
                    securityService!!.user = baseMessage.user
                    val seriesItem = baseMessage.getObject() as SeriesItem

                    if (SeriesItem.Type.UpdateElement == seriesItem.type) {
                        assetManagerUpdatedEventHandler!!.handleEvent(seriesItem)
                    } else if (SeriesItem.Type.UpdateCatalog == seriesItem.type
                            || SeriesItem.Type.UpdateAcl == seriesItem.type
                            || SeriesItem.Type.Delete == seriesItem.type) {
                        seriesUpdatedEventHandler!!.handleEvent(seriesItem)
                        assetManagerUpdatedEventHandler!!.handleEvent(seriesItem)
                        workflowPermissionsUpdatedEventHandler!!.handleEvent(seriesItem)
                    }
                } catch (e: InterruptedException) {
                    logger.error("Problem while getting series update message events {}", ExceptionUtils.getStackTrace(e))
                } catch (e: ExecutionException) {
                    logger.error("Problem while getting series update message events {}", ExceptionUtils.getStackTrace(e))
                } catch (e: CancellationException) {
                    logger.trace("Listening for series update messages has been cancelled.")
                } catch (t: Throwable) {
                    logger.error("Problem while getting series update message events {}", ExceptionUtils.getStackTrace(t))
                } finally {
                    securityService!!.organization = null
                    securityService!!.user = null
                }
            }
            logger.info("Stopping listening for series update messages")
        }

    }

    /** OSGi DI callback.  */
    fun setAssetManagerUpdatedEventHandler(h: AssetManagerUpdatedEventHandler) {
        this.assetManagerUpdatedEventHandler = h
    }

    /** OSGi DI callback.  */
    fun setSeriesUpdatedEventHandler(h: SeriesUpdatedEventHandler) {
        this.seriesUpdatedEventHandler = h
    }

    /** OSGi DI callback.  */
    fun setWorkflowPermissionsUpdatedEventHandler(h: WorkflowPermissionsUpdatedEventHandler) {
        this.workflowPermissionsUpdatedEventHandler = h
    }

    /** OSGi DI callback.  */
    fun setMessageReceiver(messageReceiver: MessageReceiver) {
        this.messageReceiver = messageReceiver
    }

    /** OSGi DI callback.  */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConductingSeriesUpdatedEventHandler::class.java)
        private val QUEUE_ID = "SERIES.Conductor"
    }

}
