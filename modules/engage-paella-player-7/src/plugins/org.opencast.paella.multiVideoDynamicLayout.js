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

import {VideoLayout} from 'paella-core';

export default class MultiVideoDynamicLayout extends VideoLayout {
  get identifier() {
    return 'multiple-video-dynamic';
  }

  get layoutType() {
    return 'dynamic';
  }

  async load() {
    this.player.log.debug('Multi video layout loaded');
  }

  getValidStreams(streamData) {
    // Ignore content of streamData
    return [streamData];
  }

  getValidContentIds() {
    // Ignore content of streamData
    return this.validContentIds;
  }

  getLayoutStructure(streamData) {
    if (!this._currentVideos) {
      const size = 100 / streamData.length;
      this._currentVideos = streamData.map(d => {
        return {
          content: d.content,
          visible: true,
          size: size,
        };
      });
    }

    return {
      hidden: false,
      videos: this._currentVideos
    };
  }
}
