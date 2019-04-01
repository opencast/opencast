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

package org.opencastproject.util

import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Prelude.sleep
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.TrustedHttpClient.RequestRunner
import org.opencastproject.util.data.Collections
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function
import org.opencastproject.util.data.Tuple

import org.apache.commons.io.IOUtils
import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLEncoder

/** Functions to support Apache httpcomponents and HTTP related operations in general.  */

/** Functions to support Apache httpcomponents.  */
object HttpUtil {

    val param_: Function<Tuple<String, String>, NameValuePair> = object : Function<Tuple<String, String>, NameValuePair>() {
        override fun apply(p: Tuple<String, String>): NameValuePair {
            return param(p)
        }
    }

    val getStatusCode: Function<HttpResponse, Int> = object : Function<HttpResponse, Int>() {
        override fun apply(response: HttpResponse): Int {
            return response.statusLine.statusCode
        }
    }

    /** Return the content of the response as a string.  */
    val getContentFn: Function<HttpResponse, String> = object : Function.X<HttpResponse, String>() {
        @Throws(Exception::class)
        public override fun xapply(httpResponse: HttpResponse): String {
            val h = httpResponse.entity.contentEncoding
            return if (h != null) {
                IOUtils.toString(httpResponse.entity.content, h.value)
            } else {
                IOUtils.toString(httpResponse.entity.content)
            }
        }
    }

    fun post(vararg formParams: NameValuePair): HttpPost {
        val post = HttpPost()
        setFormParams(post, formParams)
        return post
    }

    fun post(uri: String, vararg formParams: NameValuePair): HttpPost {
        val post = HttpPost(uri)
        setFormParams(post, formParams)
        return post
    }

    fun post(uri: String, formParams: List<NameValuePair>): HttpPost {
        val post = HttpPost(uri)
        setFormParams(post, Collections.toArray(NameValuePair::class.java, formParams))
        return post
    }

    operator fun get(path: String, vararg queryParams: Tuple<String, String>): HttpGet {
        val url = mlist(path, mlist(*queryParams).map(object : Function<Tuple<String, String>, String>() {
            override fun apply(a: Tuple<String, String>): String {
                try {
                    return a.a + "=" + URLEncoder.encode(a.b, "UTF-8")
                } catch (e: UnsupportedEncodingException) {
                    return chuck(e)
                }

            }
        }).mkString("&")).mkString("?")
        return HttpGet(url)
    }

    fun path(vararg path: String): String {
        return UrlSupport.concat(*path)
    }

    private fun setFormParams(r: HttpEntityEnclosingRequest, formParams: Array<NameValuePair>) {
        val params = list(*formParams)
        try {
            r.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            chuck<Any>(e)
        }

    }

    fun param(name: String, value: String): NameValuePair {
        return BasicNameValuePair(name, value)
    }

    fun param(p: Tuple<String, String>): NameValuePair {
        return BasicNameValuePair(p.a, p.b)
    }

    /**
     * Return the content of the response as a string if its status code equals one of the given statuses. Throw an
     * exception on an unexpected status.
     *
     *
     * Function composition of [.getContentFn] and [.expect].
     */
    fun getContentOn(vararg status: Int): Function<HttpResponse, String> {
        return getContentFn.o(expect(*status))
    }

    fun getContentOn(runner: RequestRunner<String>, vararg status: Int): String {
        val res = runner.run(getContentOn(*status))
        return if (res.isRight) {
            res.right().value()
        } else {
            chuck(res.left().value())
        }
    }

    /** Return the response if its status code equals one of the given statuses or throw an exception.  */
    fun expect(vararg status: Int): Function<HttpResponse, HttpResponse> {
        return object : Function.X<HttpResponse, HttpResponse>() {
            public override fun xapply(response: HttpResponse): HttpResponse {
                val sc = response.statusLine.statusCode
                for (s in status) {
                    if (sc == s)
                        return response
                }
                var responseBody: String
                try {
                    responseBody = IOUtils.toString(response.entity.content)
                } catch (e: IOException) {
                    responseBody = ""
                }

                throw RuntimeException("Returned status " + sc + " does not match any of the expected codes. "
                        + responseBody)
            }
        }
    }

    /** Get the value or throw the exception.  */
    fun <A> getOrError(response: Either<Exception, A>): A {
        return if (response.isRight) {
            response.right().value()
        } else {
            chuck(response.left().value())
        }
    }

    fun isOk(res: HttpResponse): Boolean {
        return res.statusLine.statusCode == HttpStatus.SC_OK
    }

    /**
     * Wait for a certain status of a resource.
     *
     * @return either an exception or the status code of the last http response
     */
    fun waitForResource(http: TrustedHttpClient, resourceUri: URI,
                        expectedStatus: Int, timeout: Long, pollingInterval: Long): Either<Exception, Int> {
        var now = 0L
        while (true) {
            val head = HttpHead(resourceUri)
            val result = http.run<Int>(head).apply(getStatusCode)
            for (status in result.right()) {
                if (eq(status, expectedStatus) || now >= timeout) {
                    return right(status)
                } else if (now < timeout) {
                    if (!sleep(pollingInterval)) {
                        return left(Exception("Interrupted"))
                    } else {
                        now = now + pollingInterval
                    }
                }
            }
            for (e in result.left()) {
                return left(e)
            }
        }
    }
}
