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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * <code>PathSupport</code> is a helper class to deal with filesystem paths.
 */
@Deprecated
public final class PathSupport {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PathSupport.class);

  /**
   * This class should not be instantiated, since it only provides static utility methods.
   */
  private PathSupport() {
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
    if (prefix == null) {
      throw new IllegalArgumentException("Argument prefix is null");
    }
    if (suffix == null) {
      throw new IllegalArgumentException("Argument suffix is null");
    }
    return Paths.get(prefix, suffix).toString();
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
    if (parts == null) {
      throw new IllegalArgumentException("Argument parts is null");
    }
    if (parts.length == 0) {
      throw new IllegalArgumentException("Array parts is empty");
    }
    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        throw new IllegalArgumentException("Element " + i + " of argument parts is null");
      }
    }
    Path path = Paths.get(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    return path.toString();
  }

  @Deprecated
  public static String path(String... parts) {
    return concat(parts);
  }

}
