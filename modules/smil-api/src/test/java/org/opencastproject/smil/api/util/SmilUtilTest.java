/*
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
package org.opencastproject.smil.api.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URI;

public class SmilUtilTest {

  /**
   * The src attribute used to be built with Commons HttpClient 3.x
   * URIUtil.getPath(). It is now taken from java.net.URI.getRawPath(). The two
   * agree on every shape of track URI Opencast produces: the path is kept
   * percent-encoded, and query and fragment are dropped. These cases pin that
   * down so the old behaviour cannot regress unnoticed.
   */
  @Test
  public void testAddTrackKeepsRawPathOnly() throws Exception {
    assertSrc("/files/mediapackage/abc-123/def-456/track.mp4",
        "http://localhost:8080/files/mediapackage/abc-123/def-456/track.mp4");
    assertSrc("/files/mediapackage/mp/elem/presenter.webm",
        "https://oc.example.org:8443/files/mediapackage/mp/elem/presenter.webm");
    assertSrc("/var/lib/opencast/track.mp4", "file:///var/lib/opencast/track.mp4");

    // query and fragment are not part of the path
    assertSrc("/files/collection/c1/file.mp4",
        "http://localhost:8080/files/collection/c1/file.mp4?foo=bar");
    assertSrc("/files/collection/c1/file.mp4",
        "http://localhost:8080/files/collection/c1/file.mp4#frag");

    // percent escapes are preserved rather than decoded
    assertSrc("/files/my%20video.mp4", "http://localhost:8080/files/my%20video.mp4");
    assertSrc("/files/%C3%BCmlaut.mp4", "http://localhost:8080/files/%C3%BCmlaut.mp4");
    assertSrc("/files/a+b.mp4", "http://localhost:8080/files/a+b.mp4");
  }

  private void assertSrc(String expectedSrc, String trackUri) throws Exception {
    Document smil = SmilUtil.createSmil();
    smil = SmilUtil.addTrack(smil, SmilUtil.TrackType.PRESENTER, true, 0L, 1000L,
        new URI(trackUri), "track-1");
    Element track = (Element) smil.getElementsByTagName("video").item(0);
    assertEquals(expectedSrc, track.getAttribute("src"));
  }
}
