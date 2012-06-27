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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Support for building a querystring. Not threadsafe.
 */
public class QueryStringBuilder {

  /** The stringbuilder containing the query string itself */
  private StringBuilder sb = null;
  private String baseServicePath = "";

  /**
   * Constructs a querystring builder starting with the base path
   * 
   * @param baseServicePath
   *          the base path
   */
  public QueryStringBuilder(String baseServicePath) {
    sb = new StringBuilder(baseServicePath);
    this.baseServicePath = baseServicePath;
  }

  /**
   * Add a key/value pair to the querystring. This method may be called multiple times for a given key, and each\ value
   * will be added to the querystring.
   * 
   * @param key
   *          the parameter key
   * @param value
   *          the parameter value
   * @return the augmented querystring builder
   */
  public QueryStringBuilder add(String key, String value) {
    if (key == null) {
      throw new IllegalArgumentException("key can not be null");
    }
    if (sb.length() == baseServicePath.length()) {
      sb.append("?");
    } else {
      sb.append("&");
    }
    sb.append(key);
    sb.append("=");
    if (value != null) {
      try {
        sb.append(URLEncoder.encode(value, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException();
      }
    }
    return this;
  }

  /**
   * Returns the full querystring for this builder, including the '?'
   */
  public String toString() {
    return sb.toString();
  }
}
