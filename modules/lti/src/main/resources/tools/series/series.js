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

const player = '/play/';

var currentpage,
    defaultLang = i18ndata['en-US'],
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

function tryLocalDate(date) {
  try {
    return new Date(date).toLocaleString();
  } catch(err) {
    return date;
  }
}

function i18n(key) {
  return lang[key];
}

function getSeries() {
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.has('series')) {
    return urlParams.get('series');
  }
  return '';
}

function loadPage(page) {

  var limit = 15,
      offset = (page - 1) * limit,
      series = getSeries(),
      url = '/search/episode.json?limit=' + limit + '&offset=' + offset;

  currentpage = page;

  // attach series query if a series is requested
  if (series) {
    url += '&sid=' + series;
  }

  // load spinner
  $('main').html($('#template-loading').html());

  $.getJSON(url, function( data ) {
    data = data['search-results'];
    var rendered = '',
        results = [],
        total = parseInt(data.total);

    if (total > 0) {
      results = Array.isArray(data.result) ? data.result : [data.result];
    }

    for (var i = 0; i < results.length; i++) {
      var episode = results[i],
          i18ncreator = Mustache.render(i18n('CREATOR'), {creator: episode.dcCreator}),
          template = $('#template-episode').html(),
          tpldata = {
            player: player + episode.id,
            title: episode.dcTitle,
            i18ncreator: i18ncreator,
            created: tryLocalDate(episode.dcCreated)};

      // get preview image
      var attachments = episode.mediapackage.attachments.attachment;
      attachments = Array.isArray(attachments) ? attachments : [attachments];
      for (var j = 0; j < attachments.length; j++) {
        if (attachments[j].type.endsWith('/search+preview')) {
          tpldata['image'] = attachments[j].url;
          break;
        }
      }

      // render template
      rendered += Mustache.render(template, tpldata);
    }

    // render episode view
    $('main').html(rendered);

    // render result information
    var resultTemplate = i18n('RESULTS'),
        resultTplData = {
          total: total,
          range: {
            begin: Math.min(offset + 1, total),
            end: offset + parseInt(data.limit)
          }
        };
    $('header').text(Mustache.render(resultTemplate, resultTplData));

    // render pagination
    $('footer').pagination({
      dataSource: Array(total),
      pageSize: limit,
      pageNumber: currentpage,
      callback: function(data, pagination) {
        if (pagination.pageNumber != currentpage) {
          loadPage(pagination.pageNumber);
        }
      }
    });

  });

}

$(document).ready(function() {
  lang = matchLanguage(navigator.language);
  loadPage(1);
});
