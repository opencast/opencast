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
  parseWebVTT,
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
    const hasAllowedLicense = this.config.enableOnLicenses
      ? (this.config.enableOnLicenses.includes(metadata.license))
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
        tags: track?.tags?.tag ?? [],
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
    const log = this.player.log;
    function downloadTranscript(url) {
      return async function() {
        try {
          const response = await fetch(url);
          const vttData = await response.text();
          const vttCues = parseWebVTT(vttData);
          const vttTranscript = vttCues.cues.map(cue => cue.captions.join('\n')).join('\n');

          var element = document.createElement('a');
          element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(vttTranscript));
          element.setAttribute('download', url);

          element.style.display = 'none';
          document.body.appendChild(element);
          element.click();
          document.body.removeChild(element);
        }
        catch(e){
          log.error(`Error downloading transcript: ${e}`);
        }
      };
    }

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
        if (d.mimetype == 'text/vtt') {
          let cmeta = '';
          const lang_tag = d?.tags?.filter(x => x.startsWith('lang:'));
          if (lang_tag.length > 0) {
            const captions_lang = lang_tag[0].split(':')[1];
            const languageNames = new Intl.DisplayNames([window.navigator.language], {type: 'language'});
            cmeta = languageNames.of(captions_lang) || translate('Unknown language');
          }

          const elm = createElementWithHtmlText(`
            <li>
              <a href="${d.url}" download target="_blank">
                <span class="mimetype">[${d.mimetype}]</span><span class="res">${cmeta}</span>
              </a>
              <a href="#">
                <span class="transcript">[transcript]</span>
              </a>
            </li>
          `, list);

          elm.getElementsByTagName('a')[1].onclick = downloadTranscript(d.url);
        }
        else {
          const vmeta = d?.metadata?.video?.res;
          const ameta = d?.metadata?.audio?.samplingrate ? `${d?.metadata?.audio?.samplingrate} Hz` : null;

          const meta = vmeta ?? ameta ?? '';

          createElementWithHtmlText(`
            <li>
              <a href="${d.url}" download target="_blank">
                <span class="mimetype">[${d.mimetype}]</span><span class="res">${meta}</span>
              </a>
            </li>
          `, list);
        }
      });
    });
    return container;
  }
}
