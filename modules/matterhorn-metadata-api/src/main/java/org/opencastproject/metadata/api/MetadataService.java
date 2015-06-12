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

package org.opencastproject.metadata.api;

import org.opencastproject.mediapackage.MediaPackage;

/**
 * Generic interface for metadata providing services.
 * @param <A>
 *          the type of metadata provided
 */
public interface MetadataService<A> {

  /** The static constant used when configuring the priority */
  String PRIORITY_KEY = "priority";

  /**
   * The priority of this MetadataService compared to others when more than one is registered in the system.
   *
   * When more than one MetadataService is registered, the {@link #getMetadata(MediaPackage)} method may be
   * called on each service in order of priority. Metadata objects returned by higher priority
   * MetadataServices should override those returned by lower priority services.
   *
   * The lowest number is the highest priority (i.e. 1 is a higher priority than 2).
   *
   * @return The priority
   */
  int getPriority();

  /**
   * Gets the metadata for a {@link MediaPackage} if possible.  If no metadata can be extracted
   * from the catalogs in the {@link MediaPackage}, this returns null;
   *
   * @param mediaPackage The mediapackage to inspect for catalogs
   * @return The metadata extracted from the media package
   */
  A getMetadata(MediaPackage mediaPackage);
}
