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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.message.broker.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

import org.opencastproject.message.broker.api.MessageSender.DestinationType

import org.easymock.EasyMock
import org.junit.Test
import org.osgi.framework.BundleContext

import java.io.Serializable

import javax.jms.JMSException
import javax.jms.MessageConsumer
import javax.jms.ObjectMessage
import javax.jms.Queue
import javax.jms.Session
import javax.jms.TextMessage

/**
 * This class is ignored as it directly connects to a running activemq installation.
 */
class MessageReceiverImplTest {

    @Test
    @Throws(JMSException::class)
    fun testGetObjectMessageReturnsOnlyObjectMessage() {
        val destinationId = "Destination.Queue"
        val serializableObject = 21L
        // Setup messages
        val textMessage = EasyMock.createMock<TextMessage>(TextMessage::class.java)
        val objectMessage = EasyMock.createMock<ObjectMessage>(ObjectMessage::class.java)
        EasyMock.expect(objectMessage.getObject()).andReturn(serializableObject).anyTimes()
        // Setup queue
        val queue = EasyMock.createMock<Queue>(Queue::class.java)
        // Setup consumer
        val messageConsumer = EasyMock.createMock<MessageConsumer>(MessageConsumer::class.java)
        EasyMock.expect<Message>(messageConsumer.receive()).andReturn(textMessage)
        messageConsumer.close()
        EasyMock.expectLastCall<Any>()
        EasyMock.expect<Message>(messageConsumer.receive()).andReturn(objectMessage)
        messageConsumer.close()
        EasyMock.expectLastCall<Any>()

        // Setup session
        val session = EasyMock.createNiceMock<Session>(Session::class.java)
        EasyMock.expect(session.createQueue(destinationId)).andReturn(queue).anyTimes()
        EasyMock.expect(session.createConsumer(queue)).andReturn(messageConsumer).anyTimes()

        EasyMock.replay(messageConsumer, objectMessage, session, textMessage)

        val messageReceiverImpl = MockMessageReceiver(session)
        messageReceiverImpl.enable(true)
        val messageObject = messageReceiverImpl.getSerializable(destinationId, DestinationType.Queue)
        assertEquals(serializableObject, messageObject)
    }

    @Test
    @Throws(Exception::class)
    fun testActivate() {
        val bctx = EasyMock.createMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bctx.getProperty(MessageBaseFacility.ACTIVEMQ_BROKER_URL_KEY))
                .andReturn("failover://(tcp://127.0.0.1:9)?initialReconnectDelay=2000&maxReconnectAttempts=2")
        EasyMock.expect(bctx.getProperty(MessageBaseFacility.ACTIVEMQ_BROKER_USERNAME_KEY)).andReturn(null)
        EasyMock.expect(bctx.getProperty(MessageBaseFacility.ACTIVEMQ_BROKER_PASSWORD_KEY)).andReturn(null)
        EasyMock.replay(bctx)

        /* Regular (de-)activate */
        val messageReceiverImpl = MessageReceiverImpl()
        messageReceiverImpl.activate(bctx)
        assertFalse(messageReceiverImpl.reconnect())
        messageReceiverImpl.deactivate()
        assertFalse(messageReceiverImpl.isConnected)
    }

    @Test
    @Throws(Exception::class)
    fun testGetter() {
        val messageReceiverImpl = MessageReceiverImpl()
        assertNotNull(messageReceiverImpl.receiveSerializable("", DestinationType.Queue))
        assertNull(messageReceiverImpl.session)
        assertNull(messageReceiverImpl.messageProducer)
    }

}
