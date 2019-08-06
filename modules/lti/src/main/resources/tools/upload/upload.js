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

/* global $, Mustache, i18ndata */

'use strict';

var defaultLang = i18ndata['en-US'],
    lang = defaultLang;

function matchLanguage(lang) {
  // break for too short codes
  if (lang.length < 2) {
    return defaultLang;
  }
  // Check for exact match
  if (lang in i18ndata) {
    return i18ndata[lang];
  }
  // Check if there is a more specific language (e.g. 'en-US' if 'en' is requested)
  for (const key of Object.keys(i18ndata)) {
    if (key.startsWith(lang)) {
      return i18ndata[key];
    }
  }
  // check if there is a less specific language
  return matchLanguage(lang.substring(0, lang.length - 1));
}

function i18n(key) {
  if (key in lang) {
    return lang[key];
  }
  return 'NO TRANSLATION FOUND FOR "' + key + '"';
}

function getParam(name) {
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.has(name)) {
    return urlParams.get(name);
  }
  return '';
}

function refreshTable() {
  $.getJSON(
    '/lti-service-gui/jobs?series_name=' + getParam('series_name') + '&series=' + getParam('series'),
    function ( eventList ) {
      var listTemplate = $('#template-upload-list').html();

      var translatedEvents = eventList.map(event => ({
        title: event.title,
        status: i18n(event.status) }));

      // render episode view
      $('#processed-table').html(
        Mustache.render(
          listTemplate,
          {
            events: translatedEvents,
            hasProcessing: eventList.length > 0,
            i18ncurrentJobs: i18n('CURRENT_JOBS'),
            i18ntitle: i18n('TITLE'),
            i18nstatus: i18n('STATUS')
          }));

      window.setTimeout(refreshTable, 5000);
    }
  );
}

function loadPage() {
  // load spinner
  $('upload-form').html($('#template-loading').html());

  var uploadTemplate = $('#template-upload-dialog').html(),
      tpldata = {
        seriesName: getParam('series_name'),
        series: getParam('series'),
        i18nnewUpload: i18n('NEW_UPLOAD'),
        i18ntitle: i18n('TITLE'),
        i18nstatus: i18n('STATUS'),
        i18ntitleDescription: i18n('TITLE_DESCRIPTION'),
        i18npresenter: i18n('PRESENTER'),
        i18npresenterDescription: i18n('PRESENTER_DESCRIPTION'),
        i18nupload: i18n('UPLOAD')
      };

  // render template
  $('#upload-form').html(Mustache.render(uploadTemplate, tpldata));

  refreshTable();
}


$(document).ready(function() {
  lang = matchLanguage(navigator.language);
  loadPage();
});
