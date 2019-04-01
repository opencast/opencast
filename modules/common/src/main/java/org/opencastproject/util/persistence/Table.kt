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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util.persistence

import org.opencastproject.util.data.functions.Misc.chuck

import org.joda.time.DateTime

import java.lang.reflect.Field
import java.util.Date
import java.util.HashMap

/**
 * Type safe access of a database table.
 * <h3>Usage</h3>
 * Extend this class and describe the columns as public final fields in their order of selection.
 * <pre>
 * public class Person extends Table&lt;Person&gt; {
 * // definition order is crucial and must match the order in which fields are selected
 * public final Col&lt;String&gt; name = stringCol();
 * public final Col&lt;Date&gt; age = date();
 *
 * public Person(Object[] row) {
 * super(row);
 * // necessary call to init();
 * init();
 * }
 * }
 *
 * // usage
 *
 * // (SELECT name, age FROM Person;)
 * // ATTENTION! SELECT age, name FROM Person; does NOT work!
 * final Object[] select = sqlSelect();
 * final MyTable t = new MyTable(select);
 * final String name = t.get(t.name);
 * final Date age = t.get(t.age);
</pre> *
 */
abstract class Table<R : Table<R>>(private val row: Array<Any>) {
    private val cols = HashMap<Col<*>, Int>()

    protected abstract inner class Col<A> {
        abstract fun convert(v: Any): A
    }

    private inner class StringCol : Col<String>() {
        override fun convert(v: Any): String {
            return v as String
        }
    }

    /** Define a column of type String.  */
    fun stringCol(): Col<String> {
        return StringCol()
    }

    private inner class DateTimeCol : Col<DateTime>() {
        override fun convert(v: Any): DateTime {
            return DateTime((v as Date).time)
        }
    }

    /**
     * Define a column of type DateTime.
     * DateTime columns must be mappable to [Date] by JPA.
     */
    fun dateTimeCol(): Col<DateTime> {
        return DateTimeCol()
    }

    private inner class DateCol : Col<Date>() {
        override fun convert(v: Any): Date {
            return v as Date
        }
    }

    /** Define a column of type Date.  */
    fun dateCol(): Col<Date> {
        return DateCol()
    }

    private inner class BooleanCol : Col<Boolean>() {
        override fun convert(v: Any): Boolean? {
            return v as Boolean
        }
    }

    /** Define a column of type boolean.  */
    fun booleanCol(): Col<Boolean> {
        return BooleanCol()
    }

    private inner class LongCol : Col<Long>() {
        override fun convert(v: Any): Long? {
            return v as Long
        }
    }

    /** Define a column of type long.  */
    fun longCol(): Col<Long> {
        return LongCol()
    }

    /**
     * Call this in the subclass's constructor!
     *
     *
     * A call to init() can't happen in the abstract class's constructor
     * since field definitions haven't been initialized yet.
     */
    protected fun init() {
        var index = 0
        for (f in this.javaClass.getFields()) {
            if (Col<*>::class.java!!.isAssignableFrom(f.getType())) {
                try {
                    cols[f.get(this)] = index
                } catch (e: IllegalAccessException) {
                    chuck<Any>(e)
                }

                index++
            }
        }
        if (index > row.size)
            throw IllegalArgumentException("Row defines more fields than available in data set")
    }

    /** Access a column.  */
    operator fun <A> get(col: Col<A>): A {
        return col.convert(row[cols[col]])
    }
}
