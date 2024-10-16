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
package org.opencastproject.publication.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Publication;

public interface EngagePublicationService {

  String JOB_TYPE = "org.opencastproject.publication.engage";

  /**
   * Name constant for the 'merge' strategy
   */
  String PUBLISH_STRATEGY_MERGE = "merge";

  /**
   * Name constant for the 'default' 'strategy
   */
  String PUBLISH_STRATEGY_DEFAULT = "default";

  String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";

  /**
   * Path the REST endpoint which will re-direct users to the currently configured video player
   */
  String PLAYER_PATH = "/play/";

  /** Publish a media package to Engage */
  Job publish(MediaPackage mediaPackage, String checkAvailability, String strategy, String downloadSourceFlavors,
      String downloadSourceTags, String downloadTargetSubflavor, String downloadTargetTags,
      String streamingSourceFlavors, String streamingSourceTags, String streamingTargetSubflavor,
      String streamingTargetTags, String mergeForceFlavors, String addForceFlavors) throws PublicationException;

  /** Synchronous variant of {@link #publish} */
  Publication publishSync(MediaPackage mediaPackage, String checkAvailability, String strategy,
      String downloadSourceFlavors, String downloadSourceTags, String downloadTargetSubflavor,
      String downloadTargetTags, String streamingSourceFlavors, String streamingSourceTags,
      String streamingTargetSubflavor, String streamingTargetTags, String mergeForceFlavors, String addForceFlavors)
          throws PublicationException;
}
