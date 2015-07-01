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

import org.opencastproject.message.broker.api.MessageSender.DestinationType;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

/**
 * This class is ignored as it directly connects to a running activemq installation.
 */
public class MessageReceiverImplTest {
  private static String url = "failover://tcp://mh-allinone.localdomain:61616";
  private static final Logger logger = LoggerFactory.getLogger(MessageReceiverImplTest.class);
  private ActiveMQConnectionFactory connectionFactory;
  private String destinationId = "Destination.Queue";
  private byte[] byteData = "Byte Data".getBytes();
  private String messageText = "This is the message text";
  // Setting up messages.
  private StreamMessage streamMessage = EasyMock.createMock(StreamMessage.class);
  private BytesMessage byteMessage = EasyMock.createMock(BytesMessage.class);
  private TextMessage textMessage = EasyMock.createMock(TextMessage.class);
  private ObjectMessage objectMessage = EasyMock.createMock(ObjectMessage.class);
  private Capture<byte[]> passedByteArray = new Capture<byte[]>();
  private Long serializableObject = new Long(21L);
  private Session session;

  @Before
  public void setUp() throws JMSException {
    // Setup messages
    passedByteArray = new Capture<byte[]>();
    EasyMock.expect(byteMessage.getBodyLength()).andReturn((long) byteData.length).anyTimes();
    EasyMock.expect(byteMessage.readBytes(EasyMock.capture(passedByteArray))).andAnswer(new IAnswer<Integer>() {
      @Override
      public Integer answer() throws Throwable {
        byte[] array = passedByteArray.getValue();
        for (int i = 0; i < array.length; i++) {
          array[i] = byteData[i];
        }
        return new Integer(array.length);
      }
    });

    EasyMock.expect(textMessage.getText()).andReturn(messageText).anyTimes();
    EasyMock.expect(objectMessage.getObject()).andReturn(serializableObject).anyTimes();
    // Setup queue
    Queue queue = EasyMock.createMock(Queue.class);
    // Setup consumer
    MessageConsumer messageConsumer = EasyMock.createMock(MessageConsumer.class);
    EasyMock.expect(messageConsumer.receive()).andReturn(streamMessage);
    messageConsumer.close();
    EasyMock.expectLastCall();
    EasyMock.expect(messageConsumer.receive()).andReturn(byteMessage);
    messageConsumer.close();
    EasyMock.expectLastCall();
    EasyMock.expect(messageConsumer.receive()).andReturn(textMessage);
    messageConsumer.close();
    EasyMock.expectLastCall();
    EasyMock.expect(messageConsumer.receive()).andReturn(objectMessage);
    messageConsumer.close();
    EasyMock.expectLastCall();

    // Setup session
    session = EasyMock.createNiceMock(Session.class);
    EasyMock.expect(session.createQueue(destinationId)).andReturn(queue).anyTimes();
    EasyMock.expect(session.createConsumer(queue)).andReturn(messageConsumer).anyTimes();

    // Setup up connection
    Connection connection = EasyMock.createNiceMock(Connection.class);
    EasyMock.expect(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).andReturn(session).anyTimes();

    // Setting up Connection Factory.
    connectionFactory = EasyMock.createMock(ActiveMQConnectionFactory.class);
    EasyMock.expect(connectionFactory.createConnection()).andReturn(connection).anyTimes();
    EasyMock.replay(byteMessage, connection, connectionFactory, messageConsumer, objectMessage, session, streamMessage,
            textMessage);
  }

  @Test
  public void getMessageReturnsAllMessageTypes() throws JMSException {
    MockMessageReceiver messageReceiverImpl = new MockMessageReceiver(session);
    Message resultMessage = messageReceiverImpl.getMessage(destinationId, DestinationType.Queue);
    assertEquals(streamMessage, resultMessage);
    resultMessage = messageReceiverImpl.getMessage(destinationId, DestinationType.Queue);
    assertEquals(byteMessage, resultMessage);
    resultMessage = messageReceiverImpl.getMessage(destinationId, DestinationType.Queue);
    assertEquals(textMessage, resultMessage);
  }

  @Test
  public void getTextMessageReturnsOnlyTextMessage() throws JMSException {
    MockMessageReceiver messageReceiverImpl = new MockMessageReceiver(session);
    String message = messageReceiverImpl.getString(destinationId, DestinationType.Queue);
    assertEquals(messageText, message);
  }

  @Test
  public void getByteMessageReturnsOnlyByteMessage() throws JMSException {
    MockMessageReceiver messageReceiverImpl = new MockMessageReceiver(session);
    byte[] resultBytes = messageReceiverImpl.getByteArray(destinationId, DestinationType.Queue);
    assertEquals(byteData.length, resultBytes.length);
    for (int i = 0; i < byteData.length; i++) {
      assertEquals(byteData[i], resultBytes[i]);
    }
  }

  @Test
  public void getObjectMessageReturnsOnlyObjectMessage() throws JMSException {
    MockMessageReceiver messageReceiverImpl = new MockMessageReceiver(session);
    Serializable messageObject = messageReceiverImpl.getSerializable(destinationId, DestinationType.Queue);
    assertEquals(serializableObject, messageObject);
  }

}
