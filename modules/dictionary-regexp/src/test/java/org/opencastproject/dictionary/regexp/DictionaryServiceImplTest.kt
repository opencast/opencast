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

package org.opencastproject.dictionary.regexp

import org.junit.Assert
import org.junit.Test

class DictionaryServiceImplTest {

    @Test
    @Throws(Exception::class)
    fun testSetPattern() {
        val service = DictionaryServiceImpl()
        val pattern = "123"
        service.pattern = pattern
        Assert.assertEquals(pattern, service.pattern)
    }

    @Test
    @Throws(Exception::class)
    fun testSetInvalidPattern() {
        val service = DictionaryServiceImpl()
        val pattern = service.pattern
        /* The service should fail to compile this */
        service.pattern = "*[[[["
        Assert.assertEquals(pattern, service.pattern)
    }

    @Test
    @Throws(Exception::class)
    fun testEmpty() {
        val service = DictionaryServiceImpl()
        Assert.assertEquals(null, service.cleanUpText(""))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanUp() {
        val service = DictionaryServiceImpl()
        val `in` = "This is a test sentence."
        val out = "This is a test sentence"
        Assert.assertEquals(out, service.cleanUpText(`in`).text)
    }

    @Test
    @Throws(Exception::class)
    fun testSpecialCharactersDE() {
        val service = DictionaryServiceImpl()
        val `in` = "Zwölf Boxkämpfer jagten Victor quer über den großen Sylter Deich."
        /* This will match German special characters and basic punctuation */
        service.pattern = "[\\wßäöüÄÖÜ,.!]+"
        Assert.assertEquals(`in`, service.cleanUpText(`in`).text)
    }

    @Test
    @Throws(Exception::class)
    fun testSpecialCharactersES() {
        val service = DictionaryServiceImpl()
        val `in` = "El veloz murciélago hindú comía feliz cardillo y kiwi. " + "La cigüeña tocaba el saxofón detrás del palenque de paja."
        /* This will match Spanish special characters and basic punctuation */
        service.pattern = "[¿¡(]*[\\wáéíóúÁÉÍÓÚüÜñÑ]+[)-.,:;!?]*"
        Assert.assertEquals(`in`, service.cleanUpText(`in`).text)
    }


}
