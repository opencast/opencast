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

import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.SecurityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * A class built to send JMS messages through ActiveMQ.
 */
public class MessageSenderImpl extends MessageBaseFacility implements MessageSender {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MessageSenderImpl.class);

  /** The OSGi service PID */
  private static final String SERVICE_PID = "org.opencastproject.message.broker.impl.MessageSenderImpl";

  /** The security service */
  private SecurityService securityService;

  @Override
  public void sendObjectMessage(String destinationId, DestinationType type, Serializable object) {
    if (!isConnected()) {
      logger.error("Could not send message. No connection to message broker.");
      return;
    }
    try {
      synchronized (this) {
        Session session = getSession();
        // This shouldn't happen after a connection has been successfully
        // established at least once, but better be safe than sorry.
        if (session == null)
          return;
        // Create a message or use the provided one.
        Message message = session.createObjectMessage(
                new BaseMessage(securityService.getOrganization(), securityService.getUser(), object));

        Destination destination;
        // Create the destination (Topic or Queue)
        if (type.equals(DestinationType.Queue)) {
          destination = session.createQueue(destinationId);
        } else {
          destination = session.createTopic(destinationId);
        }

        // Tell the producer to send the message
        logger.trace("Sent message: " + message.hashCode() + " : " + Thread.currentThread().getName());

        // Send the message
        getMessageProducer().send(destination, message);
      }
    } catch (JMSException e) {
      logger.error("Had an exception while trying to send a message", e);
    }
  }

  /** OSGi DI callback */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
