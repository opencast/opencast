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
import OpencastAuthButtonPlugin from '../js/OpencastAuthButtonPlugin.js';

import LeadPencilIcon from '../icons/lead-pencil.svg';

export default class EditorPlugin extends OpencastAuthButtonPlugin {
  async load() {
    this.icon = LeadPencilIcon;
  }

  async isEnabled() {
    try {
      if (!(await super.isEnabled())) {
        return false;
      }
      else {
        if (this.config.editorUrl) {
          const userInfo = await this._getUserInfo();
          const isAnonymous = ((userInfo.roles.length == 1) && (userInfo.roles[0] == userInfo.org.anonymousRole));
          if (isAnonymous) {
            return (this.config.showIfAnonymous === true);
          }

          if (this.config.showIfCanWrite === true) {
            return await this._canWrite();
          }
          else {
            return true;
          }
        }
        return false;
      }
    }
    catch(_e) {
      return false;
    }
  }

  async action() {
    var editorUrl = this.config.editorUrl.replace('{id}', this.player.videoId);
    window.location.href = editorUrl;
  }
}
