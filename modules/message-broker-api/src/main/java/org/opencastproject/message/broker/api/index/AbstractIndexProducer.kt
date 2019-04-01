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

package org.opencastproject.message.broker.api.index

import com.entwinemedia.fn.Stream.`$`
import java.lang.String.format

import org.opencastproject.index.IndexProducer
import org.opencastproject.message.broker.api.BaseMessage
import org.opencastproject.message.broker.api.MessageReceiver
import org.opencastproject.message.broker.api.MessageSender
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.util.SecurityUtil
import org.opencastproject.util.RequireUtil

import com.entwinemedia.fn.P1
import com.entwinemedia.fn.Products
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.fns.Booleans

import org.apache.commons.lang3.text.WordUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

/**
 * This service produces messages for an elastic search index
 */
abstract class AbstractIndexProducer : IndexProducer {

    abstract val className: String

    abstract val messageReceiver: MessageReceiver?

    abstract val messageSender: MessageSender

    abstract val securityService: SecurityService

    abstract val service: IndexRecreateObject.Service

    abstract val systemUserName: String

    /** The message watcher  */
    private var messageWatcher: MessageWatcher? = null

    /** Single thread executor  */
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    /**
     * Initialize the index producer.
     */
    fun activate() {
        messageWatcher = MessageWatcher()
        singleThreadExecutor.execute(messageWatcher!!)
    }

