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


package org.opencastproject.security.util

import org.apache.http.Header
import org.apache.http.HeaderIterator
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.StatusLine
import org.apache.http.params.HttpParams

import java.util.Locale
import java.util.UUID

/**
 * A wrapper for [org.apache.http.HttpResponse] objects that implements
 * [.hashCode] and [.equals] to allow for usage in hash based data structures.
 *
 * todo document motivation of this class
 */
class HttpResponseWrapper(private val response: HttpResponse) : HttpResponse {
    private val id: String

    init {
        this.id = UUID.randomUUID().toString()
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return if (o is HttpResponseWrapper) {
            id == o.id
        } else {
            false
        }
    }

    override fun getStatusLine(): StatusLine {
        return response.statusLine
    }

    override fun setStatusLine(statusLine: StatusLine) {
        response.statusLine = statusLine
    }

    override fun setStatusLine(protocolVersion: ProtocolVersion, i: Int) {
        response.setStatusLine(protocolVersion, i)
    }

    override fun setStatusLine(protocolVersion: ProtocolVersion, i: Int, s: String) {
        response.setStatusLine(protocolVersion, i, s)
    }

    @Throws(IllegalStateException::class)
    override fun setStatusCode(i: Int) {
        response.setStatusCode(i)
    }

    @Throws(IllegalStateException::class)
    override fun setReasonPhrase(s: String) {
        response.setReasonPhrase(s)
    }

    override fun getEntity(): HttpEntity {
        return response.entity
    }

    override fun setEntity(httpEntity: HttpEntity) {
        response.entity = httpEntity
    }

    override fun getLocale(): Locale {
        return response.locale
    }

    override fun setLocale(locale: Locale) {
        response.locale = locale
    }

    override fun getProtocolVersion(): ProtocolVersion {
        return response.protocolVersion
    }

    override fun containsHeader(s: String): Boolean {
        return response.containsHeader(s)
    }

    override fun getHeaders(s: String): Array<Header> {
        return response.getHeaders(s)
    }

    override fun getFirstHeader(s: String): Header {
        return response.getFirstHeader(s)
    }

    override fun getLastHeader(s: String): Header {
        return response.getLastHeader(s)
    }

    override fun getAllHeaders(): Array<Header> {
        return response.allHeaders
    }

    override fun addHeader(header: Header) {
        response.addHeader(header)
    }

    override fun addHeader(s: String, s2: String) {
        response.addHeader(s, s2)
    }

    override fun setHeader(header: Header) {
        response.setHeader(header)
    }

    override fun setHeader(s: String, s2: String) {
        response.setHeader(s, s2)
    }

    override fun setHeaders(headers: Array<Header>) {
        response.setHeaders(headers)
    }

    override fun removeHeader(header: Header) {
        response.removeHeader(header)
    }

    override fun removeHeaders(s: String) {
        response.removeHeaders(s)
    }

    override fun headerIterator(): HeaderIterator {
        return response.headerIterator()
    }

    override fun headerIterator(s: String): HeaderIterator {
        return response.headerIterator(s)
    }

    override fun getParams(): HttpParams {
        return response.params
    }

    override fun setParams(httpParams: HttpParams) {
        response.params = httpParams
    }
}
