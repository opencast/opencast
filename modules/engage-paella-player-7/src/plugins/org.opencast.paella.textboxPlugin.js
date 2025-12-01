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

import { Events, EventLogPlugin, createElementWithHtmlText } from 'paella-core';
import '../css/TextboxPlugin.css';
import infoIcon from '../icons/info.svg';

export default class TextBoxPlugin extends EventLogPlugin {

  // Initialize
  async load() {
    this._textboxes = new Map(); // DOM elements
    this._textboxJSON = []; // Infos from the Opencast mediapackage

    // Create textbox container
    this._container = document.createElement('div');
    this._container.className = 'textbox-plugin-container';
    // Append to player-container to not dissappear during fullscreen
    document.querySelector('.player-container').appendChild(this._container);

    /* Demo json for quick developing purposes */
    // const myTestJson = [
    //   {
    //     start: 2000,
    //     text: 'Samalamadingdong',
    //   },
    //   {
    //     start: 3000,
    //     text: 'Get in the comments',
    //     link: 'https://opencast.org',
    //   }
    // ];
    // this._textboxJSON = myTestJson;
  }

  // Define which events we subscribe too
  get events() {
    return [
      Events.PLAYER_LOADED,
      Events.TIMEUPDATE
    ];
  }

  async onEvent(event, params) {
    // Load textbox info from Opencast
    if (event === Events.PLAYER_LOADED) {
      // TODO: Handle multiple textbox files
      const box = this.player?._videoManifest?.textboxes?.[0];
      if (box) {
        this._textboxJSON = await this.loadTextboxesFromOpencast(box.url);
      }
    }

    // Display/Hide textboxes
    this._textboxJSON.forEach((boxInfo, index) => {
      const start = (boxInfo.start / 1000);
      const end = (boxInfo.start / 1000) + 10;
      if (params.currentTime > start && params.currentTime < end && !this._textboxes.get(index)) {
        this.createBox(boxInfo, index);
      }
      if ((params.currentTime < start || params.currentTime > end) && this._textboxes.get(index)) {
        this.removeBox(boxInfo, index);
      }
    });

  }

  // Add a textbox to the DOM
  createBox(info, index) {
    if (!this._textboxes.get(index)) {
      let textbox = undefined;
      // if (info.link) {
      //   textbox = createElementWithHtmlText(`
      //     <a class="textbox-plugin-box" href=${ info.link }></a>
      //   `, this._container);
      // } else {
      textbox = createElementWithHtmlText(`
        <details class="textbox-plugin-box-details">
          <summary class="textbox-plugin-box-summary">
            <span class="left">
              <a href=${ info.link }>${ infoIcon }</a>
            </span>
            <span class="middle">${ info.text }</span>
            <span class="right"></span>
          </summary>
          ${ info.text }
        </details>
        `, this._container);
      // }

      // createElementWithHtmlText(`
      //   <details class="textbox-plugin-box">${ infoIcon }
      //     <summary class="textbox-plugin-box-text">
      //       <i class="textbox-plugin-box-icon">${ infoIcon }</i>
      //       ${ info.text }
      //     </summary>
      //     ${ info.text }
      //   </details>
      // `, textbox);

      // createElementWithHtmlText(`
      //   <i class="textbox-plugin-box-icon">${ infoIcon }</i>
      // `, textbox);

      // createElementWithHtmlText(`
      //   <span class="textbox-plugin-box-text">${ info.text }</span>
      // `, textbox);

      this._textboxes.set(index, textbox);
    }
  }

  // Remove a textbox from the DOM
  removeBox(info, index) {
    if (this._textboxes.get(index)) {
      this._container.removeChild(this._textboxes.get(index));
      this._textboxes.set(index, null);
    }
  }

  // Query Opencast for a json with info about textboxes
  loadTextboxesFromOpencast = async (url) => {
    let boxes = [];
    const response = await fetch(url);
    if (response.ok) {
      try {
        boxes = response.json();
      }
      catch (e) {
        this.player.log.warn('Error loading boxes');
      }
    }
    return boxes;
  };
}
