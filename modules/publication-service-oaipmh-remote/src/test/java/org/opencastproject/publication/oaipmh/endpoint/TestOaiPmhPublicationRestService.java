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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.publication.oaipmh.endpoint;

import static org.junit.Assert.assertEquals;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.util.NotFoundException;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.Set;

import javax.ws.rs.Path;

/**
 * REST service under test.
 */
@Path("/")
@Ignore
public class TestOaiPmhPublicationRestService extends OaiPmhPublicationRestService {
  public TestOaiPmhPublicationRestService() throws Exception {
    final OaiPmhPublicationService pubSvc = EasyMock.createNiceMock(OaiPmhPublicationService.class);
    // delegate calls to #publish to check the creator
    EasyMock.expect(
            pubSvc.publish(EasyMock.<MediaPackage>anyObject(), EasyMock.anyString(), EasyMock.<Set<String>>anyObject(), EasyMock.<Set<String>>anyObject(), EasyMock.anyBoolean()))
            .andDelegateTo(new PubSvcDelegate()).anyTimes();
    EasyMock.replay(pubSvc);
    setService(pubSvc);
  }

  public static final class PubSvcDelegate implements OaiPmhPublicationService {
    @Override
    public Job publish(
            MediaPackage mediaPackage, String repository, Set<String> downloadIds,
            Set<String> streamingIds, boolean checkAvailability)
            throws PublicationException, MediaPackageException {
      // assert the creator name is preserved
      assertEquals(OaiPmhPublicationRestServiceTest.CREATOR, mediaPackage.getCreators()[0]);
      // return a mocked job
      final Job job = EasyMock.createNiceMock(Job.class);
      EasyMock.expect(job.getUri()).andReturn(OaiPmhPublicationRestServiceTest.JOB_URI).anyTimes();
      EasyMock.replay(job);
      return job;
    }

    @Override
    public Job replace(MediaPackage mediaPackage, String repository,
           Set<? extends MediaPackageElement> downloadElements, Set<? extends MediaPackageElement> streamingElements,
           Set<MediaPackageElementFlavor> retractDownloadFlavors,
           Set<MediaPackageElementFlavor> retractStreamingFlavors,
           Set<? extends Publication> publications, boolean checkAvailability) throws PublicationException {
      return null;
    }

    @Override
    public Publication replaceSync(
        MediaPackage mediaPackage, String repository, Set<? extends MediaPackageElement> downloadElements,
        Set<? extends MediaPackageElement> streamingElements, Set<MediaPackageElementFlavor> retractDownloadFlavors,
        Set<MediaPackageElementFlavor> retractStreamingFlavors,
        Set<? extends Publication> publications, boolean checkAvailability) throws PublicationException {
      return null;
    }

    // not used
    @Override
    public Job retract(MediaPackage mediaPackage, String repository) throws PublicationException, NotFoundException {
      return null;
    }

    // not used
    @Override
    public Job updateMetadata(MediaPackage mediaPackage, String repository, Set<String> flavors, Set<String> tags,
            boolean checkAvailability) throws PublicationException, MediaPackageException {
      return null;
    }

  }
}
