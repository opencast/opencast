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
  PopUpButtonPlugin,
  createElementWithHtmlText,
  translate,
  utils
} from 'paella-core';

import '../css/DescriptionPlugin.css';

import InfoIcon from '../icons/info.svg';
export default class DescriptionPlugin extends PopUpButtonPlugin {



  async getContent() {
    const metadata = this.player.videoManifest.metadata;

    const presenters = metadata.presenters
      ?.map((p) => `<a href="/engage/ui/index.html?q=${p}">${p}</a>`)
      ?.join(', ');
    const contributors = metadata.contributors
      ?.map((p) => `<a href="/engage/ui/index.html?q=${p}">${p}</a>`)
      ?.join(', ');
    const language = metadata.language
      ? (new Intl.DisplayNames([metadata.language], {type: 'language'}))
        .of(metadata.language)
      : '';

    const content = createElementWithHtmlText('<div class="description-plugin"></div>');
    createElementWithHtmlText(`
      <div class="row">
        <div class="key"> ${translate('Title')}: </div>
        <div class="value"> ${metadata.title || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Subject')}:</div>
        <div class="value">
          <a href="/engage/ui/index.html?q=${metadata.subject}">${metadata.subject || ''}</a>
         </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('Description')}:</div>
        <div class="value"> ${metadata.description || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('Language')}:</div>
        <div class="value"> ${language} </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('Rights')}:</div>
        <div class="value"> ${metadata.rights || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`        
      <div class="row">
        <div class="key">${translate('License')}:</div>
        <div class="value"> ${metadata.license || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('Series')}:</div>
        <div class="value">
          <a href="/engage/ui/index.html?epFrom=${metadata.series}">${metadata.seriestitle || ''}</a>
        </div>
      </div>
    `, content);
    createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Presenter(s)')}:</div>
        <div class="value"> ${presenters || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Contributor(s)')}:</div>
        <div class="value"> ${contributors || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Start date')}:</div>
        <div class="value"> ${(new Date(metadata.startDate)).toLocaleDateString()} </div>
      </div>
    `, content);
    createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Duration')}:</div>
        <div class="value"> ${utils.secondsToTime(metadata.duration) || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('Location')}:</div>
        <div class="value"> ${metadata.location || ''} </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('UID')}:</div>
        <div class="value"> 
          <a href="?id=${metadata.UID}">${metadata.UID}</a>
        </div>
      </div>
    `, content);
    createElementWithHtmlText(`    
      <div class="row">
        <div class="key">${translate('Views')}:</div>
        <div class="value"> ${metadata.views} </div>
      </div>      
    `, content);

    return content;
  }

  get popUpType() {
    return 'no-modal';
  }

  async load() {
    this.icon = InfoIcon;
  }
}
