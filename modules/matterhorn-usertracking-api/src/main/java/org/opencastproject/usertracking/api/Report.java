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
package org.opencastproject.usertracking.api;

import java.util.Calendar;

/**
 * A class that represents a report
 *
 */
public interface Report {

  /**
   * Sets the the date the data of the report starts from
   *
   * @param from
   */
  void setFrom(Calendar from);

  /**
   * Sets the date the data of the report is referring to
   *
   * @param to
   */
  void setTo(Calendar to);

  /**
   * Sets the sum of views of all report items
   *
   * @param views
   */
  void setViews(int views);

  /**
   * Sets the sum of the number of played seconds of all report items
   *
   * @param played
   */
  void setPlayed(long played);

  /**
   * Sets the total of report items
   *
   * @param total
   */
  void setTotal(int total);

  /**
   * Sets the maximum number of report items of the report (used for paging)
   *
   * @param limit
   */
  void setLimit(int limit);

  /**
   * Sets the number of the report item to start with in the list of all report items (used for paging)
   *
   * @param offset
   */
  void setOffset(int offset);

  /**
   * Add an report item to the report
   *
   * @param reportItem
   */
  void add(ReportItem reportItem);

  /**
   * Gets the date the data of the report starts from
   *
   * @return
   */
  Calendar getFrom();

  /**
   * Gets the date the data of the report is referring to
   *
   * @return
   */
  Calendar getTo();

  /**
   * Gets the sum of views of all report items
   *
   * @return
   */
  int getViews();

  /**
   * Gets the sum of the number of played seconds of all report items
   *
   * @return
   */
  long getPlayed();

  /**
   * Gets the total of report items
   *
   * @return
   */
  int getTotal();

  /**
   * Gets the maximum number of report items of the report (used for paging)
   *
   * @return
   */
  int getLimit();

  /**
   * Gets the number of the report item to start with in the list of all report items (used for paging)
   *
   * @return
   */
  int getOffset();

}
