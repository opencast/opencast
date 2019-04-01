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

import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.transport.TransportListener
import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

import javax.jms.Connection
import javax.jms.DeliveryMode
import javax.jms.JMSException
import javax.jms.MessageProducer
import javax.jms.Session

/**
 * This is a base facility that handles connections and sessions to an ActiveMQ message broker.
 */
open class MessageBaseFacility {

    /** The connection to the ActiveMQ broker  */
    private var connection: Connection? = null

    /** Session used to communicate with the ActiveMQ broker  */
    /**
     * Returns an open session or `null` if the facility is not yet connected.
     */
    open var session: Session? = null
        private set

    /** The message producer  */
    /**
     * Returns an anonymous message producer or `null` if the facility is not yet connected.
     *
     *
     * The destination needs to be defined when sending the message (`producer.send(destination, message)`)
     */
    open var messageProducer: MessageProducer? = null
        private set

    /** Disabled state of the JMS connection.  */
    private val enabled = AtomicBoolean(false)

    /** Connection details  */
    private var url = ACTIVEMQ_DEFAULT_URL
    private var username: String? = null
    private var password: String? = null

    /**
     * Return if there is a connection to the message broker.
     */
    open val isConnected: Boolean
        get() = this.enabled.get()

    /** OSGi component activate callback  */
    @Throws(Exception::class)
    fun activate(bc: BundleContext) {
        val name = this.javaClass.simpleName
        url = bc.getProperty(ACTIVEMQ_BROKER_URL_KEY)
        if (StringUtils.isBlank(url)) {
            logger.info("No valid URL found. Using default URL")
            url = ACTIVEMQ_DEFAULT_URL
        }
        username = bc.getProperty(ACTIVEMQ_BROKER_USERNAME_KEY)
        password = bc.getProperty(ACTIVEMQ_BROKER_PASSWORD_KEY)

        logger.info("{} is configured to connect with URL {}", name, url)
        if (reconnect()) {
            logger.info("{} service successfully started", name)
        }
    }

    /** OSGi component deactivate callback  */
    fun deactivate() {
        logger.info("{} service is stopping...", this.javaClass.simpleName)
        disconnectMessageBroker()
        logger.info("{} service successfully stopped", this.javaClass.simpleName)
    }

    /** Opens new sessions and connections to the message broker  */
    @Synchronized
    fun reconnect(): Boolean {
        disconnectMessageBroker(false)
        try {
            /* Create a ConnectionFactory for establishing connections to the Active MQ broker */
            val connectionFactory = ActiveMQConnectionFactory(url)
            connectionFactory.trustedPackages = listOf("org.opencastproject.message.broker.api")
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                connectionFactory.userName = username
                connectionFactory.password = password
            }
            connectionFactory.transportListener = object : TransportListener {
                override fun transportResumed() {
                    enable(true)
                    logger.info("Connection to ActiveMQ is working")
                }

                override fun transportInterupted() {
                    enable(false)
                    logger.error("Connection to ActiveMQ message broker interrupted ({}, username: {})", url, username)
                }

                override fun onException(ex: IOException) {
                    enable(false)
                    logger.error("ActiveMQ transport exception: {}", ex.message)
                }

                override fun onCommand(obj: Any) {
                    logger.trace("ActiveMQ command: {}", obj)
                }
            }

            logger.info("Starting connection to ActiveMQ message broker, waiting until connection is established...")
            connection = connectionFactory.createConnection()
            connection!!.start()

            session = connection!!.createSession(false, Session.AUTO_ACKNOWLEDGE)

            messageProducer = session!!.createProducer(null)
            messageProducer!!.deliveryMode = DeliveryMode.NON_PERSISTENT
        } catch (e: JMSException) {
            logger.error("Failed connecting to ActiveMQ message broker using url '{}'", url)
            /* Make sure to set session, etc. to null if connecting failed */
            disconnectMessageBroker(false)
            return false
        }

        logger.info("Connection to ActiveMQ message broker successfully started")
        return true
    }

    /** Closes all open sessions and connections to the message broker  */
    protected fun disconnectMessageBroker() {
        disconnectMessageBroker(true)
    }

    /** Closes all open sessions and connections to the message broker  */
    @Synchronized
    protected fun disconnectMessageBroker(verbose: Boolean) {
        if (messageProducer != null || session != null || connection != null) {
            if (verbose) {
                logger.info("Stopping connection to ActiveMQ message broker...")
            }

            try {
                if (messageProducer != null) {
                    messageProducer!!.close()
                }
            } catch (e: JMSException) {
                if (verbose) {
                    logger.error("Error while trying to close producer:", e)
                }
            }

            messageProducer = null

            try {
                if (session != null) {
                    session!!.close()
                }
            } catch (e: JMSException) {
                if (verbose) {
                    logger.error("Error while trying to close session:", e)
                }
            }

            session = null

            try {
                if (connection != null) {
                    connection!!.close()
                }
            } catch (e: JMSException) {
                if (verbose) {
                    logger.error("Error while trying to close session:", e)
                }
            }

            connection = null

            if (verbose) {
                logger.info("Connection to ActiveMQ message broker successfully stopped")
            }
        }
        enable(false)
    }

    fun enable(state: Boolean) {
        synchronized(this.enabled) {
            this.enabled.set(state)
            this.enabled.notifyAll()
        }
    }

    /**
     * Wait for a valid ActiveMQ connection (this could return immediately)
     */
    protected fun waitForConnection() {
        synchronized(this.enabled) {
            while (!this.enabled.get()) {
                try {
                    this.enabled.wait()
                } catch (e: InterruptedException) {
                }

            }
        }
    }

    companion object {

        /** The key to find the URL to connect to the ActiveMQ Message Broker  */
        val ACTIVEMQ_BROKER_URL_KEY = "activemq.broker.url"

        /** The key to find the username to connect to the ActiveMQ Message Broker  */
        val ACTIVEMQ_BROKER_USERNAME_KEY = "activemq.broker.username"

        /** The key to find the password to connect to the ActiveMQ Message Broker  */
        val ACTIVEMQ_BROKER_PASSWORD_KEY = "activemq.broker.password"

        /** Default Broker URL  */
        private val ACTIVEMQ_DEFAULT_URL = "failover://(tcp://127.0.0.1:61616)?initialReconnectDelay=2000&maxReconnectDelay=60000"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(MessageBaseFacility::class.java)
    }
}
