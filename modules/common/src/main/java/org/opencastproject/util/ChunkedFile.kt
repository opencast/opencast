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

import java.io.File
import java.io.IOException

class ChunkedFile
/**
 * Creates a new instance that fetches data from the specified file.
 *
 * @param offset the offset of the file where the transfer begins
 * @param contentLength the number of bytes to transfer
 */
@Throws(IOException::class)
@JvmOverloads constructor(
        /**
         * @return the file
         */
        val file: File?,
        /**
         * @return the offset in the file where the transfer began.
         */
        val offset: Long = 0,
        /**
         * @return the size of the content
         */
        val contentLength: Long = file.length()) {

    init {
        if (file == null) {
            throw NullPointerException("file")
        }
        if (offset < 0) {
            throw IllegalArgumentException("offset: $offset (expected: 0 or greater)")
        }
        if (contentLength < 0) {
            throw IllegalArgumentException("length: $contentLength (expected: 0 or greater)")
        }

    }
}
/**
 *
 * @param file the file containing the data
 * @throws IOException if an error occured
 */
