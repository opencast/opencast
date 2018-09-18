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

package org.opencastproject.publication.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Publication;

import java.util.Collection;
import java.util.Set;

public interface ConfigurablePublicationService {

  String JOB_TYPE = "org.opencastproject.publication.configurable";

  /**
   * Replaces media package elements.
   * 
   * @param mediaPackage
   *          the media package
   * @param channelId
   *          the id of the publication channel
   * @param addElements
   *          the media package elements to be added
   * @param retractElementIds
   *          the ids of the media package elements to be removed
   * @return The job
   * @throws PublicationException
   *           if there was a problem publishing the media
   * @throws MediaPackageException
   *           if there was a problem with the mediapackage element
   */
  Job replace(MediaPackage mediaPackage, String channelId, Collection<? extends MediaPackageElement> addElements,
          Set<String> retractElementIds) throws PublicationException, MediaPackageException;

  /**
   * Synchronously replaces media package elements.
   *
   * @param mediaPackage
   *          the media package
   * @param channelId
   *          the id of the publication channel
   * @param addElements
   *          the media package elements to be added
   * @param retractElementIds
   *          the ids of the media package elements to be removed
   * @return The publication with the updated media package.
   * @throws PublicationException
   *           if there was a problem publishing the media
   * @throws MediaPackageException
   *           if there was a problem with the mediapackage element
   */
  Publication replaceSync(MediaPackage mediaPackage, String channelId, Collection<? extends MediaPackageElement> addElements,
                  Set<String> retractElementIds) throws PublicationException, MediaPackageException;
}
