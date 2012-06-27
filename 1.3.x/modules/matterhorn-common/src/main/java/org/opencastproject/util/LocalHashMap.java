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

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * See this JAXB bug for the full explanation: https://jaxb.dev.java.net/issues/show_bug.cgi?id=223
 */
public class LocalHashMap {

  /** The internal backing map */
  protected Map<String, String> map = new HashMap<String, String>();

  /** Returns the internal map storing the properties */
  public Map<String, String> getMap() {
    return map;
  }

  /** No-arg constructor needed by JAXB */
  public LocalHashMap() {
  }

  /**
   * Constructs this map from a properties list, expressed as a string:
   * 
   * <code>
   * foo=bar
   * this=that
   * </code>
   * 
   * @param in
   *          The properties list
   * @throws IOException
   *           if parsing the string fails
   */
  public LocalHashMap(String in) throws IOException {
    Properties properties = new Properties();
    properties.load(IOUtils.toInputStream(in, "UTF-8"));
    for (Entry<Object, Object> e : properties.entrySet()) {
      map.put((String) e.getKey(), (String) e.getValue());
    }
  }
}
