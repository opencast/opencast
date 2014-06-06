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
package org.opencastproject.mediapackage;

import java.util.Collection;

/**
 * A <code>MedikaPackageElementSelector</code> is the way to set up rules for extracting elements from a media package
 * dependent on their flavor.
 */
public interface MediaPackageElementSelector<T extends MediaPackageElement> {

  /**
   * Returns the media package elements that are matched by this selector.
   *
   * @param mediaPackage
   *          the media package
   * @param withTagsAndFlavors
   *          define if the elements must match with flavors and tags, or just one of these parameters
   * @return the selected element
   */
  Collection<T> select(MediaPackage mediaPackage, boolean withTagsAndFlavors);

}
