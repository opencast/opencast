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


package org.opencastproject.util;

import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Misc.chuck;
import static org.opencastproject.util.data.functions.Strings.asStringNull;

import org.opencastproject.util.data.Function2;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

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
   * Concatenates the two urls with respect to leading and trailing slashes.
   * <p>
   * Note that returned path will only end with a slash if <code>suffix</code> does. If you need a trailing slash, see
   * {@link #concat(String, String, boolean)}.
   *
   * @return the concatenated url of the two arguments
   */
  public static String concat(String prefix, String suffix) {
    return concat(prefix, suffix, false);
  }

  /**
   * Concatenates the two urls with respect to leading and trailing slashes. The path will always end with a trailing
   * slash.
   *
   * @return the concatenated url of the two arguments
   */
  public static String concat(String prefix, String suffix, boolean close) {
    if (prefix == null)
      throw new IllegalArgumentException("Argument prefix is null");
    if (suffix == null)
      throw new IllegalArgumentException("Argument suffix is null");

    prefix = checkSeparator(prefix);
    suffix = checkSeparator(suffix);
    prefix = removeDoubleSeparator(prefix);
    suffix = removeDoubleSeparator(suffix);

    if (!prefix.endsWith("/") && !suffix.startsWith("/"))
      prefix += "/";
    if (prefix.endsWith("/") && suffix.startsWith("/"))
      suffix = suffix.substring(1);

    prefix += suffix;

    // Close?
    if (close && !prefix.endsWith("/")) {
      prefix += "/";
    }
    return prefix;
  }

  /**
   * Concatenates the urls with respect to leading and trailing slashes.
   *
   * @param parts
   *          the parts to concat
   * @return the concatenated url
   */
  public static String concat(String... parts) {
    if (parts == null)
      throw new IllegalArgumentException("Argument parts is null");
    if (parts.length == 0)
      throw new IllegalArgumentException("Array parts is empty");
    String path = parts[0];
    for (int i = 1; i < parts.length; i++) {
      if (parts[i] != null) {
        path = concat(path, parts[i]);
      }
    }
    return path;
  }

  /**
   * Concatenates the urls with respect to leading and trailing slashes.
   *
   * @param parts
   *          the parts to concat
   * @return the concatenated url
   */
  public static String concat(List<String> parts) {
    if (parts == null)
      throw new IllegalArgumentException("Argument parts is null");
    if (parts.size() == 0)
      throw new IllegalArgumentException("Array parts is empty");
    return mlist(parts).reducel(new Function2<String, String, String>() {
      @Override
      public String apply(String s, String s1) {
        return concat(s, s1);
      }
    });
  }

  /** Create a URI from the given parts. */
  public static URI uri(Object... parts) {
    return URI.create(concat(mlist(parts).map(asStringNull()).value()));
  }

  /**
   * Checks that the path only contains the web path separator "/". If not, wrong ones are replaced.
   */
  private static String checkSeparator(String path) {
    String sp = File.separator;
    if ("\\".equals(sp))
      sp = "\\\\";
    return path.replaceAll(sp, "/");
  }

  /**
   * Removes any occurrence of double separators ("//") and replaces it with "/".
   * Any double separators right after the protocol part are left untouched so that, e.g. http://localhost
   * stays http://localhost
   *
   * @param path
   *          the path to check
   * @return the corrected path
   */
  public static String removeDoubleSeparator(String path) {
    String protocol = "";
    int index = path.indexOf("://");

    // Strip off the protocol
    if (index != -1) {
      protocol = path.substring(0, index + 3);
      path = path.substring(index + 3);
    }

    // Search rest of path for double separators
    index = path.indexOf("//", index);
    while (index != -1) {
      path = path.substring(0, index) + path.substring(index + 1);
      index = path.indexOf("//", index);
    }
    return protocol + path;
  }

}
