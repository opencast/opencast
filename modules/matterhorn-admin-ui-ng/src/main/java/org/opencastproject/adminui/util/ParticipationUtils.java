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

package org.opencastproject.adminui.util;

import org.opencastproject.adminui.api.SortType;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils method for the Participation implementation
 */
public final class ParticipationUtils {

  private static final Logger logger = LoggerFactory.getLogger(ParticipationUtils.class);

  private ParticipationUtils() {
  }

  /**
   * @param input
   *          The input text from the endpoint.
   * @return The enum that matches the input string or null if none can be found.
   */
  public static Option<SortType> getMessagesSortField(String input) {
    if (StringUtils.isNotBlank(input)) {
      String upperCase = input.toUpperCase();
      SortType sortType = null;
      try {
        sortType = SortType.valueOf(upperCase);
      } catch (IllegalArgumentException e) {
        return Option.<SortType> none();
      }
      return Option.option(sortType);
    }
    return Option.<SortType> none();
  }

}
