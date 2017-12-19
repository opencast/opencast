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


package org.opencastproject.workflow.api;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A single result of searching.
 */
@XmlJavaTypeAdapter(WorkflowSetImpl.Adapter.class)
public interface WorkflowSet {

  /**
   * The search item list
   *
   * @return Item list.
   */
  WorkflowInstance[] getItems();

  /**
   * Get the total number of items returned
   *
   * @return The number.
   */
  long size();

  /**
   * Get the start page.
   *
   * @return The start page.
   */
  long getStartPage();

  /**
   * Get the count limit.
   *
   * @return The count limit.
   */
  long getPageSize();

  /**
   * Get the search time.
   *
   * @return The time in ms.
   */
  long getSearchTime();

  /**
   * The total number of items without paging.
   *
   * @return The total number of items
   */
  long getTotalCount();

}
