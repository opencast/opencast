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

import java.io.IOException
import java.io.StringWriter

import javax.xml.bind.JAXBContext
import javax.xml.bind.SchemaOutputResolver
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import javax.xml.transform.Result
import javax.xml.transform.stream.StreamResult

/**
 * Provides utility methods for transforming a [JAXBContext] into an XML schema.
 */
object JaxbXmlSchemaGenerator {

    /**
     * Builds an xml schema from a JAXBContext.
     *
     * @param jaxbContext
     * the jaxb context
     * @return the xml as a string
     * @throws IOException
     * if the JAXBContext can not be transformed into an xml schema
     */
    @Throws(IOException::class)
    fun getXmlSchema(jaxbContext: JAXBContext): String {
        val writer = StringWriter()
        jaxbContext.generateSchema(object : SchemaOutputResolver() {
            @Throws(IOException::class)
            override fun createOutput(namespaceUri: String, suggestedFileName: String): Result {
                val streamResult = StreamResult(writer)
                streamResult.systemId = ""
                return streamResult
            }
        })
        return writer.toString()
    }

    /**
     * Builds an xml schema. If the class is not XmlType or XmlRootElement annotated, return null;
     *
     * @param clazz
     * the jaxb annotated class
     * @return the xml as a string, or null if the class can not be transformed to a schema
     */
    fun getXmlSchema(clazz: Class<*>?): String? {
        if (clazz == null || (!clazz.isAnnotationPresent(XmlType::class.java) && !clazz.isAnnotationPresent(XmlRootElement::class.java)
                        && !clazz.isAnnotationPresent(XmlJavaTypeAdapter::class.java))) {
            return null
        }
        try {
            val jaxbContext = JAXBContext.newInstance(clazz)
            return getXmlSchema(jaxbContext)
        } catch (e: Exception) {
            return null
        }

    }

}
/** Private constructor to disable creation of new instances.  */
