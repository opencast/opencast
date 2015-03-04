/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opencastproject.workflow.handler.distribution.PublishInternalWorkflowOperationHandler.CHANNEL_ID;
import static org.opencastproject.workflow.handler.distribution.PublishInternalWorkflowOperationHandler.SOURCE_FLAVORS;
import static org.opencastproject.workflow.handler.distribution.PublishInternalWorkflowOperationHandler.SOURCE_TAGS;

import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

/**
 * Unit tests for {@link PublishInternalWorkflowOperationHandler}
 */
public class PublishInternalWorkflowOperationHandlerTest {

  private static final String WAVEFORM_ELEMENT_ID = "waveform";
  private static final String TRACK_ELEMENT_ID = "track";

  private static final String WAVEFORM_DISTRIBUTION_URI = "http://distribution.tld/internal/waveform/waveform.png";
  private static final String TRACK1_DISTRIBUTION_URI = "http://distribution.tld/internal/track-1/track-1.mp4";

  @Test
  public void testStart() throws Exception {

    // Override the waitForStatus method to not block the jobs
    PublishInternalWorkflowOperationHandler woh = new PublishInternalWorkflowOperationHandler() {
      @Override
      protected Result waitForStatus(long timeout, Job... jobs) {
        HashMap<Job, Status> map = Stream.mk(jobs).foldl(new HashMap<Job, Status>(),
                new Fn2<HashMap<Job, Status>, Job, HashMap<Job, Status>>() {
                  @Override
                  public HashMap<Job, Status> ap(HashMap<Job, Status> a, Job b) {
                    a.put(b, Status.FINISHED);
                    return a;
                  }
                });
        return new Result(map);
      }
    };

    MediaPackage mp;
    try (InputStream is = this.getClass().getResourceAsStream("/publish-internal-mp.xml")) {
      mp = MediaPackageParser.getFromXml(IOUtils.toString(is));
    }

    WorkflowOperationInstance woi = EasyMock.createNiceMock(WorkflowOperationInstance.class);
    EasyMock.expect(woi.getConfiguration(SOURCE_FLAVORS)).andStubReturn("*/preview");
    EasyMock.expect(woi.getConfiguration(SOURCE_TAGS)).andStubReturn("internal");
    EasyMock.replay(woi);

    WorkflowInstance wi = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(wi.getMediaPackage()).andStubReturn(mp);
    EasyMock.expect(wi.getCurrentOperation()).andStubReturn(woi);
    EasyMock.replay(wi);

    MediaPackageElement track = MediaPackageElementParser.getFromXml(MediaPackageElementParser.getAsXml(mp
            .getElementById(TRACK_ELEMENT_ID)));
    track.setURI(new URI(TRACK1_DISTRIBUTION_URI));
    track.setIdentifier(null);

    Job job1 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job1.getId()).andStubReturn(1L);
    EasyMock.expect(job1.getPayload()).andStubReturn(MediaPackageElementParser.getAsXml(track));
    EasyMock.replay(job1);

    MediaPackageElement waveform = MediaPackageElementParser.getFromXml(MediaPackageElementParser.getAsXml(mp
            .getElementById(WAVEFORM_ELEMENT_ID)));
    waveform.setURI(new URI(WAVEFORM_DISTRIBUTION_URI));
    waveform.setIdentifier(null);

    Job job2 = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job2.getId()).andStubReturn(2L);
    EasyMock.expect(job2.getPayload()).andStubReturn(MediaPackageElementParser.getAsXml(waveform));
    EasyMock.replay(job2);

    DownloadDistributionService distribution = EasyMock.createNiceMock(DownloadDistributionService.class);
    EasyMock.expect(distribution.distribute(CHANNEL_ID, mp, TRACK_ELEMENT_ID, true)).andReturn(job1);
    EasyMock.expect(distribution.distribute(CHANNEL_ID, mp, WAVEFORM_ELEMENT_ID, true)).andReturn(job2);
    EasyMock.replay(distribution);

    woh.setDownloadDistributionService(distribution);

    MediaPackage result = woh.start(wi, null).getMediaPackage();
    Publication[] publications = result.getPublications();
    assertEquals(2, publications.length);

    for (Publication pub : publications) {
      // general assertions
      assertEquals(CHANNEL_ID, pub.getChannel());

      switch (pub.getURI().toString()) {
        case TRACK1_DISTRIBUTION_URI:
          assertEquals("presentation/preview", pub.getFlavor().toString());
          break;
        case WAVEFORM_DISTRIBUTION_URI:
          assertEquals("image/waveform", pub.getFlavor().toString());
          break;
        default:
          fail("Found unexpected publication: " + pub);
          break;
      }
    }

  }

}
