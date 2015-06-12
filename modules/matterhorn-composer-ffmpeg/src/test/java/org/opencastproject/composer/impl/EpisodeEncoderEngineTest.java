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

package org.opencastproject.composer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.impl.episode.EpisodeEncoderEngine;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Test suite for the Telestream Episode encoding engine. Since you would need an instance of that engine in order to
 * fully test it, there are no actual encoding tests done, just configuration and setup.
 */
public class EpisodeEncoderEngineTest {

  /** Instance of the episode encoder engine */
  private EpisodeEncoderEngine episodeEngine = null;

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EpisodeEncoderEngineTest.class);

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

  @Ignore
  @Test
  public void testGetXmlrpcHost() {
    assertEquals("opencastproject.org", episodeEngine.getXmlrpcHost());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcPort()}.
   */
  @Ignore
  @Test
  public void testGetXmlrpcPort() {
    assertEquals(12345, episodeEngine.getXmlrpcPort());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcPath()}.
   */
  @Ignore
  @Test
  public void testGetXmlrpcPath() {
    assertEquals("/RPC2", episodeEngine.getXmlrpcPath());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#getXmlrpcPath()}.
   */
  @Ignore
  @Test
  public void testGetMonitorFrequency() {
    assertEquals(10, episodeEngine.getMonitoringFrequency());
  }

  /**
   * Test method for {@link org.opencastproject.composer.impl.episode.EpisodeEncoderEngine#needsLocalWorkCopy()}.
   */
  @Ignore
  @Test
  public void testNeedsLocalWorkCopy() {
    assertTrue(episodeEngine.needsLocalWorkCopy());
  }

  @Test
  public void testConfigureNPE() throws Exception {
    try {
      // since the method is private we need to use reflection to get in there
      Method method = episodeEngine.getClass().getDeclaredMethod("configure", Properties.class);
      method.setAccessible(true);
      // check for NPE
      method.invoke(episodeEngine, (Object) null);
      // if no exception has been thrown then die
      Assert.fail();
    } catch (Exception e) {
      assertTrue("Episode engine setup failed: Properties must not be null".equals(e.getCause().getMessage()));
    }
  }

  @Test
  public void testConfigureProperties() throws Exception {
    try {
      // since the method is private we need to use reflection to get in there
      Method method = episodeEngine.getClass().getDeclaredMethod("configure", Properties.class);
      method.setAccessible(true);
      // some minimal configuration values we will use in tests
      Properties p = new Properties();
      p.put(EpisodeEncoderEngine.OPT_XMLRPC_PATH, "/dogfood");
      method.invoke(episodeEngine, (Object) p);
      // still here? let's add some more
      p.put(EpisodeEncoderEngine.OPT_EPISODE_MONITOR_FREQUENCY, "20");
      p.put(EpisodeEncoderEngine.OPT_MONITORTYPE, "monitortype");
      p.put(EpisodeEncoderEngine.OPT_XMLRPC_HOST , "24.64.64.64");
      p.put(EpisodeEncoderEngine.OPT_XMLRPC_PASSWORD , "a dog eats cat food");
      p.put(EpisodeEncoderEngine.OPT_XMLRPC_PORT, "40000");
      method.invoke(episodeEngine, (Object) p);
      // lets see if we get those values back
      Assert.assertEquals(episodeEngine.getMonitoringFrequency(), 20);
      Assert.assertEquals(episodeEngine.getXmlrpcHost(), "24.64.64.64");
      Assert.assertEquals(episodeEngine.getXmlrpcPath(), "/dogfood");
      Assert.assertEquals(episodeEngine.getXmlrpcPort(), 40000);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

}
