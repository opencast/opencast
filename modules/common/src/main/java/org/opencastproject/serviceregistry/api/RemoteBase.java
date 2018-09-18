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

package org.opencastproject.serviceregistry.api;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class serving as a convenience implementation for remote services.
 */
public class RemoteBase {

  private static final int TIMEOUT = 10000;

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(RemoteBase.class);

  /** The service type, used to look up remote implementations */
  protected String serviceType = null;

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** The http client */
  protected ServiceRegistry remoteServiceManager = null;

  /** A list of known http statuses */
  private static final List<Integer> knownHttpStatuses = Arrays.asList(HttpStatus.SC_SERVICE_UNAVAILABLE);

  /**
   * Creates a remote implementation for the given type of service.
   *
   * @param type
   *          the service type
   */
  protected RemoteBase(String type) {
    if (type == null)
      throw new IllegalArgumentException("Service type must not be null");
    this.serviceType = type;
  }

  /**
   * Sets the trusted http client
   *
   * @param client
   */
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /**
   * Sets the remote service manager.
   *
   * @param remoteServiceManager
   */
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  protected <A> Option<A> runRequest(HttpRequestBase req, Function<HttpResponse, A> f) {
    HttpResponse res = null;
    try {
      res = getResponse(req);
      return res != null ? some(f.apply(res)) : Option.<A> none();
    } finally {
      closeConnection(res);
    }
  }



  public static final Function<HttpResponse, Option<List<MediaPackageElement>>> elementsFromHttpResponse =
    new Function<HttpResponse, Option<List<MediaPackageElement>>>() {
    @Override
    public Option<List<MediaPackageElement>> apply(HttpResponse response) {
      try {
        final String xml = IOUtils.toString(response.getEntity().getContent(), Charset.forName("utf-8"));
        List<MediaPackageElement> result = new ArrayList<>(MediaPackageElementParser.getArrayFromXml(xml));
        return some(result);
      } catch (Exception e) {
        logger.error("Error parsing Job from HTTP response", e);
        return none();
      }
    }
  };

  /**
   * Makes a request to all available remote services and returns the response as soon as the first of them returns the
   * {@link HttpStatus#SC_OK} as the status code.
   *
   * @param httpRequest
   *          the http request. If the URI is specified, it should include only the path beyond the service endpoint.
   *          For example, a request intended for http://{host}/{service}/extra/path/info.xml should include the URI
   *          "/extra/path/info.xml".
   * @return the response object, or null if we can not connect to any services
   */
  protected HttpResponse getResponse(HttpRequestBase httpRequest) {
    return getResponse(httpRequest, HttpStatus.SC_OK);
  }

  /**
   * Makes a request to all available remote services and returns the response as soon as the first of them returns the
   * expected http status code.
   *
   * @param httpRequest
   *          the http request. If the URI is specified, it should include only the path beyond the service endpoint.
   *          For example, a request intended for http://{host}/{service}/extra/path/info.xml should include the URI
   *          "/extra/path/info.xml".
   * @param expectedHttpStatus
   *          any expected status codes to include in the return.
   * @return the response object, or null if we can not connect to any services
   */
  protected HttpResponse getResponse(HttpRequestBase httpRequest, Integer... expectedHttpStatus) {

    final long maxWaitTimeMillis = System.currentTimeMillis() + DateTimeConstants.MILLIS_PER_DAY;
    boolean warnedUnavailability = false;

    // Try forever
    while (true) {

      List<ServiceRegistration> remoteServices = null;
      List<String> servicesInWarningState = new ArrayList<String>();
      List<String> servicesInKnownState = new ArrayList<String>();

      // Find available services
      boolean warned = false;
      while (remoteServices == null || remoteServices.size() == 0) {
        try {
          remoteServices = remoteServiceManager.getServiceRegistrationsByLoad(serviceType);
          if (remoteServices == null || remoteServices.size() == 0) {
            if (!warned) {
              logger.warn("No services of type '{}' found, waiting...", serviceType);
              warned = true;
            }
            logger.debug("Still no services of type '{}' found, waiting...", serviceType);
            try {
              Thread.sleep(TIMEOUT);
            } catch (InterruptedException e) {
              logger.warn("Interrupted while waiting for remote service of type '{}'", serviceType);
              return null;
            }
          }
        } catch (ServiceRegistryException e) {
          logger.warn("Unable to obtain a list of remote services", e);
          return null;
        }
      }

      URI originalUri = httpRequest.getURI();
      String uriSuffix = null;
      if (originalUri != null && StringUtils.isNotBlank(originalUri.toString())) {
        uriSuffix = originalUri.toString();
      }

      // Try each available service
      String fullUrl = null;
      for (ServiceRegistration remoteService : remoteServices) {
        HttpResponse response = null;
        try {
          if (uriSuffix == null) {
            fullUrl = UrlSupport.concat(remoteService.getHost(), remoteService.getPath());
          } else {
            fullUrl = UrlSupport.concat(new String[] { remoteService.getHost(), remoteService.getPath(), uriSuffix });
          }

          logger.debug("Connecting to remote service of type '{}' at {}", serviceType, fullUrl);

          URI uri = new URI(fullUrl);
          httpRequest.setURI(uri);
          response = client.execute(httpRequest);
          StatusLine status = response.getStatusLine();
          if (Arrays.asList(expectedHttpStatus).contains(status.getStatusCode())) {
            if (servicesInWarningState.contains(fullUrl) || servicesInKnownState.contains(fullUrl)) {
              logger.warn("Service at {} is back to normal with expected status code {}", fullUrl,
                      status.getStatusCode());
            }
            return response;
          } else {
            if (!knownHttpStatuses.contains(status.getStatusCode()) && !servicesInWarningState.contains(fullUrl)) {
              logger.warn("Service at {} returned unexpected response code {}", fullUrl, status.getStatusCode());
              servicesInWarningState.add(fullUrl);
              servicesInKnownState.remove(fullUrl);
            } else if (knownHttpStatuses.contains(status.getStatusCode()) && !servicesInKnownState.contains(fullUrl)) {
              logger.info("Service at {} returned known response code {}", fullUrl, status.getStatusCode());
              servicesInKnownState.add(fullUrl);
              servicesInWarningState.remove(fullUrl);
            }
          }
        } catch (Exception e) {
          logger.error("Exception while trying to dispatch job to {}: {}", fullUrl, e);
          servicesInWarningState.add(fullUrl);
        }
        closeConnection(response);
      }

      if (servicesInKnownState.isEmpty()) {
        logger.warn("All services of type '{}' are in unknown state, abort remote call {}", serviceType, originalUri);
        return null;
      }

      // Reset Original URI
      httpRequest.setURI(originalUri);

      // If none of them accepted the request, let's wait and retry
      if (!warnedUnavailability) {
        logger.warn("No service of type '{}' is currently readily available", serviceType);
        warnedUnavailability = true;
      } else {
        logger.debug("All services of type '{}' are still unavailable", serviceType);
      }

      try {
        if (System.currentTimeMillis() > maxWaitTimeMillis) {
          logger.warn(
                  "Still no service of type '{}' available while waiting for more than one day, abort remote call {}",
                  serviceType, originalUri);
          return null;
        }
        Thread.sleep(TIMEOUT);
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for remote service of type '{}'", serviceType);
        return null;
      }

    }
  }

