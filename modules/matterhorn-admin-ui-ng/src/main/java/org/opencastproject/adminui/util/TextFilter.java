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

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to simplify the implementation of the filter 'text' in case no search index is available.
 * This is a no trick implementation, please feel free to optimize. Normally, the final solution would be,
 * however, to use the filter facilities of a search index
 */
public final class TextFilter {

  private TextFilter() {

  }

  /**
   * Check whether at least one of the search strings is contained in one of the text strings
   * Notes:
   *   - caseinsensitive
   *   - using union in case of multiple search strings
   *
   * @param searchStrings
   *          whitespace-separated list of search strings
   * @param text
   *          open array of strings to be matched against
   * @return
   *        true, if at least one of the search strings occurs in at least one of the text strings
   *        false, otherwise
   */
  public static boolean match(String searchStrings, String... text) {

    for (String searchString : StringUtils.split(searchStrings)) {
      for (String word : text) {
        if (StringUtils.indexOfIgnoreCase(word, searchString) >= 0) {
          return true;
        }
      }
    }
    return false;
  }
}
