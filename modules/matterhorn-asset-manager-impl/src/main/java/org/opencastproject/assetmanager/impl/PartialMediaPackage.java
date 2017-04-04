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
package org.opencastproject.assetmanager.impl;

import static com.entwinemedia.fn.Stream.$;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;

import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Stream;

/**
 * Product of a media package and a media package element filter.
 * <p>
 * Get the filtered set of elements by calling {@link #getElements()}.
 * Get the full set of elements by calling {@link #getMediaPackage()}<code>.getElements()</code>.
 */
public class PartialMediaPackage {
  private final MediaPackage mediaPackage;
  private final Pred<MediaPackageElement> filter;

  public PartialMediaPackage(MediaPackage mediaPackage, Pred<MediaPackageElement> filter) {
    this.mediaPackage = mediaPackage;
    this.filter = filter;
  }

  public static PartialMediaPackage mk(MediaPackage mp, Pred<MediaPackageElement> filter) {
    return new PartialMediaPackage(mp, filter);
  }

  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  public Pred<MediaPackageElement> getPredicate() {
    return filter;
  }

  public Stream<MediaPackageElement> getElements() {
    return $(mediaPackage.getElements()).filter(filter);
  }
}
