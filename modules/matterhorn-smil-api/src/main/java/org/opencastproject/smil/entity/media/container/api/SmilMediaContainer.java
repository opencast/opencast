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
package org.opencastproject.smil.entity.media.container.api;

import java.util.List;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;

/**
 * Represent a media container like {@code par}, {@code seq},...
 */
public interface SmilMediaContainer extends SmilMediaObject {

  /**
   * SMIL media container type.
   */
  enum ContainerType {
    PAR, SEQ
  }

  /**
   * Returns the type of the container.
   *
   * @return container type
   */
  ContainerType getContainerType();

  /**
   * Returns media elements or containers inside as {@link List} of
   * {@link SmilMediaObject}s. The {@link List} is immutable, use
   * {@link SmilService} to modify it.
   *
   * @return
   */
  List<SmilMediaObject> getElements();

  /**
   * Returns true, if has a child element with same Id.
   *
   * @param childId child Id
   * @return true if has a child with same Id
   */
  boolean isParentOf(String childId);
}
