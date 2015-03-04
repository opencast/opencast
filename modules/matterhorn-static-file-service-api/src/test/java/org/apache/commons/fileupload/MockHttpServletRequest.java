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
package org.apache.commons.fileupload;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This is a slightly modified version of the test contained in commons-fileupload's unit tests. I've changed the
 * getMethod() method to return "post" rather than null. Since this class is not part of the commons-fileupload jar,
 * extending the class isn't an option. - jmh
 */
public class MockHttpServletRequest implements HttpServletRequest {

  private final InputStream mRequestData;
  private final int length;
  private String mStrContentType;
  @SuppressWarnings("unchecked")
  private Map mHeaders = new java.util.HashMap();

  /**
   * Creates a new instance with the given request data and content type.
   */
  public MockHttpServletRequest(final byte[] requestData, final String strContentType) {
    this(new ByteArrayInputStream(requestData), requestData.length, strContentType);
  }

  /**
   * Creates a new instance with the given request data and content type.
   */
  @SuppressWarnings("unchecked")
  public MockHttpServletRequest(final InputStream requestData, final int requestLength, final String strContentType) {
    mRequestData = requestData;
    length = requestLength;
    mStrContentType = strContentType;
    mHeaders.put(FileUploadBase.CONTENT_TYPE, strContentType);
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getAuthType()
   */
  @Override
  public String getAuthType() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getCookies()
   */
  @Override
  public Cookie[] getCookies() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getDateHeader(String)
   */
  @Override
  public long getDateHeader(String arg0) {
    return 0;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getHeader(String)
   */
  @Override
  public String getHeader(String headerName) {
    return (String) mHeaders.get(headerName);
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration getHeaders(String arg0) {
    // todo - implement
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration getHeaderNames() {
    // todo - implement
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getIntHeader(String)
   */
  @Override
  public int getIntHeader(String arg0) {
    return 0;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getMethod()
   */
  @Override
  public String getMethod() {
    return "post";
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getPathInfo()
   */
  @Override
  public String getPathInfo() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
   */
  @Override
  public String getPathTranslated() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getContextPath()
   */
  @Override
  public String getContextPath() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getQueryString()
   */
  @Override
  public String getQueryString() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
   */
  @Override
  public String getRemoteUser() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
   */
  @Override
  public boolean isUserInRole(String arg0) {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
   */
  @Override
  public String getRequestedSessionId() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestURI()
   */
  @Override
  public String getRequestURI() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestURL()
   */
  @Override
  public StringBuffer getRequestURL() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getServletPath()
   */
  @Override
  public String getServletPath() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
   */
  @Override
  public HttpSession getSession(boolean arg0) {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getSession()
   */
  @Override
  public HttpSession getSession() {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
   */
  @Override
  public boolean isRequestedSessionIdValid() {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
   */
  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
   */
  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
   * @deprecated
   */
  @Deprecated
  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  /**
   * @see javax.servlet.ServletRequest#getAttribute(String)
   */
  @Override
  public Object getAttribute(String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getAttributeNames()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getCharacterEncoding()
   */
  @Override
  public String getCharacterEncoding() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
   */
  @Override
  public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
  }

  /**
   * @see javax.servlet.ServletRequest#getContentLength()
   */
  @Override
  public int getContentLength() {
    int iLength = 0;

    if (null == mRequestData) {
      iLength = -1;
    } else {
      iLength = length;
    }
    return iLength;
  }

  /**
   * @see javax.servlet.ServletRequest#getContentType()
   */
  @Override
  public String getContentType() {
    return mStrContentType;
  }

  /**
   * @see javax.servlet.ServletRequest#getInputStream()
   */
  @Override
  public ServletInputStream getInputStream() throws IOException {
    ServletInputStream sis = new MyServletInputStream(mRequestData);
    return sis;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameter(String)
   */
  @Override
  public String getParameter(String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameterNames()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration getParameterNames() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameterValues(String)
   */
  @Override
  public String[] getParameterValues(String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameterMap()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Map getParameterMap() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getProtocol()
   */
  @Override
  public String getProtocol() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getScheme()
   */
  @Override
  public String getScheme() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getServerName()
   */
  @Override
  public String getServerName() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocalName()
   */
  @Override
  public String getLocalName() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getServerPort()
   */
  @Override
  public int getServerPort() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocalPort()
   */
  @Override
  public int getLocalPort() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletRequest#getRemotePort()
   */
  @Override
  public int getRemotePort() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletRequest#getReader()
   */
  @Override
  public BufferedReader getReader() throws IOException {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getRemoteAddr()
   */
  @Override
  public String getRemoteAddr() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocalAddr()
   */
  @Override
  public String getLocalAddr() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getRemoteHost()
   */
  @Override
  public String getRemoteHost() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#setAttribute(String, Object)
   */
  @Override
  public void setAttribute(String arg0, Object arg1) {
  }

  /**
   * @see javax.servlet.ServletRequest#removeAttribute(String)
   */
  @Override
  public void removeAttribute(String arg0) {
  }

  /**
   * @see javax.servlet.ServletRequest#getLocale()
   */
  @Override
  public Locale getLocale() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocales()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration getLocales() {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#isSecure()
   */
  @Override
  public boolean isSecure() {
    return false;
  }

  /**
   * @see javax.servlet.ServletRequest#getRequestDispatcher(String)
   */
  @Override
  public RequestDispatcher getRequestDispatcher(String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getRealPath(String)
   * @deprecated
   */
  @Deprecated
  @Override
  public String getRealPath(String arg0) {
    return null;
  }

  /**
   * This class wraps an {@link InputStream} to a {@link ServletInputStream}
   */
  private static class MyServletInputStream extends javax.servlet.ServletInputStream {
    private final InputStream in;

    /**
     * Creates a new instance, which returns the given streams data.
     */
    public MyServletInputStream(InputStream pStream) {
      in = pStream;
    }

    @Override
    public int read() throws IOException {
      return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return in.read(b, off, len);
    }
  }
}
