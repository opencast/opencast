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


package org.opencastproject.metadata.api.util;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.api.MediaPackageMetadata;

/**
 * This class contains useful methods to work with MediaPackageMetadata.
 */
public final class MediaPackageMetadataSupport {

  private MediaPackageMetadataSupport() {
  }

  /**
   * Updates all of the given MediaPackage's meta data with the given MediaPackageMetadata content.
   */
  public static void populateMediaPackageMetadata(MediaPackage mp, MediaPackageMetadata metadata) {
    if (metadata == null) {
      return;
    }

    // Series identifier
    if (isNotBlank(metadata.getSeriesIdentifier())) {
      mp.setSeries(metadata.getSeriesIdentifier());
    }

    // Series title
    if (isNotBlank(metadata.getSeriesTitle())) {
      mp.setSeriesTitle(metadata.getSeriesTitle());
    }

    // Episode title
    if (isNotBlank(metadata.getTitle())) {
      mp.setTitle(metadata.getTitle());
    }

    // Episode date
    if (metadata.getDate() != null) {
      mp.setDate(metadata.getDate());
    }

    // Episode subjects
    if (metadata.getSubjects().length > 0) {
      if (mp.getSubjects() != null) {
        for (String subject : mp.getSubjects()) {
          mp.removeSubject(subject);
        }
      }
      for (String subject : metadata.getSubjects()) {
        mp.addSubject(subject);
      }
    }

    // Episode contributers
    if (metadata.getContributors().length > 0) {
      if (mp.getContributors() != null) {
        for (String contributor : mp.getContributors()) {
          mp.removeContributor(contributor);
        }
      }
      for (String contributor : metadata.getContributors()) {
        mp.addContributor(contributor);
      }
    }

    // Episode creators
    if (mp.getCreators().length == 0 && metadata.getCreators().length > 0) {
      if (mp.getCreators() != null) {
        for (String creator : mp.getCreators()) {
          mp.removeCreator(creator);
        }
      }
      for (String creator : metadata.getCreators()) {
        mp.addCreator(creator);
      }
    }

    // Episode license
    if (isNotBlank(metadata.getLicense())) {
      mp.setLicense(metadata.getLicense());
    }

    // Episode language
    if (isNotBlank(metadata.getLanguage())) {
      mp.setLanguage(metadata.getLanguage());
    }

  }

}
