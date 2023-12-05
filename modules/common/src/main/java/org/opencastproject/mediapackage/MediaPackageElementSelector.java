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

package org.opencastproject.mediapackage;

import java.util.Collection;
import java.util.List;

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
   * @return the selected elements
   */
  Collection<T> select(MediaPackage mediaPackage, boolean withTagsAndFlavors);

  /**
   * Returns the media package elements that are matched by this selector.
   *
   * @param elements
   *          the elements to select from
   * @param withTagsAndFlavors
   *          define if the elements must match with flavors and tags, or just one of these parameters
   * @return the selected elements
   */
  Collection<T> select(List<MediaPackageElement> elements, boolean withTagsAndFlavors);

}
