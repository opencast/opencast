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
package org.opencastproject.scheduler.api

import org.opencastproject.util.EqualsUtil.eq
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.option
import org.opencastproject.util.data.Option.some

import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.EqualsUtil
import org.opencastproject.util.Jsons
import org.opencastproject.util.Jsons.Obj
import org.opencastproject.util.Jsons.Prop
import org.opencastproject.util.data.Option

import java.util.ArrayList
import java.util.Date

import javax.annotation.concurrent.ThreadSafe

/** Business object for a period.  */
@ThreadSafe
class Period
/**
 * Creates a period
 *
 * @param id
 * the id
 * @param start
 * the start date
 * @param end
 * the end date
 * @param purpose
 * the purpose
 * @param comment
 * the comment
 */
(
        /** The period identifier  */
        /**
         * Returns the period id
         *
         * @return the id
         */
        val id: Option<Long>,
        /** The start date  */
        /**
         * Returns the start date
         *
         * @return the start date
         */
        val start: Date,
        /** The end date  */
        /**
         * Returns the end date
         *
         * @return the end date
         */
        val end: Date,
        /** The purpose  */
        /**
         * Returns the purpose
         *
         * @return the purpose
         */
        val purpose: Option<String>,
        /** The comment  */
        /**
         * Returns the comment
         *
         * @return the comment
         */
        val comment: Option<String>) {

    override fun equals(that: Any?): Boolean {
        return this === that || that is Period && eqFields((that as Period?)!!)
    }

    private fun eqFields(that: Period): Boolean {
        return (eq(this.id, that.id) && eq(this.start, that.start) && eq(this.end, that.end)
                && eq(this.purpose, that.purpose) && eq(this.comment, that.comment))
    }

    override fun hashCode(): Int {
        return EqualsUtil.hash(id, start, end, purpose, comment)
    }

    fun toJson(): Obj {
        val props = ArrayList<Prop>()
        for (identifier in id)
            props.add(Jsons.p("id", identifier))
        props.add(Jsons.p("start", DateTimeSupport.toUTC(start.time)))
        props.add(Jsons.p("end", DateTimeSupport.toUTC(end.time)))
        for (p in purpose)
            props.add(Jsons.p("purpose", p))
        for (c in comment)
            props.add(Jsons.p("comment", c))

        return Jsons.obj(*props.toTypedArray())
    }

    companion object {

        fun period(id: Long, start: Date, end: Date, purpose: String, comment: String): Period {
            return Period(some(id), start, end, option(purpose), option(comment))
        }

        fun period(id: Long, start: Date, end: Date): Period {
            return Period(some(id), start, end, Option.none(), Option.none())
        }

        fun period(start: Date, end: Date): Period {
            return Period(none(Long::class.java), start, end, Option.none(), Option.none())
        }

        fun period(start: Date, end: Date, purpose: String, comment: String): Period {
            return Period(none(Long::class.java), start, end, option(purpose), option(comment))
        }
    }

}
