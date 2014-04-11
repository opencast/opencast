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
 * This is a slightly modified version of the test contained in commons-fileupload's unit tests.  I've changed the
 * getMethod() method to return "post" rather than null.  Since this class is not part of the commons-fileupload jar,
 * extending the class isn't an option. - jmh
 */
public class MockHttpServletRequest implements HttpServletRequest
{

  private final InputStream mRequestData;
  private final int length;
  private String mStrContentType;
  @SuppressWarnings("unchecked")
  private Map mHeaders = new java.util.HashMap();

  /**
   * Creates a new instance with the given request data
   * and content type.
   */
  public MockHttpServletRequest(
      final byte[] requestData,
      final String strContentType)
  {
    this(new ByteArrayInputStream(requestData),
        requestData.length, strContentType);
  }

  /**
   * Creates a new instance with the given request data
   * and content type.
   */
  @SuppressWarnings("unchecked")
  public MockHttpServletRequest(
      final InputStream requestData,
      final int requestLength,
      final String strContentType)
  {
    mRequestData = requestData;
    length = requestLength;
    mStrContentType = strContentType;
    mHeaders.put(FileUploadBase.CONTENT_TYPE, strContentType);
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getAuthType()
   */
  public String getAuthType()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getCookies()
   */
  public Cookie[] getCookies()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getDateHeader(String)
   */
  public long getDateHeader(String arg0)
  {
    return 0;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getHeader(String)
   */
  public String getHeader(String headerName)
  {
    return (String) mHeaders.get(headerName);
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
   */
  @SuppressWarnings("unchecked")
  public Enumeration getHeaders(String arg0)
  {
    // todo - implement
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getHeaderNames()
  {
    // todo - implement
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getIntHeader(String)
   */
  public int getIntHeader(String arg0)
  {
    return 0;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getMethod()
   */
  public String getMethod()
  {
    return "post";
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getPathInfo()
   */
  public String getPathInfo()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
   */
  public String getPathTranslated()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getContextPath()
   */
  public String getContextPath()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getQueryString()
   */
  public String getQueryString()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
   */
  public String getRemoteUser()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
   */
  public boolean isUserInRole(String arg0)
  {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  public Principal getUserPrincipal()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
   */
  public String getRequestedSessionId()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestURI()
   */
  public String getRequestURI()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestURL()
   */
  public StringBuffer getRequestURL()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getServletPath()
   */
  public String getServletPath()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
   */
  public HttpSession getSession(boolean arg0)
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#getSession()
   */
  public HttpSession getSession()
  {
    return null;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
   */
  public boolean isRequestedSessionIdValid()
  {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
   */
  public boolean isRequestedSessionIdFromCookie()
  {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
   */
  public boolean isRequestedSessionIdFromURL()
  {
    return false;
  }

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl()
  {
    return false;
  }

  /**
   * @see javax.servlet.ServletRequest#getAttribute(String)
   */
  public Object getAttribute(String arg0)
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getAttributeNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getCharacterEncoding()
   */
  public String getCharacterEncoding()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
   */
  public void setCharacterEncoding(String arg0)
    throws UnsupportedEncodingException
  {
  }

  /**
   * @see javax.servlet.ServletRequest#getContentLength()
   */
  public int getContentLength()
  {
    int iLength = 0;

    if (null == mRequestData)
    {
      iLength = -1;
    }
    else
    {
      iLength = length;
    }
    return iLength;
  }

  /**
   * @see javax.servlet.ServletRequest#getContentType()
   */
  public String getContentType()
  {
    return mStrContentType;
  }

  /**
   * @see javax.servlet.ServletRequest#getInputStream()
   */
  public ServletInputStream getInputStream() throws IOException
  {
    ServletInputStream sis = new MyServletInputStream(mRequestData);
    return sis;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameter(String)
   */
  public String getParameter(String arg0)
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameterNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getParameterNames()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameterValues(String)
   */
  public String[] getParameterValues(String arg0)
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getParameterMap()
   */
  @SuppressWarnings("unchecked")
  public Map getParameterMap()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getProtocol()
   */
  public String getProtocol()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getScheme()
   */
  public String getScheme()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getServerName()
   */
  public String getServerName()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocalName()
   */
  public String getLocalName()
  {
      return null;
  }

    /**
   * @see javax.servlet.ServletRequest#getServerPort()
   */
  public int getServerPort()
  {
    return 0;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocalPort()
   */
  public int getLocalPort()
  {
      return 0;
  }

  /**
   * @see javax.servlet.ServletRequest#getRemotePort()
   */
  public int getRemotePort()
  {
      return 0;
  }

    /**
   * @see javax.servlet.ServletRequest#getReader()
   */
  public BufferedReader getReader() throws IOException
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getRemoteAddr()
   */
  public String getRemoteAddr()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocalAddr()
   */
  public String getLocalAddr()
  {
      return null;
  }

    /**
   * @see javax.servlet.ServletRequest#getRemoteHost()
   */
  public String getRemoteHost()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#setAttribute(String, Object)
   */
  public void setAttribute(String arg0, Object arg1)
  {
  }

  /**
   * @see javax.servlet.ServletRequest#removeAttribute(String)
   */
  public void removeAttribute(String arg0)
  {
  }

  /**
   * @see javax.servlet.ServletRequest#getLocale()
   */
  public Locale getLocale()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getLocales()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getLocales()
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#isSecure()
   */
  public boolean isSecure()
  {
    return false;
  }

  /**
   * @see javax.servlet.ServletRequest#getRequestDispatcher(String)
   */
  public RequestDispatcher getRequestDispatcher(String arg0)
  {
    return null;
  }

  /**
   * @see javax.servlet.ServletRequest#getRealPath(String)
   * @deprecated
   */
  public String getRealPath(String arg0)
  {
    return null;
  }

  /**
   *
   *
   *
   *
   */
  private static class MyServletInputStream
    extends javax.servlet.ServletInputStream
  {
    private final InputStream in;

    /**
     * Creates a new instance, which returns the given
     * streams data.
     */
    public MyServletInputStream(InputStream pStream)
    {
      in = pStream;
    }

    public int read() throws IOException
    {
      return in.read();
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        return in.read(b, off, len);
    }
  }
}
