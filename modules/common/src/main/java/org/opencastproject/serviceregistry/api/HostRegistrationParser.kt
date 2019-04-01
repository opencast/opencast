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
 * Marshals and unmarshals [HostRegistration]s.
 */
object HostRegistrationParser {

    /** The jaxb context to use when creating marshallers and unmarshallers  */
    private val jaxbContext: JAXBContext

    /** Static initializer to setup the jaxb context  */
    init {
        try {
            jaxbContext = JAXBContext.newInstance("org.opencastproject.serviceregistry.api:org.opencastproject.job.api",
                    HostRegistrationParser::class.java!!.getClassLoader())
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * Parses an xml string representing a [HostRegistration]
     *
     * @param xml
     * The serialized data
     * @return The HostRegistration
     */
    @Throws(IOException::class)
    fun parseXml(xml: String): HostRegistration {
        return parse(IOUtils.toInputStream(xml, "UTF-8"))
    }

    /**
     * Parses a stream representing a [HostRegistration]
     *
     * @param in
     * The serialized data
     * @return The HostRegistration
     */
    @Throws(IOException::class)
    fun parse(`in`: InputStream): HostRegistration {
        val unmarshaller: Unmarshaller
        try {
            unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<JaxbHostRegistration>(StreamSource(`in`), JaxbHostRegistration::class.java).value
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    /**
     * Gets a serialized representation of a [HostRegistration]
     *
     * @param registration
     * The host registration to marshal
     * @return the serialized host registration
     */
    @Throws(IOException::class)
    fun toXmlStream(registration: HostRegistration): InputStream {
        return IOUtils.toInputStream(toXml(registration), "UTF-8")
    }

    /**
     * Gets an xml representation of a [HostRegistration]
     *
     * @param registration
     * The host registration to marshal
     * @return the serialized registration
     */
    @Throws(IOException::class)
    fun toXml(registration: HostRegistration): String {
        val marshaller: Marshaller
        try {
            marshaller = jaxbContext.createMarshaller()
            val writer = StringWriter()
            marshaller.marshal(registration, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IOException(e)
        }

    }

    @Throws(IOException::class)
    fun parseRegistrations(`in`: InputStream): JaxbHostRegistrationList {
        val unmarshaller: Unmarshaller
        try {
            unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<JaxbHostRegistrationList>(StreamSource(`in`), JaxbHostRegistrationList::class.java).value
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }
}
/** Disallow construction of this utility class  */
