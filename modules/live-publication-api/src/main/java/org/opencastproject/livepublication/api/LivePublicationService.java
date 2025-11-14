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
package org.opencastproject.livepublication.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;

import java.util.Date;

public interface LivePublicationService {

  String LIVE_CHANNEL_ID = "engage-live";

  void createLiveEvent(MediaPackage archivedMediaPackage, Date startDate, Date endDate, String agentId)
          throws LivePublicationException;

  void updateLiveTracks(String mpId, Date startDate, Date endDate, String agentId) throws LivePublicationException,
          NotFoundException;

  void updateLiveEvent(MediaPackage archivedMediaPackage, String version) throws NotFoundException,
          LivePublicationException;

  void deleteLiveEvent(String mpId, boolean updateAssetManager) throws LivePublicationException;

  void handleCaptureError(String mpId);
}
