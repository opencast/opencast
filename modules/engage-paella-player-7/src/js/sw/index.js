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

import { setUpServiceWorker } from '@opencast/jwtify';

// Get initial parameters (containing video ID and JWT)
const params = new URLSearchParams(self.location.search);
const jwt = params.get('jwt');
const videoId = params.get('videoId');

// Only of those are set do we actually register anything.
if (jwt && videoId) {
  const fetchJwts = async (eventIds) => {
    return new Map(eventIds.has(videoId) ? [[videoId, jwt]] : []);
  };

  setUpServiceWorker({
    getJwts: fetchJwts,
    trustedOcOrigins: ['https://stable.opencast.org', 'http://localhost:8080', 'http://localhost:8081'], // TODO
    debugLog: true, // TODO: remove
  });
}
