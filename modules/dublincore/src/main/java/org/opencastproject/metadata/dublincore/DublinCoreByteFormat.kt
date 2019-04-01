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

import com.entwinemedia.fn.Prelude.chuck

import com.entwinemedia.fn.Fn

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Byte serialization of Dublin Core catalogs.
 */
object DublinCoreByteFormat {

    /**
     * [.read] as a function.
     */
    val readFromArray: Fn<ByteArray, DublinCoreCatalog> = object : Fn<ByteArray, DublinCoreCatalog>() {
        override fun apply(bytes: ByteArray): DublinCoreCatalog {
            return DublinCoreByteFormat.read(bytes)
        }
    }

    fun read(bytes: ByteArray): DublinCoreCatalog {
        return DublinCores.read(ByteArrayInputStream(bytes))
    }

    /** Serialize a DublinCore catalog to a UTF-8 encoded byte array.  */
    fun writeByteArray(dc: DublinCoreCatalog): ByteArray {
        try {
            return dc.toXmlString().toByteArray(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            return chuck(e)
        }

    }
}
