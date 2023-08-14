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

package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Default implementation of the media time.
 */
public class SpatioTemporalLocatorImpl implements SpatioTemporalLocator {

  /** The media time */
  protected MediaTime mediaTime = null;

  /**
   * Creates a new spatio temporal locator.
   *
   * @param time
   *          the time
   */
  public SpatioTemporalLocatorImpl(MediaTime time) {
    this.mediaTime = time;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalLocator#setMediaTime(org.opencastproject.metadata.mpeg7.MediaTime)
   */
  @Override
  public void setMediaTime(MediaTime time) {
    if (time == null)
      throw new IllegalArgumentException("The media time must not be null");
    this.mediaTime = time;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalLocator#getMediaTime()
   */
  @Override
  public MediaTime getMediaTime() {
    return mediaTime;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  @Override
  public Node toXml(Document document) {
    Element l = document.createElement("SpatioTemporalLocator");
    l.appendChild(mediaTime.toXml(document));
    return l;
  }

}
