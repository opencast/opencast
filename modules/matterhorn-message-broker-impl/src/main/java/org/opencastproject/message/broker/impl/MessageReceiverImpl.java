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

import static org.opencastproject.util.OsgiUtil.getCfg;

import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

/**
 * A class to receive messages from a ActiveMQ Message Broker.
 */
public class MessageReceiverImpl extends MessageBaseFacility implements MessageReceiver, ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MessageReceiverImpl.class);

  /** The OSGi service PID */
  private static final String SERVICE_PID = "org.opencastproject.message.broker.impl.MessageReceiverImpl";

  /** The OSGi configuration admin service */
  private ConfigurationAdmin configAdmin;

  /** OSGi component activate callback */
  public void activate() throws Exception {
    logger.info("MessageReceiver service is starting...");

    Dictionary<?, ?> config = configAdmin.getConfiguration(SERVICE_PID).getProperties();
    if (config != null) {
      updated(config);
    } else {
      throw new IllegalStateException(String.format("Configuration for service with PID %s is missing.", SERVICE_PID));
    }

    logger.info("MessageReceiver service successfully started");
  }

  /** OSGi component deactivate callback */
  public void deactivate() {
    logger.info("MessageReceiver service is stopping...");
    disconnectMessageBroker();
    logger.info("MessageReceiver service successfully stopped");
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
  private Option<Message> waitForMessage(String destinationId, DestinationType type) {
    MessageConsumer consumer = null;
    try {

      // Create the destination (Topic or Queue)
      Destination destination;
      if (type.equals(DestinationType.Queue)) {
        destination = getSession().createQueue(destinationId);
      } else {
        destination = getSession().createTopic(destinationId);
      }

      // Create a MessageConsumer from the Session to the Topic or Queue
      consumer = getSession().createConsumer(destination);

      // Wait for a message
      Message message = consumer.receive();
      return Option.option(message);
    } catch (JMSException e) {
      logger.error("Unable to receive messages {}", ExceptionUtils.getStackTrace(e));
      return Option.<Message> none();
    } finally {
      try {
        if (consumer != null) {
          consumer.close();
        }
      } catch (JMSException e) {
        logger.error("Unable to close connections after receipt of message {}", ExceptionUtils.getStackTrace(e));
      }
    }
  }

  /**
   * Wait until a message is available from the destination queue.
   *
   * @param destinationId
   *          The id of the destination to listen to messages.
   * @param type
   *          The type of the destination either queue or topic.
   * @return The received Message.
   */
  protected Message getMessage(String destinationId, DestinationType type) {
    Option<Message> message = Option.<Message> none();
    do {
      message = waitForMessage(destinationId, type);
    } while (message.isNone());
    return message.get();
  }

  /**
   * Determines if a message is a jms TextMessage
   *
   * @param message
   *          The message to check.
   * @return true if it is a TextMessage with textual content.
   * @throws JMSException
   *           Thrown if there is a problem getting the text of the message.
   */
  private boolean isValidTextMessage(Option<Message> message) throws JMSException {
    return message.isSome() && message.get() instanceof TextMessage && ((TextMessage) message.get()).getText() != null;
  }

  /**
   * Receive a JMS TextMessage.
   *
   * @param destinationId
   *          The id of the destination queue to listen to.
   * @param type
   *          The type of the destination either queue or topic.
   * @return A message with text content.
   */
  protected String getString(String destinationId, DestinationType type) {
    Option<String> messageText = Option.<String> none();
    do {
      // Wait for a message
      Option<Message> message = waitForMessage(destinationId, type);
      try {
        if (isValidTextMessage(message)) {
          messageText = Option.option(((TextMessage) message.get()).getText());
        } else {
          logger.debug("Skipping invalid message {}", message);
          messageText = Option.<String> none();
        }
      } catch (JMSException e) {
        logger.error("Unable to get message {} because {}", message, ExceptionUtils.getStackTrace(e));
        messageText = Option.<String> none();
      }
    } while (messageText.isNone());
    return messageText.get();
  }

  /**
   * @param message
   *          The message to check.
   * @return True if the message is a JMS ByteMessage and has some content.
   * @throws JMSException
   *           Thrown if there is a problem getting the length of the content.
   */
  private boolean isValidByteMessage(Option<Message> message) throws JMSException {
    return message.isSome() && message.get() instanceof BytesMessage
            && ((BytesMessage) message.get()).getBodyLength() > 0;
  }

  /**
   * @param destinationId
   *          The id of the destination queue to listen to.
   * @param type
   *          The type of the destination either queue or topic.
   * @return Receive a JMS ByteMessage from an ActiveMQ Message Broker.
   */
  protected byte[] getByteArray(String destinationId, DestinationType type) {
    Option<byte[]> receivedBytes = Option.<byte[]> none();
    do {
      // Wait for a message
      Option<Message> message = waitForMessage(destinationId, type);
      try {
        if (isValidByteMessage(message)) {
          BytesMessage bytesMessage = (BytesMessage) message.get();
          byte[] payload;
          payload = new byte[(int) bytesMessage.getBodyLength()];
          bytesMessage.readBytes(payload);
          receivedBytes = Option.option(payload);
        } else {
          logger.debug("Skipping invalid message:" + message);
          receivedBytes = Option.<byte[]> none();
        }
      } catch (JMSException e) {
        logger.error("Unable to get message {} because {}", message, ExceptionUtils.getStackTrace(e));
        receivedBytes = Option.<byte[]> none();
      }
    } while (receivedBytes.isNone());
    return receivedBytes.get();
  }

  /**
   * @param message
   *          The message to check.
   * @return True if the message is a JMS ObjectMessage and has some content.
   */
  private boolean isValidObjectMessage(Option<Message> message) {
    return message.isSome() && message.get() instanceof ObjectMessage;
  }

  protected Serializable getSerializable(String destinationId, DestinationType type) {
    Option<Serializable> messageObject = Option.<Serializable> none();
    do {
      // Wait for a message
      Option<Message> message = waitForMessage(destinationId, type);
      try {
        if (isValidObjectMessage(message)) {
          ObjectMessage objectMessage = (ObjectMessage) message.get();
          messageObject = Option.option(objectMessage.getObject());
        } else {
          logger.debug("Skipping invalid message:" + message);
          messageObject = Option.<Serializable> none();
        }
      } catch (JMSException e) {
        logger.error("Unable to get message {} because {}", message, ExceptionUtils.getStackTrace(e));
        messageObject = Option.<Serializable> none();
      }
    } while (messageObject.isNone());
    return messageObject.get();
  }

  @Override
  public FutureTask<Message> receiveMessage(final String destinationId, final DestinationType type) {
    FutureTask<Message> futureTask = new FutureTask<Message>(new Callable<Message>() {
      @Override
      public Message call() {
        return getMessage(destinationId, type);
      }
    });
    return futureTask;
  }

  @Override
  public FutureTask<String> receiveString(final String destinationId, final DestinationType type) {
    FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
      @Override
      public String call() {
        return getString(destinationId, type);
      }
    });
    return futureTask;
  }

  @Override
  public FutureTask<byte[]> receiveByteArray(final String destinationId, final DestinationType type) {
    FutureTask<byte[]> futureTask = new FutureTask<byte[]>(new Callable<byte[]>() {
      @Override
      public byte[] call() {
        return getByteArray(destinationId, type);
      }
    });
    return futureTask;
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

  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null) {
      final String url = getCfg(properties, ACTIVEMQ_BROKER_URL_KEY);
      logger.info("MessageReceiver is configured to connect with URL {}", url);

      try {
        disconnectMessageBroker();
        connectMessageBroker(url);
      } catch (JMSException e) {
        throw new ConfigurationException(ACTIVEMQ_BROKER_URL_KEY, null, e);
      }
    }
  }

  /** OSGi DI callback */
  void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

}