  /**
   * Closes any http connections kept open by this http response.
   */
  protected void closeConnection(HttpResponse response) {
    if (response != null)
      client.close(response);
  }

  /**
   * A stream wrapper that closes the http response when the stream is closed. If a remote service proxy returns an
   * inputstream, this implementation should be used to ensure that the http connection is closed properly.
   */
  public class HttpClientClosingInputStream extends InputStream {

    /** The input stream delivering the actual data */
    protected InputStream delegateStream = null;

    /** The http response to close when the stream is closed */
    protected HttpResponse httpResponse = null;

    /**
     * Constructs an HttpClientClosingInputStream from a source stream and an http response.
     *
     * @throws IOException
     * @throws IllegalStateException
     */
    public HttpClientClosingInputStream(HttpResponse resp) throws IllegalStateException, IOException {
      this.delegateStream = resp.getEntity().getContent();
      this.httpResponse = resp;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
      return delegateStream.read();
    }

    /**
     * {@inheritDoc}
     *
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
      return delegateStream.available();
    }

    /**
     * @throws IOException
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
      delegateStream.close();
      closeConnection(httpResponse);
    }

    /**
     * @param readlimit
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public void mark(int readlimit) {
      delegateStream.mark(readlimit);
    }

    /**
     * @return whether this stream supports marking
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
      return delegateStream.markSupported();
    }

    /**
     * @param b
     *          the buffer into which the data is read.
     * @param off
     *          the start offset in array <code>b</code> at which the data is written.
     * @param len
     *          the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no more data because the
     *         end of the stream has been reached.
     * @exception IOException
     *              If the first byte cannot be read for any reason other than end of file, or if the input stream has
     *              been closed, or if some other I/O error occurs.
     * @exception NullPointerException
     *              If <code>b</code> is <code>null</code>.
     * @exception IndexOutOfBoundsException
     *              If <code>off</code> is negative, <code>len</code> is negative, or <code>len</code> is greater than
     *              <code>b.length - off</code>
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return delegateStream.read(b, off, len);
    }

    /**
     * @param b
     *          the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or <code>-1</code> is there is no more data because the
     *         end of the stream has been reached.
     * @exception IOException
     *              If the first byte cannot be read for any reason other than the end of the file, if the input stream
     *              has been closed, or if some other I/O error occurs.
     * @exception NullPointerException
     *              if <code>b</code> is <code>null</code>.
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(byte[] b) throws IOException {
      return delegateStream.read(b);
    }

    /**
     * @throws IOException
     * @see java.io.InputStream#reset()
     */
    @Override
    public void reset() throws IOException {
      delegateStream.reset();
    }

    /**
     * @param n
     *          the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @exception IOException
     *              if the stream does not support seek, or if some other I/O error occurs.
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException {
      return delegateStream.skip(n);
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return getClass().getName() + " : " + delegateStream.toString();
    }
  }

}
