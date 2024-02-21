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
package org.opencastproject.assetmanager.api.fn;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.ARecord;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Functions to deal with {@link ARecord}s.
 */
public final class ARecords {
  public static String getMediaPackageId(LinkedHashSet<ARecord> records) {
    return records.stream()
        .map(r -> r.getMediaPackageId())
        .findFirst()
        .get();

  }

  public static List<Property> getProperties(LinkedHashSet<ARecord> records) {
    return records.stream()
        .map(r -> r.getProperties())
        .flatMap(List::stream)
        .collect(Collectors.toList());
  };

  public static boolean hasProperties(ARecord record) {
    return record.getProperties().isEmpty();
  }

  /**
   * Get the snapshot from a record.
   *
   * @see ARecord#getSnapshot()
   */
  public static List<Snapshot> getSnapshot(LinkedHashSet<ARecord> records) {
    return records.stream()
        .map(r -> r.getSnapshot())
        .filter(s -> s.isPresent())
        .map(s -> s.get())
        .collect(Collectors.toList());

  }

  private ARecords() {
  }
}
