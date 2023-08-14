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

package org.opencastproject.smil.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.entity.api.Smil;

import java.io.File;

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
   * @return edited {@link Smil} and the new SmilMediaContainer
   * @throws SmilException
   */
  SmilResponse addParallel(Smil smil) throws SmilException;

  /**
   * Add new par element to {@link Smil} inside an element with given Id.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new par element
   * @return edited {@link Smil} and the new SmilMediaContainer
   * @throws SmilException if there is no element with given parentId
   */
  SmilResponse addParallel(Smil smil, String parentId) throws SmilException;

  /**
   * Add new seq element to {@link Smil}.
   *
   * @param smil {@link Smil} to edit
   * @return edited {@link Smil} and the new SmilMediaContainer
   * @throws SmilException
   */
  SmilResponse addSequence(Smil smil) throws SmilException;

  /**
   * Add new seq element to {@link Smil} inside an element with given Id.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new seq element
   * @return edited {@link Smil} and the new SmilMediaContainer
   * @throws SmilException if there is no element with given parentId
   */
  SmilResponse addSequence(Smil smil, String parentId) throws SmilException;

  /**
   * Add a SmilMediaElement based on given track and start/duration
   * information.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new SmilMediaElement
   * @param track {@link Track} to add as SmilMediaElement
   * @param start start position in {@link Track} in milliseconds
   * @param duration duration in milliseconds
   * @return edited {@link Smil}, the new SmilMediaElement and generated
   * meta data
   * @throws SmilException if there is no element with the given parentId
   */
  SmilResponse addClip(Smil smil, String parentId, Track track, long start, long duration) throws SmilException;

  /**
   * Add a SmilMediaElement based on given track and start/duration
   * information.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new SmilMediaElement
   * @param track {@link Track} to add as SmilMediaElement
   * @param start start position in {@link Track} in milliseconds
   * @param duration duration in milliseconds
   * @param paramGroupId clip should be added as a part of a previously created param group
   * @return edited {@link Smil}, the new SmilMediaElement and generated
   * meta data
   * @throws SmilException if there is no element with the given parentId
   */
  SmilResponse addClip(Smil smil, String parentId, Track track, long start, long duration, String paramGroupId)
          throws SmilException;

  /**
   * Add a list of SmilMediaElements based on given tracks and
   * start/duration information.
   *
   * @param smil {@link Smil} to edit
   * @param parentId element id, where to add new SmilMediaElements
   * @param tracks {@link Track}s to add as SmilMediaElements
   * @param start start position in {@link Track}s in milliseconds
   * @param duration duration in milliseconds
   * @return edited {@link Smil}, the new SmilMediaElements and tracks meta data
   * @throws SmilException if there is no element with the given parentId
   */
  SmilResponse addClips(Smil smil, String parentId, Track[] tracks, long start, long duration) throws SmilException;

  /**
   * Add a meta element to {@link Smil} head.
   *
   * @param smil {@link Smil} to edit
   * @param name meta name
   * @param content meta content
   * @return edited {@link Smil} and the new SmilMeta
   */
  SmilResponse addMeta(Smil smil, String name, String content);

  /**
   * Remove element (identified by elementId) from {@link Smil} if exists.
   *
   * @param smil {@link Smil} to edit
   * @param elementId element Id to remove
   * @return edited Smil and removed SmilMediaElement if {@link Smil} contains an element with given Id
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
