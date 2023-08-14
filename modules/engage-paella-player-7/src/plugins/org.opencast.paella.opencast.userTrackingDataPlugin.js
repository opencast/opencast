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
import { DataPlugin, Events } from 'paella-core';

export default class OpencastUserTrackingDataPlugin extends DataPlugin {

  async isEnabled() {
    try {
      if (!(await super.isEnabled())) {
        return false;
      }
      const response = await fetch('/usertracking/detailenabled');
      const data = await response.text();
      const enabled = /true/i.test(data);
      return enabled;
    }
    catch(e) {
      return false;
    }
  }

  async write(context, { id }, data) {
    const currentTime = await this.player.videoContainer.currentTime();
    const playing = !(await this.player.videoContainer.paused());
    this.player.log.debug(`Logging event for video id ${ id } at time: ${ currentTime }`);

    const opencastLog = {
      id,
      type: data.event.substr(0,128),
      in: Math.round(currentTime),
      out: Math.round(currentTime),
      playing
    };

    switch (data.event) {
    case Events.PLAY:
      opencastLog.type = 'PLAY';
      break;
    case Events.PAUSE:
      opencastLog.type = 'PAUSE';
      break;
    case Events.SEEK:
      opencastLog.type = 'SEEK';
      opencastLog.in = Math.round(data.params.prevTime);
      break;
    case Events.FULLSCREEN_CHANGED:

      break;
    case Events.RESIZE_END:
      opencastLog.type = `RESIZE-TO-${ data.params.size.w }x${ data.params.size.h }`;
      break;
    case Events.BUTTON_PRESS:
      opencastLog.type += '-' + data.plugin;
      break;
    default:
      opencastLog.type += params;
    }

    const params = (new URLSearchParams(opencastLog)).toString();
    const requestUrl = `/usertracking/?_method=PUT&${ params }`;
    const result = await fetch(requestUrl);
    if (!result.ok) {
      this.player.log.error('Error in user data log');
    }
    else {
      this.player.log.debug(`Opencast user log event done: '${ opencastLog.type }'`);
    }
  }
}
