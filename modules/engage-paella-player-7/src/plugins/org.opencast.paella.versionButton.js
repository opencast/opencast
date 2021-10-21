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
import {
  createElementWithHtmlText,
  PopUpButtonPlugin,
  translate
} from 'paella-core';

import '../css/OpencastPaellaVersionPlugin.css';

import TagIcon from '../icons/tag.svg';

export default class OpencastPaellaVersionPlugin extends PopUpButtonPlugin {
  async load() {
    this.icon = TagIcon;
  }

  async getContent() {
    const pluginVersionsHTML = this.player.version.pluginModules.map(p => {
      const i = p.split(':');
      return `<div class="row">
                <div class="component"> ${i[0].trim()} </div>
                <div class="version"> ${i[1].trim()}</div>
              </div>`;
    }).join('');
    const container = createElementWithHtmlText(`
        <div class="OpencastPaellaVersionPlugin">
            <h4>${translate('Opencast player version')}</h4>
            <div class="downloadStream">
              <div class="row">
                <div class="component"> ${translate('Opencast player')} </div>
                <div class="version"> ${this.player.version.player} </div>
              </div>
              <div class="paella-plugins">
                <div class="row">
                  <div class="component"> ${translate('Paella core version')} </div>
                  <div class="version"> ${this.player.version.coreLibrary} </div>
                </div>
                ${pluginVersionsHTML}
              </div>
            </div>
        </div>`);
    return container;
  }
}
