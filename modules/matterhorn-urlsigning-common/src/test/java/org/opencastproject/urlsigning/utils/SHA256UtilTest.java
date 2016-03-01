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
package org.opencastproject.urlsigning.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SHA256UtilTest {
  @Test
  public void testSha256() throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {
    String testString = "{\"Statement\":{\"Condition\":{\"DateLessThan\":1425768129644},\"Resource\":\"rtmp:\\/\\/mh-wowza.localdomain\\/matterhorn-engage\\/mp4:engage-player\\/2c2c438d-bb4d-404c-a677-0ebc072d91e2\\/5dbfdbcd-a983-44ea-93b6-e1c457acb61f\\/short\"}}";
    assertEquals("bf344862e1d317b246cb4336525146a4312081925c9641efaa5ebf272b944d78", SHA256Util.digest(testString, "abc123"));
    assertEquals("5169ea7246cf084413228c5ca3590b9045e3a53a625074530ad222857c6d3b7c", SHA256Util.digest(testString, "123abc"));
  }
}
