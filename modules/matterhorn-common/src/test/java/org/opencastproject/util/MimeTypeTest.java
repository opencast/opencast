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

package org.opencastproject.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.MimeType.mimeType;

public class MimeTypeTest {
  @Test
  public void testMimeType() {
    assertEquals("video/mp4", mimeType("video", "mp4").toString());
    assertEquals("video/mp4", MimeTypes.parseMimeType("video/mp4").toString());
  }

  @Test
  public void testMimeTypesFromSuffix() throws Exception {
    assertTrue(MimeTypes.fromSuffix("xml").eq(mimeType("text", "xml")));
    assertTrue(MimeTypes.fromSuffix("avi").eq(mimeType("video", "msvideo")));
  }

  @Test
  public void testSuffixes() {
    assertTrue(MimeTypes.parseMimeType("video/mpeg").getSuffixes().length > 1);
  }
}
