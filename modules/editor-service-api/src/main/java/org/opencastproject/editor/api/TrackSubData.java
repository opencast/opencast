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
import static com.entwinemedia.fn.data.json.Jsons.v;

import com.entwinemedia.fn.data.json.JObject;

import org.json.simple.JSONObject;

public final class TrackSubData {
  public static final String ENABLED = "enabled";
  public static final String AVAILABLE = "available";
  public static final String THUMBNAIL_URI = "thumbnail_uri";
  private final boolean available;
  private final String thumbnailUri;
  private final boolean enabled;

  public TrackSubData(final boolean available, final String thumbnailUri, final boolean enabled) {
    this.available = available;
    this.thumbnailUri = thumbnailUri;
    this.enabled = enabled;
  }

  protected static TrackSubData parse(final JSONObject object) {
    Boolean hidden = (Boolean) object.get(ENABLED);
    Boolean enabled = (Boolean) object.get(AVAILABLE);

    return new TrackSubData(
            enabled == null ? Boolean.FALSE : enabled,
            (String) object.get(THUMBNAIL_URI),
            hidden == null ? Boolean.FALSE : hidden);
  }

  protected JObject toJson() {
    if (available) {
      if (thumbnailUri != null) {
        return obj(f(AVAILABLE, true), f(THUMBNAIL_URI, v(thumbnailUri)), f(ENABLED, enabled));
      } else {
        return obj(f(AVAILABLE, true), f(ENABLED, enabled));
      }
    }
    return obj(f(AVAILABLE, false));
  }

  public boolean isEnabled() {
    return enabled;
  }
}
