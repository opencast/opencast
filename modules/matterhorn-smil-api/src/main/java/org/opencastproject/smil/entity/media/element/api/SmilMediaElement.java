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
package org.opencastproject.smil.entity.media.element.api;

import java.net.URI;
import java.util.List;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;

/**
 * Represent a media element like {@code audio}, {@code video},...
 */
public interface SmilMediaElement extends SmilMediaObject {

  /**
   * SMIL media element type.
   */
  public enum MediaType {
    AUDIO, VIDEO, IMAGE, REF
  }

  /**
   * Returns clip start position.
   *
   * @return the clipBegin
   */
  String getClipBegin();

  /**
   * Returns clip end position.
   *
   * @return the clipEnd
   */
  String getClipEnd();

  /**
   * Returns media element type.
   *
   * @return this media element type
   */
  MediaType getMediaType();

  /**
   * Returns {@link SmilMediaParamGroup} Id given with this element.
   *
   * @return the paramGroup Id
   */
  String getParamGroup();

  /**
   * Returns {@link SmilMediaParam}s for this media element. The {@link List} is
   * immutable, use {@link SmilService} to modify it.
   *
   * @return the {@link List} with {@link SmilMediaParam}s
   */
  List<SmilMediaParam> getParams();

  /**
   * Returns media source URI.
   *
   * @return the media src URI
   */
  URI getSrc();

  /**
   * Returns clip start position in milliseconds.
   *
   * @throws SmilException if clip begin position can't parsed.
   * @return clip start position in milliseconds
   */
  long getClipBeginMS() throws SmilException;

  /**
   * Returns clip end position in milliseconds.
   *
   * @throws SmilException if clip end position can't parsed.
   * @return clip end position in milliseconds
   */
  long getClipEndMS() throws SmilException;
}
