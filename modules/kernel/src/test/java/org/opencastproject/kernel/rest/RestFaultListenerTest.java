/*
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

package org.opencastproject.kernel.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

public class RestFaultListenerTest {

  private final RestFaultListener listener = new RestFaultListener();
  private Exchange exchange;
  private Message outMessage;

  @BeforeEach
  public void setUp() {
    HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(request.getMethod()).andStubReturn("GET");
    EasyMock.expect(request.getRequestURI()).andStubReturn("/admin-ng/event/events.json");
    EasyMock.replay(request);

    exchange = new ExchangeImpl();
    Message inMessage = new MessageImpl();
    inMessage.put(AbstractHTTPDestination.HTTP_REQUEST, request);
    inMessage.setExchange(exchange);
    exchange.setInMessage(inMessage);

    outMessage = new MessageImpl();
    outMessage.setExchange(exchange);
    exchange.setOutMessage(outMessage);
  }

  @Test
  public void testDescribeRequest() {
    assertEquals("GET /admin-ng/event/events.json", RestFaultListener.describeRequest(outMessage));
    assertEquals("<unknown request>", RestFaultListener.describeRequest(new MessageImpl()));
  }

  @Test
  public void testIsClientDisconnect() {
    assertTrue(RestFaultListener.isClientDisconnect(new Fault(new IOException(new ClosedChannelException()))));
    assertFalse(RestFaultListener.isClientDisconnect(new Fault(new IOException("Disk full"))));
    assertFalse(RestFaultListener.isClientDisconnect(new Fault(new NullPointerException())));
  }

  @Test
  public void testClientDisconnectSuppressesFollowUpFaults() {
    assertFalse(listener.faultOccurred(new Fault(new ClosedChannelException()), "service", outMessage));
    assertEquals(Boolean.TRUE, exchange.get(RestFaultListener.CLIENT_DISCONNECTED));
    // CXF's attempt to write the error response fails as well
    assertFalse(listener.faultOccurred(new Fault(new IOException("Closed")), "service", outMessage));
  }

  @Test
  public void testWebApplicationExceptionIsHandled() {
    assertFalse(listener.faultOccurred(new BadRequestException(), "service", outMessage));
    assertNull(exchange.get(RestFaultListener.CLIENT_DISCONNECTED));
  }

  @Test
  public void testFailedWriteOfMappedExceptionIsClientDisconnect() {
    // JAXB providers wrap write failures in a WebApplicationException
    Exception exception = new InternalServerErrorException(new IOException(new ClosedChannelException()));
    assertFalse(listener.faultOccurred(exception, "service", outMessage));
    assertEquals(Boolean.TRUE, exchange.get(RestFaultListener.CLIENT_DISCONNECTED));
  }

  @Test
  public void testOtherFaultsAreHandled() {
    assertFalse(listener.faultOccurred(new Fault(new NullPointerException()), "service", outMessage));
    assertNull(exchange.get(RestFaultListener.CLIENT_DISCONNECTED));
  }
}
