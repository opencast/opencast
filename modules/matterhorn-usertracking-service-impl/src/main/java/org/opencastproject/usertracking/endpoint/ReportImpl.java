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
package org.opencastproject.usertracking.endpoint;

import org.opencastproject.usertracking.api.Report;
import org.opencastproject.usertracking.api.ReportItem;

import java.util.ArrayList;
import java.util.Calendar;
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
@XmlType(name = "report", namespace = "http://usertracking.opencastproject.org")
@XmlRootElement(name = "report", namespace = "http://usertracking.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReportImpl implements Report {

  @XmlAttribute(name = "from")
  protected Calendar from;
  
  @XmlAttribute(name = "to")
  protected Calendar to;
  
  @XmlAttribute(name = "views")
  protected int views;
  
  @XmlAttribute(name = "played")
  protected long played;
  
  @XmlAttribute(name = "total")
  protected int total;

  @XmlAttribute(name = "offset")
  protected int offset;

  @XmlAttribute(name = "limit")
  protected int limit;

  @XmlElement(name = "report-item", namespace = "http://usertracking.opencastproject.org")
  protected List<ReportItemImpl> reportItems;

  public void add(ReportItem reportItem) {
    reportItems.add((ReportItemImpl)reportItem);
  }

  /**
   * A no-arg constructor needed by JAXB
   */
  public ReportImpl() {
    this.reportItems = new ArrayList<ReportItemImpl>();
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public Calendar getFrom() {
    return from;
  }

  public void setFrom(Calendar from) {
    this.from = from;
  }

  public Calendar getTo() {
    return to;
  }

  public void setTo(Calendar to) {
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

  public int getOffset() {
    return offset;
  }

  public int getLimit() {
    return limit;
  }
}
