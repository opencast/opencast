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

package org.opencastproject.serviceregistry.api

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

/**
 * Marshals and unmarshals [SystemLoad]s.
 */
object SystemLoadParser {

    /** The jaxb context to use when creating marshallers and unmarshallers  */
    private val jaxbContext: JAXBContext

    /** Static initializer to setup the jaxb context  */
    init {
        try {
            jaxbContext = JAXBContext.newInstance("org.opencastproject.serviceregistry.api",
                    SystemLoadParser::class.java!!.getClassLoader())
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * Parses an xml string representing a [SystemLoad]
     *
     * @param xml
     * The serialized data
     * @return The SystemLoad
     */
    @Throws(IOException::class)
    fun parseXml(xml: String): SystemLoad {
        IOUtils.toInputStream(xml, "UTF-8").use { `in` -> return parse(`in`) }
    }

    /**
     * Parses a stream representing a [SystemLoad]
     *
     * @param in
     * The serialized data
     * @return The SystemLoad
     */
    @Throws(IOException::class)
    fun parse(`in`: InputStream): SystemLoad {
        val unmarshaller: Unmarshaller
        try {
            unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<SystemLoad>(StreamSource(`in`), SystemLoad::class.java).value
        } catch (e: Exception) {
            throw IOException(e)
        }

    }

    /**
     * Gets a serialized representation of a [SystemLoad]
     *
     * @param systemLoad
     * The SystemLoad to marshal
     * @return the serialized SystemLoad
     */
    @Throws(IOException::class)
    fun toXmlStream(systemLoad: SystemLoad): InputStream {
        return IOUtils.toInputStream(toXml(systemLoad), "UTF-8")
    }

    /**
     * Gets an xml representation of a [SystemLoad]
     *
     * @param systemLoad
     * The SystemLoad to marshal
     * @return the serialized registration
     */
    @Throws(IOException::class)
    fun toXml(systemLoad: SystemLoad): String {
        val marshaller: Marshaller
        try {
            marshaller = jaxbContext.createMarshaller()
            val writer = StringWriter()
            marshaller.marshal(systemLoad, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IOException(e)
        }

    }
}
/** Disallow construction of this utility class  */
