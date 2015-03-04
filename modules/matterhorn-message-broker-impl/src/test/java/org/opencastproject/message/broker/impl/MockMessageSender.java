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

import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * A mock class of {@link MessagerSenderImpl} to be used in unit tests.
 */
public class MockMessageSender extends MessageSenderImpl {

  private final Session session;
  private final MessageProducer producer;

  MockMessageSender(Session session, MessageProducer producer) {
    this.session = session;
    this.producer = producer;
  }

  @Override
  protected Session getSession() {
    return session;
  }

  @Override
  protected MessageProducer getMessageProducer() {
    return producer;
  }

}
