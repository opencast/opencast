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
package org.opencastproject.smil.api;

import java.io.File;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.entity.api.Smil;

/**
 * {@link SmilService} provides {@link Smil} manipulation.
 */
public interface SmilService {

  /**
   * Create a new {@link Smil}.
   *
   * @return a new {@link Smil}
   */
  SmilResponse createNewSmil();

  /**
   * Create a new {@link Smil} and store the {@link MediaPackage} Id as meta
   * data.
   *
   * @param mediaPackage
   * @return a new {@link Smil}
   */
  SmilResponse createNewSmil(MediaPackage mediaPackage);

  /**
   * Add new par element to {@link Smil}.
   *
   * @param smil {@link Smil} to edit
   * @return edited {@link Smil} and the new {@link SmilMediaContainer}
   * @throws SmilException
   */
  SmilResponse addParallel(Smil smil) throws SmilException;

  /**
   * Add new par element to {@link Smil} inside an element with given Id.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new par element
   * @return edited {@link Smil} and the new {@link SmilMediaContainer}
   * @throws SmilException if there is no element with given parentId
   */
  SmilResponse addParallel(Smil smil, String parentId) throws SmilException;

  /**
   * Add new seq element to {@link Smil}.
   *
   * @param smil {@link Smil} to edit
   * @return edited {@link Smil} and the new {@link SmilMediaContainer}
   * @throws SmilException
   */
  SmilResponse addSequence(Smil smil) throws SmilException;

  /**
   * Add new seq element to {@link Smil} inside an element with given Id.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new seq element
   * @return edited {@link Smil} and the new {@link SmilMediaContainer}
   * @throws SmilException if there is no element with given parentId
   */
  SmilResponse addSequence(Smil smil, String parentId) throws SmilException;

  /**
   * Add a {@link SmilMediaElement} based on given track and start/duration
   * information.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new {@link SmilMediaElement}
   * @param track {@link Track} to add as {@link SmilMediaElement}
   * @param start start position in {@link Track} in milliseconds
   * @param duration duration in milliseconds
   * @return edited {@link Smil}, the new {@link SmilMediaElement} and generated
   * meta data
   * @throws SmilException if there is no element with the given parentId
   */
  SmilResponse addClip(Smil smil, String parentId, Track track, long start, long duration) throws SmilException;

  /**
   * Add a {@link SmilMediaElement} based on given track and start/duration
   * information.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new {@link SmilMediaElement}
   * @param track {@link Track} to add as {@link SmilMediaElement}
   * @param start start position in {@link Track} in milliseconds
   * @param duration duration in milliseconds
   * @param paramGroupId clip should be added as a part of a previously created param group
   * @return edited {@link Smil}, the new {@link SmilMediaElement} and generated
   * meta data
   * @throws SmilException if there is no element with the given parentId
   */
  SmilResponse addClip(Smil smil, String parentId, Track track, long start, long duration, String paramGroupId)
          throws SmilException;

  /**
   * Add a list of {@link SmilMediaElement}s based on given tracks and
   * start/duration information.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new {@link SmilMediaElement}s
   * @param tracks {@link Track}s to add as {@link SmilMediaElement}s
   * @param start start position in {@link Track}s in milliseconds
   * @param duration duration in milliseconds
   * @return edited {@link Smil}, the new {@link SmilMediaElement}s and tracks
   * meta data
   * @throws SmilException if there is no element with the given parentId
   */
  SmilResponse addClips(Smil smil, String parentId, Track[] tracks, long start, long duration) throws SmilException;

  /**
   * Add a meta element to {@link Smil} head.
   *
   * @param smil {@link Smil} to edit
   * @param name meta name
   * @param content meta content
   * @return edited {@link Smil} and the new {@link SmilMeta}
   */
  SmilResponse addMeta(Smil smil, String name, String content);

  /**
   * Remove element (identified by elementId) from {@link Smil} if exists.
   *
   * @param smil {@link Smil} to edit
   * @param elementId element Id to remove
   * @return edited {@link Smil} and removed {@link SmilMediaElement} if
   * {@link Smil} contains an element with given Id
   */
  SmilResponse removeSmilElement(Smil smil, String elementId);

  /**
   * Returns {@link Smil} from Xml {@code String}.
   *
   * @param smilXml Smil document Xml as {@code String}
   * @return parsed {@link Smil}
   * @throws SmilException if an error occures while parsing {@link Smil}
   */
  SmilResponse fromXml(String smilXml) throws SmilException;

  /**
   * Returns {@link Smil} from Xml {@code File}.
   *
   * @param smilXmlFile Smil document Xml as {@code File}
   * @return parsed {@link Smil}
   * @throws SmilException if an error occures while parsing {@link Smil}
   */
  SmilResponse fromXml(File smilXmlFile) throws SmilException;
}
