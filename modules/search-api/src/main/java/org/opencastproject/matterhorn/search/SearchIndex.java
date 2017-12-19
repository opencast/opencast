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


package org.opencastproject.matterhorn.search;

import java.io.IOException;

public interface SearchIndex {

  /** Version of this index */
  int INDEX_VERSION = 1000;

  /**
   * Clears the search index.
   * 
   * @throws IOException
   *           if clearing the index fails
   */
  void clear() throws IOException;

  /**
   * Returns the index's version number. If that number is different from {@link #INDEX_VERSION}, a reindex is needed,
   * since the index's structure could have changed significantly.
   * 
   * @return the index version
   */
  int getIndexVersion();

}
