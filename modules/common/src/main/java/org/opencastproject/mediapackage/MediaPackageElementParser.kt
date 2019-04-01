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

package org.opencastproject.mediapackage

import org.apache.commons.io.IOUtils.toInputStream

import org.opencastproject.util.data.Function

import org.xml.sax.InputSource

import java.io.StringWriter
import java.util.LinkedList

import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller

/**
 * Convenience implementation that supports serializing and deserializing media package elements.
 */
object MediaPackageElementParser {

    /** [.getFromXml] as function.  */
    val getFromXml: Function<String, MediaPackageElement> = object : Function.X<String, MediaPackageElement>() {
        @Throws(Exception::class)
        public override fun xapply(s: String): MediaPackageElement {
            return getFromXml(s)
        }
    }

    val getArrayFromXmlFn: Function<String, List<MediaPackageElement>> = object : Function.X<String, List<MediaPackageElement>>() {
        @Throws(Exception::class)
        public override fun xapply(xml: String): List<MediaPackageElement> {
            return getArrayFromXml(xml)
        }
    }

    /**
     * Serializes the media package element to a string.
     *
     * @param element
     * the element
     * @return the serialized media package element
     * @throws MediaPackageException
     * if serialization failed
     */
    @Throws(MediaPackageException::class)
    fun getAsXml(element: MediaPackageElement?): String {
        if (element == null)
            throw IllegalArgumentException("Mediapackage element must not be null")
        val writer = StringWriter()
        var m: Marshaller? = null
        try {
            m = MediaPackageImpl.context.createMarshaller()
            m!!.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            m.marshal(element, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw MediaPackageException(if (e.linkedException != null) e.linkedException else e)
        }

    }

    /** [.getAsXml] as function.  */
    fun <A : MediaPackageElement> getAsXml(): Function<A, String> {
        return object : Function.X<A, String>() {
            @Throws(Exception::class)
            protected override fun xapply(elem: MediaPackageElement): String {
                return getAsXml(elem)
            }
        }
    }

    /**
     * Parses the serialized media package element and returns its object representation.
     *
     * @param xml
     * the serialized element
     * @return the media package element instance
     * @throws MediaPackageException
     * if de-serializing the element fails
     */
    @Throws(MediaPackageException::class)
    fun getFromXml(xml: String): MediaPackageElement {
        var m: Unmarshaller? = null
        try {
            m = MediaPackageImpl.context.createUnmarshaller()
            return m!!.unmarshal(InputSource(toInputStream(xml))) as MediaPackageElement
        } catch (e: JAXBException) {
            throw MediaPackageException(if (e.linkedException != null) e.linkedException else e)
        }

    }

    /**
     * Serializes media package element list to a string.
     *
     * @param elements
     * element list to be serialized
     * @return serialized media package element list
     * @throws MediaPackageException
     * if serialization fails
     */
    @Throws(MediaPackageException::class)
    fun getArrayAsXml(elements: Collection<MediaPackageElement>?): String {
        // TODO write real serialization function
        if (elements == null || elements.isEmpty()) return ""
        try {
            val builder = StringBuilder()
            val it = elements.iterator()
            builder.append(getAsXml(it.next()))
            while (it.hasNext()) {
                builder.append("###")
                builder.append(getAsXml(it.next()))
            }
            return builder.toString()
        } catch (e: Exception) {
            if (e is MediaPackageException) {
                throw e
            } else {
                throw MediaPackageException(e)
            }
        }

    }

    /**
     * Parses the serialized media package element list.
     *
     * @param xml
     * String to be parsed
     * @return parsed media package element list
     * @throws MediaPackageException
     * if de-serialization fails
     */
    @Throws(MediaPackageException::class)
    fun getArrayFromXml(xml: String): List<MediaPackageElement> {
        // TODO write real deserialization function
        try {
            val elements = LinkedList<MediaPackageElement>()
            val xmlArray = xml.split("###".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            for (xmlElement in xmlArray) {
                if ("" == xmlElement.trim({ it <= ' ' })) continue
                elements.add(getFromXml(xmlElement.trim({ it <= ' ' })))
            }
            return elements
        } catch (e: Exception) {
            if (e is MediaPackageException) {
                throw e
            } else {
                throw MediaPackageException(e)
            }
        }

    }

    /**
     * Same as getArrayFromXml(), but throwing a RuntimeException instead of a checked exception. Useful in streams.
     *
     * @param xml
     * String to be parsed
     * @return parsed media package element list
     *
     * @throws MediaPackageRuntimeException
     * if de-serialization fails
     */
    fun getArrayFromXmlUnchecked(xml: String): List<MediaPackageElement> {
        try {
            return getArrayFromXml(xml)
        } catch (e: MediaPackageException) {
            throw MediaPackageRuntimeException(e)
        }

    }

}
/**
 * Private constructor to prohibit instances of this static utility class.
 */// Nothing to do
