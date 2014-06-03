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

package org.opencastproject.search.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Part of a search result that models a video segment.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "segment", namespace = "http://search.opencastproject.org")
@XmlRootElement(name = "segment", namespace = "http://search.opencastproject.org")
public class MediaSegmentImpl implements MediaSegment, Comparable<MediaSegmentImpl> {

  /** The segment number **/
  @XmlAttribute(name = "index")
  private int number = -1;

  /** The segment time point **/
  @XmlAttribute(name = "time")
  private long time = -1;

  /** The segment duration **/
  @XmlAttribute(name = "duration")
  private long duration = -1;

  /** The segment text **/
  @XmlElement(name = "text")
  private String text = null;

  /** The segment image **/
  @XmlElement(name = "image")
  private String imageUrl = null;

  /** The segment relevance **/
  @XmlAttribute(name = "relevance")
  private int relevance = -1;

  /** The 'segment is a hit' flag **/
  @XmlAttribute(name = "hit")
  private boolean hit = false;

  /** The preview urls */
  @XmlElementWrapper(name = "previews")
  @XmlElement(name = "preview")
  private List<MediaSegmentPreviewImpl> previewUrls = new ArrayList<MediaSegmentPreviewImpl>();

  /**
   * A no-arg constructor, which is needed for JAXB serialization.
   */
  public MediaSegmentImpl() {
  }

  /**
   * Creates a new segment that is located at position <code>sequenceId</code> within the sequence of segments.
   *
   * @param segment
   *          the segment number
   */
  public MediaSegmentImpl(int segment) {
    this.number = segment;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#getIndex()
   */
  public int getIndex() {
    return number;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#getTime()
   */
  public long getTime() {
    return time;
  }

  /**
   * Set the segment time.
   *
   * @param time
   *          The time to set
   */
  public void setTime(long time) {
    this.time = time;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#getDuration()
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Set the segment duration.
   *
   * @param duration
   *          The duration to set.
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#getImageUrl()
   */
  public String getImageUrl() {
    return imageUrl;
  }

  /**
   * Set the image url.
   *
   * @param imageUrl
   *          the image to set
   */
  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#getText()
   */
  public String getText() {
    return text;
  }

  /**
   * Set the segment text.
   *
   * @param text
   *          The text to set.
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#isHit()
   */
  public boolean isHit() {
    return hit;
  }

  /**
   * Set the 'segment is a hit' flag.
   *
   * @param hit
   *          The flag.
   */
  public void setHit(boolean hit) {
    this.hit = hit;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#getRelevance()
   */
  public int getRelevance() {
    return relevance;
  }

  /**
   * Set the segment relevance.
   *
   * @param relevance
   *          The relevance to set.
   */
  public void setRelevance(int relevance) {
    this.relevance = relevance;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.search.api.MediaSegment#addPreview(java.lang.String, java.lang.String)
   */
  @Override
  public void addPreview(String url, String reference) {
    previewUrls.add(new MediaSegmentPreviewImpl(url, reference));
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(MediaSegmentImpl o) {
    return this.getIndex() - o.getIndex();
  }

}
