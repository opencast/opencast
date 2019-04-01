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

package org.opencastproject.staticfiles.jmx

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

import org.joda.time.DateTime

import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.TimeUnit

class UploadStatistics : UploadStatisticsMXBean {

    /**
     * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean.getTotalBytes
     */
    override var totalBytes = 0L
        private set
    /**
     * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean.getSuccessfulUploadOperations
     */
    override var successfulUploadOperations = 0
        private set
    /**
     * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean.getFailedUploadOperations
     */
    override var failedUploadOperations = 0
        private set
    private val bytesCounter = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build<Long, Long>()

    /**
     * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean.getBytesInLastMinute
     */
    override val bytesInLastMinute: Long
        get() {
            val key = getKeyByTime(DateTime().minusMinutes(1).millis)
            return if (key != 0L) totalBytes - bytesCounter.getIfPresent(key)!! else 0
        }

    /**
     * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean.getBytesInLastFiveMinutes
     */
    override val bytesInLastFiveMinutes: Long
        get() {
            val key = getKeyByTime(DateTime().minusMinutes(5).millis)
            return if (key != 0L) totalBytes - bytesCounter.getIfPresent(key)!! else 0
        }

    /**
     * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean.getBytesInLastFifteenMinutes
     */
    override val bytesInLastFifteenMinutes: Long
        get() {
            val key = getKeyByTime(DateTime().minusMinutes(15).millis)
            return if (key != 0L) totalBytes - bytesCounter.getIfPresent(key)!! else 0
        }

    private fun getKeyByTime(timeBeforeFiveMinute: Long): Long {
        var key = 0L
        val bytes = ArrayList(bytesCounter.asMap().keys)
        Collections.sort(bytes)
        for (milis in bytes) {
            if (milis > timeBeforeFiveMinute) {
                key = milis!!
                break
            }
        }
        return key
    }

    fun add(bytes: Long) {
        if (totalBytes == 0L)
            bytesCounter.put(System.currentTimeMillis(), 0L)
        totalBytes += bytes
        bytesCounter.put(System.currentTimeMillis(), totalBytes)
    }

    fun successful() {
        successfulUploadOperations++
    }

    fun failed() {
        failedUploadOperations++
    }

}
