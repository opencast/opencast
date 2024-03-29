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

package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.track.SubtitleStreamImpl;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A stream containing subtitle data.
 */
@XmlJavaTypeAdapter(SubtitleStream.Adapter.class)
public interface SubtitleStream extends Stream {

  String getFormat();

  String getFormatVersion();

  String getEncoderLibraryVendor();

  class Adapter extends XmlAdapter<SubtitleStreamImpl, Stream> {
    @Override
    public SubtitleStreamImpl marshal(Stream s) throws Exception {
      return (SubtitleStreamImpl) s;
    }

    @Override
    public Stream unmarshal(SubtitleStreamImpl s) throws Exception {
      return s;
    }
  }
}
