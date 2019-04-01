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

package org.opencastproject.distribution.streaming.remote

import java.lang.String.format
import org.opencastproject.util.HttpUtil.param
import org.opencastproject.util.HttpUtil.post
import org.opencastproject.util.JobUtil.jobFromHttpResponse
import org.opencastproject.util.data.functions.Options.join

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.StreamingDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.util.OsgiUtil

import com.google.gson.Gson

import org.apache.http.client.methods.HttpPost
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashSet

/**
 * A remote distribution service invoker.
 */
class StreamingDistributionServiceRemoteImpl : RemoteBase("waiting for activation"), StreamingDistributionService {

    /** The distribution channel identifier  */
    override var distributionType: String
        protected set(value: String) {
            super.distributionType = value
        }

    /** activates the component  */
    protected fun activate(cc: ComponentContext) {
        this.distributionType = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE)
        super.serviceType = JOB_TYPE_PREFIX + this.distributionType
    }

    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediaPackage: MediaPackage, elementId: String): Job {
        val elementIds = HashSet<String>()
        elementIds.add(elementId)
        return distribute(channelId, mediaPackage, elementIds)
    }


    @Throws(DistributionException::class, MediaPackageException::class)
    override fun distribute(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): Job {
        logger.info(format("Distributing %s elements to %s@%s", elementIds.size, channelId, distributionType))
        val req = post(param(PARAM_CHANNEL_ID, channelId),
                param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)))
        for (job in join(runRequest<A>(req, jobFromHttpResponse))) {
            return job
        }
        throw DistributionException(format("Unable to distribute '%s' elements of " + "mediapackage '%s' using a remote destribution service proxy",
                elementIds.size, mediaPackage.identifier.toString()))
    }

    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediaPackage: MediaPackage, elementId: String): Job {
        val elementIds = HashSet<String>()
        elementIds.add(elementId)
        return retract(channelId, mediaPackage, elementIds)
    }

    @Throws(DistributionException::class)
    override fun retract(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): Job {
        logger.info(format("Retracting %s elements from %s@%s", elementIds.size, channelId, distributionType))
        val req = post("/retract",
                param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)),
                param(PARAM_CHANNEL_ID, channelId))
        for (job in join(runRequest<A>(req, jobFromHttpResponse))) {
            return job
        }
        throw DistributionException(format("Unable to retract '%s' elements of " + "mediapackage '%s' using a remote destribution service proxy",
                elementIds.size, mediaPackage.identifier.toString()))
    }

    @Throws(DistributionException::class)
    override fun distributeSync(channelId: String, mediapackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement> {
        logger.info(format("Distributing %s elements to %s@%s", elementIds.size, channelId, distributionType))
        val req = post("/distributesync", param(PARAM_CHANNEL_ID, channelId),
                param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediapackage)),
                param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)))
        for (elements in join(runRequest<A>(req, RemoteBase.elementsFromHttpResponse))) {
            return elements
        }
        throw DistributionException(format("Unable to distribute '%s' elements of " + "mediapackage '%s' using a remote destribution service proxy",
                elementIds.size, mediapackage.identifier.toString()))
    }

    @Throws(DistributionException::class)
    override fun retractSync(channelId: String, mediaPackage: MediaPackage, elementIds: Set<String>): List<MediaPackageElement> {
        logger.info(format("Retracting %s elements from %s@%s", elementIds.size, channelId, distributionType))
        val req = post("/retract",
                param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)),
                param(PARAM_CHANNEL_ID, channelId))
        for (elements in join(runRequest<A>(req, RemoteBase.elementsFromHttpResponse))) {
            return elements
        }
        throw DistributionException(format("Unable to retract '%s' elements of " + "mediapackage '%s' using a remote destribution service proxy",
                elementIds.size, mediaPackage.identifier.toString()))
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(StreamingDistributionServiceRemoteImpl::class.java)

        /** The property to look up and append to REMOTE_SERVICE_TYPE_PREFIX  */
        private val PARAM_CHANNEL_ID = "channelId"
        private val PARAM_MEDIAPACKAGE = "mediapackage"
        private val PARAM_ELEMENT_IDS = "elementIds"

        private val gson = Gson()
    }
}// the service type is not available at construction time. we need to wait for activation to set this value
