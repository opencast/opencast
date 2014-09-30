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
package org.opencastproject.smil.entity.media.element;

import java.net.URI;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represent audio media element and implement {@link SmilMediaElement}.
 */
@XmlRootElement(name = "audio")
public class SmilMediaAudioImpl extends SmilMediaElementImpl {

  /**
   * Empty constructor, needed for JAXB.
   */
  private SmilMediaAudioImpl() {
    super(null, null, null);
  }

  /**
   * Constructor.
   *
   * @param src audio media URI
   * @param clipBegin clip begin position
   * @param clipEnd clip end position
   */
  public SmilMediaAudioImpl(URI src, String clipBegin, String clipEnd) {
    super(src, clipBegin, clipEnd);
  }

  /**
   * Constructor.
   *
   * @param src audio media URI
   * @param clipBegin clip begin position
   * @param clipEnd clip end position
   * @param paramGroupId paramGroup element Id
   */
  public SmilMediaAudioImpl(URI src, String clipBegin, String clipEnd, String paramGroupId) {
    super(src, clipBegin, clipEnd, paramGroupId);
  }

  /**
   * Constructor.
   *
   * @param src audio media URI
   * @param clipBeginMS clip begin position in milliseconds
   * @param clipEndMS clip end position in milliseconds
   */
  public SmilMediaAudioImpl(URI src, long clipBeginMS, long clipEndMS) {
    super(src, clipBeginMS + "ms", clipEndMS + "ms");
  }

  /**
   * Constructor.
   *
   * @param src audio media URI
   * @param clipBeginMS clip begin position in milliseconds
   * @param clipEndMS clip end position in milliseconds
   * @param paramGroupId paramGroup element Id
   */
  public SmilMediaAudioImpl(URI src, long clipBeginMS, long clipEndMS, String paramGroupId) {
    super(src, clipBeginMS + "ms", clipEndMS + "ms", paramGroupId);
  }

  /**
   * Returns {@link MediaType}.AUDIO
   *
   * @return media type audio
   */
  @Override
  public MediaType getMediaType() {
    return MediaType.AUDIO;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getIdPrefix() {
    return "a";
  }
}
