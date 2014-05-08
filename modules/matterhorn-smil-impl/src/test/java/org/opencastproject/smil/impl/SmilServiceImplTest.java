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
package org.opencastproject.smil.impl;

import java.net.URI;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.SmilMediaParallelImpl;
import org.opencastproject.smil.entity.media.container.SmilMediaSequenceImpl;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.SmilMediaAudioImpl;
import org.opencastproject.smil.entity.media.element.SmilMediaVideoImpl;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test of {@link SmilServiceImpl} class.
 */
public class SmilServiceImplTest {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(SmilServiceImplTest.class);
  /**
   * Test SMIL document
   */
  private static final String TEST_SMIL = "<!DOCTYPE smil PUBLIC \"-//W3C//DTD SMIL 3.0 Language//EN\" \"http://www.w3.org/2008/SMIL30/SMIL30Language.dtd\">\n"
          + "<smil xmlns=\"http://www.w3.org/ns/SMIL\" baseProfile=\"Language\" version=\"3.0\" xml:id=\"s-c4af7197-8496-46ae-a80b-bc15ead58c87\">\n"
          + "  <head xml:id=\"h-37abdf0c-95e8-4d39-a574-a71c927e7381\">\n"
          + "    <paramGroup xml:id=\"pg-19fc18d1-e94b-401a-91a7-08f8242642a8\">\n"
          + "      <param value=\"track-1\" name=\"track-id\" valuetype=\"data\" xml:id=\"param-04677935-3868-404c-bf1e-0f0559d718b3\"/>\n"
          + "      <param value=\"http://hostname/video.mp4\" name=\"track-src\" valuetype=\"data\" xml:id=\"param-08f9c91c-7593-4073-b0c5-82148468b03d\"/>\n"
          + "      <param value=\"source/presentation\" name=\"track-flavor\" valuetype=\"data\" xml:id=\"param-fe788faa-96c1-4c10-b3bd-a2dc5f6c5516\"/>\n"
          + "    </paramGroup>\n"
          + "    <paramGroup xml:id=\"pg-4a65ad2b-5380-4722-8a41-a82597a490a2\">\n"
          + "      <param value=\"track-2\" name=\"track-id\" valuetype=\"data\" xml:id=\"param-e4e55e4f-1432-4bf7-b7fa-3377b6337a2a\"/>\n"
          + "      <param value=\"http://hostname/audio.mp3\" name=\"track-src\" valuetype=\"data\" xml:id=\"param-313a5bff-f93a-41e2-b191-8c173d5d965a\"/>\n"
          + "      <param value=\"source/presenter\" name=\"track-flavor\" valuetype=\"data\" xml:id=\"param-073b2769-391c-42bd-a740-c30b75abbf12\"/>\n"
          + "    </paramGroup>\n"
          + "  </head>\n"
          + "  <body xml:id=\"b-135a51f4-0849-48ee-89d1-c420825ff73d\">\n"
          + "    <par xml:id=\"par-701ba9dc-d96e-404c-b286-6670f572e804\">\n"
          + "      <video src=\"http://hostname/video.mp4\" paramGroup=\"pg-19fc18d1-e94b-401a-91a7-08f8242642a8\" clipEnd=\"1001000ms\" clipBegin=\"1000ms\" xml:id=\"v-321fd617-15ed-4a05-8c62-fd1332cb66d8\"/>\n"
          + "      <audio src=\"http://hostname/audio.mp3\" paramGroup=\"pg-4a65ad2b-5380-4722-8a41-a82597a490a2\" clipEnd=\"1001000ms\" clipBegin=\"1000ms\" xml:id=\"a-65999c74-b713-4fc0-bab3-c45f4fc22280\"/>\n"
          + "    </par>\n"
          + "    <par xml:id=\"par-e9d14e26-cf32-45a1-8c4f-e986db9005b1\">\n"
          + "      <audio src=\"http://hostname/audio.mp3\" paramGroup=\"pg-4a65ad2b-5380-4722-8a41-a82597a490a2\" clipEnd=\"16000ms\" clipBegin=\"15000ms\" xml:id=\"a-f661b039-3b77-4c64-ae1d-e5f3afeaf174\"/>\n"
          + "      <video src=\"http://hostname/video.mp4\" paramGroup=\"pg-19fc18d1-e94b-401a-91a7-08f8242642a8\" clipEnd=\"16000ms\" clipBegin=\"15000ms\" xml:id=\"v-56880f2b-2d29-4717-aba6-491eea06a2c0\"/>\n"
          + "    </par>\n"
          + "  </body>\n"
          + "</smil>";
  /**
   * SmilService to test with
   */
  private static SmilService smilService;

