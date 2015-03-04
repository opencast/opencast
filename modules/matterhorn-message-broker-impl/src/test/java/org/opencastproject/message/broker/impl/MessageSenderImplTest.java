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

import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;
import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * This class is ignored as it directly connects to a running activemq installation.
 */
public class MessageSenderImplTest {

  private String destinationId = "Destination.Queue";
  private String messageText = "This is the message text";

  @Test
  public void testSendMessage() throws JMSException {
    Message message = EasyMock.createMock(Message.class);
    // Create MessageProducer
    MessageProducer messageProducer = EasyMock.createMock(MessageProducer.class);
    messageProducer.send(EasyMock.anyObject(Destination.class), EasyMock.eq(message));
    EasyMock.expectLastCall();

    // Create queue.
    Queue queue = EasyMock.createMock(Queue.class);
    // Create session.
    Session session = EasyMock.createMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue);
    EasyMock.expect(session.createProducer(queue)).andReturn(messageProducer);
    session.close();
    EasyMock.expectLastCall();

    // Replay all of the mocks
    EasyMock.replay(message, messageProducer, queue, session);

    MockMessageSender messageSenderImpl = new MockMessageSender(session, messageProducer);
    messageSenderImpl.sendMessage(destinationId, DestinationType.Queue, message);
  }

  @Test
  @Ignore
  public void testSendTextMessage() throws JMSException {
    TextMessage textMessage = EasyMock.createMock(TextMessage.class);
    // Create MessageProducer
    MessageProducer messageProducer = EasyMock.createMock(MessageProducer.class);
    messageProducer.send(EasyMock.anyObject(Destination.class), EasyMock.eq(textMessage));
    EasyMock.expectLastCall();

    // Create queue.
    Queue queue = EasyMock.createMock(Queue.class);
    // Create session.
    Session session = EasyMock.createMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue);
    EasyMock.expect(session.createProducer(queue)).andReturn(messageProducer);
    EasyMock.expect(session.createTextMessage(messageText)).andReturn(textMessage);
    session.close();
    EasyMock.expectLastCall();

    // Replay all of the mocks
    EasyMock.replay(messageProducer, queue, session, textMessage);

    MockMessageSender messageSenderImpl = new MockMessageSender(session, messageProducer);
    messageSenderImpl.sendTextMessage(destinationId, DestinationType.Queue, messageText);
  }

  @Test
  public void testSendByteMessage() throws JMSException {
    byte[] bytes = "These are the bytes".getBytes();
    BytesMessage bytesMessage = EasyMock.createMock(BytesMessage.class);
    bytesMessage.writeBytes(bytes);
    EasyMock.expectLastCall();

    // Create MessageProducer
    MessageProducer messageProducer = EasyMock.createMock(MessageProducer.class);
    messageProducer.send(EasyMock.anyObject(Destination.class), EasyMock.eq(bytesMessage));
    EasyMock.expectLastCall();

    // Create queue.
    Queue queue = EasyMock.createMock(Queue.class);
    // Create session.
    Session session = EasyMock.createMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue);
    EasyMock.expect(session.createProducer(queue)).andReturn(messageProducer);
    EasyMock.expect(session.createBytesMessage()).andReturn(bytesMessage);

    // Replay all of the mocks
    EasyMock.replay(bytesMessage, messageProducer, queue, session);

    MockMessageSender messageSenderImpl = new MockMessageSender(session, messageProducer);
    messageSenderImpl.sendByteMessage(destinationId, DestinationType.Queue, bytes);
  }

  @Test
  public void testSendByteLimitedMessage() throws JMSException {
    byte[] bytes = "These are the other bytes".getBytes();
    BytesMessage bytesMessage = EasyMock.createMock(BytesMessage.class);
    bytesMessage.writeBytes(bytes, 0, bytes.length);
    EasyMock.expectLastCall();

    // Create MessageProducer
    MessageProducer messageProducer = EasyMock.createMock(MessageProducer.class);
    messageProducer.send(EasyMock.anyObject(Destination.class), EasyMock.eq(bytesMessage));
    EasyMock.expectLastCall();

    // Create queue.
    Queue queue = EasyMock.createMock(Queue.class);
    // Create session.
    Session session = EasyMock.createMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue);
    EasyMock.expect(session.createProducer(queue)).andReturn(messageProducer);
    EasyMock.expect(session.createBytesMessage()).andReturn(bytesMessage);
    session.close();
    EasyMock.expectLastCall();

    // Replay all of the mocks
    EasyMock.replay(bytesMessage, messageProducer, queue, session);

    MockMessageSender messageSenderImpl = new MockMessageSender(session, messageProducer);
    messageSenderImpl.sendByteMessage(destinationId, DestinationType.Queue, bytes, 0, bytes.length);
  }

  @Test
  public void testSendSerializableObjectMessage() throws JMSException {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization());
    EasyMock.expect(securityService.getUser()).andReturn(new JaxbUser());

    Serializable serailizableObject = new Long(20L);
    ObjectMessage objectMessage = EasyMock.createMock(ObjectMessage.class);
    EasyMock.expect(objectMessage.getObject()).andReturn(serailizableObject);
    // Create MessageProducer
    MessageProducer messageProducer = EasyMock.createMock(MessageProducer.class);
    messageProducer.send(EasyMock.anyObject(Destination.class), EasyMock.eq(objectMessage));
    EasyMock.expectLastCall();
    // Create queue.
    Queue queue = EasyMock.createMock(Queue.class);

    // Create session.
    Session session = EasyMock.createMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue);
    EasyMock.expect(session.createProducer(queue)).andReturn(messageProducer);
    EasyMock.expect(session.createObjectMessage((Serializable) EasyMock.anyObject())).andReturn(objectMessage);
    session.close();
    EasyMock.expectLastCall();

    // Replay all of the mocks
    EasyMock.replay(objectMessage, messageProducer, queue, session, securityService);

    MockMessageSender messageSenderImpl = new MockMessageSender(session, messageProducer);
    messageSenderImpl.setSecurityService(securityService);
    messageSenderImpl.sendObjectMessage(destinationId, DestinationType.Queue, serailizableObject);
  }

}
