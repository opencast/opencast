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

package org.opencastproject.workflow.handler.distribution;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.opencastproject.mediapackage.MediaPackage;

import java.util.ArrayList;
import java.util.List;

/**
 * Criteria for whether a media package is eligible for publication to Engage, shared by
 * PublishEngageWorkflowOperationHandler and PartialRetractEngageWorkflowOperationHandler.
 */
final class EngagePublicationSupport {

  private EngagePublicationSupport() {
  }

  /**
   * Checks the media package against the criteria it must meet in order to be published, returning a description
   * of each unmet criterion, or an empty list if the media package is publishable.
   */
  static List<String> getPublicationCriteriaViolations(MediaPackage mp) {
    List<String> violations = new ArrayList<>();
    if (isBlank(mp.getTitle())) {
      violations.add("missing title");
    }
    if (!mp.hasTracks()) {
      violations.add("no tracks selected");
    }
    return violations;
  }
}
