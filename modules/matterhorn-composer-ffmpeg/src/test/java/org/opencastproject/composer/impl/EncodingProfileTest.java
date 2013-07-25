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

package org.opencastproject.composer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Tests for encoding format handling.
 */
public class EncodingProfileTest {

  /** Map with encoding profiles */
  private Map<String, EncodingProfile> profiles = null;

  /** Name of the h264 profile */
  private String h264ProfileId = "h264.rtsp";

  /** Name of the cover ui profile */
  private String coverProfileId = "cover-ui.http";

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    URL url = EncodingProfileTest.class.getResource("/encodingtestprofiles.properties");
    EncodingProfileScanner mgr = new EncodingProfileScanner();
    profiles = mgr.loadFromProperties(new File(url.toURI()));
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl}.
   */
  @Test
  public void testMediaTypes() {
    assertNotNull(EncodingProfile.MediaType.parseString("audio"));
    assertNotNull(EncodingProfile.MediaType.parseString("visual"));
    assertNotNull(EncodingProfile.MediaType.parseString("audiovisual"));
    assertNotNull(EncodingProfile.MediaType.parseString("enhancedaudio"));
    assertNotNull(EncodingProfile.MediaType.parseString("image"));
    assertNotNull(EncodingProfile.MediaType.parseString("imagesequence"));
    assertNotNull(EncodingProfile.MediaType.parseString("cover"));
    try {
      EncodingProfile.MediaType.parseString("foo");
      fail("Test should have failed for media type 'foo'");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl}.
   */
  @Test
  public void testInitializationFromProperties() {
    assertNotNull(profiles);
    assertEquals(10, profiles.size());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getIdentifier()}.
   */
  @Test
  public void testGetIdentifier() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertEquals(h264ProfileId, profile.getIdentifier());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getName()}.
   */
  @Test
  public void testGetName() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertEquals("h.264 streaming medium quality", profile.getName());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getOutputType()}.
   */
  @Test
  public void testGetType() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertEquals(MediaType.Visual, profile.getOutputType());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getSuffix()}.
   */
  @Test
  public void testGetSuffix() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertEquals("-sm.mp4", profile.getSuffix());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getApplicableMediaTypes()}.
   */
  @Test
  public void testGetApplicableMediaTypes() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    MediaType type = profile.getApplicableMediaType();
    assertNotNull(type);
    assertEquals(MediaType.Visual, type);
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getApplicableMediaTypes()}.
   */
  @Test
  public void testApplicableTo() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertTrue(profile.isApplicableTo(MediaType.Visual));
    assertFalse(profile.isApplicableTo(MediaType.Audio));
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getMimeType()}.
   */
  @Test
  public void testGetMimeType() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertEquals("visual/mp4v-es", profile.getMimeType());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getExtension(java.lang.String)}.
   */
  @Test
  public void testGetExtension() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertNull(profile.getExtension("test"));

    // Test profile with existing extension
    profile = profiles.get(coverProfileId);
    String commandline = "-i #{in.path} -y -r 1 -t 1 -f image2 -s 160x120 #{out.dir}/#{in.name}#{out.suffix}";
    assertEquals(commandline, profile.getExtension("ffmpeg.command"));
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#getExtensions()}.
   */
  @Test
  public void testGetExtensions() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    profile.isApplicableTo(MediaType.Visual);
    assertEquals(Collections.emptyMap(), profile.getExtensions());

    // Test profile with existing extension
    profile = profiles.get(coverProfileId);
    assertEquals(1, profile.getExtensions().size());
  }

  /**
   * Test method for {@link org.opencastproject.composer.api.EncodingProfileImpl#hasExtensions()}.
   */
  @Test
  public void testHasExtensions() {
    EncodingProfile profile = profiles.get(h264ProfileId);
    assertFalse(profile.hasExtensions());

    // Test profile with existing extension
    profile = profiles.get(coverProfileId);
    assertTrue(profile.hasExtensions());
  }

}