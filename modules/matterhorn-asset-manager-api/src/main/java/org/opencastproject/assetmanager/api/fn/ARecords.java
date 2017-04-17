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
package org.opencastproject.assetmanager.api.fn;

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.ARecord;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

/**
 * Functions to deal with {@link ARecord}s.
 */
public final class ARecords {
  public static final Fn<ARecord, String> getMediaPackageId = new Fn<ARecord, String>() {
    @Override public String apply(ARecord item) {
      return item.getMediaPackageId();
    }
  };

  public static final Fn<ARecord, Stream<Property>> getProperties = new Fn<ARecord, Stream<Property>>() {
    @Override public Stream<Property> apply(ARecord item) {
      return item.getProperties();
    }
  };

  public static final Pred<ARecord> hasProperties = new Pred<ARecord>() {
    @Override public Boolean apply(ARecord item) {
      return !item.getProperties().isEmpty();
    }
  };

  /**
   * Get the snapshot from a record.
   *
   * @see ARecord#getSnapshot()
   */
  public static final Fn<ARecord, Opt<Snapshot>> getSnapshot = new Fn<ARecord, Opt<Snapshot>>() {
    @Override public Opt<Snapshot> apply(ARecord a) {
      return a.getSnapshot();
    }
  };

  private ARecords() {
  }
}
