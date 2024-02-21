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
package org.opencastproject.assetmanager.api.query;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.ARecords;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Extensions for {@link AResult}.
 */
public final class RichAResult implements AResult {
  private final AResult result;

  public RichAResult(AResult result) {
    this.result = result;
  }

  public List<Property> getProperties() {
    return ARecords.getProperties(result.getRecords());
  }

  /** Count all properties contained in the result. */
  public int countProperties() {
    return new HashSet(getProperties()).size();
  }

  /** Get all selected snapshots. */
  public List<Snapshot> getSnapshots() {
    return result.getRecords()
        .stream()
        .map(record -> record.getSnapshot())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /** Count all snapshots contained in the result. */
  public int countSnapshots() {
    return new HashSet(getSnapshots()).size();
  }

  /** Get all selected versions. */
  public List<Version> getVersions() {
    return getSnapshots()
        .stream()
        .map(snapshot -> snapshot.getVersion())
        .collect(Collectors.toList());
  }

  //
  // delegates
  //

  @Override public Iterator<ARecord> iterator() {
    return result.iterator();
  }

  @Override public LinkedHashSet<ARecord> getRecords() {
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
