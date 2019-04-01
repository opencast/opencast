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

import org.apache.commons.io.input.ProxyInputStream

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.IOException
import java.io.InputStream

/**
 * An [InputStream] that counts the number of bytes read.
 */
class ProgressInputStream(`in`: InputStream) : ProxyInputStream(`in`) {

    private val propertyChangeSupport = PropertyChangeSupport(this)

    @Volatile
    var totalNumBytesRead: Long = 0
        private set

    /**
     * Adds a [PropertyChangeListener]
     *
     * The listener gets notified as soon as the input stream is read.
     *
     * @param l
     * the [PropertyChangeListener]
     */
    fun addPropertyChangeListener(l: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(l)
    }

    /**
     * Removes a [PropertyChangeListener]
     *
     * The listener gets notified as soon as the input stream is read.
     *
     * @param l
     * the [PropertyChangeListener]
     */
    fun removePropertyChangeListener(l: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(l)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return updateProgress(super.read().toLong()).toInt()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return updateProgress(super.read(b).toLong()).toInt()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return updateProgress(super.read(b, off, len).toLong()).toInt()
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return updateProgress(super.skip(n))
    }

    override fun mark(readlimit: Int) {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun reset() {
        throw UnsupportedOperationException()
    }

    override fun markSupported(): Boolean {
        return false
    }

    private fun updateProgress(numBytesRead: Long): Long {
        if (numBytesRead > 0) {
            val oldTotalNumBytesRead = totalNumBytesRead
            totalNumBytesRead += numBytesRead
            propertyChangeSupport.firePropertyChange("totalNumBytesRead", oldTotalNumBytesRead, totalNumBytesRead)
        }
        return numBytesRead
    }
}