  @BeforeClass
  public static void setUpClass() {
    smilService = new SmilServiceImpl();
  }

  /**
   * Test of createNewSmil methods, of class SmilServiceImpl.
   */
  @Test
  public void testCreateNewSmil() {
    SmilResponse smilResponse = smilService.createNewSmil();
    assertNotNull(smilResponse);
    Smil smil = smilResponse.getSmil();
    assertNotNull(smil);
    // TODO: test with MediaPackage
  }

  /**
   * Test of addParallel methods, of class SmilServiceImpl.
   */
  @Test
  public void testAddParallel() throws Exception {
    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addParallel(smilResponse.getSmil());
    assertNotNull(smilResponse.getSmil().getBody().getMediaElements().get(0));
    assertEquals(smilResponse.getSmil().getBody().getMediaElements().get(0), smilResponse.getEntity());
    assertTrue(smilResponse.getSmil().getBody().getMediaElements().get(0) instanceof SmilMediaParallelImpl);
    SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
    assertTrue(par.isContainer());
    assertSame(SmilMediaContainer.ContainerType.PAR, par.getContainerType());

    smilResponse = smilService.addParallel(smilResponse.getSmil(), smilResponse.getEntity().getId());
    assertNotNull(smilResponse.getSmil().getBody().getMediaElements().get(0));
    assertTrue(smilResponse.getSmil().getBody().getMediaElements().get(0) instanceof SmilMediaContainer);
    SmilMediaContainer parent = (SmilMediaContainer) smilResponse.getSmil().getBody().getMediaElements().get(0);
    assertNotNull(parent.getElements().get(0));
    assertTrue(parent.getElements().get(0) instanceof SmilMediaParallelImpl);
    assertEquals(parent.getElements().get(0).getId(), smilResponse.getEntity().getId());
  }

  /**
   * Test of addSequence methods, of class SmilServiceImpl.
   */
  @Test
  public void testAddSequence() throws Exception {
    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addSequence(smilResponse.getSmil());
    assertNotNull(smilResponse.getSmil().getBody().getMediaElements().get(0));
    assertEquals(smilResponse.getSmil().getBody().getMediaElements().get(0), smilResponse.getEntity());
    assertTrue(smilResponse.getSmil().getBody().getMediaElements().get(0) instanceof SmilMediaSequenceImpl);
    SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
    assertTrue(par.isContainer());
    assertSame(SmilMediaContainer.ContainerType.SEQ, par.getContainerType());

    smilResponse = smilService.addSequence(smilResponse.getSmil(), smilResponse.getEntity().getId());
    assertNotNull(smilResponse.getSmil().getBody().getMediaElements().get(0));
    assertTrue(smilResponse.getSmil().getBody().getMediaElements().get(0) instanceof SmilMediaSequenceImpl);
    SmilMediaContainer parent = (SmilMediaContainer) smilResponse.getSmil().getBody().getMediaElements().get(0);
    assertNotNull(parent.getElements().get(0));
    assertTrue(parent.getElements().get(0) instanceof SmilMediaSequenceImpl);
    assertEquals(parent.getElements().get(0).getId(), smilResponse.getEntity().getId());
    // logger.info(((SmilImpl)smilResponse.getSmil()).toXML());
  }

