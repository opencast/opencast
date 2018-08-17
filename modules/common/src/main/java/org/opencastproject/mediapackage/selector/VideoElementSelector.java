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

package org.opencastproject.mediapackage.selector;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This <code>MediaPackageElementSelector</code> selects tracks from a <code>MediaPackage</code> that contain video
 * streams.
 */
public class VideoElementSelector extends AbstractMediaPackageElementSelector<Track> {

  /** Explicit video flavor */
  protected MediaPackageElementFlavor videoFlavor = null;

  /**
   * Creates a new selector.
   */
  public VideoElementSelector() {
  }

  /**
   * Creates a new selector that will restrict the result of <code>select()</code> to the given flavor.
   *
   * @param flavor
   *          the flavor
   */
  public VideoElementSelector(String flavor) {
    this(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Creates a new selector that will restrict the result of <code>select()</code> to the given flavor.
   *
   * @param flavor
   *          the flavor
   */
  public VideoElementSelector(MediaPackageElementFlavor flavor) {
    addFlavor(flavor);
  }

  /**
   * Specifies an explicit video flavor.
   *
   * @param flavor
   *          the flavor
   */
  public void setVideoFlavor(String flavor) {
    if (flavor == null) {
      videoFlavor = null;
      return;
    }
    setVideoFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Specifies an explicit video flavor.
   *
   * @param flavor
   *          the flavor
   */
  public void setVideoFlavor(MediaPackageElementFlavor flavor) {
    if (flavor != null)
      addFlavor(flavor);
    videoFlavor = flavor;
  }

  /**
   * Returns the explicit video flavor or <code>null</code> if none was specified.
   *
   * @return the video flavor
   */
  public MediaPackageElementFlavor getVideoFlavor() {
    return videoFlavor;
  }

  /**
   * Returns a track or a number of tracks from the media package that together contain video and video. If no such
   * combination can be found, e. g. there is no video or video at all, an empty array is returned.
   *
   * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.mediapackage.MediaPackage, boolean)
   */
  @Override
  public Collection<Track> select(MediaPackage mediaPackage, boolean withTagsAndFlavors) {
    // instead of relying on the broken superclass, we'll inspect every track
    // Collection<Track> candidates = super.select(mediaPackage);
    Collection<Track> candidates = Arrays.asList(mediaPackage.getTracks());
    Set<Track> result = new HashSet<Track>();

    boolean foundVideo = false;

    // Look for a track containing video
    for (Track t : candidates) {
      if (TrackSupport.byType(t.getStreams(), VideoStream.class).length > 0) {
        if (!foundVideo && (videoFlavor == null || videoFlavor.equals(t.getFlavor()))) {
          result.add(t);
          foundVideo = true;
        }
      }
    }
    return result;
  }

}
