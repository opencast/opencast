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

package org.opencastproject.message.broker.endpoint;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;

import org.opencastproject.message.broker.impl.MessageReceiverImpl;
import org.opencastproject.message.broker.impl.MessageSenderImpl;

import org.easymock.EasyMock;
import org.junit.Test;

public class MessageBrokerServiceEndpointTest {

  @Test
  public void testGetStatus() {
    /* Mock message broker connections */
    MessageReceiverImpl receiver = EasyMock.createMock(MessageReceiverImpl.class);
    EasyMock.expect(receiver.isConnected()).andReturn(true).once();
    EasyMock.expect(receiver.isConnected()).andReturn(false).once();
    MessageSenderImpl sender = EasyMock.createMock(MessageSenderImpl.class);
    EasyMock.expect(sender.isConnected()).andReturn(true).once();
    EasyMock.expect(sender.isConnected()).andReturn(false).once();
    EasyMock.replay(sender, receiver);

    MessageBrokerServiceEndpoint endpoint = new MessageBrokerServiceEndpoint();
    endpoint.setMessageReceiver(receiver);
    endpoint.setMessageSender(sender);

    assertEquals(endpoint.getStatus().getStatus(), SC_NO_CONTENT);
    assertEquals(endpoint.getStatus().getStatus(), SC_SERVICE_UNAVAILABLE);
  }

}
