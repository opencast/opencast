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

package org.opencastproject.util.persistence;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.opencastproject.util.data.functions.Misc.chuck;

/**
 * Type safe access of a database table.
 * <h3>Usage</h3>
 * Extend this class and describe the columns as public final fields in their order of selection.
 * <pre>
 *   public class Person extends Table&lt;Person&gt; {
 *     // definition order is crucial and must match the order in which fields are selected
 *     public final Col&lt;String&gt; name = stringCol();
 *     public final Col&lt;Date&gt; age = date();
 *
 *     public Person(Object[] row) {
 *       super(row);
 *       // necessary call to init();
 *       init();
 *     }
 *   }
 *
 *   // usage
 *
 *   // (SELECT name, age FROM Person;)
 *   // ATTENTION! SELECT age, name FROM Person; does NOT work!
 *   final Object[] select = sqlSelect();
 *   final MyTable t = new MyTable(select);
 *   final String name = t.get(t.name);
 *   final Date age = t.get(t.age);
 * </pre>
 */
public abstract class Table<R extends Table<R>> {
  private final Map<Col<?>, Integer> cols = new HashMap<Col<?>, Integer>();
  private final Object[] row;

  protected abstract class Col<A> {
    public abstract A convert(Object v);
  }

  private class StringCol extends Col<String> {
    @Override public String convert(Object v) {
      return (String) v;
    }
  }

  /** Define a column of type String. */
  public Col<String> stringCol() {
    return new StringCol();
  }

  private class DateTimeCol extends Col<DateTime> {
    @Override public DateTime convert(Object v) {
      return new DateTime(((Date) v).getTime());
    }
  }

  /**
   * Define a column of type DateTime.
   * DateTime columns must be mappable to {@link Date} by JPA.
   */
  public Col<DateTime> dateTimeCol() {
    return new DateTimeCol();
  }

  private class DateCol extends Col<Date> {
    @Override public Date convert(Object v) {
      return (Date) v;
    }
  }

  /** Define a column of type Date. */
  public Col<Date> dateCol() {
    return new DateCol();
  }

  private class BooleanCol extends Col<Boolean> {
    @Override public Boolean convert(Object v) {
      return (Boolean) v;
    }
  }

  /** Define a column of type boolean. */
  public Col<Boolean> booleanCol() {
    return new BooleanCol();
  }

  private class LongCol extends Col<Long> {
    @Override public Long convert(Object v) {
      return (Long) v;
    }
  }

  /** Define a column of type long. */
  public Col<Long> longCol() {
    return new LongCol();
  }

  public Table(Object[] row) {
    this.row = row;
  }

  /**
   * Call this in the subclass's constructor!
   * <p/>
   * A call to init() can't happen in the abstract class's constructor
   * since field definitions haven't been initialized yet.
   */
  protected void init() {
    int index = 0;
    for (Field f : this.getClass().getFields()) {
      if (Col.class.isAssignableFrom(f.getType())) {
        try {
          cols.put((Col<?>) f.get(this), index);
        } catch (IllegalAccessException e) {
          chuck(e);
        }
        index++;
      }
    }
    if (index > row.length)
      throw new IllegalArgumentException("Row defines more fields than available in data set");
  }

  /** Access a column. */
  public <A> A get(Col<A> col) {
    return col.convert(row[cols.get(col)]);
  }
}
