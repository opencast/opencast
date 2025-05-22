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
  PopUpButtonPlugin,
  createElementWithHtmlText,
  translate,
  utils
} from 'paella-core';

import '../css/TranscriptionsPlugin.css';

import TranscriptionsIcon from '../icons/transcriptions.svg';

export default class TranscriptionsPlugin extends PopUpButtonPlugin {

  get popUpType() {
    return 'no-modal';
  }

  get moveable() {
    return true;
  }

  get resizeable() {
    return true;
  }

  get closeActions() {
    return {
      clickOutside: false,
      closeButton: true
    };
  }

  get customPopUpClass() {
    return 'transcription-plugin-popup';
  }

  get menuTitle() {
    return 'Transcriptions';
  }

  async isEnabled() {
    const enabled = await super.isEnabled();
    this.transcriptions = this?.player?.videoManifest?.transcriptions?.filter(t => t?.text != '') || [];
    return enabled && this.transcriptions.length > 0;
  }

  async load() {
    this.icon = this.player.getCustomPluginIcon(this.name, 'buttonIcon') || TranscriptionsIcon;
  }


  rebuildList(search = '') {
    const { videoContainer } = this.player;
    this._transcriptionsContainer.innerHTML = '';
    this.transcriptions
    .filter(t => {  // filter trimming
      if (videoContainer.isTrimEnabled) {
        return (t.time > videoContainer.trimStart)
          && (t.time < videoContainer.trimEnd);
      }
      return true;
    })
    .filter(t => {  // filter search
      if (search !== '') {
        const searchExp = search.split(' ')
                    .map(s => `(?:${s})`)
                    .join('|');
        const re = new RegExp(searchExp, 'i');
        return re.test(t.text);
      }
      return true;
    })
    .forEach(t => {
      const id = `transcriptionItem${t.index}`;
      const trimmingOffset = videoContainer.isTrimEnabled ? videoContainer.trimStart : 0;
      const instant = t.time - trimmingOffset;
      const transcriptionItem = createElementWithHtmlText(`
        <li>
          <img id="${id}" src="${t.preview}" alt="${t.text}"/>
          <span><strong>${utils.secondsToTime(instant)}:</strong> ${t.text}</span>
        </li>`,
      this._transcriptionsContainer);
      transcriptionItem.addEventListener('click', async evt => {
        const trimmingOffset = videoContainer.isTrimEnabled ? videoContainer.trimStart : 0;
        this.player.videoContainer.setCurrentTime(t.time - trimmingOffset);
        evt.stopPropagation();
      });
    });
  }

  async getContent() {
    const container = createElementWithHtmlText('<div class="transcriptions-container"></div>');
    const searchContainer = createElementWithHtmlText(`
        <input type="search" placeholder="${translate('Search')}"></input>`, container);
    searchContainer.addEventListener('click', evt => evt.stopPropagation());
    searchContainer.addEventListener('keyup', evt => {
      this.rebuildList(evt.target.value);
    });
    const transcriptionsContainer = createElementWithHtmlText(`
        <ul class="transcriptions-list"></ul>`, container);
    this._transcriptionsContainer = transcriptionsContainer;
    this.rebuildList();
    return container;
  }
}
