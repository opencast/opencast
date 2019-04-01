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

package org.opencastproject.util

import java.lang.String.format

import org.opencastproject.util.data.Prelude

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.spi.LocationAwareLogger
import java.util.Collections
import java.util.Stack
import java.util.UUID

/**
 * A logger that maintains a "unit of work" context to facilitate the grouping of log statements.
 *
 *
 * Log methods that take message formats and arguments use the
 * [String.format] string format syntax.
 */
class Log
/** Create a new logger from an slf4j logger.  */
(private val logger: Logger) {
    private val isLocationAware: Boolean

    /** Return the current log context.  */
    val context: List<String>
        get() = Collections.unmodifiableList(ctx.get())

    init {
        this.isLocationAware = logger is LocationAwareLogger
    }

    /** Start a new unit of work.  */
    fun startUnitOfWork() {
        ctx.get().push(randomString())
        updateCurrent()
    }

    /** End a unit of work.  */
    fun endUnitOfWork() {
        if (ctx.get().size > 1) {
            ctx.get().pop()
            updateCurrent()
        }
    }

    /** Continue a log context.  */
    fun continueContext(init: Collection<String>) {
        val stack = Stack<String>()
        stack.addAll(init)
        ctx.set(stack)
        updateCurrent()
    }

    fun debug(msg: String) {
        log(LocationAwareLogger.DEBUG_INT, null, msg)
    }

    fun debug(msg: String, vararg args: Any) {
        log(LocationAwareLogger.DEBUG_INT, null, msg, *args)
    }

    fun info(msg: String) {
        log(LocationAwareLogger.INFO_INT, null, msg)
    }

    fun info(msg: String, vararg args: Any) {
        log(LocationAwareLogger.INFO_INT, null, msg, *args)
    }

    fun warn(msg: String) {
        log(LocationAwareLogger.WARN_INT, null, msg)
    }

    fun warn(msg: String, vararg args: Any) {
        log(LocationAwareLogger.WARN_INT, null, msg, *args)
    }

    fun warn(t: Throwable, msg: String) {
        log(LocationAwareLogger.WARN_INT, t, msg)
    }

    fun error(msg: String) {
        log(LocationAwareLogger.ERROR_INT, null, msg)
    }

    fun error(msg: String, vararg args: Any) {
        log(LocationAwareLogger.ERROR_INT, null, msg, *args)
    }

    fun error(t: Throwable, msg: String, vararg args: Any) {
        log(LocationAwareLogger.ERROR_INT, t, msg, *args)
    }

    /** `t` maybe null  */
    private fun log(level: Int, t: Throwable?, format: String, vararg args: Any) {
        val msg = current.get() + format(convertCurlyBraces(format), *args)
        if (isLocationAware) {
            (logger as LocationAwareLogger).log(null, FQCN, level, msg, null, t)
        } else {
            when (level) {
                LocationAwareLogger.INFO_INT -> logger.info(msg)
                LocationAwareLogger.WARN_INT -> logger.warn(msg)
                LocationAwareLogger.ERROR_INT -> logger.error(msg)
                else -> Prelude.unexhaustiveMatch<Any>()
            }
        }
    }

    companion object {
        /** The number of seconds in a minute.  */
        private val SECONDS_IN_MINUTES = 60

        /** The number of seconds in an hour.  */
        private val SECONDS_IN_HOURS = 3600

        private val FQCN = Log::class.java!!.getName()

        private val JVM_SESSION = randomString()

        /** Hold the context stack.  */
        private val ctx = object : ThreadLocal<Stack<String>>() {
            override fun initialValue(): Stack<String> {
                val stack = Stack<String>()
                stack.add(JVM_SESSION)
                return stack
            }
        }

        /** Hold the current unit of work hierarchy as a string ready to use for the log methods.  */
        private val current = object : ThreadLocal<String>() {
            override fun initialValue(): String {
                return ctxAsString()
            }
        }

        /**
         * Create a new log instance based on an slf4j logger for class `clazz`.
         *
         * @see org.slf4j.LoggerFactory.getLogger
         */
        fun mk(clazz: Class<*>): Log {
            return Log(LoggerFactory.getLogger(clazz))
        }

        private fun updateCurrent() {
            current.set(ctxAsString())
        }

        private fun ctxAsString(): String {
            return "[>" + ctx.get().joinToString("_") + "] "
        }

        private fun randomString(): String {
            return UUID.randomUUID().toString().split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        }

        /**
         * Renders a string representation of seconds that is easier to read by showing hours, minutes and seconds.
         *
         * @param seconds
         * The number of seconds that you want to represent in hours, minutes and remainder seconds.
         * @return A human readable string representation of seconds into hours, minutes and seconds.
         */
        fun getHumanReadableTimeString(seconds: Long): String {
            var result = ""
            val hours = seconds / SECONDS_IN_HOURS
            if (hours == 1L) {
                result += "$hours hour "
            } else if (hours > 1) {
                result += "$hours hours "
            }
            val minutes = seconds % SECONDS_IN_HOURS / SECONDS_IN_MINUTES
            if (minutes == 1L) {
                result += "$minutes minute "
            } else if (minutes > 1) {
                result += "$minutes minutes "
            }
            val remainderSeconds = seconds % SECONDS_IN_HOURS % SECONDS_IN_MINUTES
            if (remainderSeconds == 1L) {
                result += "$remainderSeconds second"
            } else if (remainderSeconds > 1) {
                result += "$remainderSeconds seconds"
            }
            result = StringUtils.trim(result)
            return result
        }

        private fun convertCurlyBraces(format: String?): String {
            return if (format == null) "(null message)" else format.replace("\\{\\}".toRegex(), "%s")

        }
    }
}
