/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.pm.api;

import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.Jsons.Prop;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

/** Business object for a period. */
@ThreadSafe
public final class Period {
  /** The period identifier */
  private final Option<Long> id;

  /** The start date */
  private final Date start;

  /** The end date */
  private final Date end;

  /** The purpose */
  private final Option<String> purpose;

  /** The comment */
  private final Option<String> comment;

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
  public Period(Option<Long> id, Date start, Date end, Option<String> purpose, Option<String> comment) {
    this.id = id;
    this.start = start;
    this.end = end;
    this.purpose = purpose;
    this.comment = comment;
  }

  public static Period period(long id, Date start, Date end, String purpose, String comment) {
    return new Period(some(id), start, end, option(purpose), option(comment));
  }

  public static Period period(long id, Date start, Date end) {
    return new Period(some(id), start, end, Option.<String> none(), Option.<String> none());
  }

  public static Period period(Date start, Date end) {
    return new Period(none(Long.class), start, end, Option.<String> none(), Option.<String> none());
  }

  public static Period period(Date start, Date end, String purpose, String comment) {
    return new Period(none(Long.class), start, end, option(purpose), option(comment));
  }

  /**
   * Returns the period id
   * 
   * @return the id
   */
  public Option<Long> getId() {
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
  public Option<String> getPurpose() {
    return purpose;
  }

  /**
   * Returns the comment
   * 
   * @return the comment
   */
  public Option<String> getComment() {
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

  public Obj toJson() {
    List<Prop> props = new ArrayList<Prop>();
    for (Long identifier : id)
      props.add(Jsons.p("id", identifier));
    props.add(Jsons.p("start", DateTimeSupport.toUTC(start.getTime())));
    props.add(Jsons.p("end", DateTimeSupport.toUTC(end.getTime())));
    for (String p : purpose)
      props.add(Jsons.p("purpose", p));
    for (String c : comment)
      props.add(Jsons.p("comment", c));

    return Jsons.obj(props.toArray(new Prop[props.size()]));
  }

}
