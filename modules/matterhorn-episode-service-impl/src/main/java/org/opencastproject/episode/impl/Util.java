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
package org.opencastproject.episode.impl;

import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.util.data.Function;

import java.util.List;

import static org.opencastproject.util.data.Monadics.mlist;

/** Utility functions. */
public final class Util {
  private Util() {
  }

  /** Create a new result set from <code>r</code> possibly reduced by <code>f</code>. */
  public static SearchResult filterItems(final SearchResult r, Function<SearchResultItem, List<SearchResultItem>> f) {
    final List<SearchResultItem> filtered = mlist(r.getItems()).bind(f).value();
    // reduce total by number of filtered out items
    final long newTotal = r.getTotalSize() - (r.size() - filtered.size());
    return new SearchResult() {
      @Override public List<SearchResultItem> getItems() {
        return filtered;
      }

      @Override public String getQuery() {
        return r.getQuery();
      }

      @Override public long size() {
        return filtered.size();
      }

      @Override public long getTotalSize() {
        return newTotal;
      }

      @Override public long getOffset() {
        return r.getOffset();
      }

      @Override public long getLimit() {
        return r.getLimit();
      }

      @Override public long getSearchTime() {
        return r.getSearchTime();
      }

      @Override public long getPage() {
        return r.getPage();
      }
    };
  }
}
