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
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.ARecords;
import org.opencastproject.assetmanager.api.fn.Snapshots;

import com.entwinemedia.fn.Stream;

import java.util.Iterator;

/**
 * Extensions for {@link AResult}.
 */
public final class RichAResult implements AResult {
  private final AResult result;

  public RichAResult(AResult result) {
    this.result = result;
  }

  public Stream<Property> getProperties() {
    return result.getRecords().bind(ARecords.getProperties);
  }

  /** Count all properties contained in the result. */
  public int countProperties() {
    return sizeOf(getProperties());
  }

  /** Get all selected snapshots. */
  public Stream<Snapshot> getSnapshots() {
    return result.getRecords().bind(ARecords.getSnapshot);
  }

  /** Count all snapshots contained in the result. */
  public int countSnapshots() {
    return sizeOf(getSnapshots());
  }

  /** Get all selected versions. */
  public Stream<Version> getVersions() {
    return getSnapshots().map(Snapshots.getVersion);
  }

  private static <A> int sizeOf(Stream<A> stream) {
    int count = 0;
    for (A ignore : stream) {
      count++;
    }
    return count;
  }

  //
  // delegates
  //

  @Override public Iterator<ARecord> iterator() {
    return result.iterator();
  }

  @Override public Stream<ARecord> getRecords() {
    return result.getRecords();
  }

  @Override public long getSize() {
    return result.getSize();
  }

  @Override public String getQuery() {
    return result.getQuery();
  }

  @Override public long getTotalSize() {
    return result.getTotalSize();
  }

  @Override public long getLimit() {
    return result.getLimit();
  }

  @Override public long getOffset() {
    return result.getOffset();
  }

  @Override public long getSearchTime() {
    return result.getSearchTime();
  }
}
