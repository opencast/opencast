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

package org.opencastproject.test.rest

import org.opencastproject.util.data.Collections.toArray
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Option.some
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Option

import com.sun.jersey.api.core.ClassNamesResourceConfig
import com.sun.jersey.api.core.PackagesResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.spi.container.servlet.ServletContainer

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.util.Random
import java.util.regex.Pattern

/**
 * Helper environment for creating REST service unit tests.
 *
 *
 * The REST endpoint to test needs a no-arg constructor in order to be created by the framework.
 *
 *
 * Write REST unit tests using [rest assured](http://code.google.com/p/rest-assured/).
 * <h3>Example Usage</h3>
 *
 * <pre>
 * import static com.jayway.restassured.RestAssured.*;
 * import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
 * import static org.hamcrest.Matchers.*;
 * import static org.opencastproject.rest.RestServiceTestEnv.*
 *
 * public class RestEndpointTest {
 * // create a local environment running on some random port
 * // use rt.host("/path/to/service") to wrap all URL creations for HTTP request methods
 * private static final RestServiceTestEnv rt = testEnvScanAllPackages(localhostRandomPort());
 *
 * \@BeforeClass public static void oneTimeSetUp() {
 * env.setUpServer();
 * }
 *
 * \@AfterClass public static void oneTimeTearDown() {
 * env.tearDownServer();
 * }
 * }
</pre> *
 *
 * Add the following dependencies to your pom
 *
 * <pre>
 * &lt;dependency&gt;
 * &lt;groupId&gt;com.jayway.restassured&lt;/groupId&gt;
 * &lt;artifactId&gt;rest-assured&lt;/artifactId&gt;
 * &lt;version&gt;1.7.2&lt;/version&gt;
 * &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * &lt;dependency&gt;
 * &lt;groupId&gt;org.apache.httpcomponents&lt;/groupId&gt;
 * &lt;artifactId&gt;httpcore&lt;/artifactId&gt;
 * &lt;version&gt;4.2.4&lt;/version&gt;
 * &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
</pre> *
 */
class RestServiceTestEnv
/**
 * Create an environment for `baseUrl`. The base URL should be the URL where the service to test is
 * mounted, e.g. http://localhost:8090/test
 */
private constructor(private val baseUrl: URL, private val cfg: Option<out ResourceConfig>) {
    private var hs: Server? = null

    /** Return the port the configured server is running on.  */
    val port: Int
        get() = baseUrl.port

    /** Create a URL suitable for rest-assured's post(), get() e.al. methods.  */
    fun host(path: String): String {
        return UrlSupport.url(baseUrl, path).toString()
    }

    /**
     * Return the base URL of the HTTP server. `http://host:port` public URL getBaseUrl() { return baseUrl; }
     *
     * Call in @BeforeClass annotated method.
     */
    fun setUpServer() {
        try {
            // cut of any base pathbasestUrl might have
            val port = baseUrl.port
            logger.info("Start http server at port $port")
            hs = Server(port)
            val servletContainer = if (cfg.isSome) ServletContainer(cfg.get()) else ServletContainer()
            val jerseyServlet = ServletHolder(servletContainer)
            val context = ServletContextHandler(hs, "/")
            context.addServlet(jerseyServlet, "/*")
            hs!!.start()
        } catch (e: Exception) {
            chuck(e)
        }

    }

    /** Call in @AfterClass annotated method.  */
    fun tearDownServer() {
        if (hs != null) {
            logger.info("Stop http server")
            try {
                hs!!.stop()
            } catch (e: Exception) {
                logger.warn("Stop http server - failed {}", e.message)
            }

        }
    }

    // Hamcrest matcher

    class RegexMatcher(pattern: String) : BaseMatcher<String>() {
        private val p: Pattern

        init {
            p = Pattern.compile(pattern)
        }

        override fun matches(item: Any?): Boolean {
            return item != null && p.matcher(item.toString()).matches()
        }

        override fun describeTo(description: Description) {
            description.appendText("regex [" + p.pattern() + "]")
        }

        companion object {

            fun regex(pattern: String): RegexMatcher {
                return RegexMatcher(pattern)
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(RestServiceTestEnv::class.java)

        fun testEnvScanAllPackages(baseUrl: URL): RestServiceTestEnv {
            return RestServiceTestEnv(baseUrl, Option.none())
        }

        fun testEnvScanPackages(baseUrl: URL, vararg servicePkgs: Package): RestServiceTestEnv {
            return RestServiceTestEnv(baseUrl,
                    some(PackagesResourceConfig(toArray(String::class.java, mlist(servicePkgs).map(pkgName).value()))))
        }

        fun testEnvForClasses(baseUrl: URL, vararg restServices: Class<*>): RestServiceTestEnv {
            return RestServiceTestEnv(baseUrl, some(ClassNamesResourceConfig(*restServices)))
        }

        fun testEnvForCustomConfig(baseUrl: String, cfg: ResourceConfig): RestServiceTestEnv {
            return RestServiceTestEnv(UrlSupport.url(baseUrl), some(cfg))
        }

        fun testEnvForCustomConfig(baseUrl: URL, cfg: ResourceConfig): RestServiceTestEnv {
            return RestServiceTestEnv(baseUrl, some(cfg))
        }

        /**
         * Return a localhost base URL with a random port between 8081 and 9000. The method features a port usage detection to
         * ensure it returns a free port.
         */
        fun localhostRandomPort(): URL {
            for (tries in 100 downTo 1) {
                val url = UrlSupport.url("http", "localhost", 8081 + Random(System.currentTimeMillis()).nextInt(919))
                try {
                    val con = url.openConnection()
                    con.connectTimeout = 1000
                    con.readTimeout = 1000
                    con.getInputStream()
                } catch (e: IOException) {
                    return url
                }

            }
            throw RuntimeException("Cannot find free port. Giving up.")
        }

        private val pkgName = object : Function<Package, String>() {
            override fun apply(pkg: Package): String {
                return pkg.name
            }
        }
    }
}
