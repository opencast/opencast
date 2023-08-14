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
package org.opencastproject.assetmanager.impl.query;

import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;

import com.entwinemedia.fn.Stream;

import java.util.Iterator;

public final class AResultImpl implements AResult {
  private final Stream<ARecord> records;
  private final long size;
  private final long limit;
  private final long offset;
  private final long searchTime;

  public AResultImpl(Stream<ARecord> records, long size, long offset, long limit, long searchTime) {
    this.records = records;
    this.size = size;
    this.limit = limit;
    this.offset = offset;
    this.searchTime = searchTime;
  }

  @Override public Iterator<ARecord> iterator() {
    return records.iterator();
  }

  @Override public Stream<ARecord> getRecords() {
    return records;
  }

  @Override public long getSize() {
    return size;
  }

  @Override public String getQuery() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override public long getTotalSize() {
    return -1;
  }

  @Override public long getLimit() {
    return limit;
  }

  @Override public long getOffset() {
    return offset;
  }

  @Override public long getSearchTime() {
    return searchTime;
  }
}
