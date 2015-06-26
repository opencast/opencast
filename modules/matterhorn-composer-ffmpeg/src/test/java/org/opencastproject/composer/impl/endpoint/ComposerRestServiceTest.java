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


package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Tests the behavior of the composer rest endpoint, using a mock composer service.
 */
public class ComposerRestServiceTest {

  private JaxbJob job;
  private EncodingProfileImpl profile;
  private EncodingProfileList profileList;
  private Track audioTrack;
  private Track videoTrack;
  private String profileId;
  private ComposerRestService restService;

  @Before
  public void setUp() throws Exception {
    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    // Set up our arguments and return values
    audioTrack = (Track) builder.newElement(Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    audioTrack.setIdentifier("audio1");

    videoTrack = (Track) builder.newElement(Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    videoTrack.setIdentifier("video1");

    profileId = "profile1";

    job = new JaxbJob();
    job.setStatus(Job.Status.QUEUED);
    job.setJobType(ComposerService.JOB_TYPE);
    profile = new EncodingProfileImpl();
    profile.setIdentifier(profileId);
    List<EncodingProfileImpl> list = new ArrayList<EncodingProfileImpl>();
    list.add(profile);
    profileList = new EncodingProfileList(list);

    // Train a mock composer with some known behavior
    ComposerService composer = EasyMock.createNiceMock(ComposerService.class);
    EasyMock.expect(composer.encode(videoTrack, profileId)).andReturn(job).anyTimes();
    EasyMock.expect(composer.mux(videoTrack, audioTrack, profileId)).andReturn(job).anyTimes();
    EasyMock.expect(composer.listProfiles()).andReturn(list.toArray(new EncodingProfile[list.size()]));
    EasyMock.expect(composer.getProfile(profileId)).andReturn(profile);
    EasyMock.replay(composer);

    // Set up the rest endpoint
    restService = new ComposerRestService();
    restService.setComposerService(composer);
    restService.activate(null);
  }

  @Test
  public void testMissingArguments() throws Exception {
    // Ensure the rest endpoint tests for missing parameters
    Response response = restService.encode(generateVideoTrack(), null);
    Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    response = restService.encode(null, "profile");
    Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    response = restService.mux(generateAudioTrack(), null, "profile");
    Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    response = restService.mux(null, generateVideoTrack(), "profile");
    Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    response = restService.mux(generateAudioTrack(), generateVideoTrack(), null);
    Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testEncode() throws Exception {
    Response response = restService.encode(generateVideoTrack(), profileId);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(new JaxbJob(job), response.getEntity());
  }

  @Test
  public void testMux() throws Exception {
    Response response = restService.mux(generateAudioTrack(), generateVideoTrack(), profileId);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(new JaxbJob(job), response.getEntity());
  }

  @Test
  public void testProfiles() throws Exception {
    Response response = restService.getProfile(profileId);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals(profile, response.getEntity());

    try {
      restService.getProfile("some other ID");
      Assert.fail("This ID should cause the rest endpoint to throw");
    } catch (NotFoundException e) {
      // expected
    }

    EncodingProfileList list = restService.listProfiles();
    Assert.assertEquals(profileList, list);
  }

  protected String generateVideoTrack() {
    return "<track id=\"video1\" type=\"presentation/source\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/camera.mpg</url>\n"
            + "  <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
            + "  <duration>14546</duration>\n" + "  <video>\n"
            + "    <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
            + "    <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
            + "    <resolution>640x480</resolution>\n" + "    <scanType type=\"progressive\" />\n"
            + "    <bitrate>540520</bitrate>\n" + "    <frameRate>2</frameRate>\n" + "  </video>\n" + "</track>";
  }

  protected String generateAudioTrack() {
    return "<track id=\"audio1\" type=\"presentation/source\">\n" + "  <mimetype>audio/mp3</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/audio.mp3</url>\n"
            + "  <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n"
            + "  <duration>10472</duration>\n" + "  <audio>\n" + "    <channels>2</channels>\n"
            + "    <bitdepth>0</bitdepth>\n" + "    <bitrate>128004.0</bitrate>\n"
            + "    <samplingrate>44100</samplingrate>\n" + "  </audio>\n" + "</track>";
  }

}
