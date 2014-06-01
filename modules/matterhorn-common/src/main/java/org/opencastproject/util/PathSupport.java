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
   * This class should not be instanciated, since it only provides static utility methods.
   */
  private PathSupport() {
  }

  /**
   * Returns the absolute path from a path optionally starting with a tilde <code>~</code>.
   *
   * @param path
   *          the path
   * @return the absolute path
   */
  public static String toAbsolute(String path) {
    if (path == null)
      throw new IllegalArgumentException("Path must not be null");
    if (path.startsWith("~")) {
      String homeDir = System.getProperty("user.home");
      path = path.substring(1);
      path = concat(homeDir, path);
    }
    return path;
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
    String safeString = null;
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
   */
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
   */
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

  public static String path(String... parts) {
    return concat(parts);
  }

  /**
   * Returns the trimmed url. Trimmed means that the url is free from leading or trailing whitespace characters, and
   * that a directory url like <code>/news/</code> is closed by a slash (<code>/</code>).
   *
   * @param path
   *          the path to trim
   * @return the trimmed path
   */
  public static String trim(String path) {
    if (path == null)
      throw new IllegalArgumentException("Argument path is null");
    path = path.trim();
    path = removeDoubleSeparator(adjustSeparator(path));
    if (path.endsWith(File.separator) || (path.length() == 1))
      return path;

    int index = path.lastIndexOf(File.separator);
    index = path.indexOf(".", index);
    if (index == -1)
      path += File.separator;
    return path;
  }

  /**
   * Returns the file extension. If the file does not have an extension, then <code>null</code> is returned.
   *
   * @param path
   *          the file path
   * @return the file extension
   */
  public static String getFileExtension(String path) {
    if (path == null) {
      throw new IllegalArgumentException("Argument path is null");
    }
    int index = path.lastIndexOf('.');
    if (index > 0 && index < path.length()) {
      return path.substring(index + 1);
    }
    return null;
  }

  /**
   * Removes a file extension from the end of the path. If there is no extension or the file name starts with the
   * extension separator "." <code>path</code> will be returned untouched.
   *
   * @return the path with the extension removed or null if <code>path</code> was null
   */
  public static String removeFileExtension(String path) {
    if (path != null) {
      int index = path.lastIndexOf('.');
      if (index > 0) {
        if (path.charAt(index - 1) != File.separatorChar) {
          return path.substring(0, index);
        }
      }
    }
    return path;
  }

  /**
   * Removes any existing file extension from the end of the path and replaces it with the given one.
   *
   * @param path
   *          path to the file
   * @param extension
   *          the new file extension
   * @return the path with the new extension
   */
  public static String changeFileExtension(String path, String extension) {
    if (path != null) {
      int index = path.lastIndexOf('.');
      if (index > 0) {
        if (path.charAt(index - 1) != File.separatorChar) {
          path = path.substring(0, index);
        }
      }
    }
    return path + extension;
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
   * Removes any occurence of double file separators and replaces it with a single one.
   *
   * @param path
   *          the path to check
   * @return the corrected path
   */
  private static String removeDoubleSeparator(String path) {
    int index = 0;
    String s = File.separator + File.separatorChar;
    while ((index = path.indexOf(s, index)) != -1) {
      path = path.substring(0, index) + path.substring(index + 1);
    }
    return path;
  }

}
