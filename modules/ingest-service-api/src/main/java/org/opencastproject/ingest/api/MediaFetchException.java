/*
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

package org.opencastproject.ingest.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Thrown if content to ingest cannot be fetched from the URI a client specified, e.g. because the host is not
 * reachable, the server responds with an error or the connection breaks off during the download.
 *
 * This is a problem of the client's input, not of the ingest service.
 */
public class MediaFetchException extends IOException {

  private static final long serialVersionUID = 1L;

  private final String source;

  /**
   * Create a new exception.
   *
   * @param uri
   *          the URI that could not be fetched
   * @param reason
   *          why fetching the URI failed
   * @param cause
   *          the original cause or {@code null}
   */
  public MediaFetchException(URI uri, String reason, Throwable cause) {
    super("Unable to fetch " + sanitize(uri) + ": " + reason, cause);
    this.source = sanitize(uri);
  }

  /**
   * Create a new exception, using the cause to describe why fetching the URI failed.
   *
   * @param uri
   *          the URI that could not be fetched
   * @param cause
   *          the original cause
   */
  public MediaFetchException(URI uri, Throwable cause) {
    this(uri, describe(cause), cause);
  }

  /**
   * The URI that could not be fetched, without user information, query or fragment, which may contain credentials.
   *
   * @return the sanitized URI
   */
  public String getSource() {
    return source;
  }

  private static String sanitize(URI uri) {
    if (uri == null) {
      return "unknown location";
    }
    try {
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
    } catch (URISyntaxException | IllegalArgumentException e) {
      return uri.getScheme() + ":…";
    }
  }

  private static String describe(Throwable cause) {
    if (cause == null) {
      return "unknown reason";
    }
    // HTTP clients like to wrap the exception which actually describes the problem, e.g. a NoRouteToHostException
    Throwable root = cause;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String message = root.getMessage();
    return message == null
        ? root.getClass().getSimpleName()
        : root.getClass().getSimpleName() + " (" + message + ")";
  }
}
