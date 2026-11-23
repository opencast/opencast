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


package org.opencastproject.util;

import static org.opencastproject.util.data.functions.Misc.chuck;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

/**
 * <code>UrlSupport</code> is a helper class to deal with urls.
 */
public final class UrlSupport {
  public static final String DEFAULT_BASE_URL = "http://localhost:8080";

  /**
   * This class should not be instanciated, since it only provides static utility methods.
   */
  private UrlSupport() {
  }

  /** URI constructor function without checked exceptions. */
  public static URI uri(String uri) {
    try {
      return new URI(uri);
    } catch (Exception e) {
      return chuck(e);
    }
  }

  /** URL constructor function without checked exceptions. */
  public static URL url(String url) {
    try {
      return new URL(url);
    } catch (Exception e) {
      return chuck(e);
    }
  }

  /** URL constructor function without checked exceptions. */
  public static URL url(String protocol, String host, int port) {
    try {
      return new URL(protocol, host, port, "/");
    } catch (Exception e) {
      return chuck(e);
    }
  }

  /**
   * URL constructor function without checked exceptions.
   *
   * @see URL
   */
  public static URL url(URL context, String spec) {
    try {
      return new URL(context, spec);
    } catch (Exception e) {
      return chuck(e);
    }
  }

  /**
   * Concatenates URL path parts, making sure that there is exactly one slash between two consecutive parts.
   * <p>
   * Only the slashes at the boundaries of the parts are touched. Slashes within a part are left as they are, as is a
   * leading slash of the first part and a trailing slash of the last part. Parts which are <code>null</code> or empty
   * are ignored.
   * <p>
   * All slashes at the boundaries are replaced by a single one. Hence, a part must not end in a bare scheme like
   * <code>http://</code>, since <code>concat("http://", "host")</code> would result in <code>http:/host</code>.
   * <p>
   * This is meant for path segments only. Query strings and fragments have to be appended to the result, and file
   * system paths should be built with {@link java.nio.file.Path} instead.
   * <p>
   * Examples:
   * <ul>
   *   <li><code>concat("http://localhost:8080/", "/files", "mediapackage")</code> &rarr;
   *       <code>http://localhost:8080/files/mediapackage</code></li>
   *   <li><code>concat("/", "oaipmh")</code> &rarr; <code>/oaipmh</code></li>
   *   <li><code>concat("a", "", "b/")</code> &rarr; <code>a/b/</code></li>
   * </ul>
   *
   * @param parts
   *          the parts to concatenate
   * @return the concatenated parts, or an empty string if there are no non-empty parts
   */
  public static String concat(String... parts) {
    String result = null;
    for (String part : parts) {
      if (StringUtils.isEmpty(part)) {
        continue;
      }
      result = result == null
          ? part
          : StringUtils.stripEnd(result, "/") + "/" + StringUtils.stripStart(part, "/");
    }
    return Objects.toString(result, "");
  }

  /**
   * Create a URI from the given parts. The parts are converted to strings and concatenated using
   * {@link #concat(String...)}. Parts which are <code>null</code> are ignored.
   */
  public static URI uri(Object... parts) {
    return URI.create(concat(Arrays.stream(parts)
        .map(part -> Objects.toString(part, null))
        .toArray(String[]::new)));
  }

  /**
   * Returns the URI as a string, without its query and fragment. The URI is not decoded or normalized, so that the
   * result can be compared with other URIs, e.g. to look up a file. This is useful when query and fragment are not part
   * of the identity of a resource, like the signature that stream security adds to a URL.
   *
   * @param uri
   *          the uri
   * @return the uri without query and fragment
   */
  public static String withoutQueryAndFragment(URI uri) {
    final String uriString = uri.toString();
    int end = uriString.length();
    if (uri.getRawFragment() != null) {
      end = uriString.indexOf('#');
    }
    // A question mark after the start of the fragment is not a query
    if (uri.getRawQuery() != null) {
      end = Math.min(end, uriString.indexOf('?'));
    }
    return uriString.substring(0, end);
  }

}
