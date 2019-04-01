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

import org.opencastproject.message.broker.api.BaseMessage
import org.opencastproject.message.broker.api.MessageItem
import org.opencastproject.message.broker.api.MessageReceiver
import org.opencastproject.message.broker.api.MessageSender
import org.opencastproject.security.api.SecurityService

import com.google.common.util.concurrent.Striped

import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable
import java.util.HashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.locks.Lock

/**
 * Very simple approach to serialize the work of all three dependent update handlers.
 */
class LiveScheduleMessageReceiver {

    private var securityService: SecurityService? = null
    private var messageReceiver: MessageReceiver? = null
    private val updateHandlers = HashMap<String, UpdateHandler>()

    // One thread for each type of message watcher/queue id
    private val messageWatchers = HashMap<String, MessageWatcher>()
    // Where message watchers are executed
    private val executor = Executors.newCachedThreadPool()

    // Pool of threads for executing updates
    private val updateExecutor = Executors.newCachedThreadPool()

    fun activate(cc: ComponentContext) {
        logger.info("Activating {}", LiveScheduleMessageReceiver::class.java.name)
    }

    fun deactivate(cc: ComponentContext) {
        logger.info("Deactivating {}", LiveScheduleMessageReceiver::class.java.name)
        for (queue in messageWatchers.keys) {
            val mw = messageWatchers[queue]
            mw.stopListening()
        }
        executor.shutdown()
    }

    private inner class MessageWatcher internal constructor(private val queueId: String) : Runnable {

        private val logger = LoggerFactory.getLogger(MessageWatcher::class.java)

        private var listening = true
        private var future: FutureTask<Serializable>? = null
        private val executor = Executors.newSingleThreadExecutor()

        fun stopListening() {
            this.listening = false
            future!!.cancel(true)
        }

        override fun run() {
            logger.info("Starting to listen for {} update messages", queueId)
            while (listening) {
                future = messageReceiver!!.receiveSerializable(queueId, MessageSender.DestinationType.Queue)
                executor.execute(future!!)
                try {
                    val baseMessage = future!!.get() as BaseMessage
                    val handler = updateHandlers[queueId]

                    // Start execution in a new thread so that we don't block listening to messages.
                    // We will synchronize by media package id
                    updateExecutor.execute {
                        securityService!!.organization = baseMessage.organization
                        securityService!!.user = baseMessage.user
                        if (handler != null) {
                            val l = lock.get(baseMessage.id.get())
                            try {
                                l.lock()
                                handler.execute(baseMessage.getObject() as MessageItem)
                            } finally {
                                l.unlock()
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    logger.error("Problem while getting {} message events {}", queueId, ExceptionUtils.getStackTrace(e))
                } catch (e: ExecutionException) {
                    logger.error("Problem while getting {} message events {}", queueId, ExceptionUtils.getStackTrace(e))
                } catch (e: CancellationException) {
                    logger.trace("Listening for {} messages has been cancelled.", queueId)
                } catch (t: Throwable) {
                    logger.error("Problem while getting {} message events {}", queueId, ExceptionUtils.getStackTrace(t))
                } finally {
                    securityService!!.organization = null
                    securityService!!.user = null
                }
            }
            logger.info("Stopping listening for {} update messages", queueId)
        }
    }

    // === Set by OSGI begin
    fun setMessageReceiver(messageReceiver: MessageReceiver) {
        this.messageReceiver = messageReceiver
    }

    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    fun addUpdateHandler(handler: UpdateHandler) {
        val queueId = handler.destinationId
        if (updateHandlers[queueId] == null) {
            logger.info("Adding live schedule message handler for {}", queueId)
            updateHandlers[queueId] = handler
            val mw = MessageWatcher(queueId)
            messageWatchers[queueId] = mw
            executor.execute(mw)
        }
    }

    fun removeUpdateHandler(handler: UpdateHandler) {
        val queueId = handler.destinationId
        if (updateHandlers[queueId] != null) {
            logger.info("Removing live schedule message handler for {}", queueId)
            val mw = messageWatchers[queueId]
            mw.stopListening()
            messageWatchers.remove(queueId)
            updateHandlers.remove(queueId)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(LiveScheduleMessageReceiver::class.java)
        // Striped lock for synchronizing on media package
        private val lock = Striped.lazyWeakLock(1024)
    }
    // === Set by OSGI end

}
