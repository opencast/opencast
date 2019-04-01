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

package org.opencastproject.dictionary.hunspell

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class DictionaryServiceImplTest {

    @Test
    @Throws(Exception::class)
    fun testSetBinary() {
        val service = DictionaryServiceImpl()
        val binary = "123"
        service.binary = binary
        Assert.assertEquals(binary, service.binary)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCommand() {
        val service = DictionaryServiceImpl()
        val command = "123"
        service.command = command
        Assert.assertEquals(command, service.command)
    }

    @Test
    @Throws(Exception::class)
    fun testEmpty() {
        if (hunspellEngDictAvailable) {
            val service = DictionaryServiceImpl()
            service.command = "-d en_US -G"
            Assert.assertEquals(null, service.cleanUpText(""))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCleanUp() {
        if (hunspellEngDictAvailable) {
            val service = DictionaryServiceImpl()
            service.command = "-d en_US -G"
            val `in` = "This is a test sentence."
            val out = "This is a test sentence"
            Assert.assertEquals(out, service.cleanUpText(`in`).text)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSpecialCharacters() {
        if (hunspellDeuDictAvailable) {
            val service = DictionaryServiceImpl()
            service.command = "-i utf-8 -d de_DE -G"
            val `in` = "Ich hab' hier bloß ein Amt und keine Meinung."
            val out = "Ich hab hier bloß ein Amt und keine Meinung."
            Assert.assertEquals(out, service.cleanUpText(`in`).text)
        }
    }

    companion object {

        private var hunspellInstalled = true
        private var hunspellEngDictAvailable = true
        private var hunspellDeuDictAvailable = true

        @BeforeClass
        fun testHunspell() {
            val service = DictionaryServiceImpl()

            /* Check if hunspell is available */
            service.command = "-v"
            try {
                service.runHunspell("")
            } catch (t: Throwable) {
                /* Seems like no hunspell is available */
                hunspellInstalled = false
            }

            /* Check if the English dictionary is available */
            service.command = "-d en_US"
            try {
                service.runHunspell("")
            } catch (t: Throwable) {
                /* Seems like no hunspell is available */
                hunspellEngDictAvailable = false
            }

            /* Check if the German dictionary is available */
            service.command = "-d de_DE"
            try {
                service.runHunspell("")
            } catch (t: Throwable) {
                /* Seems like no hunspell is available */
                hunspellDeuDictAvailable = false
            }

        }
    }


}
