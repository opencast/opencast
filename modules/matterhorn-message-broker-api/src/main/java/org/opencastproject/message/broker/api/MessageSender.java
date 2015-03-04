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
package org.opencastproject.message.broker.api;

import java.io.Serializable;

import javax.jms.Message;

public interface MessageSender {

  /**
   * The types of message destinations there are to send to.
   */
  public static enum DestinationType {
    Queue, Topic
  };

  /**
   * Send a {@link Message} asynchronously through a message broker to a queue or topic.
   *
   * @param destinationId
   *          The id of the queue or topic.
   * @param type
   *          The type of destination either queue or topic.
   * @param messageText
   *          The text to send.
   */
  void sendMessage(String destinationId, DestinationType type, Message message);

  /**
   * Send a {@link String} asynchronously through a message broker to a queue or topic.
   *
   * @param destinationId
   *          The id of the queue or topic.
   * @param type
   *          The type of destination either queue or topic.
   * @param messageText
   *          The text to send.
   */
  void sendTextMessage(String destinationId, DestinationType type, String messageText);


  /**
   * Send a message asynchronously with a payload of a byte array.
   *
   * @param destinationId
   *          The id of the destination location.
   * @param type
   *          The type of the destination either queue or topic.
   * @param bytes
   *          The bytes to send.
   */
  void sendByteMessage(String destinationId, DestinationType type, byte[] bytes);

  /**
   * Send a message asynchronously with a byte array payload with a given offset and length.
   *
   * @param destinationId
   *          The id of the destination location.
   * @param type
   *          The type of the destination.
   * @param bytes
   *          The bytes to send.
   * @param offset
   *          The offset to start sending the bytes.
   * @param length
   *          The length of the bytes to send.
   */
  void sendByteMessage(String destinationId, DestinationType type, byte[] bytes, int offset, int length);

  /**
   * Send a message asynchronously with a {@link Serializable} object.
   *
   * @param destinationId
   *          The id of the destination location.
   * @param type
   *          The type of the destination.
   * @param bytes
   *          The bytes to send.
   * @param offset
   *          The offset to start sending the bytes.
   * @param length
   *          The length of the bytes to send.
   */
  void sendObjectMessage(String destinationId, DestinationType type, Serializable object);
}
