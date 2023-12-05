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
package org.opencastproject.assetmanager.api;

import java.io.Serializable;

/**
 * The version of an archived media package or element.
 * Versions have a natural order where v1 &lt; v2 means that v1 is older that v2.
 */
public interface Version extends Comparable<Version>, Serializable {
  /** Check if this version is older than <code>v</code>. */
  boolean isOlder(Version v);

  /** Check if this version is younger than <code>v</code>. */
  boolean isYounger(Version v);

  /** A version must implement hashCode and equals. */
  boolean equals(Object that);

  /** A version must implement hashCode and equals. */
  int hashCode();

  /**
   * Serialize to a string. This is the inverse function of
   * {@link org.opencastproject.assetmanager.api.AssetManager#toVersion(String)}.
   */
  String toString();
}
