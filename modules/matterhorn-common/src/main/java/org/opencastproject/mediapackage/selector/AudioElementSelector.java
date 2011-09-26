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
package org.opencastproject.mediapackage.selector;

import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This <code>MediaPackageElementSelector</code> selects tracks from a <code>MediaPackage</code> that contain audio
 * stream.
 */
public class AudioElementSelector extends AbstractMediaPackageElementSelector<Track> {

  /** Explicit audio flavor */
  protected MediaPackageElementFlavor audioFlavor = null;

  /**
   * Creates a new selector.
   */
  public AudioElementSelector() {
  }

  /**
   * Creates a new selector that will restrict the result of <code>select()</code> to the given flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public AudioElementSelector(String flavor) {
    this(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Creates a new selector that will restrict the result of <code>select()</code> to the given flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public AudioElementSelector(MediaPackageElementFlavor flavor) {
    addFlavor(flavor);
  }

  /**
   * Specifies an explicit audio flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setAudioFlavor(String flavor) {
    if (flavor == null) {
      audioFlavor = null;
      return;
    }
    setAudioFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Specifies an explicit audio flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setAudioFlavor(MediaPackageElementFlavor flavor) {
    if (flavor != null)
      addFlavor(flavor);
    audioFlavor = flavor;
  }

  /**
   * Returns the explicit audio flavor or <code>null</code> if none was specified.
   * 
   * @return the audio flavor
   */
  public MediaPackageElementFlavor getAudioFlavor() {
    return audioFlavor;
  }

  /**
   * Returns a track or a number of tracks from the media package that together contain audio and video. If no such
   * combination can be found, e. g. there is no audio or video at all, an empty array is returned.
   * 
   * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public Collection<Track> select(MediaPackage mediaPackage) {
    // instead of relying on the broken superclass, we'll inspect every track
    // Collection<Track> candidates = super.select(mediaPackage);
    Collection<Track> candidates = Arrays.asList(mediaPackage.getTracks());
    Set<Track> result = new HashSet<Track>();

    boolean foundAudio = false;

    // Look for a track containing audio
    for (Track t : candidates) {
      if (TrackSupport.byType(t.getStreams(), AudioStream.class).length > 0) {
        if (!foundAudio && (audioFlavor == null || audioFlavor.equals(t.getFlavor()))) {
          result.add(t);
          foundAudio = true;
        }
      }
    }
    return result;
  }

}
