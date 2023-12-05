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

package org.opencastproject.kernel.bundleinfo;

import java.util.List;

/** Persistence for bundle information. */
public interface BundleInfoDb {
  /** Store a bundle info object. */
  void store(BundleInfo info) throws BundleInfoDbException;

  /** Delete a bundle. */
  void delete(String host, long bundleId) throws BundleInfoDbException;

  /** Clear the database for a certain host. */
  void clear(String host) throws BundleInfoDbException;

  /** Clear the complete database. */
  void clearAll() throws BundleInfoDbException;

  /** Return a list of all running bundles. */
  List<BundleInfo> getBundles() throws BundleInfoDbException;

  /** Return a list of all running bundles whose symbolic names start with one of the given prefixes. */
  List<BundleInfo> getBundles(String... prefixes) throws BundleInfoDbException;
}
