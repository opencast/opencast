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

package org.opencastproject.util.jaxb

import org.opencastproject.util.data.functions.Misc.chuck

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.io.Writer

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.transform.stream.StreamSource

/** Base class for JAXB parser classes.  */
abstract class JaxbParser
/**
 * Create a new parser.
 *
 * @param contextPath see [javax.xml.bind.JAXBContext.newInstance]
 */
protected constructor(contextPath: String) {
    val ctx: JAXBContext

    init {
        this.ctx = init(contextPath)
    }

    private fun init(contextPath: String): JAXBContext {
        try {
            return JAXBContext.newInstance(contextPath, this.javaClass.getClassLoader())
        } catch (e: JAXBException) {
            return chuck(e)
        }

    }

    /** Unmarshal an instance of class `dtoClass` from `source` and close it.  */
    @Throws(IOException::class)
    fun <A> unmarshal(dtoClass: Class<A>, source: InputStream): A {
        try {
            val unmarshaller = ctx.createUnmarshaller()
            return unmarshaller.unmarshal(StreamSource(source), dtoClass).value
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            IOUtils.closeQuietly(source)
        }
    }

    /**
     * Marshal an object into a string.
     */
    @Throws(IOException::class)
    fun marshal(o: Any): String {
        try {
            val marshaller = ctx.createMarshaller()
            val writer = StringWriter()
            marshaller.marshal(o, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IOException(e)
        }

    }
}
