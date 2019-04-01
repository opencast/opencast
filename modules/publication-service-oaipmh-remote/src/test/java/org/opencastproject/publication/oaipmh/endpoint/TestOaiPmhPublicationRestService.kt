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
package org.opencastproject.publication.oaipmh.endpoint

import org.junit.Assert.assertEquals

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.util.NotFoundException

import org.easymock.EasyMock
import org.junit.Ignore

import javax.ws.rs.Path

/**
 * REST service under test.
 */
@Path("/")
@Ignore
class TestOaiPmhPublicationRestService @Throws(Exception::class)
constructor() : OaiPmhPublicationRestService() {
    init {
        val pubSvc = EasyMock.createNiceMock<OaiPmhPublicationService>(OaiPmhPublicationService::class.java)
        // delegate calls to #publish to check the creator
        EasyMock.expect(
                pubSvc.publish(EasyMock.anyObject(), EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyBoolean()))
                .andDelegateTo(PubSvcDelegate()).anyTimes()
        EasyMock.replay(pubSvc)
        setService(pubSvc)
    }

    class PubSvcDelegate : OaiPmhPublicationService {
        @Throws(PublicationException::class, MediaPackageException::class)
        override fun publish(
                mediaPackage: MediaPackage, repository: String, downloadIds: Set<String>,
                streamingIds: Set<String>, checkAvailability: Boolean): Job {
            // assert the creator name is preserved
            assertEquals(OaiPmhPublicationRestServiceTest.CREATOR, mediaPackage.creators[0])
            // return a mocked job
            val job = EasyMock.createNiceMock<Job>(Job::class.java)
            EasyMock.expect<URI>(job.uri).andReturn(OaiPmhPublicationRestServiceTest.JOB_URI).anyTimes()
            EasyMock.replay(job)
            return job
        }

        @Throws(PublicationException::class)
        override fun replace(mediaPackage: MediaPackage, repository: String,
                             downloadElements: Set<MediaPackageElement>, streamingElements: Set<MediaPackageElement>,
                             retractDownloadFlavors: Set<MediaPackageElementFlavor>,
                             retractStreamingFlavors: Set<MediaPackageElementFlavor>,
                             publications: Set<Publication>, checkAvailability: Boolean): Job? {
            return null
        }

        @Throws(PublicationException::class)
        override fun replaceSync(
                mediaPackage: MediaPackage, repository: String, downloadElements: Set<MediaPackageElement>,
                streamingElements: Set<MediaPackageElement>, retractDownloadFlavors: Set<MediaPackageElementFlavor>,
                retractStreamingFlavors: Set<MediaPackageElementFlavor>,
                publications: Set<Publication>, checkAvailability: Boolean): Publication? {
            return null
        }

        // not used
        @Throws(PublicationException::class, NotFoundException::class)
        override fun retract(mediaPackage: MediaPackage, repository: String): Job? {
            return null
        }

        // not used
        @Throws(PublicationException::class, MediaPackageException::class)
        override fun updateMetadata(mediaPackage: MediaPackage, repository: String, flavors: Set<String>, tags: Set<String>,
                                    checkAvailability: Boolean): Job? {
            return null
        }

    }
}
