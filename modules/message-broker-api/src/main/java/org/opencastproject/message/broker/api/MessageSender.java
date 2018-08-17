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

package org.opencastproject.message.broker.api;

import java.io.Serializable;

public interface MessageSender extends MessageBrokerConnector {

  /**
   * The types of message destinations there are to send to.
   */
  enum DestinationType {
    Queue, Topic
  };

  /**
   * Send a message asynchronously with a {@link Serializable} object.
   *
   * @param destinationId
   *          The id of the destination location.
   * @param type
   *          The type of the destination.
   * @param object
   *          The serializable object to send.
   */
  void sendObjectMessage(String destinationId, DestinationType type, Serializable object);
}
