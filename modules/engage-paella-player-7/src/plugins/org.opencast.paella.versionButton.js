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
import {
  createElementWithHtmlText,
  PopUpButtonPlugin,
  translate
} from 'paella-core';

import '../css/OpencastPaellaVersionPlugin.css';

import TagIcon from '../icons/tag.svg';

export default class OpencastPaellaVersionPlugin extends PopUpButtonPlugin {
  async load() {
    this.icon = this.player.getCustomPluginIcon(this.name, 'buttonIcon') || TagIcon;
  }

  async getContent() {
    const pluginVersionsHTML = this.player.version.pluginModules.map(p => {
      const i = p.split(':');
      const c =  createElementWithHtmlText(`<div class="row">
                <div class="component"> </div>
                <div class="version"> </div>
              </div>`);
      c.querySelector('div.component').innerText = i[0].trim();
      c.querySelector('div.version').innerText = i[1].trim();
      return c.outerHTML;
    }).join('');

    const row_oc = createElementWithHtmlText(`<div class="row">
      <div class="component"> ${translate('Opencast player')} </div>
      <div class="version"> </div>
    </div>`);
    row_oc.querySelector('.version').innerText = this.player.version.player;

    const row_core = createElementWithHtmlText(`<div class="row">
      <div class="component"> ${translate('Paella core version')} </div>
      <div class="version"> </div>
    </div>`);
    row_core.querySelector('.version').innerText = this.player.version.coreLibrary;


    const container = createElementWithHtmlText(`
        <div class="OpencastPaellaVersionPlugin">
            <h4>${translate('Opencast player version')}</h4>
            <div class="downloadStream">
              ${row_oc.outerHTML}
              <div class="paella-plugins">
                ${row_core.outerHTML}
                ${pluginVersionsHTML}
              </div>
            </div>
        </div>`);
    return container;
  }
}
