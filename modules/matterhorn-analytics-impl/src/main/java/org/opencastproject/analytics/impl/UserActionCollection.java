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
package org.opencastproject.analytics.impl;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A {@link Report}
 */
@XmlType(name = "user-action-collection", namespace = "http://analytics.opencastproject.org")
@XmlRootElement(name = "user-action-collection", namespace = "http://analytics.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserActionCollection {

  @XmlAttribute(name = "from")
  protected Date from;

  @XmlAttribute(name = "to")
  protected Date to;

  @XmlAttribute(name = "interval")
  protected long interval;

  @XmlAttribute(name = "views")
  protected int views;

  @XmlAttribute(name = "played")
  protected long played;

  @XmlAttribute(name = "total")
  protected int total;

  @XmlAttribute(name = "limit")
  protected long limit;

  @XmlElement(name = "view-item", namespace = "http://analytics.opencastproject.org")
  protected List<ViewItem> viewDurationItems;

  public void add(ViewItem reportItem) {
    viewDurationItems.add((ViewItem) reportItem);
  }

  /**
   * A no-arg constructor needed by JAXB
   */
  public UserActionCollection() {
    this.viewDurationItems = new ArrayList<ViewItem>();
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public void setLimit(long limit) {
    this.limit = limit;
  }

  public Date getFrom() {
    return from;
  }

  public void setFrom(Date from) {
    this.from = from;
  }

  public Date getTo() {
    return to;
  }

  public void setTo(Date to) {
    this.to = to;
  }

  public int getViews() {
    return views;
  }

  public void setViews(int views) {
    this.views = views;
  }

  public long getPlayed() {
    return played;
  }

  public void setPlayed(long played) {
    this.played = played;
  }

  public int getTotal() {
    return total;
  }

  public long getLimit() {
    return limit;
  }

  public long getInterval() {
    return interval;
  }

  public void setInterval(long interval) {
    this.interval = interval;
  }
}
