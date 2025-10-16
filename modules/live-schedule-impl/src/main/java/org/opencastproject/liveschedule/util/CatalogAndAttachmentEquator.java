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
package org.opencastproject.liveschedule.util;

import org.opencastproject.mediapackage.MediaPackageElement;

import org.apache.commons.collections4.Equator;

import java.util.Objects;

public class CatalogAndAttachmentEquator implements Equator<MediaPackageElement> {
  @Override
  public boolean equate(MediaPackageElement mpe1, MediaPackageElement mpe2) {
    return Objects.equals(mpe1.getIdentifier(), mpe2.getIdentifier()) && Objects.equals(mpe1.getElementType(),
        mpe2.getElementType()) && Objects.equals(mpe1.getChecksum(), mpe2.getChecksum()) && Objects.equals(
        mpe1.getFlavor(), mpe2.getFlavor());
  }

  @Override
  public int hash(MediaPackageElement mpe) {
    return Objects.hash(mpe.getIdentifier(), mpe.getElementType(), mpe.getChecksum(), mpe.getFlavor());
  }
}