    /**
     * Clean-up resources at shutdown.
     */
    open fun deactivate() {
        if (messageWatcher != null) {
            messageWatcher!!.stopListening()
        }
        singleThreadExecutor.shutdown()
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    /**
     * Create a new batch.
     *
     * @param indexName
     * the name of the index to recreate
     * @param queuePrefix
     * the message queue prefix where messages are sent to
     * @param updatesTotal
     * the number of updates that will be sent, i.e. how many times will the
     * [org.opencastproject.message.broker.api.index.AbstractIndexProducer.IndexRecreationBatch.update]
     * method be called
     * @param endMessageOrg
     * the organization under which the batch's end message should be sent;
     * if none use the organization of the last update message
     */
    fun mkRecreationBatch(indexName: String, queuePrefix: String, updatesTotal: Int,
                          endMessageOrg: Opt<Organization>): IndexRecreationBatch {
        return IndexRecreationBatch(indexName, queuePrefix, updatesTotal, endMessageOrg)
    }

    /**
     * Create a new batch. The organization under which the final end message is sent is set to [DefaultOrganization].
     *
     * @param indexName
     * the name of the index to recreate
     * @param queuePrefix
     * the message queue prefix where messages are sent to
     * @param updatesTotal
     * the number of updates that will be sent, i.e. how many times will the
     * [org.opencastproject.message.broker.api.index.AbstractIndexProducer.IndexRecreationBatch.update]
     * method be called
     * @see .mkRecreationBatch
     */
    fun mkRecreationBatch(indexName: String, queuePrefix: String, updatesTotal: Int): IndexRecreationBatch {
        return IndexRecreationBatch(indexName, queuePrefix, updatesTotal, Opt.some(DefaultOrganization()))
    }

    /**
     * State management for a batch of recreate index update messages.
     * Messages are always sent under the identity of the system user.
     */
    inner class IndexRecreationBatch
    /**
     * Create a new batch.
     *
     * @param indexName
     * the name of the index to recreate
     * @param queuePrefix
     * the message queue prefix where messages are sent to
     * @param updatesTotal
     * the number of updates that will be sent, i.e. how many times will the [.update]
     * method be called
     * @param endMessageOrg
     * the organization under which the batch's end message should be sent;
     * if none use the organization of the last update message
     */
    private constructor(private val indexName: String, queuePrefix: String, updatesTotal: Int,
                        private val endMessageOrg: Opt<Organization>) {
        private val logger = LoggerFactory.getLogger(IndexRecreationBatch::class.java)
        private val destinationId: String
        val updatesTotal: Int
        private val responseInterval: Int

        private var updatesCurrent: Int = 0

        init {
            this.destinationId = queuePrefix + WordUtils.capitalize(indexName)
            this.updatesTotal = RequireUtil.min(updatesTotal, 0)
            this.updatesCurrent = 0
            this.responseInterval = if (updatesTotal < 100) 1 else updatesTotal / 100
        }

        /**
         * Send one update to recreate the index. An update may consist of multiple messages.
         * Updates are sent under the identity of the system user of the given organization.
         *
         *
         * [.IDENTITY_MSG] is the identity element of messages, i.e. identity message will be filtered out
         */
        fun update(org: Organization, messages: Iterable<P1<out Serializable>>) {
            if (updatesCurrent < updatesTotal) {
                val user = SecurityUtil.createSystemUser(systemUserName, org)
                SecurityUtil.runAs(securityService, org, user, {
                    for (m in `$`(messages).filter(Booleans.ne(IDENTITY_MSG))) {
                        messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue, m.get1())
                    }
                    updatesCurrent = updatesCurrent + 1
                    if (updatesCurrent % responseInterval == 0 || updatesCurrent == updatesTotal) {
                        messageSender.sendObjectMessage(
                                IndexProducer.RESPONSE_QUEUE,
                                MessageSender.DestinationType.Queue,
                                IndexRecreateObject.update(
                                        indexName,
                                        service,
                                        updatesTotal,
                                        updatesCurrent))
                    }
                    if (updatesCurrent >= updatesTotal) {
                        // send end-of-batch message
                        val emo = endMessageOrg.getOr(org)
                        val emu = SecurityUtil.createSystemUser(systemUserName, emo)
                        SecurityUtil.runAs(securityService, emo, emu, {
                            messageSender.sendObjectMessage(
                                    destinationId,
                                    MessageSender.DestinationType.Queue,
                                    IndexRecreateObject.end(indexName, service))
                        })
                    }
                })
            } else {
                throw IllegalStateException(format("The number of allowed update messages (%d) has already been sent", updatesTotal))
            }
        }

        /**
         * @see .update
         */
        fun update(org: Organization, vararg messages: P1<out Serializable>) {
            update(org, `$`(*messages))
        }

        @Throws(Throwable::class)
        protected fun finalize() {
            super.finalize()
            if (updatesCurrent < updatesTotal) {
                logger.warn(format("Only %d messages have been sent even though the batch has been initialized with %d", updatesCurrent, updatesTotal))
            }
        }
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    private inner class MessageWatcher : Runnable {
        private val logger = LoggerFactory.getLogger(MessageWatcher::class.java)
        @Volatile
        private var listening = true
        @Volatile
        private var future: FutureTask<Serializable>? = null
        private val executor = Executors.newSingleThreadExecutor()

        fun stopListening() {
            this.listening = false
            if (future != null) {
                future!!.cancel(true)
            }
            executor.shutdown()
        }

        override fun run() {
            if (messageReceiver == null) {
                logger.warn("The message receiver for " + className
                        + " was null so unable to listen for repopulate index messages. Ignore this warning if this is a test.")
                listening = false
                return
            }
            logger.info("Starting to listen for {} Messages", className)
            while (listening) {
                try {
                    future = messageReceiver!!.receiveSerializable(IndexProducer.RECEIVER_QUEUE + "." + service,
                            MessageSender.DestinationType.Queue)
                    executor.execute(future!!)
                    val message = future!!.get() as BaseMessage
                    if (message == null || message.`object` !is IndexRecreateObject)
                        continue

                    val indexObject = message.`object` as IndexRecreateObject
                    if (indexObject.service != service || indexObject.status != IndexRecreateObject.Status.Start)
                        continue
                    logger.info("Index '{}' has received a start repopulating command for service '{}'.",
                            indexObject.indexName, service)
                    repopulate(indexObject.indexName!!)
                    logger.info("Index '{}' has finished repopulating service '{}'.", indexObject.indexName, service)
                } catch (e: InterruptedException) {
                    logger.error("Problem while getting {} message events", className, e)
                } catch (e: ExecutionException) {
                    logger.error("Problem while getting {} message events", className, e)
                } catch (e: CancellationException) {
                    logger.trace("Listening for messages {} has been cancelled.", className)
                } catch (t: Throwable) {
                    logger.error("Problem while getting {} message events", className, t)
                }

            }
            logger.info("Stopping listening for {} Messages", className)
        }
    }

    companion object {
        val IDENTITY_MSG = Products.E.p1<Serializable>(object : Serializable {

        })
    }
}
