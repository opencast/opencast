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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * input stream to get only a part of a file
 *
 */
class ChunkedFileInputStream
/**
 * constructor
 *
 * @param file the file to load
 * @param offset the starting offset
 * @param endOffset the ending offset
 * @throws FileNotFoundException if the requested file was not found
 */
@Throws(FileNotFoundException::class)
constructor(file: File?,
        /**
         * starting offset
         */
        /***
         * get the starting offset
         *
         * @return the starting offset
         */
            /**
             * set the starting offset
             *
             * @param offset the starting offset
             */
            var offset: Long, endOffset: Long) : FileInputStream(file) {
    /**
     * the current offset
     */
    private var currentOffset: Long = 0
    /**
     * ending offset
     */
    /**
     * get the ending offset
     *
     * @return the ending offset
     */
    /**
     * set the ending offset
     *
     * @param endOffset the ending offset
     */
    var endOffset: Long = 0

    /**
     * constructor
     *
     * @param name the name of the file
     * @throws FileNotFoundException if the file was not found
     */
    @Throws(FileNotFoundException::class)
    constructor(name: String?) : this(if (name != null) File(name) else null, 0, 0) {
    }

    init {
        this.currentOffset = offset
        this.endOffset = if (endOffset == 0L) file!!.length() else endOffset
        if (offset != 0L) {
            logger.debug("skipping first {} bytes", offset)
            try {
                this.skip(offset)
            } catch (e: IOException) {
                logger.error(e.message, e)
            }

        }
    }

    /**
     * read the next byte
     *
     * @return the next byte or -1 if the expected offset has been reached
     */
    @Throws(IOException::class)
    override fun read(): Int {
        this.currentOffset++
        return if (currentOffset > endOffset) {
            -1
        } else super.read()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ChunkedFileInputStream::class.java!!)
    }

}
