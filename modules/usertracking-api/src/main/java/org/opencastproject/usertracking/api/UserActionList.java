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

package org.opencastproject.usertracking.api;

import java.util.Collection;
import java.util.List;

/**
 * A List of {@link UserAction}s
 *
 */
public interface UserActionList {
  /** Set the total number of results for this particular query. **/
  void setTotal(int total);
  /** Set the upper limit of the total number of results to return upon request. **/
  void setLimit(int limit);
  /** Set the offset * limit to skip over before returning the results. **/
  void setOffset(int offset);
  /** Add a single UserAction to this collection. **/
  void add(UserAction annotation);
  /** Add a complete Collection of UserActions to this collection. **/
  void add(Collection<UserAction> userActions);
  /** Return the total number of possible results for this query. **/
  int getTotal();
  /** Return the maximum number of results to collect. **/
  int getLimit();
  /** Return the offset of result lists to skip over to get these results. **/
  int getOffset();
  /** Return a list of the UserActions in this Collection. **/
  List<UserAction> getUserActions();
}
