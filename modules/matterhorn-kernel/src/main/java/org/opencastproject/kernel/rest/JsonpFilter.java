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
package org.opencastproject.kernel.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Adds padding to json responses when the 'jsonp' parameter is specified.
 */
public class JsonpFilter implements Filter {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JsonpFilter.class);

  /** The content type HTTP header name */
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  /** The querystring parameter that indicates the response should be padded */
  public static final String CALLBACK_PARAM = "jsonp";

  /** The regular expression to ensure that the callback is safe for display to a browser */
  public static final Pattern SAFE_PATTERN = Pattern.compile("[a-zA-Z0-9\\.]+");

  /** The content type for jsonp is "application/x-javascript", not "application/json". */
  public static final String JS_CONTENT_TYPE = "application/x-javascript";

  /** The character encoding. */
  public static final String CHARACTER_ENCODING = "UTF-8";

  /** The default padding to use if the specified padding contains invalid characters */
  public static final String DEFAULT_CALLBACK = "handleMatterhornData";

  /** The '(' constant. */
  public static final String OPEN_PARENS = "(";

  /** The post padding, which is always ');' no matter what the pre-padding looks like */
  public static final String POST_PADDING = ");";

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig config) throws ServletException {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
          ServletException {

    // Cast the request and response to HTTP versions
    HttpServletRequest request = (HttpServletRequest) req;

    // Determine whether the response must be wrapped
    String callbackValue = request.getParameter(CALLBACK_PARAM);
    if (callbackValue == null || callbackValue.isEmpty()) {
      logger.debug("No json padding requested from {}", request);
      chain.doFilter(request, resp);
    } else {
      logger.debug("Json padding '{}' requested from {}", callbackValue, request);

      // Ensure the callback value contains only safe characters
      if (!SAFE_PATTERN.matcher(callbackValue).matches()) {
        callbackValue = DEFAULT_CALLBACK;
      }

      // Write the padded response
      HttpServletResponse originalResponse = (HttpServletResponse) resp;
      HttpServletResponseContentWrapper wrapper = new HttpServletResponseContentWrapper(originalResponse, callbackValue);
      chain.doFilter(request, wrapper);
      wrapper.flushWrapper();
    }
  }

  /**
   * A response wrapper that allows for json padding.
   */
  static class HttpServletResponseContentWrapper extends HttpServletResponseWrapper {

    protected ByteArrayServletOutputStream buffer;
    protected PrintWriter bufferWriter;
    protected boolean committed = false;
    protected boolean enableWrapping = false;
    protected String preWrapper;

    /**
     * Construct a response wrapper.
     * 
     * @param response
     *          the response
     * @param callbackValue
     *          the jsonp callback value
     */
    public HttpServletResponseContentWrapper(HttpServletResponse response, String callbackValue) {
      super(response);
      this.preWrapper = callbackValue + OPEN_PARENS;
      this.buffer = new ByteArrayServletOutputStream();
    }

    /**
     * Flush the buffer for this response wrapper.
     * 
     * @throws IOException
     */
    public void flushWrapper() throws IOException {
      if (enableWrapping) {
        if (bufferWriter != null)
          bufferWriter.close();
        if (buffer != null)
          buffer.close();
        getResponse().setContentType(JS_CONTENT_TYPE);
        getResponse().setContentLength(
                preWrapper.getBytes(CHARACTER_ENCODING).length + buffer.size() + POST_PADDING.getBytes().length);
        getResponse().setCharacterEncoding(CHARACTER_ENCODING);
        getResponse().getOutputStream().write(preWrapper.getBytes(CHARACTER_ENCODING));
        getResponse().getOutputStream().write(buffer.toByteArray());
        getResponse().getOutputStream().write(POST_PADDING.getBytes());
        getResponse().flushBuffer();
        committed = true;
      }
    }

    /**
     * If we set a {@link javax.ws.rs.core.MediaType#APPLICATION_JSON} {@link JsonpFilter#CONTENT_TYPE_HEADER} header,
     * enable padding.
     * 
     * {@inheritDoc}
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#setHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setHeader(String name, String value) {
      if (CONTENT_TYPE_HEADER.equalsIgnoreCase(name) && APPLICATION_JSON.equals(value)) {
        enableWrapping = true;
      }
      super.setHeader(name, value);
    }

    /**
     * If we add a {@link javax.ws.rs.core.MediaType#APPLICATION_JSON} {@link JsonpFilter#CONTENT_TYPE_HEADER} header,
     * enable padding.
     * 
     * {@inheritDoc}
     * 
     * @see javax.servlet.http.HttpServletResponseWrapper#addHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addHeader(String name, String value) {
      if (CONTENT_TYPE_HEADER.equalsIgnoreCase(name) && APPLICATION_JSON.equals(value)) {
        enableWrapping = true;
      }
      super.addHeader(name, value);
    }

    /**
     * Returns the content type. If we are wrapping json with padding, return {@link JsonpFilter#JS_CONTENT_TYPE}.
     * 
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#getContentType()
     */
    @Override
    public String getContentType() {
      return enableWrapping ? JS_CONTENT_TYPE : getResponse().getContentType();
    }

    /**
     * If the content type is set to JSON, we enable wrapping. Otherwise, we leave it disabled.
     * 
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
     */
    @Override
    public void setContentType(String type) {
      enableWrapping = APPLICATION_JSON.equals(type);
      super.setContentType(type);
    }

    /**
     * If we are wrapping json with padding, , return the wrapped buffer. Otherwise, return the original outputstream.
     * 
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      return enableWrapping ? buffer : getResponse().getOutputStream();
    }

    /**
     * If we are wrapping json with padding, , return the wrapped writer. Otherwise, return the original writer.
     * 
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    @Override
    public PrintWriter getWriter() throws IOException {
      if (enableWrapping) {
        if (bufferWriter == null) {
          bufferWriter = new PrintWriter(new OutputStreamWriter(buffer, this.getCharacterEncoding()));
        }
        return bufferWriter;
      } else {
        return getResponse().getWriter();
      }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#setBufferSize(int)
     */
    @Override
    public void setBufferSize(int size) {
      if (enableWrapping) {
        buffer.enlarge(size);
      } else {
        getResponse().setBufferSize(size);
      }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#getBufferSize()
     */
    @Override
    public int getBufferSize() {
      return enableWrapping ? buffer.size() : getResponse().getBufferSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    @Override
    public void flushBuffer() throws IOException {
      if (!enableWrapping)
        getResponse().flushBuffer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#isCommitted()
     */
    @Override
    public boolean isCommitted() {
      return enableWrapping ? committed : getResponse().isCommitted();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#reset()
     */
    @Override
    public void reset() {
      getResponse().reset();
      buffer.reset();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletResponseWrapper#resetBuffer()
     */
    @Override
    public void resetBuffer() {
      getResponse().resetBuffer();
      buffer.reset();
    }
  }

  /**
   * A buffered output stream for jsonp padding.
   */
  static class ByteArrayServletOutputStream extends ServletOutputStream {

    /** The buffer */
    protected byte[] buf;

    /** The current write count */
    protected int count;

    /**
     * Creates a new buffered stream with the default size (32).
     */
    public ByteArrayServletOutputStream() {
      this(32);
    }

    /**
     * Creates a new buffered stream with the specified size.
     * 
     * @param size
     *          the buffer size
     */
    public ByteArrayServletOutputStream(int size) {
      if (size < 0) {
        throw new IllegalArgumentException("Negative initial size: " + size);
      }
      buf = new byte[size];
    }

    /**
     * Returns a copy of the buffer as an array.
     * 
     * @return the buffer as a byte array
     */
    public synchronized byte toByteArray()[] {
      return Arrays.copyOf(buf, count);
    }

    /**
     * Resets the stream.
     */
    public synchronized void reset() {
      count = 0;
    }

    /**
     * Gets the size of the stream
     * 
     * @return the stream size
     */
    public synchronized int size() {
      return count;
    }

    /**
     * Expands the size of the stream.
     * 
     * @param size
     *          the new size of the buffer
     */
    public void enlarge(int size) {
      if (size > buf.length) {
        buf = Arrays.copyOf(buf, Math.max(buf.length << 1, size));
      }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public synchronized void write(int b) throws IOException {
      int newcount = count + 1;
      enlarge(newcount);
      buf[count] = (byte) b;
      count = newcount;
    }
  }
}