  /**
   * Test of addClip(s) methods, of class SmilServiceImpl.
   */
  @Test
  public void testAddClip() throws Exception {
    TrackImpl videoTrack = new TrackImpl();
    videoTrack.setIdentifier("track-1");
    videoTrack.setFlavor(new MediaPackageElementFlavor("source", "presentation"));
    videoTrack.setURI(new URI("http://hostname/video.mp4"));
    videoTrack.addStream(new VideoStreamImpl());
    videoTrack.setDuration(1000000000000L);

    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addParallel(smilResponse.getSmil());
    SmilMediaContainer par = (SmilMediaContainer) smilResponse.getEntity();
    // add video track into parallel element
    smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), videoTrack, 1000L, 1000000L);
    // logger.info(smilResponse.getSmil().toXML());
    SmilMediaObject media = null;
    for (SmilObject entity : smilResponse.getEntities()) {
      if (entity instanceof SmilMediaObject) {
        media = (SmilMediaObject) entity;
        break;
      }
    }
    assertNotNull(media);
    assertEquals(media.getId(), ((SmilMediaContainer) smilResponse.getSmil().getBody().getMediaElements().get(0))
            .getElements().get(0).getId());
    assertTrue(media instanceof SmilMediaVideoImpl);
    assertSame(((SmilMediaElement) media).getMediaType(), SmilMediaElement.MediaType.VIDEO);
    // 1000 milliseconds = 1 second
    assertEquals(1000L, ((SmilMediaElement) media).getClipBeginMS());
    // duration is 1000000 milliseconds = 1000 soconds
    // start + duration = 1s + 1000s = 1001s
    assertEquals(1001000L, ((SmilMediaElement) media).getClipEndMS());

    TrackImpl audioTrack = new TrackImpl();
    audioTrack.setIdentifier("track-2");
    audioTrack.setFlavor(new MediaPackageElementFlavor("source", "presenter"));
    audioTrack.setURI(new URI("http://hostname/audio.mp3"));
    audioTrack.addStream(new AudioStreamImpl());
    audioTrack.setDuration(1000000000000L);

    // add audio track into parallel element
    smilResponse = smilService.addClip(smilResponse.getSmil(), par.getId(), audioTrack, 1000L, 1000000L);
    // logger.info(smilResponse.getSmil().toXML());
    media = null;
    for (SmilObject entity : smilResponse.getEntities()) {
      if (entity instanceof SmilMediaObject) {
        media = (SmilMediaObject) entity;
        break;
      }
    }
    assertNotNull(media);
    assertEquals(media.getId(), ((SmilMediaContainer) smilResponse.getSmil().getBody().getMediaElements().get(0)).getElements().get(1).getId());
    assertTrue(media instanceof SmilMediaAudioImpl);
    assertSame(((SmilMediaElement) media).getMediaType(), SmilMediaElement.MediaType.AUDIO);
    // 1000 milliseconds = 1 second
    assertEquals(1000L, ((SmilMediaElement) media).getClipBeginMS());
    // duration is 1000000 milliseconds = 1000 soconds
    // start + duration = 1s + 1000s = 1001s
    assertEquals(1001000L, ((SmilMediaElement) media).getClipEndMS());

    // add new par
    smilResponse = smilService.addParallel(smilResponse.getSmil());
    par = (SmilMediaContainer) smilResponse.getEntity();
    // add tracks (as array) to par
    smilResponse = smilService.addClips(smilResponse.getSmil(), par.getId(),
            new Track[]{audioTrack, videoTrack}, 15000L, 1000L);
    // logger.info(smilResponse.getSmil().toXML());
    assertSame(2, smilResponse.getEntitiesCount());
    assertTrue(smilResponse.getEntities()[0] instanceof SmilMediaElement);
    // get audio element
    SmilMediaElement mediaElement = (SmilMediaElement) smilResponse.getEntities()[0];
    assertTrue(mediaElement.getMediaType() == SmilMediaElement.MediaType.AUDIO);
    // 15000ms = 15s
    assertEquals(15000L, mediaElement.getClipBeginMS());
    // start + duration = 15s + 1s = 16s
    assertEquals(16000L, mediaElement.getClipEndMS());
    // get video element
    mediaElement = (SmilMediaElement) smilResponse.getEntities()[1];
    assertTrue(mediaElement.getMediaType() == SmilMediaElement.MediaType.VIDEO);
    // 15000ms = 15s
    assertEquals(15000L, mediaElement.getClipBeginMS());
    // start + duration = 15s + 1s = 16s
    assertEquals(16000L, mediaElement.getClipEndMS());
  }

  @Test(expected = SmilException.class)
  public void testAddClipWithInvalidTrack() throws Exception {
    TrackImpl videoTrack = new TrackImpl();

    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addClip(smilResponse.getSmil(), null, videoTrack, 0, 10);
    fail("SmilException schould be thrown if you try to add an invalid track.");
  }

  @Test(expected = SmilException.class)
  public void testAddClipWithInvalidTrackDuration() throws Exception {
    TrackImpl videoTrack = new TrackImpl();
    videoTrack.setIdentifier("track-1");
    videoTrack.setFlavor(new MediaPackageElementFlavor("source", "presentation"));
    videoTrack.setURI(new URI("http://hostname/video.mp4"));
    videoTrack.addStream(new VideoStreamImpl());
    // no track duration set

    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addClip(smilResponse.getSmil(), null, videoTrack, 0, 10);
    fail("SmilException schould be thrown if you try to add an invalid track.");
  }

  @Test(expected = SmilException.class)
  public void testAddClipWithInvalidTrackURI() throws Exception {
    TrackImpl videoTrack = new TrackImpl();
    videoTrack.setIdentifier("track-1");
    videoTrack.setFlavor(new MediaPackageElementFlavor("source", "presentation"));
    videoTrack.addStream(new VideoStreamImpl());
    videoTrack.setDuration(Long.MAX_VALUE);
    // no track URI set

    SmilResponse smilResponse = smilService.createNewSmil();
    smilResponse = smilService.addClip(smilResponse.getSmil(), null, videoTrack, 0, 10);
    fail("SmilException schould be thrown if you try to add an invalid track.");
  }

  /**
   * Test of removeSmilElement method, of class SmilServiceImpl.
   */
  @Test
  public void testRemoveSmilElement() throws Exception {
    SmilResponse smilResponse = smilService.fromXml(TEST_SMIL);
    assertNotNull(smilResponse.getSmil());
    SmilMediaContainer par = (SmilMediaContainer) smilResponse.getSmil().getBody().getMediaElements().get(0);
    assertSame(2, par.getElements().size());

    // remove first element from parallel
    smilResponse = smilService.removeSmilElement(smilResponse.getSmil(), par.getElements().get(0).getId());
    assertTrue(smilResponse.getEntity() instanceof SmilMediaElement);
    assertSame(2, smilResponse.getSmil().getBody().getMediaElements().size());
    par = (SmilMediaContainer) smilResponse.getSmil().getBody().getMediaElements().get(0);
    assertSame(1, par.getElements().size());

    // remove parallel from smil
    smilResponse = smilService.removeSmilElement(smilResponse.getSmil(), par.getId());
    assertSame(1, smilResponse.getSmil().getBody().getMediaElements().size());
    assertEquals(par.getId(), smilResponse.getEntity().getId());

    // remove the same parallel again
    // should not fail
    // response should return the same smil without entities
    smilResponse = smilService.removeSmilElement(smilResponse.getSmil(), par.getId());
    assertSame(1, smilResponse.getSmil().getBody().getMediaElements().size());
    assertSame(0, smilResponse.getEntitiesCount());
  }

  /**
   * Test of fromXml methods, of class SmilServiceImpl.
   */
  @Test
  public void testFromXml() throws Exception {
    SmilResponse smilResponse = smilService.fromXml(TEST_SMIL);
    assertNotNull(smilResponse.getSmil());
    Smil smil = smilResponse.getSmil();

    // test head
    assertSame(2, smil.getHead().getParamGroups().size());

    // test body
    assertSame(2, smil.getBody().getMediaElements().size());
    assertTrue(smil.getBody().getMediaElements().get(0) instanceof SmilMediaParallelImpl);
    assertTrue(smil.getBody().getMediaElements().get(1) instanceof SmilMediaParallelImpl);
    SmilMediaContainer par = (SmilMediaContainer) smil.getBody().getMediaElements().get(0);
    assertSame(2, par.getElements().size());
    assertTrue(par.getElements().get(0) instanceof SmilMediaVideoImpl);
    assertTrue(par.getElements().get(1) instanceof SmilMediaAudioImpl);
  }
}
