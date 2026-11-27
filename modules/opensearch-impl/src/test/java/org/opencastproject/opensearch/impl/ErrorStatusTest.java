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

package org.opencastproject.opensearch.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpServer;

import org.apache.hc.core5.http.HttpHost;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Makes sure we recognize failed requests to OpenSearch correctly, no matter which exception the client throws, and
 * retry only temporary failures.
 */
public class ErrorStatusTest {

  private static final String ERROR_BODY = "{\"error\":{\"type\":\"some_exception\",\"reason\":\"reason\"},"
      + "\"status\":%d}";

  private HttpServer server;
  private OpenSearchClient client;
  private int status;

  @Before
  public void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", exchange -> {
      final byte[] body = String.format(ERROR_BODY, status).getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    client = clientFor(server.getAddress().getPort());
  }

  @After
  public void tearDown() throws Exception {
    client._transport().close();
    server.stop(0);
  }

  private static OpenSearchClient clientFor(int port) {
    final HttpHost host = new HttpHost("http", "127.0.0.1", port);
    return new OpenSearchClient(ApacheHttpClient5TransportBuilder.builder(host)
        .setMapper(new JacksonJsonpMapper())
        .build());
  }

  /** The exception thrown by the client for a search request. */
  private static Exception failedSearch(OpenSearchClient client) {
    try {
      client.search(s -> s.index("index"), Map.class);
    } catch (RuntimeException | IOException e) {
      return e;
    }
    fail("Request did not fail");
    return null;
  }

  /** The exception thrown when OpenSearch answers a search request with the given status. */
  private Exception failedSearch(int responseStatus) {
    status = responseStatus;
    final Exception e = failedSearch(client);
    assertEquals(responseStatus, AbstractOpenSearchIndex.httpStatus(e));
    return e;
  }

  @Test
  public void testTemporaryErrorsAreRetried() {
    for (int responseStatus : new int[] { 408, 429, 500, 502, 503, 504 }) {
      assertTrue(String.valueOf(responseStatus),
          AbstractOpenSearchIndex.isTemporaryFailure(failedSearch(responseStatus)));
    }
  }

  @Test
  public void testOtherErrorsAreNotRetried() {
    for (int responseStatus : new int[] { 400, 401, 403, 404, 405, 409 }) {
      assertFalse(String.valueOf(responseStatus),
          AbstractOpenSearchIndex.isTemporaryFailure(failedSearch(responseStatus)));
    }
  }

  @Test
  public void testRefusedConnectionIsRetried() throws Exception {
    final int port;
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      port = socket.getLocalPort();
    }
    final OpenSearchClient unreachable = clientFor(port);
    try {
      assertTrue(AbstractOpenSearchIndex.isTemporaryFailure(failedSearch(unreachable)));
    } finally {
      unreachable._transport().close();
    }
  }

  @Test
  public void testClosedConnectionIsRetried() throws Exception {
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      // Accept the connection and close it without sending a response
      final Thread closer = new Thread(() -> {
        try (Socket connection = socket.accept()) {
          connection.getInputStream().read();
        } catch (IOException e) {
          // the test fails anyway if the client does not see a closed connection
        }
      });
      closer.start();
      final OpenSearchClient closing = clientFor(socket.getLocalPort());
      try {
        assertTrue(AbstractOpenSearchIndex.isTemporaryFailure(failedSearch(closing)));
      } finally {
        closing._transport().close();
        closer.join();
      }
    }
  }
}
