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
package org.opencastproject.editor.api;

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import com.entwinemedia.fn.data.json.JObject;

import org.json.simple.JSONObject;

public final class TrackData {
  public static final String SUBTYPE = "subtype";
  public static final String TYPE = "type";
  public static final String FLAVOR = "flavor";
  public static final String AUDIO_STREAM = "audio_stream";
  public static final String VIDEO_STREAM = "video_stream";
  public static final String URI = "uri";
  public static final String ID = "id";
  private final String flavorType;
  private final String flavorSubtype;
  private final TrackSubData audio;
  private final TrackSubData video;
  private final String uri;
  private final String id;

  public MediaPackageElementFlavor getFlavor() {
    return new MediaPackageElementFlavor(flavorType, flavorSubtype);
  }

  public TrackData(final String flavorType, final String flavorSubtype, final TrackSubData audio,
          final TrackSubData video, String uri, String id) {
    this.flavorType = flavorType;
    this.flavorSubtype = flavorSubtype;
    this.audio = audio;
    this.video = video;
    this.uri = uri;
    this.id = id;
  }

  protected static TrackData parse(final JSONObject object) {
    if (object == null) {
      return null;
    }

    final JSONObject flavor = (JSONObject) object.get(FLAVOR);
    return new TrackData(flavor == null ? null : (String) flavor.get(TYPE),
            flavor == null ? null : (String) flavor.get(SUBTYPE),
            TrackSubData.parse((JSONObject) object.get(AUDIO_STREAM)),
            TrackSubData.parse((JSONObject) object.get(VIDEO_STREAM)),
            (String) object.get(URI),
            (String) object.get(ID));
  }

  protected JObject toJson() {
    final JObject flavor = obj(f(TYPE, flavorType), f(SUBTYPE, flavorSubtype));
    return obj(f(ID, id), f(URI, uri), f(FLAVOR, flavor), f(AUDIO_STREAM, audio.toJson()),
            f(VIDEO_STREAM, video.toJson()));
  }

  public TrackSubData getAudio() {
    return this.audio;
  }

  public TrackSubData getVideo() {
    return this.video;
  }

  public String getId() {
    return this.id;
  }
}
