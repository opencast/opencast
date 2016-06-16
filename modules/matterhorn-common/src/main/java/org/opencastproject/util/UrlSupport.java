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
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

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
   * Sorts the given urls by path.
   *
   * @param urls
   *          the urls to sort
   * @return the sorted urls
   */
  public static String[] sort(String[] urls) {
    TreeSet<String> set = new TreeSet<String>();
    for (int i = 0; i < urls.length; i++)
      set.add(urls[i]);
    String[] result = new String[urls.length];
    Iterator<String> i = set.iterator();
    int index = 0;
    while (i.hasNext()) {
      result[index++] = i.toString();
    }
    return result;
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
   * Returns the trimmed url. Trimmed means that the url is free from leading or trailing whitespace characters, and
   * that a directory url like <code>/news/</code> is closed by a slash (<code>/</code>).
   *
   * @param url
   *          the url to trim
   * @return the trimmed url
   */
  public static String trim(String url) {
    if (url == null)
      throw new IllegalArgumentException("Argument url is null");

    url = url.trim();
    url = checkSeparator(url);

    if (url.endsWith("/") || (url.length() == 1))
      return url;

    int index = url.lastIndexOf("/");
    index = url.indexOf(".", index);
    if (index == -1)
      url += "/";
    return url;
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

  /**
   * Returns <code>true</code> if url <code>a</code> is a direct prefix of url <code>b</code>. For example,
   * <code>/news</code> is the parent of <code>/news/today</code>.
   * <p>
   * Note that <code>a</code> is also an extended prefix of <code>b</code> if <code>a</code> and <code>b</code> are
   * equal.
   *
   * @param a
   *          the first url
   * @param b
   *          the second url
   * @return <code>true</code> if <code>a</code> is the direct prefix of <code>b</code>
   */
  public static boolean isPrefix(String a, String b) {
    if (isExtendedPrefix(a, b)) {
      if (a.length() < b.length()) {
        String bRest = b.substring(a.length() + 1);
        if (bRest.endsWith("/"))
          bRest = bRest.substring(0, bRest.length() - 2);
        return bRest.indexOf("/", 1) < 0;
      } else {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns <code>true</code> if url <code>a</code> is a prefix of url <code>b</code>. For example, <code>/news</code>
   * is an ancestor of <code>/news/today/morning</code>.
   * <p>
   * Note that <code>a</code> is also an extended prefix of <code>b</code> if <code>a</code> and <code>b</code> are
   * equal.
   *
   * @param a
   *          the first url
   * @param b
   *          the second url
   * @return <code>true</code> if <code>a</code> is a prefix of <code>b</code>
   */
  public static boolean isExtendedPrefix(String a, String b) {
    if (b.startsWith(a)) {
      if (b.length() > a.length())
        return a.endsWith("/") || b.substring(a.length()).startsWith("/");
      else
        return true;
    }
    return false;
  }

  /**
   * Returns the url extension that <code>url</code> defines over <code>prefix</code>. For example, the extension of url
   * <code>/news/today</code> and prefix <code>/news</code> is <code>today</code>.
   * <p>
   * If <code>prefix</code> is not a prefix of <code>url</code>, this method returns <code>null</code>, if
   * <code>url</code> and <code>prefix</code> match, the empty string is returned.
   *
   * @param prefix
   *          the url prefix
   * @param url
   *          the url
   * @return the url extension over the prefix
   */
  public static String getExtension(String prefix, String url) {
    prefix = prefix.trim();
    if (isExtendedPrefix(prefix, url)) {
      if (url.length() > prefix.length()) {
        String extension = url.substring(prefix.length() + 1);
        if (extension.endsWith("/")) {
          extension = extension.substring(0, extension.length() - 1);
        }
        return extension;
      } else
        return "";
    }
    return null;
  }

  /**
   * Returns the extension that is encoded into the url. Possible extensions are:
   * <ul>
   * <li>/*</li>
   * <li>/**</li>
   * <li><code>null</code></li>
   * </ul>
   *
   * @param url
   *          the url with extension
   * @return the url extension or <code>null</code> if no extension can be found
   */
  public static String getExtension(String url) {
    if (url.endsWith("/**"))
      return "/**";
    else if (url.endsWith("/*"))
      return "/*";
    else
      return null;
  }

  /**
   * Strips off the extension and returns the pure url.
   *
   * @param url
   *          the url with extension
   * @return the url
   */
  public static String stripExtension(String url) {
    String extension = getExtension(url);
    if (extension == null)
      return url;
    else
      return url.substring(0, url.length() - extension.length());
  }

  /**
   * Returns <code>true</code> if the url is valid, that is, if it contains only allowed characters.
   *
   * @return <code>true</code> or the invalid character
   */
  public static boolean isValid(String url) {
    return (checkUrl(url) == null);
  }

  /**
   * Returns <code>null</code> if the url is valid, that is, if it contains only allowed characters. Otherwhise, the
   * invalid character is returned.
   *
   * @return <code>null</code> or the invalid character
   */
  public static Character getInvalidCharacter(String url) {
    Character c = checkUrl(url);
    return c;
  }

  /**
   * Returns <code>null</code> if the url is valid, that is, if it contains only allowed characters. Otherwhise, the
   * invalid character is returned.
   *
   * @return <code>null</code> or the invalid character
   */
  private static Character checkUrl(String url) {
    StringBuffer original = new StringBuffer(url);
    for (int i = 0; i < original.length(); i++) {
      int value = (new Character(original.charAt(i))).charValue();
      // a-z
      if (value >= new Character('a').charValue() && value <= new Character('z').charValue()) {
        continue;
      }
      // A-Z
      if (value >= new Character('A').charValue() && value <= new Character('Z').charValue()) {
        continue;
      }
      // 0-9
      if (value >= new Character('0').charValue() && value <= new Character('9').charValue()) {
        continue;
      }
      // Special characters
      if ((value == new Character('-').charValue()) || (value == '_') || (value == '.') || (value == ',')
              || (value == ';')) {
        continue;
      }
      return Character.valueOf(original.charAt(i));
    }
    return null;
  }

}
