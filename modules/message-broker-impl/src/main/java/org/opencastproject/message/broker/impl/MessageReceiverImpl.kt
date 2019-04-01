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

package org.opencastproject.message.broker.impl

import org.opencastproject.message.broker.api.MessageReceiver
import org.opencastproject.message.broker.api.MessageSender.DestinationType

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

import javax.jms.Destination
import javax.jms.JMSException
import javax.jms.Message
import javax.jms.MessageConsumer
import javax.jms.ObjectMessage
import javax.jms.Session

/**
 * A class to receive messages from a ActiveMQ Message Broker.
 */
open class MessageReceiverImpl : MessageBaseFacility(), MessageReceiver {

    /**
     * Wait for a connection and then create a consumer from it
     * @param destinationId
     * The destination queue or topic to create the consumer from.
     * @param type
     * The type of the destination either queue or topic.
     * @return A consumer or `null` if there was a problem creating it.
     */
    @Throws(JMSException::class)
    private fun createConsumer(destinationId: String, type: DestinationType): MessageConsumer? {
        waitForConnection()
        synchronized(this) {
            // Create the destination (Topic or Queue)
            val destination: Destination
            val session = session
            // This shouldn't happen after a connection has been successfully
            // established at least once, but better be safe than sorry.
            if (session == null) {
                logger.warn("No session object, consumer could not be created.")
                return null
            }
            if (type == DestinationType.Queue) {
                destination = session.createQueue(destinationId)
            } else {
                destination = session.createTopic(destinationId)
            }

            // Create a MessageConsumer from the Session to the Topic or Queue
            return session.createConsumer(destination)
        }
    }

    /**
     * Private function to get a message or none if there is an error.
     *
     * @param destinationId
     * The destination queue or topic to pull the message from.
     * @param type
     * The type of the destination either queue or topic.
     * @return A message or none if there was a problem getting the message.
     */
    @Throws(JMSException::class)
    private fun waitForMessage(destinationId: String, type: DestinationType): Message? {
        waitForConnection()
        var consumer: MessageConsumer? = null
        try {
            consumer = createConsumer(destinationId, type)
            if (consumer != null)
                return consumer.receive()
            logger.trace("Consumer could not be created.")
            return null
        } finally {
            if (consumer != null) {
                try {
                    consumer.close()
                } catch (e: JMSException) {
                    logger.error("Unable to close connections after receipt of message", e)
                }

            }
        }
    }

    /**
     * Get serializable object from the message bus.
     *
     * @param destinationId The destination queue or topic to pull the message from.
     * @param type The type of the destination either queue or topic.
     * @return serializable object from the message bus
     * @throws JMSException if an error occures during the communication with the message bus.
     */
    @Throws(JMSException::class)
    fun getSerializable(destinationId: String, type: DestinationType): Serializable {
        while (true) {
            // Wait for a message
            val message = waitForMessage(destinationId, type)
            if (message != null && message is ObjectMessage) {
                val objectMessage = message as ObjectMessage?
                return objectMessage!!.getObject()
            }

            logger.debug("Skipping invalid message: {}", message)
        }
    }

    override fun receiveSerializable(destinationId: String, type: DestinationType): FutureTask<Serializable> {
        return FutureTask(Callable { getSerializable(destinationId, type) })
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(MessageReceiverImpl::class.java)

        /** The OSGi service PID  */
        private val SERVICE_PID = "org.opencastproject.message.broker.impl.MessageReceiverImpl"
    }

}
