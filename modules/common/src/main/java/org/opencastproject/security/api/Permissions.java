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

package org.opencastproject.security.api;

/**
 * Represent the common permissions in Opencast
 */
public interface Permissions {

  enum Action {

    /** Identifier for read permissions */
    READ("read"),

    /** Identifier for write permissions */
    WRITE("write"),

    /** Identifier for contribute permissions */
    CONTRIBUTE("contribute");

    private final String value;

    Action(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return this.getValue();
    }

    public static Action getEnum(String value) {
      for (Action v : values())
        if (v.getValue().equalsIgnoreCase(value))
          return v;
      throw new IllegalArgumentException();
    }

  }

}
