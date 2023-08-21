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
import { MatomoUserTrackingDataPlugin } from 'paella-user-tracking';
import { getUrlFromOpencastServer } from '../js/PaellaOpencast';

export default class OpencastMatomoUserTrackingDataPlugin extends MatomoUserTrackingDataPlugin {

  async getCurrentUserId() {
    try {
      if (this.config.logUserId) {

        if (this.opencast_username === undefined) {
          const data = await fetch(getUrlFromOpencastServer('/info/me.json'));
          const me = await data.json();
          if ((me?.roles?.length == 1) && (me.roles[0] == me.org.anonymousRole)) {
            this.opencast_username = null;
          }
          else {
            this.opencast_username = me.user.username;
          }
        }
        return this.opencast_username;
      }
    }
    catch(e) {
      this.player.log.debug('Error getting opencast username');
    }

    return null;
  }
}
