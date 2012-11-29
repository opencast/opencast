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

import java.util.Date;



/**
 * A class that represents a summary of a user's activity.
 */
public interface UserSummary {
  void combine(UserSummary other);
  
  String getUserId();

  void setUserId(String userId);

  long getSessionCount();

  void setSessionCount(long sessionCount);

  long getUniqueMediapackages();

  void setUniqueMediapackages(long uniqueMediapackages);

  long getLength();

  void setLength(long length);

  Date getLast();

  void setLast(Date last);
}
