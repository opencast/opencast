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

package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * Business object for a blacklist.
 */
public class Blacklist {

  /** The blacklist id */
  private Long id;

  /** The type */
  private String type;

  /** The blacklisted object */
  private Blacklistable blacklisted;

  /** The list of periods */
  private List<Period> periods = new ArrayList<Period>();

  /**
   * Creates a building
   *
   * @param id
   *          the id
   * @param blacklisted
   *          the blacklisted object
   */
  public Blacklist(Long id, Blacklistable blacklisted) {
    this.id = id;
    this.blacklisted = notNull(blacklisted, "blacklisted");
    this.type = blacklisted.getType();
  }

  /**
   * Creates a building
   *
   * @param blacklisted
   *          the blacklisted object
   * @param periods
   *          the period list
   */
  public Blacklist(Blacklistable blacklisted, List<Period> periods) {
    this.blacklisted = notNull(blacklisted, "blacklisted");
    this.periods = notNull(periods, "periods list");
    this.type = blacklisted.getType();
  }

  /**
   * Creates a building
   *
   * @param id
   *          the id
   * @param blacklisted
   *          the blacklisted object
   * @param periods
   *          the period list
   */
  public Blacklist(Long id, Blacklistable blacklisted, List<Period> periods) {
    this(blacklisted, periods);
    this.id = id;
  }

  /**
   * Sets the id
   *
   * @param id
   *          the Blacklist id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the Blacklist id
   *
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the blacklisted object
   *
   * @param blacklisted
   *          the blacklisted object
   */
  public void setBlacklisted(Blacklistable blacklisted) {
    this.blacklisted = notNull(blacklisted, "blacklisted");
    this.type = blacklisted.getType();
  }

  /**
   * Returns the blacklisted object
   *
   * @return the blacklisted object
   */
  public Blacklistable getBlacklisted() {
    return blacklisted;
  }

  /**
   * Returns the type
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the periods list
   *
   * @param periods
   *          the periods list
   */
  public void setPeriods(List<Period> periods) {
    this.periods = notNull(periods, "periods");
  }

  /**
   * Returns the list of periods
   *
   * @return the periods list
   */
  public List<Period> getPeriods() {
    return periods;
  }

  /**
   * Add a time period to the blacklist
   *
   * @param period
   *          the time period to add to the blacklist
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean addPeriod(Period period) {
    return periods.add(notNull(period, "period"));
  }

  /**
   * Remove the given time period from the blacklist
   *
   * @param period
   *          the time period to remove
   *
   * @return true if this collection changed as a result of the call
   */
  public boolean removePeriod(Period period) {
    return periods.remove(notNull(period, "period"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Blacklist blacklist = (Blacklist) o;
    return blacklisted.equals(blacklist.getBlacklisted()) && periods.equals(blacklist.getPeriods());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, blacklisted, periods);
  }

  @Override
  public String toString() {
    return "Blacklist:" + id + "|" + blacklisted.getName() + "|" + blacklisted.getType();
  }

  public List<Obj> toJson(Option<Long> periodId) {
    List<Jsons.Obj> objArr = new ArrayList<Jsons.Obj>();

    Jsons.Obj b;
    if (Person.TYPE.equals(type)) {
      b = ((Person) blacklisted).toJson();
    } else if (Room.TYPE.equals(type)) {
      b = ((Room) blacklisted).toJson();
    } else {
      b = Jsons.ZERO_OBJ;
    }

    for (Period p : periods) {
      if (periodId.isSome() && !periodId.equals(p.getId()))
        continue;
      String id = "";
      if (p.getId().isSome()) {
        id = Long.toString(p.getId().get());
      }

      String purpose = p.getPurpose().getOrElse("");
      String comment = p.getComment().getOrElse("");

      objArr.add(Jsons.obj(Jsons.p("id", id),
              Jsons.p("resourceType", type),
              Jsons.p("resource", b),
              Jsons.p("start", DateTimeSupport.toUTC(p.getStart().getTime())),
              Jsons.p("end", DateTimeSupport.toUTC(p.getEnd().getTime())),
              Jsons.p("purpose", purpose),
              Jsons.p("comment", comment)));
    }
    return objArr;
  }
}
