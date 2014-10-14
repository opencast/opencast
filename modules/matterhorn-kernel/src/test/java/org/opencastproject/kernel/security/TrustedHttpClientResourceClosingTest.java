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
package org.opencastproject.kernel.security;

import static org.junit.Assert.assertEquals;

import org.opencastproject.kernel.http.api.HttpClient;
import org.opencastproject.kernel.http.impl.HttpClientFactory;
import org.opencastproject.security.api.SecurityService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrustedHttpClientResourceClosingTest {
  private static final int PORT = 8952;

  private static final class TestHttpClient extends TrustedHttpClientImpl {
    TestHttpClient() {
      super("user", "pass");
      setHttpClientFactory(new HttpClientFactory());
      setSecurityService(EasyMock.createNiceMock(SecurityService.class));
    }

    Map<HttpResponse, HttpClient> getResponseMap() {
      return responseMap;
    }
  }

  @Test
  public void testResourceClosing() throws Exception {
    startServer(PORT);
    final TestHttpClient client = new TestHttpClient();
    final HttpResponse response;
    response = client.execute(new HttpGet("http://localhost:" + PORT));
    assertEquals("Request should be stored in response map", 1, client.getResponseMap().size());
    client.close(response);
    assertEquals("Request should be removed from response map", 0, client.getResponseMap().size());
  }

  private void startServer(int port) throws Exception {
    final ServerSocket socket = new ServerSocket(port);
    final ExecutorService es = Executors.newFixedThreadPool(1);
    final CountDownLatch barrier = new CountDownLatch(1);
    final Callable<Void> server = new Callable<Void>() {
      @Override public Void call() throws Exception {
        // notify that the server is ready
        barrier.countDown();
        System.out.println("Waiting for incoming connection");
        final Socket s = socket.accept();
        System.out.println("Connected");
        final PrintStream out = new PrintStream(s.getOutputStream());
        out.println("HTTP/1.1 200 OK\n\n");
        out.flush();
        out.close();
        s.getInputStream().close();
        s.close();
        es.shutdown();
        System.out.println("Terminate server");
        return null;
      }
    };
    es.submit(server);
    System.out.println("Waiting for server...");
    barrier.await();
  }
}
