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
import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.impl.episode.EpisodeEncoderEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test suite for the Telestream Episode encoding engine. Since you would need an instance of that engine in order to
 * fully test it, there are no actual encoding tests done, just configuration and setup.
 */
@Ignore
public class EpisodeEncoderEngineTest {

  /** Instance of the episode encoder engine */
  private EpisodeEncoderEngine episodeEngine = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    EncoderEngine engine = new EpisodeEncoderEngine();
    assertEquals(EpisodeEncoderEngine.class, engine.getClass());
    episodeEngine = (EpisodeEncoderEngine) engine;
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcHost()}.
   */
  @Test
  public void testGetXmlrpcHost() {
    assertEquals("opencastproject.org", episodeEngine.getXmlrpcHost());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcPort()}.
   */
  @Test
  public void testGetXmlrpcPort() {
    assertEquals(12345, episodeEngine.getXmlrpcPort());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcPath()}.
   */
  @Test
  public void testGetXmlrpcPath() {
    assertEquals("/RPC2", episodeEngine.getXmlrpcPath());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcPath()}.
   */
  @Test
  public void testGetMonitorFrequency() {
    assertEquals(10, episodeEngine.getMonitoringFrequency());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#needsLocalWorkCopy()}.
   */
  @Test
  public void testNeedsLocalWorkCopy() {
    assertTrue(episodeEngine.needsLocalWorkCopy());
  }

}