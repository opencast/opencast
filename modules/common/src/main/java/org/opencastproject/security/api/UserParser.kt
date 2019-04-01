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

package org.opencastproject.security.api

import org.apache.commons.io.IOUtils

import java.io.InputStream
import java.io.StringWriter
import java.io.Writer

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.transform.stream.StreamSource

/**
 * Marshals and unmarshalls [User]s to/from XML.
 */
object UserParser {

    private val jaxbContext: JAXBContext

    init {
        try {
            jaxbContext = JAXBContext.newInstance("org.opencastproject.security.api", UserParser::class.java!!.getClassLoader())
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }

    }

    fun fromXml(xml: String): User {
        var `in`: InputStream? = null
        try {
            `in` = IOUtils.toInputStream(xml)
            val unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<JaxbUser>(StreamSource(`in`), JaxbUser::class.java).value
        } catch (e: JAXBException) {
            throw IllegalStateException(if (e.linkedException != null) e.linkedException else e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    fun toXml(user: User): String {
        try {
            val marshaller = jaxbContext.createMarshaller()
            val writer = StringWriter()
            marshaller.marshal(user, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IllegalStateException(if (e.linkedException != null) e.linkedException else e)
        }

    }

}
/**
 * Disallow construction of this utility class.
 */
