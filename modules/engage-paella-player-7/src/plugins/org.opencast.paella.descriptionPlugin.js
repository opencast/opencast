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

import '../css/DescriptionPlugin.css';

import InfoIcon from '../icons/info.svg';
export default class DescriptionPlugin extends PopUpButtonPlugin {



  async getContent() {
    const metadata = this.player.videoManifest.metadata;

    const presenters = metadata.presenters
      ?.map((p) => {
        const elm = createElementWithHtmlText('<a href=""> </a>');
        elm.href = `/engage/ui/index.html?q=${p}`;
        elm.innerText = p;
        return elm.outerHTML;
      })
      ?.join(', ');
    const contributors = metadata.contributors
      ?.map((p) => {
        const elm = createElementWithHtmlText('<a href=""> </a>');
        elm.href = `/engage/ui/index.html?q=${p}`;
        elm.innerText = p;
        return elm.outerHTML;
      })
      ?.join(', ');
    const language = metadata.language
      ? (new Intl.DisplayNames([metadata.language], {type: 'language'}))
        .of(metadata.language)
      : '';

    const content = createElementWithHtmlText('<div class="description-plugin"></div>');
    const elm_title = createElementWithHtmlText(`
      <div class="row">
        <div class="key"> ${translate('Title')}: </div>
        <div class="value">  </div>
      </div>
    `, content);
    elm_title.querySelector('.value').innerText = metadata.title || '';
    const elm_subject = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Subject')}:</div>
        <div class="value">
          <a href=""></a>
         </div>
      </div>
    `, content);
    elm_subject.querySelector('.value a').innerText = metadata.subject || '';
    elm_subject.querySelector('.value a').href = `/engage/ui/index.html?q=${metadata.subject}`;

    const elm_description = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Description')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_description.querySelector('.value').innerText = metadata.description || '';
    const elm_language = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Language')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_language.querySelector('.value').innerText = language || '';
    const elm_rights = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Rights')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_rights.querySelector('.value').innerText = metadata.rights || '';

    const elm_license = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('License')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_license.querySelector('.value').innerText = metadata.license || '';
    const elm_series = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Series')}:</div>
        <div class="value">
          <a href=""> </a>
        </div>
      </div>
    `, content);
    elm_series.querySelector('.value a').href = `/engage/ui/index.html?epFrom=${metadata.series}`;
    elm_series.querySelector('.value a').innerText = metadata.seriestitle || '';
    const elm_presenters = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Presenter(s)')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_presenters.querySelector('.value').innerHTML = presenters || '';
    const elm_contributors = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Contributor(s)')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_contributors.querySelector('.value').innerHTML = contributors || '';
    const elm_startDate = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Start date')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_startDate.querySelector('.value').innerText = (new Date(metadata.startDate)).toLocaleDateString() || '';
    const elm_duration = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Duration')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_duration.querySelector('.value').innerText = utils.secondsToTime(metadata.duration) || '';
    const elm_location = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('Location')}:</div>
        <div class="value"> </div>
      </div>
    `, content);
    elm_location.querySelector('.value').innerText = metadata.location || '';
    const elm_uid = createElementWithHtmlText(`
      <div class="row">
        <div class="key">${translate('UID')}:</div>
        <div class="value"> 
          <a href=""></a>
        </div>
      </div>
    `, content);
    elm_uid.querySelector('.value a').href = `?id=${metadata.UID}`;
    elm_uid.querySelector('.value a').innerText = metadata.UID || '';
    if (metadata.views) {
      const elm_views = createElementWithHtmlText(`
        <div class="row">
          <div class="key">${translate('Views')}:</div>
          <div class="value"> </div>
        </div>      
      `, content);
      elm_views.querySelector('.value').innerText = metadata.views;
    }

    return content;
  }

  get popUpType() {
    return 'no-modal';
  }

  async load() {
    this.icon = this.player.getCustomPluginIcon(this.name, 'buttonIcon') || InfoIcon;
  }
}
