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

package org.opencastproject.test.rest;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.util.UrlSupport;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Helper environment for creating REST service unit tests.
 * <p>
 * The REST endpoint to test needs a no-arg constructor in order to be created by the framework.
 * <p>
 * Write REST unit tests using <a href="http://code.google.com/p/rest-assured/">rest assured</a>.
 * <h2>Example Usage</h2>
 * 
 * <pre>
 *   import static com.jayway.restassured.RestAssured.*;
 *   import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
 *   import static org.hamcrest.Matchers.*;
 *   import static org.opencastproject.rest.RestServiceTestEnv.*
 *
 *   public class RestEndpointTest {
 *   // create a local environment running on some random port
 *   // use rt.host("/path/to/service") to wrap all URL creations for HTTP request methods
 *   private static final RestServiceTestEnv rt = testEnvScanAllPackages(localhostRandomPort());
 *
 *   \@BeforeClass public static void oneTimeSetUp() {
 *   env.setUpServer();
 *   }
 *
 *   \@AfterClass public static void oneTimeTearDown() {
 *   env.tearDownServer();
 *   }
 *   }
 * </pre>
 * 
 * Add the following dependencies to your pom
 * 
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.jayway.restassured&lt;/groupId&gt;
 *   &lt;artifactId&gt;rest-assured&lt;/artifactId&gt;
 *   &lt;version&gt;1.7.2&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * &lt;dependency&gt;
 *   &lt;groupId&gt;org.apache.httpcomponents&lt;/groupId&gt;
 *   &lt;artifactId&gt;httpcore&lt;/artifactId&gt;
 *   &lt;version&gt;4.2.4&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
public final class RestServiceTestEnv {
  private Server server;

  private URL baseUrl = null;
  private final ResourceConfig cfg;

  private static final Logger logger = LoggerFactory.getLogger(RestServiceTestEnv.class);

  /**
   * Create an environment for <code>baseUrl</code>. The base URL should be the URL where the service to test is
   * mounted, e.g. http://localhost:8090/test
   */
  private RestServiceTestEnv(ResourceConfig cfg) {
    this.cfg = cfg;
  }

  public static RestServiceTestEnv testEnvForClasses(Class<?>... restServices) {
    return new RestServiceTestEnv(new ResourceConfig(restServices));
  }

  /** Create a URL suitable for rest-assured's post(), get() e.al. methods. */
  public String host(String path) {
    if (baseUrl == null) {
      throw new RuntimeException("Server not yet started");
    }
    return UrlSupport.url(baseUrl, path).toString();
  }

  /**
   * Return the base URL of the HTTP server. <code>http://host:port</code> public URL getBaseUrl() { return baseUrl; }
   *
   * Call in @BeforeClass annotated method.
   */
  public void setUpServer() {
    try {
      for (int tries = 100; tries > 0; tries--) {
        try {
          final int port = 3000 + tries + ThreadLocalRandom.current().nextInt(62000);
          logger.error("Start http server at port {}", port);
          server = new Server(port);
          ServletContainer servletContainer = new ServletContainer(cfg);
          ServletHolder jerseyServlet = new ServletHolder(servletContainer);
          ServletContextHandler context = new ServletContextHandler(server, "/");
          context.addServlet(jerseyServlet, "/*");
          server.start();
          baseUrl = UrlSupport.url("http", "127.0.0.1", port);
          return;
        } catch (BindException e) {
          // Rethrow exception after last try
          if (tries == 1) {
            throw e;
          }
          Thread.sleep(100);
        }
      }
    } catch (Exception e) {
      chuck(e);
    }
  }

  /** Call in @AfterClass annotated method. */
  public void tearDownServer() {
    if (server != null) {
      logger.info("Stop http server");
      try {
        server.stop();
      } catch (Exception e) {
        logger.warn("Stop http server - failed {}", e.getMessage());
      }
    }
  }

}
