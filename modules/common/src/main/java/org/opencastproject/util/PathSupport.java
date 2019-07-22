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

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

/**
 * <code>PathSupport</code> is a helper class to deal with filesystem paths.
 */
public final class PathSupport {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PathSupport.class);

  /**
   * This class should not be instantiated, since it only provides static utility methods.
   */
  private PathSupport() {
  }

  /**
   * Returns the filename translated into a version that can safely be used as part of a file system path.
   *
   * @param fileName
   *          The file name
   * @return the safe version
   */
  public static String toSafeName(String fileName) {
    String urlExtension = FilenameUtils.getExtension(fileName);
    String baseName = FilenameUtils.getBaseName(fileName);
    String safeBaseName = baseName.replaceAll("\\W", "_"); // TODO -- ensure that this filename is safe on all platforms
    String safeString;
    if ("".equals(urlExtension)) {
      safeString = safeBaseName;
    } else {
      safeString = safeBaseName + "." + urlExtension;
    }
    if (safeString.length() < 255)
      return safeString;
    String random = UUID.randomUUID().toString();
    if (!"".equals(urlExtension)) {
      random = random.concat(".").concat(urlExtension);
    }
    logger.info("using '{}' to represent url '{}', which is too long to store as a filename", random, fileName);
    return random;
  }

  /**
   * Concatenates the two urls with respect to leading and trailing slashes.
   *
   * @return the concatenated url of the two arguments
   * @deprecated
   *          Use Java's native <pre>Paths.get(String, …).toFile()</pre> instead
   */
  @Deprecated
  public static String concat(String prefix, String suffix) {
    if (prefix == null)
      throw new IllegalArgumentException("Argument prefix is null");
    if (suffix == null)
      throw new IllegalArgumentException("Argument suffix is null");

    prefix = adjustSeparator(prefix);
    suffix = adjustSeparator(suffix);
    prefix = removeDoubleSeparator(prefix);
    suffix = removeDoubleSeparator(suffix);

    if (!prefix.endsWith(File.separator) && !suffix.startsWith(File.separator))
      prefix += File.separator;
    if (prefix.endsWith(File.separator) && suffix.startsWith(File.separator))
      suffix = suffix.substring(1);

    prefix += suffix;
    return prefix;
  }

  /**
   * Concatenates the path elements with respect to leading and trailing slashes.
   *
   * @param parts
   *          the parts to concat
   * @return the concatenated path
   * @deprecated
   *          Use Java's native <pre>Paths.get(String, …).toFile()</pre> instead
   */
  @Deprecated
  public static String concat(String[] parts) {
    if (parts == null)
      throw new IllegalArgumentException("Argument parts is null");
    if (parts.length == 0)
      throw new IllegalArgumentException("Array parts is empty");
    String path = removeDoubleSeparator(adjustSeparator(parts[0]));
    for (int i = 1; i < parts.length; i++) {
      path = concat(path, removeDoubleSeparator(adjustSeparator(parts[i])));
    }
    return path;
  }

  @Deprecated
  public static String path(String... parts) {
    return concat(parts);
  }

  /**
   * Checks that the path only contains the system path separator. If not, wrong ones are replaced.
   */
  private static String adjustSeparator(String path) {
    String sp = File.separator;
    if ("\\".equals(sp))
      sp = "\\\\";
    return path.replaceAll("/", sp);
  }

  /**
   * Removes any occurrence of double file separators and replaces it with a single one.
   *
   * @param path
   *          the path to check
   * @return the corrected path
   * @deprecated
   *          This implements built-in Java functionality. Use instead:
   *            <ul>
   *              <li><pre>Paths.get("/a/b//c")</pre></li>
   *              <li><pre>new File("/a/b//c")</pre></li>
   *            </ul>
   */
  @Deprecated
  private static String removeDoubleSeparator(String path) {
    int index = 0;
    String s = File.separator + File.separatorChar;
    while ((index = path.indexOf(s, index)) != -1) {
      path = path.substring(0, index) + path.substring(index + 1);
    }
    return path;
  }

}
