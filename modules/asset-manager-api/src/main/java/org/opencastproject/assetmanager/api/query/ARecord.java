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
package org.opencastproject.assetmanager.api.query;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

/**
 * The result of a {@link ASelectQuery}. Each record represents a matching snapshot of a media package.
 * Its purpose is to group the various data that is associated with a media package and to support their partial loading.
 */
public interface ARecord {
  /** Get the snapshot ID.  This is from the underlying DTO, and thus may be null. */
  long getSnapshotId();
  /** Get the media package ID. */
  String getMediaPackageId();

  /**
   * Get all properties associated with an episode. If the stream contains any properties also depends on
   * the query specification. If it has not been specified to fetch properties the stream is definitely empty.
   * <p>
   * Please note that properties are not versioned but stored per episode.
   */
  Stream<Property> getProperties();

  /** Get the snapshot or return none if it has not been specified to fetch it. */
  Opt<Snapshot> getSnapshot();
}
