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


package org.opencastproject.search.api;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Part of a search result that models the preview url for a video segment.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "preview", namespace = "http://search.opencastproject.org")
@XmlRootElement(name = "preview", namespace = "http://search.opencastproject.org")
public class MediaSegmentPreviewImpl implements MediaSegmentPreview {

  /** The preview type **/
  @XmlAttribute(name = "ref")
  private String reference = null;

  /** The preview url */
  @XmlValue
  private String url = null;

  /**
   * Needed by JAXB.
   */
  public MediaSegmentPreviewImpl() {
  }

  /**
   * Creates a new preview url.
   *
   * @param url
   *          url to the preview image
   * @param reference
   *          reference to the source track
   */
  public MediaSegmentPreviewImpl(String url, String reference) {
    this.url = url;
    this.reference = reference;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegmentPreview#getReference()
   */
  @Override
  public String getReference() {
    return reference;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegmentPreview#getUrl()
   */
  @Override
  public String getUrl() {
    return url;
  }

}
