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
package org.opencastproject.oaipmh.server;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.oaipmh.server.OaiPmhServer.repositoryId;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.UrlSupport;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

public class OaiPmhServerTest {
  @Test
  public void testExtractRepositoryIdFromRequest() {
    assertEquals(none(""), repositoryId(req("http://localhost:8080/oaipmh"), "/oaipmh"));
    assertEquals(none(""), repositoryId(req("http://localhost:8080//oaipmh/"), "/oaipmh"));
    assertEquals(some("default"), repositoryId(req("http://localhost:8080//oaipmh/default"), "/oaipmh"));
    assertEquals(some("default"), repositoryId(req("http://localhost:8080//oaipmh/default/"), "/oaipmh"));
    assertEquals(some("oai"), repositoryId(req("http://localhost:8080//oai/default"), "/oaipmh"));
    assertEquals(some("default"), repositoryId(req("http://localhost:8080//oaipmh/default/more/path"), "/oaipmh"));
    assertEquals(none(""), repositoryId(req("http://localhost:8080"), "/oaipmh"));
    assertEquals(none(""), repositoryId(req("http://localhost:8080/"), "/oaipmh"));
    assertEquals(none(""), repositoryId(req("http://localhost:8080/"), "/"));
    assertEquals(none(""), repositoryId(req("http://localhost:8080/"), ""));
  }

  private static HttpServletRequest req(final String url) {
    return new HttpServletRequest() {
      @Override
      public String getAuthType() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Cookie[] getCookies() {
        return new Cookie[0]; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public long getDateHeader(String s) {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getHeader(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getHeaders(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getHeaderNames() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getIntHeader(String s) {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getMethod() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getPathInfo() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getPathTranslated() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getContextPath() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getQueryString() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRemoteUser() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isUserInRole(String s) {
        return false; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Principal getUserPrincipal() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRequestedSessionId() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRequestURI() {
        return UrlSupport.url(url).getPath();
      }

      @Override
      public StringBuffer getRequestURL() {
        return new StringBuffer(url);
      }

      @Override
      public String getServletPath() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public HttpSession getSession(boolean b) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public HttpSession getSession() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdValid() {
        return false; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdFromCookie() {
        return false; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdFromURL() {
        return false; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isRequestedSessionIdFromUrl() {
        return false; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Object getAttribute(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getAttributeNames() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getCharacterEncoding() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getContentLength() {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getContentType() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public ServletInputStream getInputStream() throws IOException {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getParameter(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getParameterNames() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String[] getParameterValues(String s) {
        return new String[0]; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Map getParameterMap() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getProtocol() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getScheme() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getServerName() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getServerPort() {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public BufferedReader getReader() throws IOException {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRemoteAddr() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRemoteHost() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void setAttribute(String s, Object o) {
        // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void removeAttribute(String s) {
        // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Locale getLocale() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public Enumeration getLocales() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public boolean isSecure() {
        return false; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public RequestDispatcher getRequestDispatcher(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getRealPath(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getRemotePort() {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getLocalName() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String getLocalAddr() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public int getLocalPort() {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public AsyncContext getAsyncContext() {
        return null;
      }

      @Override
      public DispatcherType getDispatcherType() {
        return null;
      }

      @Override
      public ServletContext getServletContext() {
        return null;
      }

      @Override
      public boolean isAsyncStarted() {
        return false;
      }

      @Override
      public boolean isAsyncSupported() {
        return false;
      }

      @Override
      public AsyncContext startAsync() {
        return null;
      }

      @Override
      public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
        return null;
      }

      @Override
      public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        return false;
      }

      @Override
      public Part getPart(String arg0) throws IOException, ServletException {
        return null;
      }

      @Override
      public Collection<Part> getParts() throws IOException, ServletException {
        return null;
      }

      @Override
      public void login(String arg0, String arg1) throws ServletException {
      }

      @Override
      public void logout() throws ServletException {
      }

      @Override
      public long getContentLengthLong() {
        return 0;
      }

      @Override
      public String changeSessionId() {
        return null;
      }

      @Override
      public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
      }
    };
  }
}
