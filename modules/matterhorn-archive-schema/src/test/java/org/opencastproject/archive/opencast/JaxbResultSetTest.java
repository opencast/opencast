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
package org.opencastproject.archive.opencast;

import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.ReflectionUtil.run;
import static org.opencastproject.util.data.Collections.nil;

public class JaxbResultSetTest {
  /** Ensure all fields of an OpencastResultSet are copied to the JAXB DTO. */
  @Test
  public void testCreate() throws Exception {
    final OpencastResultSet source = randomResultSet();
    final JaxbResultSet target = JaxbResultSet.create(source);
    run(OpencastResultSet.class, new OpencastResultSet() {
      @Override public List<OpencastResultItem> getItems() {
        assertEquals("items copy", source.getItems().size(), target.resultSet.size());
        // need to return the items here since the abstract super class relies on it
        return source.getItems();
      }

      @Override public String getQuery() {
        assertEquals("query copy", source.getQuery(), target.query);
        return null;
      }

      @Override public long getTotalSize() {
        assertEquals("total size copy", source.getTotalSize(), target.total);
        return 0;
      }

      @Override public long getLimit() {
        assertEquals("limit copy", source.getLimit(), target.limit);
        return 0;
      }

      @Override public long getOffset() {
        assertEquals("offset copy", source.getOffset(), target.offset);
        return 0;
      }

      @Override public long getSearchTime() {
        assertEquals("search time copy", source.getSearchTime(), target.searchTime);
        return 0;
      }
    });
  }

  public static OpencastResultSet randomResultSet() {
    final List<OpencastResultItem> items = nil();
    final String query = UUID.randomUUID().toString();
    final long totalSize = new Random().nextLong();
    final long offset = new Random().nextLong();
    final long limit = new Random().nextLong();
    final long searchTime = new Random().nextLong();
    return new OpencastResultSet() {
      @Override public List<OpencastResultItem> getItems() {
        return items;
      }

      @Override public String getQuery() {
        return query;
      }

      @Override public long getTotalSize() {
        return totalSize;
      }

      @Override public long getLimit() {
        return limit;
      }

      @Override public long getOffset() {
        return offset;
      }

      @Override public long getSearchTime() {
        return searchTime;
      }
    };
  }
}
