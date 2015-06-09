/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.message.broker.impl;

import org.opencastproject.util.data.Option;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.TransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.security.auth.login.FailedLoginException;

/**
 * This is a base facility that handles connections and sessions to an ActiveMQ message broker.
 */
public class MessageBaseFacility {

  /** The key to find the URL to connect to the ActiveMQ Message Broker */
  protected static final String ACTIVEMQ_BROKER_URL_KEY = "activemq.broker.url";

  /** The key to find the username to connect to the ActiveMQ Message Broker */
  protected static final String ACTIVEMQ_BROKER_USERNAME_KEY = "activemq.broker.username";

  /** The key to find the password to connect to the ActiveMQ Message Broker */
  protected static final String ACTIVEMQ_BROKER_PASSWORD_KEY = "activemq.broker.password";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MessageBaseFacility.class);

  /** Create a ConnectionFactory for establishing connections to the Active MQ broker */
  private ActiveMQConnectionFactory connectionFactory = null;

  /** The connection to the ActiveMQ broker */
  private Connection connection = null;

  /** Session used to communicate with the ActiveMQ broker */
  private Session session = null;

  /** The message producer */
  private MessageProducer producer = null;

  /** Tracks whether the username and password were incorrect when trying to connect. */
  private boolean invalidUsernameOrPassword = false;

  /** Opens new sessions and connections to the message broker */
  protected void connectMessageBroker(final String url, final Option<String> brokerUsername, Option<String> brokerPassword)
          throws JMSException {
    connectionFactory = new ActiveMQConnectionFactory(url);
    if (brokerUsername.isSome() && brokerPassword.isSome()) {
      connectionFactory.setUserName(brokerUsername.get());
      connectionFactory.setPassword(brokerPassword.get());
    }
    connectionFactory.setTransportListener(new TransportListener() {
      @Override
      public void transportResumed() {
        logger.info("Connection to ActiveMQ is working");
      }

      @Override
      public void transportInterupted() {
        if (invalidUsernameOrPassword) {
          logger.error(
                  "Unable to connect to the message broker '{}' because either the broker is unavailable, or the username '{}' and password is incorrect.",
                  url, brokerUsername);
        } else {
          logger.error("Connection to ActiveMQ ({}) got interupted!", connection);
        }
      }

      @Override
      public void onException(IOException ex) {
        logger.warn("ActiveMQ transport exception: {}", ex);
      }

      @Override
      public void onCommand(Object obj) {
        logger.trace("ActiveMQ command: {}", obj);
      }
    });

    logger.info("Starting connection to ActiveMQ message broker, waiting until connection is established...");
    connection = connectionFactory.createConnection();
    try {
      connection.start();
    } catch (JMSException e) {
      if (e.getCause() != null && e.getCause() instanceof SecurityException && e.getCause().getCause() != null
              && e.getCause().getCause() instanceof FailedLoginException) {
        invalidUsernameOrPassword = true;
        throw e;
      } else {
        throw e;
      }
    }

    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    producer = session.createProducer(null);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

    logger.info("Connection to ActiveMQ message broker successfully started");
  }

  /** Closes all open sessions and connections to the message broker */
  protected void disconnectMessageBroker() {
    if (producer != null || session != null || connection != null) {
      logger.info("Stopping connection to ActiveMQ message broker...");

      try {
        if (producer != null)
          producer.close();
      } catch (JMSException e) {
        logger.error("Error while trying to close producer: {}", e);
      }

      try {
        if (session != null)
          session.close();
      } catch (JMSException e) {
        logger.error("Error while trying to close session: {}", e);
      }

      try {
        if (connection != null)
          connection.close();
      } catch (JMSException e) {
        logger.error("Error while trying to close session: {}", e);
      }

      logger.info("Connection to ActiveMQ message broker successfully stopped");
    }
  }

  /**
   * Returns an open session or {@code null} if the facility is not yet connected.
   */
  protected Session getSession() {
    return session;
  }

  /**
   * Returns an anonymous message producer or {@code null} if the facility is not yet connected.
   * <p>
   * The destination needs to be defined when sending the message ({@code producer.send(destination, message)})
   */
  protected MessageProducer getMessageProducer() {
    return producer;
  }

}
