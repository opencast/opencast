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

import org.opencastproject.message.broker.api.MessageSender.DestinationType;

import java.io.Serializable;
import java.util.concurrent.FutureTask;

import javax.jms.Message;

public interface MessageReceiver {

  /**
   * Create a {@link java.util.concurrent.Future} for the next {@link java.jms.Message}.
   *
   * @param destinationId
   *          The unique id for the queue or topic to listen to.
   * @param type
   *          The type of destination either queue or topic.
   * @return A {@link Message}
   */
  FutureTask<Message> receiveMessage(String destinationId, DestinationType type);

  /**
   * Create a {@link java.util.concurrent.Future} for the next {@link String} from a {@link java.jms.TextMessage}.
   *
   * @param destinationId
   *          The unique id for the queue or topic to listen to.
   * @param type
   *          The type of destination either queue or topic.
   * @return A {@link String}
   */
  FutureTask<String> receiveString(String destinationId, DestinationType type);

  /**
   * {@link java.util.concurrent.Future} for the next byte[] from a {@link java.jms.BytesMessage}.
   *
   * @param destinationId
   *          The unique id for the queue or topic to listen to.
   * @param type
   *          The type of destination either queue or topic.
   * @return A byte[].
   */
  FutureTask<byte[]> receiveByteArray(String destinationId, DestinationType type);

  /**
   * {@link java.util.concurrent.Future} for the next {@link Serializable} from an {@link java.jms.ObjectMessage}.
   *
   * @param destinationId
   *          The unique id for the queue or topic to listen to.
   * @param type
   *          The type of destination either queue or topic.
   * @return An {@link Serializable} {@link Object}.
   */
  FutureTask<Serializable> receiveSerializable(String destinationId, DestinationType type);

}
