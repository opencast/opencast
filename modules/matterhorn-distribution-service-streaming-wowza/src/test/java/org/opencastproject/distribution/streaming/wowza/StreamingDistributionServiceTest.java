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
package org.opencastproject.distribution.streaming.wowza;

import static org.junit.Assert.assertTrue;

import org.opencastproject.mediapackage.track.TrackImpl;

import org.junit.Test;

import java.util.Set;

public class StreamingDistributionServiceTest {
  @Test
  public void testSupportedFormats() {
    String rtmp = "rTmP";
    String all = "rTmP, hLs, HdS, SmOOTH, dASh";
    Set<String> result = StreamingDistributionService.getSupportedFormatSet(rtmp);
    assertTrue(result.contains(TrackImpl.StreamingProtocol.RTMP.toString()));

    result = StreamingDistributionService.getSupportedFormatSet(rtmp.toLowerCase());
    assertTrue(result.contains(TrackImpl.StreamingProtocol.RTMP.toString()));

    result = StreamingDistributionService.getSupportedFormatSet(rtmp.toUpperCase());
    assertTrue(result.contains(TrackImpl.StreamingProtocol.RTMP.toString()));

    result = StreamingDistributionService.getSupportedFormatSet(all);
    assertTrue(result.contains(TrackImpl.StreamingProtocol.RTMP.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.HLS.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.HDS.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.SMOOTH.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.DASH.toString()));

    result = StreamingDistributionService.getDefaultSupportedFormatSet();
    assertTrue(result.contains(TrackImpl.StreamingProtocol.RTMP.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.HLS.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.HDS.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.SMOOTH.toString()));
    assertTrue(result.contains(TrackImpl.StreamingProtocol.DASH.toString()));
  }
}
