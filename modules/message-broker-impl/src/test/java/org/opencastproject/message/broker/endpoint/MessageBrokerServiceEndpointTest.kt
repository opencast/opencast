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

package org.opencastproject.message.broker.endpoint

import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE
import org.junit.Assert.assertEquals

import org.opencastproject.message.broker.impl.MessageReceiverImpl
import org.opencastproject.message.broker.impl.MessageSenderImpl

import org.easymock.EasyMock
import org.junit.Test

class MessageBrokerServiceEndpointTest {

    @Test
    fun testGetStatus() {
        /* Mock message broker connections */
        val receiver = EasyMock.createMock<MessageReceiverImpl>(MessageReceiverImpl::class.java)
        EasyMock.expect(receiver.isConnected).andReturn(true).once()
        EasyMock.expect(receiver.isConnected).andReturn(false).once()
        val sender = EasyMock.createMock<MessageSenderImpl>(MessageSenderImpl::class.java)
        EasyMock.expect(sender.isConnected).andReturn(true).once()
        EasyMock.expect(sender.isConnected).andReturn(false).once()
        EasyMock.replay(sender, receiver)

        val endpoint = MessageBrokerServiceEndpoint()
        endpoint.setMessageReceiver(receiver)
        endpoint.setMessageSender(sender)

        assertEquals(endpoint.status.status.toLong(), SC_NO_CONTENT.toLong())
        assertEquals(endpoint.status.status.toLong(), SC_SERVICE_UNAVAILABLE.toLong())
    }

}
