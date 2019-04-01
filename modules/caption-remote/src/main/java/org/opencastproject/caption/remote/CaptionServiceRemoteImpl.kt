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

package org.opencastproject.caption.remote

import org.opencastproject.caption.api.CaptionConverterException
import org.opencastproject.caption.api.CaptionService
import org.opencastproject.caption.api.UnsupportedCaptionFormatException
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.serviceregistry.api.RemoteBase

import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import java.util.ArrayList

import javax.xml.parsers.DocumentBuilderFactory

/**
 * Proxies a set of remote composer services for use as a JVM-local service. Remote services are selected at random.
 */
class CaptionServiceRemoteImpl : RemoteBase(JOB_TYPE), CaptionService {

    /**
     * @see org.opencastproject.caption.api.CaptionService.convert
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class, MediaPackageException::class)
    override fun convert(input: MediaPackageElement, inputFormat: String, outputFormat: String): Job {
        return convert(input, inputFormat, outputFormat, null!!)
    }

    /**
     * @see org.opencastproject.caption.api.CaptionService.convert
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class, MediaPackageException::class)
    override fun convert(input: MediaPackageElement, inputFormat: String, outputFormat: String, language: String): Job {
        val post = HttpPost("/convert")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("captions", MediaPackageElementParser.getAsXml(input)))
            params.add(BasicNameValuePair("input", inputFormat))
            params.add(BasicNameValuePair("output", outputFormat))
            if (StringUtils.isNotBlank(language))
                params.add(BasicNameValuePair("language", language))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw CaptionConverterException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val r = JobParser.parseJob(content)
                logger.info("Converting job {} started on a remote caption service", r.id)
                return r
            }
        } catch (e: Exception) {
            throw CaptionConverterException("Unable to convert catalog $input using a remote caption service", e)
        } finally {
            closeConnection(response)
        }
        throw CaptionConverterException("Unable to convert catalog $input using a remote caption service")
    }

    /**
     * @see org.opencastproject.caption.api.CaptionService.getLanguageList
     */
    @Throws(UnsupportedCaptionFormatException::class, CaptionConverterException::class)
    override fun getLanguageList(input: MediaPackageElement, format: String): Array<String> {
        val post = HttpPost("/languages")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("captions", MediaPackageElementParser.getAsXml(input)))
            params.add(BasicNameValuePair("input", format))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw CaptionConverterException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val langauges = ArrayList<String>()
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(EntityUtils.toString(response.entity, "UTF-8"))
                val languages = doc.getElementsByTagName("languages")
                for (i in 0 until languages.length) {
                    val item = languages.item(i)
                    langauges.add(item.textContent)
                }
                logger.info("Catalog languages received from remote caption service")
                return langauges.toTypedArray()
            }
        } catch (e: Exception) {
            throw CaptionConverterException("Unable to get catalog languages " + input
                    + " using a remote caption service", e)
        } finally {
            closeConnection(response)
        }
        throw CaptionConverterException("Unable to get catalog languages$input using a remote caption service")
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(CaptionServiceRemoteImpl::class.java)
    }

}
