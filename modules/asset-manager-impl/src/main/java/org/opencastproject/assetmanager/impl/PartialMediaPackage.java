/*
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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Product of a media package and a media package element filter.
 * <p>
 * Get the filtered set of elements by calling {@link #getElements()}.
 */
public class PartialMediaPackage {
  private final MediaPackage mediaPackage;
  private final Predicate<MediaPackageElement> filter;

  public PartialMediaPackage(MediaPackage mediaPackage, Predicate<MediaPackageElement> filter) {
    this.mediaPackage = mediaPackage;
    this.filter = filter;
  }

  public static PartialMediaPackage mk(MediaPackage mp, Predicate<MediaPackageElement> filter) {
    return new PartialMediaPackage(mp, filter);
  }

  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  public Predicate<MediaPackageElement> getPredicate() {
    return filter;
  }

  public List<MediaPackageElement> getElements() {
    return Arrays.stream(mediaPackage.getElements())
        .filter(filter)
        .collect(Collectors.toList());
  }
}
