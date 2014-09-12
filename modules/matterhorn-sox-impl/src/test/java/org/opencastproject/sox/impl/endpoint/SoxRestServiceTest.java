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
package org.opencastproject.sox.impl.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.sox.api.SoxService;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Tests the behavior of the SoX rest endpoint, using a mock SoX service.
 */
public class SoxRestServiceTest {

  private JaxbJob job;
  private Track audioTrack;
  private SoxRestService restService;

  @Before
  public void setUp() throws Exception {
    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    // Set up our arguments and return values
    audioTrack = (Track) builder.newElement(Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    audioTrack.setIdentifier("audio1");

    job = new JaxbJob();
    job.setStatus(Job.Status.QUEUED);
    job.setJobType(SoxService.JOB_TYPE);

    // Train a mock composer with some known behavior
    SoxService sox = EasyMock.createNiceMock(SoxService.class);
    EasyMock.expect(sox.analyze(audioTrack)).andReturn(job).anyTimes();
    EasyMock.expect(sox.normalize(audioTrack, -30f)).andReturn(job).anyTimes();
    EasyMock.replay(sox);

    // Set up the rest endpoint
    restService = new SoxRestService();
    restService.setSoxService(sox);
    restService.activate(null);
  }

  @Test
  public void testAnalyze() throws Exception {
    Response response = restService.analyze(MediaPackageElementParser.getAsXml(audioTrack));
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(new JaxbJob(job), response.getEntity());
  }

  @Test
  public void testNormalize() throws Exception {
    Response response = restService.normalize(MediaPackageElementParser.getAsXml(audioTrack), -30f);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(new JaxbJob(job), response.getEntity());
  }

}
