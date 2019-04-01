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

import org.opencastproject.message.broker.api.MessageSender.DestinationType
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.SecurityService

import org.easymock.EasyMock
import org.junit.Test

import java.io.Serializable

import javax.jms.Destination
import javax.jms.JMSException
import javax.jms.MessageProducer
import javax.jms.ObjectMessage
import javax.jms.Queue
import javax.jms.Session

/**
 * This class is ignored as it directly connects to a running activemq installation.
 */
class MessageSenderImplTest {

    private val destinationId = "Destination.Queue"
    private val messageText = "This is the message text"

    @Test
    @Throws(JMSException::class)
    fun testSendSerializableObjectMessage() {
        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<Organization>(securityService.organization).andReturn(DefaultOrganization())
        EasyMock.expect<User>(securityService.user).andReturn(JaxbUser()).anyTimes()

        val serailizableObject = 20L
        val objectMessage = EasyMock.createMock<ObjectMessage>(ObjectMessage::class.java)
        EasyMock.expect(objectMessage.getObject()).andReturn(serailizableObject)
        // Create MessageProducer
        val messageProducer = EasyMock.createMock<MessageProducer>(MessageProducer::class.java)
        messageProducer.send(EasyMock.anyObject(Destination::class.java), EasyMock.eq(objectMessage))
        EasyMock.expectLastCall<Any>()
        // Create queue.
        val queue = EasyMock.createMock<Queue>(Queue::class.java)

        // Create session.
        val session = EasyMock.createMock<Session>(Session::class.java)
        EasyMock.expect(session.createQueue(destinationId)).andReturn(queue)
        EasyMock.expect(session.createProducer(queue)).andReturn(messageProducer)
        EasyMock.expect(session.createObjectMessage(EasyMock.anyObject<Any>() as Serializable)).andReturn(objectMessage)
        session.close()
        EasyMock.expectLastCall<Any>()

        // Replay all of the mocks
        EasyMock.replay(objectMessage, messageProducer, queue, session, securityService)

        val messageSenderImpl = MockMessageSender(session, messageProducer)
        messageSenderImpl.setSecurityService(securityService)
        messageSenderImpl.enable(true)
        messageSenderImpl.sendObjectMessage(destinationId, DestinationType.Queue, serailizableObject)
    }

}
