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
package org.opencastproject.publication.youtube;

import org.junit.Test;

import org.opencastproject.util.XProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author John Crossman
 */
public class YouTubeUtilsTest {

  @Test(expected = IllegalArgumentException.class)
  public void testGetWhenRequired() {
    YouTubeUtils.get(new XProperties(), YouTubeKey.clientSecretsV3, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWhenRequiredByDefault() {
    YouTubeUtils.get(new XProperties(), YouTubeKey.clientSecretsV3);
  }

  @Test
  public void testNull() {
    assertNull(YouTubeUtils.get(new XProperties(), YouTubeKey.clientSecretsV3, false));
  }

  @Test
  public void testGet() {
    final XProperties p = new XProperties();
    final String value = "value";
    YouTubeUtils.put(p, YouTubeKey.clientSecretsV3, value);
    assertEquals(value, YouTubeUtils.get(p, YouTubeKey.clientSecretsV3, true));
    assertEquals(value, YouTubeUtils.get(p, YouTubeKey.clientSecretsV3));
    assertEquals(value, YouTubeUtils.get(p, YouTubeKey.clientSecretsV3, false));
  }

}
