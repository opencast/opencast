/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.fileupload

import org.apache.commons.fileupload.MockHttpServletRequest.MyServletInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.security.Principal
import java.util.Enumeration
import java.util.Locale

import javax.servlet.AsyncContext
import javax.servlet.DispatcherType
import javax.servlet.ReadListener
import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletInputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpUpgradeHandler
import javax.servlet.http.Part

/**
 * This is a slightly modified version of the test contained in commons-fileupload's unit tests. I've changed the
 * getMethod() method to return "post" rather than null. Since this class is not part of the commons-fileupload jar,
 * extending the class isn't an option. - jmh
 */
class MockHttpServletRequest
/**
 * Creates a new instance with the given request data and content type.
 */
(private val mRequestData: InputStream?, private val length: Int, private val mStrContentType: String) : HttpServletRequest {
    private val mHeaders = java.util.HashMap()

    /**
     * Creates a new instance with the given request data and content type.
     */
    constructor(requestData: ByteArray, strContentType: String) : this(ByteArrayInputStream(requestData), requestData.size, strContentType) {}

    init {
        mHeaders.put(FileUploadBase.CONTENT_TYPE, mStrContentType)
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getAuthType
     */
    override fun getAuthType(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getCookies
     */
    override fun getCookies(): Array<Cookie>? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getDateHeader
     */
    override fun getDateHeader(arg0: String): Long {
        return 0
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getHeader
     */
    override fun getHeader(headerName: String): String {
        return mHeaders.get(headerName)
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getHeaders
     */
    override fun getHeaders(arg0: String): Enumeration<*>? {
        // todo - implement
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getHeaderNames
     */
    override fun getHeaderNames(): Enumeration<*>? {
        // todo - implement
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getIntHeader
     */
    override fun getIntHeader(arg0: String): Int {
        return 0
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getMethod
     */
    override fun getMethod(): String {
        return "post"
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getPathInfo
     */
    override fun getPathInfo(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getPathTranslated
     */
    override fun getPathTranslated(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getContextPath
     */
    override fun getContextPath(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getQueryString
     */
    override fun getQueryString(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getRemoteUser
     */
    override fun getRemoteUser(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.isUserInRole
     */
    override fun isUserInRole(arg0: String): Boolean {
        return false
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getUserPrincipal
     */
    override fun getUserPrincipal(): Principal? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getRequestedSessionId
     */
    override fun getRequestedSessionId(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getRequestURI
     */
    override fun getRequestURI(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getRequestURL
     */
    override fun getRequestURL(): StringBuffer? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getServletPath
     */
    override fun getServletPath(): String? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getSession
     */
    override fun getSession(arg0: Boolean): HttpSession? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.getSession
     */
    override fun getSession(): HttpSession? {
        return null
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.isRequestedSessionIdValid
     */
    override fun isRequestedSessionIdValid(): Boolean {
        return false
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.isRequestedSessionIdFromCookie
     */
    override fun isRequestedSessionIdFromCookie(): Boolean {
        return false
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.isRequestedSessionIdFromURL
     */
    override fun isRequestedSessionIdFromURL(): Boolean {
        return false
    }

    /**
     * @see javax.servlet.http.HttpServletRequest.isRequestedSessionIdFromUrl
     */
    @Deprecated("")
    override fun isRequestedSessionIdFromUrl(): Boolean {
        return false
    }

    /**
     * @see javax.servlet.ServletRequest.getAttribute
     */
    override fun getAttribute(arg0: String): Any? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getAttributeNames
     */
    override fun getAttributeNames(): Enumeration<*>? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getCharacterEncoding
     */
    override fun getCharacterEncoding(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.setCharacterEncoding
     */
    @Throws(UnsupportedEncodingException::class)
    override fun setCharacterEncoding(arg0: String) {
    }

    /**
     * @see javax.servlet.ServletRequest.getContentLength
     */
    override fun getContentLength(): Int {
        var iLength = 0

        if (null == mRequestData) {
            iLength = -1
        } else {
            iLength = length
        }
        return iLength
    }

    /**
     * @see javax.servlet.ServletRequest.getContentType
     */
    override fun getContentType(): String {
        return mStrContentType
    }

    /**
     * @see javax.servlet.ServletRequest.getInputStream
     */
    @Throws(IOException::class)
    override fun getInputStream(): ServletInputStream {
        return MyServletInputStream(mRequestData)
    }

    /**
     * @see javax.servlet.ServletRequest.getParameter
     */
    override fun getParameter(arg0: String): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getParameterNames
     */
    override fun getParameterNames(): Enumeration<*>? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getParameterValues
     */
    override fun getParameterValues(arg0: String): Array<String>? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getParameterMap
     */
    override fun getParameterMap(): Map<*, *>? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getProtocol
     */
    override fun getProtocol(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getScheme
     */
    override fun getScheme(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getServerName
     */
    override fun getServerName(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getLocalName
     */
    override fun getLocalName(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getServerPort
     */
    override fun getServerPort(): Int {
        return 0
    }

    /**
     * @see javax.servlet.ServletRequest.getLocalPort
     */
    override fun getLocalPort(): Int {
        return 0
    }

    /**
     * @see javax.servlet.ServletRequest.getRemotePort
     */
    override fun getRemotePort(): Int {
        return 0
    }

    /**
     * @see javax.servlet.ServletRequest.getReader
     */
    @Throws(IOException::class)
    override fun getReader(): BufferedReader? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getRemoteAddr
     */
    override fun getRemoteAddr(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getLocalAddr
     */
    override fun getLocalAddr(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getRemoteHost
     */
    override fun getRemoteHost(): String? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.setAttribute
     */
    override fun setAttribute(arg0: String, arg1: Any) {}

    /**
     * @see javax.servlet.ServletRequest.removeAttribute
     */
    override fun removeAttribute(arg0: String) {}

    /**
     * @see javax.servlet.ServletRequest.getLocale
     */
    override fun getLocale(): Locale? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getLocales
     */
    override fun getLocales(): Enumeration<*>? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.isSecure
     */
    override fun isSecure(): Boolean {
        return false
    }

    /**
     * @see javax.servlet.ServletRequest.getRequestDispatcher
     */
    override fun getRequestDispatcher(arg0: String): RequestDispatcher? {
        return null
    }

    /**
     * @see javax.servlet.ServletRequest.getRealPath
     */
    @Deprecated("")
    override fun getRealPath(arg0: String): String? {
        return null
    }

    override fun getContentLengthLong(): Long {
        return 0
    }

    override fun getServletContext(): ServletContext? {
        return null
    }

    @Throws(IllegalStateException::class)
    override fun startAsync(): AsyncContext? {
        return null
    }

    @Throws(IllegalStateException::class)
    override fun startAsync(servletRequest: ServletRequest, servletResponse: ServletResponse): AsyncContext? {
        return null
    }

    override fun isAsyncStarted(): Boolean {
        return false
    }

    override fun isAsyncSupported(): Boolean {
        return false
    }

    override fun getAsyncContext(): AsyncContext? {
        return null
    }

    override fun getDispatcherType(): DispatcherType? {
        return null
    }

    override fun changeSessionId(): String? {
        return null
    }

    @Throws(IOException::class, ServletException::class)
    override fun authenticate(response: HttpServletResponse): Boolean {
        return false
    }

    @Throws(ServletException::class)
    override fun login(username: String, password: String) {
    }

    @Throws(ServletException::class)
    override fun logout() {
    }

    @Throws(IOException::class, ServletException::class)
    override fun getParts(): Collection<Part>? {
        return null
    }

    @Throws(IOException::class, ServletException::class)
    override fun getPart(name: String): Part? {
        return null
    }

    @Throws(IOException::class, ServletException::class)
    override fun <T : HttpUpgradeHandler> upgrade(handlerClass: Class<T>): T? {
        return null
    }

    /**
     * This class wraps an [InputStream] to a [ServletInputStream]
     */
    private class MyServletInputStream
    /**
     * Creates a new instance, which returns the given streams data.
     */
    internal constructor(private val `in`: InputStream) : javax.servlet.ServletInputStream() {

        @Throws(IOException::class)
        override fun read(): Int {
            return `in`.read()
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return `in`.read(b, off, len)
        }

        override fun isFinished(): Boolean {
            return false
        }

        override fun isReady(): Boolean {
            return false
        }

        override fun setReadListener(readListener: ReadListener) {}
    }
}
