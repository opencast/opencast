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

package org.opencastproject.kernel.security;

import static org.junit.Assert.assertEquals;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.exception.UrlSigningException;
import org.opencastproject.security.urlsigning.service.UrlSigningService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrustedHttpClientResourceClosingTest {
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClientResourceClosingTest.class);
  private static final int PORT = 8952;

  private static final class TestHttpClient extends TrustedHttpClientImpl {
    TestHttpClient() throws UrlSigningException {
      super("user", "pass");
      setSecurityService(EasyMock.createNiceMock(SecurityService.class));
      // Setup signing service
      UrlSigningService urlSigningService = EasyMock.createMock(UrlSigningService.class);
      EasyMock.expect(urlSigningService.accepts(EasyMock.anyString())).andReturn(true).anyTimes();
      EasyMock.expect(
              urlSigningService.sign(EasyMock.anyString(), EasyMock.anyLong(), EasyMock.anyLong(), EasyMock.anyString()))
              .andReturn("http://127.0.0.1:" + PORT);
      EasyMock.replay(urlSigningService);

      setUrlSigningService(urlSigningService);
    }

    Map<HttpResponse, CloseableHttpClient> getResponseMap() {
      return responseMap;
    }
  }

  @Test
  public void testResourceClosing() throws Exception {
    startServer();
    final TestHttpClient client = new TestHttpClient();
    final HttpResponse response;
    response = client.execute(new HttpGet("http://127.0.0.1:" + PORT));
    assertEquals("Request should be stored in response map", 1, client.getResponseMap().size());
    client.close(response);
    assertEquals("Request should be removed from response map", 0, client.getResponseMap().size());
  }

  private void startServer() throws Exception {
    final ServerSocket socket = new ServerSocket(PORT);
    final ExecutorService es = Executors.newFixedThreadPool(1);
    final CountDownLatch barrier = new CountDownLatch(1);
    final Callable<Void> server = () -> {
      // notify that the server is ready
      barrier.countDown();
      logger.info("Waiting for incoming connection");
      final Socket s = socket.accept();
      logger.info("Connected");
      final PrintStream out = new PrintStream(s.getOutputStream());
      out.println("HTTP/1.1 200 OK\n\n");
      out.flush();
      out.close();
      s.getInputStream().close();
      s.close();
      es.shutdown();
      logger.info("Terminate server");
      return null;
    };
    es.submit(server);
    logger.info("Waiting for server...");
    barrier.await();
  }
}
