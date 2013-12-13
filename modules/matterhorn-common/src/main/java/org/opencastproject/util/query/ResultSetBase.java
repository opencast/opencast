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
package org.opencastproject.util.query;

import java.util.List;

/**
 * A general purpose result set.
 *
 * @param <A> the type of result items
 */
public abstract class ResultSetBase<A> {
  /** Return the retrieved slice. */
  public abstract <X extends A> List<X> getItems();

  /** Return the size of retrieved {@link #getItems() slice}. */
  public long size() {
    return getItems().size();
  }

  /** Return a string representation of the query. */
  public abstract String getQuery();

  /** Return the number of items the query could potentially yield. */
  public abstract long getTotalSize();

  /** Return the set size limit. */
  public abstract long getLimit();

  /** Return the offset within the total result set. */
  public abstract long getOffset();

  /** Return the page number of this slice. */
  public long getPage() {
    return getLimit() > 0 ? getOffset() / getLimit() : 0;
  }

  public abstract long getSearchTime();
}
