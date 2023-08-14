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
package org.opencastproject.editor.api;

import java.util.Objects;

public class SegmentData {
  private Long start;
  private Long end;
  private boolean deleted = false;

  public SegmentData(Long start, Long end, boolean deleted) {
    this.start = start;
    this.end = end;
    this.deleted = deleted;
  }

  public SegmentData(Long start, Long end) {
    this.start = start;
    this.end = end;
    this.deleted = false;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public Long getStart() {
    return start;
  }

  public Long getEnd() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SegmentData that = (SegmentData) o;
    return Objects.equals(start, that.start) && Objects.equals(end, that.end) && Objects.equals(deleted, that.deleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, deleted);
  }
}
