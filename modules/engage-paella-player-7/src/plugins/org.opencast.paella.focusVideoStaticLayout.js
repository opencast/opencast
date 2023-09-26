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

import {VideoLayout, CanvasButtonPosition, utils} from 'paella-core';

import defaultIconMaximize from '../icons/maximize.svg';
import defaultIconMinimize from '../icons/minimize.svg';

export default class FocusVideoStaticLayout extends VideoLayout {
  get identifier() {
    return 'focus-video-static';
  }

  get layoutType() {
    return 'static';
  }

  async load() {
    this.player.log.debug('Focus video layout loaded');
  }

  getValidStreams(streamData) {
    // Ignore content of streamData
    return [streamData];
  }

  getValidContentIds() {
    // Ignore content of streamData
    return this.validContentIds;
  }

  async maximizeVideo(content) {
    this._focusedContent = content;
    await this.player.videoContainer.updateLayout();
  }

  async minimizeVideo() {
    if (this.player.videoContainer.validContentIds.indexOf('multi-video') === -1) {
      return;
    }

    await this.player.videoContainer.setLayout('multi-video');
  }

  getVideoCanvasButtons(layoutStructure, content) {
    const buttons = [];

    if (content !== this._focusedContent) {
      // Maximize
      buttons.push({
        icon: this.player.getCustomPluginIcon(this.name, 'iconMaximize') || defaultIconMaximize,
        position: CanvasButtonPosition.LEFT,
        title: this.player.translate('Maximize video'),
        ariaLabel: this.player.translate('Maximize video'),
        name: this.name + ':iconMaximize',
        click: async () => {
          await this.maximizeVideo(content);
        }
      });
    } else {
      // Minimize
      if (this.player.videoContainer.validContentIds.indexOf('multi-video') !== -1) {
        buttons.push({
          icon: this.player.getCustomPluginIcon(this.name, 'iconMinimize') || defaultIconMinimize,
          position: CanvasButtonPosition.LEFT,
          title: this.player.translate('Minimize video'),
          ariaLabel: this.player.translate('Minimize video'),
          name: this.name + ':iconMinimize',
          click: async () => {
            await this.minimizeVideo();
          }
        });
      }
    }

    return buttons;
  }

  getLayoutStructure(streamData) {
    // Check for focused content in cookie
    const cookieContent = utils.getCookie('focusContent');
    if (cookieContent !== '') {
      this._focusedContent = cookieContent;
      utils.setCookie('focusContent', '');
    }

    // Initially, focus on first stream
    if (!this._focusedContent) {
      this._focusedContent = streamData[0].content;
    }

    const numRightVideos = streamData.length - 1;
    const rightVideoHeight = Math.min(720 / numRightVideos, 180);
    const rightVideoMaxWidth = rightVideoHeight * 16 / 9;

    const focusVideoWidth = 1280 - rightVideoMaxWidth;

    let videos = [];
    let rightVideoIndex = 0;
    streamData.forEach(stream => {
      let rect;
      if (stream.content === this._focusedContent) {
        // Focus video
        rect = [
          {
            aspectRatio: '16/9',
            width: focusVideoWidth,
            height: focusVideoWidth * 9 / 16,
            top: (720 - focusVideoWidth * 9 / 16) / 2,
            left: 0
          },
          {
            aspectRatio: '16/10',
            width: focusVideoWidth,
            height: focusVideoWidth * 10 / 16,
            top: (720 - focusVideoWidth * 10 / 16) / 2,
            left: 0
          },
          {
            aspectRatio: '4/3',
            width: focusVideoWidth,
            height: focusVideoWidth * 3 / 4,
            top: (720 - focusVideoWidth * 3 / 4) / 2,
            left: 0
          },
          {
            aspectRatio: '5/3',
            width: focusVideoWidth,
            height: focusVideoWidth * 3 / 5,
            top: (720 - focusVideoWidth * 3 / 5) / 2,
            left: 0
          },
          {
            aspectRatio: '5/4',
            width: focusVideoWidth,
            height: focusVideoWidth * 4 / 5,
            top: (720 - focusVideoWidth * 4 / 5) / 2,
            left: 0
          }
        ];
      } else {
        // Right minimized Video
        const rightVideoTop = (720 - rightVideoHeight * numRightVideos) / 2 + rightVideoHeight * rightVideoIndex;
        rect = [
          {
            aspectRatio: '16/9',
            width: rightVideoHeight * 16 / 9,
            height: rightVideoHeight,
            top: rightVideoTop,
            left: focusVideoWidth
          },
          {
            aspectRatio: '16/10',
            width: rightVideoHeight * 16 / 10,
            height: rightVideoHeight,
            top: rightVideoTop,
            left: focusVideoWidth + (rightVideoMaxWidth - rightVideoHeight * 16 / 10) / 2
          },
          {
            aspectRatio: '4/3',
            width: rightVideoHeight * 4 / 3,
            height: rightVideoHeight,
            top: rightVideoTop,
            left: focusVideoWidth + (rightVideoMaxWidth - rightVideoHeight * 4 / 3) / 2
          },
          {
            aspectRatio: '5/3',
            width: rightVideoHeight * 5 / 3,
            height: rightVideoHeight,
            top: rightVideoTop,
            left: focusVideoWidth + (rightVideoMaxWidth - rightVideoHeight * 5 / 3) / 2
          },
          {
            aspectRatio: '5/4',
            width: rightVideoHeight * 5 / 4,
            height: rightVideoHeight,
            top: rightVideoTop,
            left: focusVideoWidth + (rightVideoMaxWidth - rightVideoHeight * 5 / 4) / 2
          }
        ];
        rightVideoIndex++;
      }

      videos.push({
        content: stream.content,
        rect: rect,
        visible: true,
        layer: 1
      });
    });

    return {
      name: {es: 'One focused video'},
      hidden: false,
      videos: videos,
    };
  }
}
