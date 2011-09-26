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
package org.opencastproject.mediapackage.track;

import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;

import junit.framework.Assert;

import org.junit.Test;

import java.net.URI;

public class TrackSupportTest {
  @Test
  public void testByType() throws Exception {
    TrackImpl t = new TrackImpl(MediaPackageElements.PRESENTER_SOURCE, MimeTypes.parseMimeType("video/avi"), new URI(
            "http://foo"), 100L, Checksum.create(ChecksumType.DEFAULT_TYPE, "1234"));
    t.addStream(new AudioStreamImpl("audio-1"));
    t.addStream(new VideoStreamImpl("video-1"));
    Assert.assertEquals(1, TrackSupport.byType(t.getStreams(), AudioStream.class).length);
    Assert.assertEquals(1, TrackSupport.byType(t.getStreams(), VideoStream.class).length);
    Assert.assertEquals(2, TrackSupport.byType(t.getStreams(), Stream.class).length);
  }
}
