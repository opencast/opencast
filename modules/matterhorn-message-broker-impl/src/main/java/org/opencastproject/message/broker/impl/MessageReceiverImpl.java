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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.message.broker.impl;

import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

/**
 * A class to receive messages from a ActiveMQ Message Broker.
 */
public class MessageReceiverImpl extends MessageBaseFacility implements MessageReceiver {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MessageReceiverImpl.class);

  /** The OSGi service PID */
  private static final String SERVICE_PID = "org.opencastproject.message.broker.impl.MessageReceiverImpl";

  /**
   * Wait for a connection and then create a consumer from it
   * @param destinationId
   *          The destination queue or topic to create the consumer from.
   * @param type
   *          The type of the destination either queue or topic.
   * @return A consumer or <code>null</code> if there was a problem creating it.
   */
  private MessageConsumer createConsumer(String destinationId, DestinationType type) throws JMSException {
    waitForConnection();
    synchronized (this) {
      // Create the destination (Topic or Queue)
      Destination destination;
      Session session = getSession();
      // This shouldn't happen after a connection has been successfully
      // established at least once, but better be safe than sorry.
      if (session == null)
        return null;
      if (type.equals(DestinationType.Queue)) {
        destination = session.createQueue(destinationId);
      } else {
        destination = session.createTopic(destinationId);
      }

      // Create a MessageConsumer from the Session to the Topic or Queue
      return session.createConsumer(destination);
    }
  }

  /**
   * Private function to get a message or none if there is an error.
   *
   * @param destinationId
   *          The destination queue or topic to pull the message from.
   * @param type
   *          The type of the destination either queue or topic.
   * @return A message or none if there was a problem getting the message.
   */
  private Message waitForMessage(String destinationId, DestinationType type) {
    waitForConnection();
    MessageConsumer consumer = null;
    try {
      consumer = createConsumer(destinationId, type);
      if (consumer != null)
          return consumer.receive();
    } catch (JMSException e) {
      if (e.getCause() instanceof InterruptedIOException || e.getCause() instanceof InterruptedException) {
        logger.trace("Exception due to message receiver shutdown: {}", e);
      } else if (isConnected()) {
        logger.error("Unable to receive messages", e);
      }
    } finally {
      try {
        if (consumer != null) {
          consumer.close();
        }
      } catch (JMSException e) {
        logger.error("Unable to close connections after receipt of message", e);
      }
    }
    return null;
  }

  protected Serializable getSerializable(String destinationId, DestinationType type) {
    while (true) {
      // Wait for a message
      Message message = waitForMessage(destinationId, type);
      if (message != null && message instanceof ObjectMessage) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
          return objectMessage.getObject();
        } catch (JMSException e) {
          logger.error("Unable to get message {}", message, e);
        }
      }
      logger.debug("Skipping invalid message: {}", message);
    }
  }

  @Override
  public FutureTask<Serializable> receiveSerializable(final String destinationId, final DestinationType type) {
    FutureTask<Serializable> futureTask = new FutureTask<Serializable>(new Callable<Serializable>() {
      @Override
      public Serializable call() {
        return getSerializable(destinationId, type);
      }
    });
    return futureTask;
  }

}
