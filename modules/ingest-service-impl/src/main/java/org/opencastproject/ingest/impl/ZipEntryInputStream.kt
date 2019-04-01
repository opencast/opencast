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

package org.opencastproject.ingest.impl

import com.google.common.base.Preconditions

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class ZipEntryInputStream
/**
 * Creates a wrapper around the input stream `in` and reads the given number of bytes from it.
 *
 * @param in
 * the input stream
 * @param length
 * the number of bytes to read
 */
(`in`: InputStream, length: Long) : FilterInputStream(`in`) {

    /** Length of the zip entry  */
    private var bytesToRead: Long = 0

    init {
        bytesToRead = length
        Preconditions.checkNotNull(`in`)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (bytesToRead == 0L)
            return -1
        bytesToRead--
        val byteContent = `in`.read()
        if (byteContent == -1)
            throw IOException("Zip entry is shorter than anticipated")
        return byteContent
    }

    @Throws(IOException::class)
    override fun close() {
        // This stream mustn't be closed because it handles the Zip entry parts of the given Zip input stream
        // make sure the given zip input stream is closed after reading
    }

}
