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
package org.opencastproject.scheduler.api;

import static org.opencastproject.util.EqualsUtil.eq;

import org.opencastproject.util.EqualsUtil;

import java.util.Date;
import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

/** Business object for a period. */
@ThreadSafe
public final class Period {
  /** The period identifier */
  private final Optional<Long> id;

  /** The start date */
  private final Date start;

  /** The end date */
  private final Date end;

  /** The purpose */
  private final Optional<String> purpose;

  /** The comment */
  private final Optional<String> comment;

  /**
   * Creates a period
   *
   * @param id
   *          the id
   * @param start
   *          the start date
   * @param end
   *          the end date
   * @param purpose
   *          the purpose
   * @param comment
   *          the comment
   */
  public Period(Optional<Long> id, Date start, Date end, Optional<String> purpose, Optional<String> comment) {
    this.id = id;
    this.start = start;
    this.end = end;
    this.purpose = purpose;
    this.comment = comment;
  }

  /**
   * Returns the period id
   *
   * @return the id
   */
  public Optional<Long> getId() {
    return this.id;
  }

  /**
   * Returns the start date
   *
   * @return the start date
   */
  public Date getStart() {
    return start;
  }

  /**
   * Returns the end date
   *
   * @return the end date
   */
  public Date getEnd() {
    return end;
  }

  /**
   * Returns the purpose
   *
   * @return the purpose
   */
  public Optional<String> getPurpose() {
    return purpose;
  }

  /**
   * Returns the comment
   *
   * @return the comment
   */
  public Optional<String> getComment() {
    return comment;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof Period && eqFields((Period) that));
  }

  private boolean eqFields(Period that) {
    return eq(this.id, that.id) && eq(this.start, that.start) && eq(this.end, that.end)
            && eq(this.purpose, that.purpose) && eq(this.comment, that.comment);
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, start, end, purpose, comment);
  }

}
