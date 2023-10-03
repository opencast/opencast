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

import '../css/DownloadsPlugin.css';
import defaultDownloadIcon from '../icons/download.svg';

export default class DownloadsPlugin extends PopUpButtonPlugin {

  async isEnabled() {
    this._downloads = {};
    const { metadata } = this.player.videoManifest;

    const hasWritePermission = this.config.enableOnWritePermission && await this.player.opencastAuth.canWrite();
    const hasAllowedLicense = this.config.enableOnlyLicenses
      ? (this.config.enableOnlyLicenses.includes(metadata.license))
      : true;
    const enabled = (await super.isEnabled()) && (hasWritePermission || hasAllowedLicense);

    if (enabled) {
      this._downloads = await this.getDownloadableContent();
    }

    return enabled && Object.keys(this._downloads).length > 0;
  }

  async load() {
    this.icon = this.player.getCustomPluginIcon(this.name, 'downloadIcon') || defaultDownloadIcon;
  }

  async getDownloadableContent() {
    const episode = await this.player.getEpisode({episodeId: this.player.videoId});
    const tracks = episode?.mediapackage?.media?.track ?? [];
    // const attachments = episode?.mediapackage?.attachments?.attachment ?? [];
    const downloadable = {};


    tracks.filter(track => {
      if (this.config.downloadFlavors) {
        const downloadFlavors = this.config.downloadFlavors?.includes(track.type);
        if (!downloadFlavors) {
          return false;
        }
      }

      if (this.config.downloadTags) {
        const tags = track?.tags?.tag ?? [];
        const downloadTags = this.config.downloadTags?.some(tag => {
          return tags.includes(tag);
        });
        if (!downloadTags) {
          return false;
        }
      }

      if (this.config.downloadMimeTypes) {
        const downloadMimeTypes = this.config.downloadMimeTypes?.includes(track.mimetype);
        if (!downloadMimeTypes) {
          return false;
        }
      }

      return true;
    }).forEach(track => {
      const vmeta = track?.video?.resolution ?
        {
          res: `${track?.video?.resolution}@${track?.video?.framerate}`,
          codec: track.video.encoder.type
        }
        : null;
      const ameta = track?.audio?.bitrate ?
        {
          bitrate: track?.audio?.bitrate,
          samplingrate: track?.audio?.samplingrate,
          channels: track?.audio?.channels,
          codec: track.audio.encoder.type
        }
        : null;
      const e = {
        type: track.type,
        mimetype: track.mimetype,
        url: track.url,
        metadata: {
          video: vmeta,
          audio: ameta
        }
      };
      if (!(e.type in downloadable)) {
        downloadable[e.type] = [];
      }
      downloadable[e.type].push(e);
    });

    return downloadable;
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
        const vmeta = d?.metadata?.video?.res;
        const ameta = d?.metadata?.audio?.samplingrate ? `${d?.metadata?.audio?.samplingrate} Hz` : null;
        const meta = vmeta ?? ameta ?? '';

        createElementWithHtmlText(`
                <li>
                  <a href="${d.src}" target="_blank">
                    <span class="mimetype">[${d.mimetype}]</span><span class="res">${meta}</span>
                  </a>
                </li>
            `, list);
      });
    });
    return container;
  }
}
