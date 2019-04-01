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

package org.opencastproject.job.api

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
 * Marshals and unmarshals [Job]s.
 */
object JobParser {
    private val jaxbContext: JAXBContext

    init {
        val sb = StringBuilder()
        sb.append("org.opencastproject.job.api:org.opencastproject.serviceregistry.api")
        try {
            jaxbContext = JAXBContext.newInstance(sb.toString(), JobParser::class.java!!.getClassLoader())
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * Parses an xml string representing a [Job]
     *
     * @param serializedForm
     * The serialized data
     * @return The job
     */
    @Throws(IOException::class)
    fun parseJob(serializedForm: String): Job {
        return parseJob(IOUtils.toInputStream(serializedForm, "UTF-8"))
    }

    /**
     * Parses a stream representing a [Job]
     *
     * @param in
     * The serialized data
     * @return The job
     */
    @Throws(IOException::class)
    fun parseJob(`in`: InputStream): Job {
        val unmarshaller: Unmarshaller
        try {
            unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<JaxbJob>(StreamSource(`in`), JaxbJob::class.java).value.toJob()
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    /**
     * Serializes the job into a string representation.
     *
     * @param job
     * the job
     * @return the job's serialized form
     * @throws IOException
     * if parsing fails
     */
    @Throws(IOException::class)
    fun toXml(job: JaxbJob): String {
        try {
            val marshaller = jaxbContext.createMarshaller()
            val writer = StringWriter()
            marshaller.marshal(job, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IOException(e)
        }

    }

    /**
     * Parses an xml string representing a [JaxbJobList]
     *
     * @param serializedForm
     * The serialized data
     * @return The job list
     */
    @Throws(IOException::class)
    fun parseJobList(serializedForm: String): JaxbJobList {
        return parseJobList(IOUtils.toInputStream(serializedForm, "UTF-8"))
    }

    /**
     * Parses a stream representing a [JaxbJobList]
     *
     * @param in
     * the serialized data
     * @return the job list
     */
    @Throws(IOException::class)
    fun parseJobList(`in`: InputStream): JaxbJobList {
        val unmarshaller: Unmarshaller
        try {
            unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<JaxbJobList>(StreamSource(`in`), JaxbJobList::class.java).value
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

}
/** Disallow constructing this utility class  */
