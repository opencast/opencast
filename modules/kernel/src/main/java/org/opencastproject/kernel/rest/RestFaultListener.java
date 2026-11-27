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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

/**
 * Replaces CXF's default fault logging for the Opencast REST endpoints.
 *
 * <p>
 * All REST endpoints share a single JAX-RS server, which CXF names after an arbitrary one of its service beans. CXF's
 * own fault log messages therefore name an unrelated service and never mention the request that failed. This listener
 * logs the HTTP method and path of the failing request instead.
 *
 * <p>
 * Clients closing the connection while the response is still being written are not server faults. CXF would log
 * several warnings with long stack traces for them, for the failed write and again for its attempt to write an error
 * response into the closed connection. They are logged as a single debug message instead.
 *
 * <p>
 * CXF's {@code WebApplicationExceptionMapper} consults this listener as well. Such exceptions are turned into regular
 * HTTP responses, mostly client errors, and are logged at debug level only.
 */
public class RestFaultListener implements FaultListener {

  private static final Logger logger = LoggerFactory.getLogger(RestFaultListener.class);

  /** Exchange key marking that the client has gone away */
  static final String CLIENT_DISCONNECTED = RestFaultListener.class.getName() + ".clientDisconnected";

  /** Jetty's exception for writes to a closed connection. Matched by name to avoid depending on Jetty. */
  private static final String JETTY_EOF_EXCEPTION = "org.eclipse.jetty.io.EofException";

  @Override
  public boolean faultOccurred(Exception exception, String description, Message message) {
    final Exchange exchange = message.getExchange();
    if (exchange != null && exchange.get(CLIENT_DISCONNECTED) != null) {
      // Follow-up failure, e.g. CXF trying to write an error response to the closed connection
      return false;
    }

    final String request = describeRequest(message);
    if (isClientDisconnect(exception)) {
      if (exchange != null) {
        exchange.put(CLIENT_DISCONNECTED, Boolean.TRUE);
      }
      logger.debug("Client closed the connection before the response to {} was written", request);
    } else if (exception instanceof WebApplicationException) {
      // Already turned into an HTTP response by CXF's exception mapper. Usually a client error.
      logger.debug("Request {} answered with HTTP status {}", request,
          ((WebApplicationException) exception).getResponse().getStatus(), exception);
    } else if (message.get(FaultMode.class) == FaultMode.CHECKED_APPLICATION_FAULT) {
      logger.debug("Request {} failed: {}", request, exception.getMessage());
    } else {
      logger.error("Request {} failed", request, exception);
    }
    return false;
  }

  /**
   * Check whether an exception was caused by the client closing the connection.
   *
   * @param exception
   *          the exception to check
   * @return if the cause chain contains an exception indicating a closed connection
   */
  static boolean isClientDisconnect(Throwable exception) {
    for (Throwable t : ExceptionUtils.getThrowableList(exception)) {
      if (t instanceof ClosedChannelException || JETTY_EOF_EXCEPTION.equals(t.getClass().getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get HTTP method and path of the request a message belongs to. The query string is left out since it may contain
   * sensitive data like tokens or signatures.
   *
   * @param message
   *          the CXF message
   * @return the request description
   */
  static String describeRequest(Message message) {
    Message inMessage = message.getExchange() != null ? message.getExchange().getInMessage() : null;
    if (inMessage == null) {
      inMessage = message;
    }
    final Object request = inMessage.get(AbstractHTTPDestination.HTTP_REQUEST);
    if (request instanceof HttpServletRequest) {
      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      return httpRequest.getMethod() + " " + httpRequest.getRequestURI();
    }
    return "<unknown request>";
  }
}
