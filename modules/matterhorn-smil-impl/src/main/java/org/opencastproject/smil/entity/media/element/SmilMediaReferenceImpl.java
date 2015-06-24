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

package org.opencastproject.smil.entity.media.element;

import java.net.URI;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represent ref media element and implement {@link SmilMediaElement}.
 */
@XmlRootElement(name = "ref")
public class SmilMediaReferenceImpl extends SmilMediaElementImpl {

  /**
   * Empty constructor, needed for JAXB.
   */
  private SmilMediaReferenceImpl() {
    super(null, null, null);
  }

  /**
   * Constructor.
   *
   * @param src reference URI
   * @param clipBegin clip begin position
   * @param clipEnd clip end position
   */
  public SmilMediaReferenceImpl(URI src, String clipBegin, String clipEnd) {
    super(src, clipBegin, clipEnd);
  }

  /**
   * Constructor.
   *
   * @param src reference URI
   * @param clipBegin clip begin position
   * @param clipEnd clip end position
   * @param paramGroupId paramGroup element Id
   */
  public SmilMediaReferenceImpl(URI src, String clipBegin, String clipEnd, String paramGroupId) {
    super(src, clipBegin, clipEnd, paramGroupId);
  }

  /**
   * Constructor.
   *
   * @param src reference URI
   * @param clipBeginMS clip begin position in milliseconds
   * @param clipEndMS clip end position in milliseconds
   */
  public SmilMediaReferenceImpl(URI src, long clipBeginMS, long clipEndMS) {
    super(src, clipBeginMS + "ms", clipEndMS + "ms");
  }

  /**
   * Constructor.
   *
   * @param src reference URI
   * @param clipBeginMS clip begin position in milliseconds
   * @param clipEndMS clip end position in milliseconds
   * @param paramGroupId paramGroup element Id
   */
  public SmilMediaReferenceImpl(URI src, long clipBeginMS, long clipEndMS, String paramGroupId) {
    super(src, clipBeginMS + "ms", clipEndMS + "ms", paramGroupId);
  }

  /**
   * Returns {@link MediaType}.REF
   *
   * @return media type ref
   */
  @Override
  public MediaType getMediaType() {
    return MediaType.REF;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getIdPrefix() {
    return "ref";
  }
}
