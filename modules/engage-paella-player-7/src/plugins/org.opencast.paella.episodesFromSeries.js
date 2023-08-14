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

import { getVideoPreview } from '../js/EpisodeConversor';

import ListIcon from '../icons/list.svg';

import '../css/EpisodesFromSeries.css';

export default class EpisodesFromSeriesPlugin extends PopUpButtonPlugin {

  async isEnabled() {
    try {
      if (!(await super.isEnabled())) {
        return false;
      }
      else {
        const { series } = this.player.videoManifest.metadata;
        if (!series) {
          return false;
        }
        const limit = this.config.maxCount || 5;
        const response = await fetch(`/search/episode.json?sid=${ series }&limit=${limit}`);
        if (response.ok) {
          this._episodesData = await response.json();
          return (this._episodesData['search-results'].total > 1);
        }
        return false;
      }
    }
    catch(_e) {
      return false;
    }
  }

  async load() {
    this.icon = ListIcon;
  }

  async getContent() {
    const {
      //series,
      seriestitle
    } = this.player.videoManifest.metadata;

    const container = createElementWithHtmlText(`
            <div class="episodes-from-series">
                <h4>${ translate('Videos in series') } ${ seriestitle }</h4>
            </div>
        `);
    const list = createElementWithHtmlText('<ul></ul>', container);

    const thisId = this.player.videoId;
    if (this._episodesData) {
      const result = this._episodesData['search-results'].result;
      (Array.isArray(result) ? result : [result]).forEach(({id,dcTitle,mediapackage}) => {
        if (id !== thisId) {
          const preview = getVideoPreview(mediapackage,this.player.config);
          const url = `watch.html?id=${id}`;
          createElementWithHtmlText(`
                  <li>
                      <a href="${url}">
                          <img src="${preview}" alt="${dcTitle}">
                          <span>${dcTitle}</span>
                      </a>
                  </li>
                  `,list);
        }
      });
    }
    return container;
  }
}
