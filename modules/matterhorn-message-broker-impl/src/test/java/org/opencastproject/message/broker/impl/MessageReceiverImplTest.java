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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.opencastproject.message.broker.api.MessageSender.DestinationType;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * This class is ignored as it directly connects to a running activemq installation.
 */
public class MessageReceiverImplTest {

  @Test
  public void testGetObjectMessageReturnsOnlyObjectMessage() throws JMSException {
    final String destinationId = "Destination.Queue";
    final Long serializableObject = new Long(21L);
    // Setup messages
    TextMessage textMessage = EasyMock.createMock(TextMessage.class);
    ObjectMessage objectMessage = EasyMock.createMock(ObjectMessage.class);
    EasyMock.expect(objectMessage.getObject()).andReturn(serializableObject).anyTimes();
    // Setup queue
    Queue queue = EasyMock.createMock(Queue.class);
    // Setup consumer
    MessageConsumer messageConsumer = EasyMock.createMock(MessageConsumer.class);
    EasyMock.expect(messageConsumer.receive()).andReturn(textMessage);
    messageConsumer.close();
    EasyMock.expectLastCall();
    EasyMock.expect(messageConsumer.receive()).andReturn(objectMessage);
    messageConsumer.close();
    EasyMock.expectLastCall();

    // Setup session
    Session session = EasyMock.createNiceMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue).anyTimes();
    EasyMock.expect(session.createConsumer(queue)).andReturn(messageConsumer).anyTimes();

    EasyMock.replay(messageConsumer, objectMessage, session, textMessage);

    MockMessageReceiver messageReceiverImpl = new MockMessageReceiver(session);
    messageReceiverImpl.enable(true);
    Serializable messageObject = messageReceiverImpl.getSerializable(destinationId, DestinationType.Queue);
    assertEquals(serializableObject, messageObject);
  }

  @Test
  public void testActivate() throws Exception {
    BundleContext bctx = EasyMock.createMock(BundleContext.class);
    EasyMock.expect(bctx.getProperty(MessageBaseFacility.ACTIVEMQ_BROKER_URL_KEY))
      .andReturn("failover://(tcp://127.0.0.1:9)?initialReconnectDelay=2000&maxReconnectAttempts=2");
    EasyMock.expect(bctx.getProperty(MessageBaseFacility.ACTIVEMQ_BROKER_USERNAME_KEY)).andReturn(null);
    EasyMock.expect(bctx.getProperty(MessageBaseFacility.ACTIVEMQ_BROKER_PASSWORD_KEY)).andReturn(null);
    EasyMock.replay(bctx);

    /* Regular (de-)activate */
    MessageReceiverImpl messageReceiverImpl = new MessageReceiverImpl();
    messageReceiverImpl.activate(bctx);
    assertFalse(messageReceiverImpl.reconnect());
    messageReceiverImpl.deactivate();
    assertFalse(messageReceiverImpl.isConnected());
  }

  @Test
  public void testGetter() throws Exception {
    MessageReceiverImpl messageReceiverImpl = new MessageReceiverImpl();
    assertNotNull(messageReceiverImpl.receiveSerializable("", DestinationType.Queue));
    assertNull(messageReceiverImpl.getSession());
    assertNull(messageReceiverImpl.getMessageProducer());
  }

}
