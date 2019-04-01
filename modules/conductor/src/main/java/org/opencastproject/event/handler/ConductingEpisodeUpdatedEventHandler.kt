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
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem
import org.opencastproject.security.api.SecurityService

import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

/**
 * This handler listens for changes to episodes. Whenever a change is done, this is propagated to OAI-PMH.
 */
class ConductingEpisodeUpdatedEventHandler {

    private var securityService: SecurityService? = null
    private var messageReceiver: MessageReceiver? = null

    private var oaiPmhUpdatedEventHandler: OaiPmhUpdatedEventHandler? = null

    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    private var messageWatcher: MessageWatcher? = null

    fun activate(cc: ComponentContext) {
        logger.info("Activating {}", ConductingEpisodeUpdatedEventHandler::class.java.name)
        messageWatcher = MessageWatcher()
        singleThreadExecutor.execute(messageWatcher!!)
    }

    fun deactivate(cc: ComponentContext) {
        logger.info("Deactivating {}", ConductingEpisodeUpdatedEventHandler::class.java.name)
        if (messageWatcher != null)
            messageWatcher!!.stopListening()

        singleThreadExecutor.shutdown()
    }

    private inner class MessageWatcher : Runnable {

        private val logger = LoggerFactory.getLogger(MessageWatcher::class.java)

        @Volatile
        private var listening = true
        private var future: FutureTask<Serializable>? = null
        private val executor = Executors.newSingleThreadExecutor()

        fun stopListening() {
            this.listening = false
            future!!.cancel(true)
        }

        override fun run() {
            logger.info("Starting to listen for episode update messages")
            while (listening) {
                future = messageReceiver!!.receiveSerializable(QUEUE_ID, MessageSender.DestinationType.Queue)
                executor.execute(future!!)
                try {
                    val baseMessage = future!!.get() as BaseMessage
                    if (baseMessage.getObject() !is AssetManagerItem.TakeSnapshot) {
                        // We don't want to handle anything but TakeSnapshot messages.
                        continue
                    }
                    securityService!!.organization = baseMessage.organization
                    securityService!!.user = baseMessage.user
                    val snapshotItem = baseMessage.getObject() as AssetManagerItem.TakeSnapshot
                    if (AssetManagerItem.Type.Update == snapshotItem.type) {
                        // the OAI-PMH handler is a dynamic dependency
                        if (oaiPmhUpdatedEventHandler != null) {
                            oaiPmhUpdatedEventHandler!!.handleEvent(snapshotItem)
                        }
                    }
                } catch (e: CancellationException) {
                    logger.trace("Listening for episode update messages has been cancelled.")
                } catch (t: Throwable) {
                    logger.error("Problem while getting episode update message events {}", ExceptionUtils.getStackTrace(t))
                } finally {
                    securityService!!.organization = null
                    securityService!!.user = null
                }
            }
            logger.info("Stopping listening for episode update messages")
        }

    }

    /**
     * OSGi DI callback.
     */
    fun setOaiPmhUpdatedEventHandler(h: OaiPmhUpdatedEventHandler) {
        this.oaiPmhUpdatedEventHandler = h
    }

    /**
     * OSGi DI callback.
     */
    fun setMessageReceiver(messageReceiver: MessageReceiver) {
        this.messageReceiver = messageReceiver
    }

    /**
     * OSGi DI callback.
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ConductingEpisodeUpdatedEventHandler::class.java)
        private val QUEUE_ID = "ASSETMANAGER.Conductor"
    }

}
