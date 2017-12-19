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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of a spatio temporal decomposition.
 */
public class SpatioTemporalDecompositionImpl implements SpatioTemporalDecomposition {

  /** Flag to indicate whether the elements in this decomposition may exhibit gaps */
  protected boolean hasGap = true;

  /** Flag to indicate whether the elements in this decomposition may overlap */
  protected boolean hasOverlap = false;

  /** The list of videotext elements */
  protected List<VideoText> videoTexts = null;

  /**
   * Creates a new spatio temporal decomposition.
   *
   * @param gap
   *          <code>true</code> if there are gaps in the decomposition
   * @param overlap
   *          <code>true</code> if there are overlapping elements
   */
  public SpatioTemporalDecompositionImpl(boolean gap, boolean overlap) {
    this.hasGap = gap;
    this.hasOverlap = overlap;
    this.videoTexts = new ArrayList<VideoText>();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#hasGap()
   */
  @Override
  public boolean hasGap() {
    return hasGap;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#isOverlapping()
   */
  @Override
  public boolean isOverlapping() {
    return hasOverlap;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#setGap(boolean)
   */
  @Override
  public void setGap(boolean hasGap) {
    this.hasGap = hasGap;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#setOverlapping(boolean)
   */
  @Override
  public void setOverlapping(boolean isOverlapping) {
    this.hasOverlap = isOverlapping;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#addVideoText(org.opencastproject.metadata.mpeg7.Textual,
   *      java.awt.Rectangle, org.opencastproject.metadata.mpeg7.MediaTime)
   */
  public VideoText addVideoText(Textual text, Rectangle boundary, MediaTime time) {
    VideoText videoText = new VideoTextImpl("videotext-" + videoTexts.size(), text, boundary, time);
    videoTexts.add(videoText);
    return videoText;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#addVideoText(org.opencastproject.metadata.mpeg7.VideoText)
   */
  @Override
  public void addVideoText(VideoText videoText) {
    videoTexts.add(videoText);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#getVideoText()
   */
  @Override
  public VideoText[] getVideoText() {
    return videoTexts.toArray(new VideoText[videoTexts.size()]);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition#getVideoText(java.lang.String)
   */
  @Override
  public VideoText getVideoText(String id) {
    for (VideoText videoText : videoTexts) {
      if (id.equals(videoText.getIdentifier()))
        return videoText;
    }
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("SpatioTemporalDecomposition");
    node.setAttribute("gap", (hasGap ? "true" : "false"));
    node.setAttribute("overlap", (hasOverlap ? "true" : "false"));
    for (VideoText videoText : videoTexts) {
      node.appendChild(videoText.toXml(document));
    }
    return node;
  }

}
