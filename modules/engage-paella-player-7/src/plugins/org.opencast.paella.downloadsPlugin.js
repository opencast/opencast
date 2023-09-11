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
import PaellaOpencastPlugins from './PaellaOpencastPlugins';

import '../css/DownloadsPlugin.css';

import defaultDownloadIcon from '../icons/download.svg';

export default class DownloadsPlugin extends PopUpButtonPlugin {
  getPluginModuleInstance() {
    return PaellaOpencastPlugins.Get();
  }

  async isEnabled() {
    this._downloads = {};
    const { streams, metadata } = this.player.videoManifest;

    const hasWritePermission = this.config.allowWritePermission && await this.player.opencastAuth.canWrite();
    const hasAllowedLicense = this.config.allowOnlyLicenses
      ? (this.config.allowOnlyLicenses.includes(metadata.license))
      : true;
    const enabled = (await super.isEnabled()) && (hasWritePermission || hasAllowedLicense);

    if (enabled) {
      streams.forEach(s => {
        let streamDownloads = [];
        const { mp4 } = s.sources;
        if (mp4) {
          mp4.forEach(v => {
            streamDownloads.push({
              id: `${s.content}_${v.res?.w || 0}_${v.res?.h || 0}`,
              src: v.src,
              res: v.res || { w: 0, h: 0 },
              mimetype: v.mimetype
            });
          });
        }
        if (streamDownloads.length > 0) {
          this._downloads[s.content] = streamDownloads;
        }
      });
    }

    return enabled && Object.keys(this._downloads).length > 0;
  }

  async load() {
    this.icon = this.player.getCustomPluginIcon(this.name, 'downloadIcon') || defaultDownloadIcon;
  }

  async getContent() {
    const container = createElementWithHtmlText(`
        <div class="downloads-plugin">
            <h4>${translate('Available downloads')}</h4>
        </div>`);
    const downloadKeys = Object.keys(this._downloads);
    downloadKeys.forEach(k => {
      const J = createElementWithHtmlText(`
        <div class="downloadStream">
          <div class="title">${k}</div>
        </div>`, container);
      const list = createElementWithHtmlText('<ul></ul>', J);
      const streamDownloads = this._downloads[k];
      streamDownloads.forEach(d => {
        const res = `${d.res.w}x${d.res.h}`;
        createElementWithHtmlText(`
                <li>
                  <a href="${d.src}" target="_blank">
                    <span class="mimetype">[${d.mimetype}]</span><span class="res">${res}</span>
                  </a>
                </li>
            `, list);
      });
    });
    return container;
  }
}
