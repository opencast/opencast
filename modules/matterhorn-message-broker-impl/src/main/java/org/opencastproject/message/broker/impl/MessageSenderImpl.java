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

import static org.opencastproject.util.OsgiUtil.getContextProperty;

import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

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

  /** The OSGi configuration admin service */
  private ConfigurationAdmin configAdmin;

  /** OSGi component activate callback */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("MessageSender service is starting...");
    final String url = getContextProperty(cc, ACTIVEMQ_BROKER_URL_KEY);
    logger.info("MessageSender is configured to connect with URL {}", url);
    try {
        disconnectMessageBroker();
        connectMessageBroker(url);
    } catch (JMSException e) {
        throw new ConfigurationException(ACTIVEMQ_BROKER_URL_KEY, null, e);
    }
    logger.info("MessageSender service successfully started");
  }

  /** OSGi component deactivate callback */
  public void deactivate() throws Exception {
    logger.info("MessageSender service is stopping...");
    disconnectMessageBroker();
    logger.info("MessageSender service successfully stopped");
  }

  /**
   * A common function for sending all types of messages that this class handles. Only one Message or one message
   * payload can be provided.
   *
   * @param destinationId
   *          The id of the queue or topic.
   * @param type
   *          The type of the destination either queue or topic.
   * @param createdMessage
   *          An optional user created message.
   * @param messageText
   *          An optional text payload for a message.
   * @param messageBytes
   *          An optional byte[] payload.
   * @param offset
   *          An optional offset for a byte[] payload.
   * @param length
   *          An optional length of bytes to add to the message byte[] payload.
   */
  private void send(String destinationId, DestinationType type, Option<Message> createdMessage,
          Option<String> messageText, Option<byte[]> messageBytes, Option<Integer> offset, Option<Integer> length,
          Option<Serializable> messageObject) {
    try {

      // Create a message or use the provided one.
      Message message = null;
      if (createdMessage.isSome()) {
        message = createdMessage.get();
      } else if (messageText.isSome()) {
        message = getSession().createTextMessage(messageText.get());
      } else if (messageBytes.isSome() && offset.isNone() && length.isNone()) {
        BytesMessage bytesMessage = getSession().createBytesMessage();
        bytesMessage.writeBytes(messageBytes.get());
        message = bytesMessage;
      } else if (messageBytes.isSome() && offset.isSome() && length.isSome()) {
        BytesMessage bytesMessage = getSession().createBytesMessage();
        bytesMessage.writeBytes(messageBytes.get(), offset.get(), length.get());
        message = bytesMessage;
      } else if (messageObject.isSome()) {
        final Organization organization = securityService.getOrganization();
        final User user = securityService.getUser();
        final BaseMessage baseMessage = new BaseMessage(organization, user, messageObject.get());
        message = getSession().createObjectMessage(baseMessage);
      } else {
        throw new IllegalArgumentException("To send a message there must be a message payload specified!");
      }

      Destination destination;
      // Create the destination (Topic or Queue)
      if (type.equals(DestinationType.Queue)) {
        destination = getSession().createQueue(destinationId);
      } else {
        destination = getSession().createTopic(destinationId);
      }

      // Tell the producer to send the message
      logger.trace("Sent message: " + message.hashCode() + " : " + Thread.currentThread().getName());

      // Send the message
      getMessageProducer().send(destination, message);
    } catch (JMSException e) {
      logger.error("Had an exception while trying to send a message {}", ExceptionUtils.getStackTrace(e));
    }

  }

  @Override
  public void sendMessage(String destinationId, DestinationType type, Message message) {
    send(destinationId, type, Option.option(message), Option.<String> none(), Option.<byte[]> none(),
            Option.<Integer> none(), Option.<Integer> none(), Option.<Serializable> none());
  }

  @Override
  public void sendTextMessage(String destinationId, DestinationType type, String messageText) {
    send(destinationId, type, Option.<Message> none(), Option.option(messageText), Option.<byte[]> none(),
            Option.<Integer> none(), Option.<Integer> none(), Option.<Serializable> none());
  }

  @Override
  public void sendByteMessage(String destinationId, DestinationType type, byte[] bytes) {
    send(destinationId, type, Option.<Message> none(), Option.<String> none(), Option.option(bytes),
            Option.<Integer> none(), Option.<Integer> none(), Option.<Serializable> none());
  }

  @Override
  public void sendByteMessage(String destinationId, DestinationType type, byte[] bytes, int offset, int length) {
    send(destinationId, type, Option.<Message> none(), Option.<String> none(), Option.option(bytes),
            Option.option(offset), Option.option(length), Option.<Serializable> none());
  }

  @Override
  public void sendObjectMessage(String destinationId, DestinationType type, Serializable object) {
    send(destinationId, type, Option.<Message> none(), Option.<String> none(), Option.<byte[]> none(),
            Option.<Integer> none(), Option.<Integer> none(), Option.option(object));
  }

  /** OSGi DI callback */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI callback */
  void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

}
