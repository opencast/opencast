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


package org.opencastproject.metadata.dublincore

import org.opencastproject.util.IoSupport

import org.junit.Ignore

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Some useful test utilities.
 */
@Ignore("Contains no tests, only helper functions")
object TestUtil {

    fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Date {
        val c = Calendar.getInstance()
        c.set(year, month - 1, day, hour, minute, second)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, tz: String): Date {
        val c = Calendar.getInstance(TimeZone.getTimeZone(tz))
        c.set(year, month - 1, day, hour, minute, second)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    fun midnight(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    fun precisionSecond(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    fun precisionDay(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    /** Read a catalog from the classpath.  */
    @Throws(Exception::class)
    fun read(dcFile: String): DublinCoreCatalog {
        return DublinCoreXmlFormat.read(IoSupport.classPathResourceAsFile(dcFile).get())
    }
}
