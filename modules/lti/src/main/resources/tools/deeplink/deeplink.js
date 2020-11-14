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

/* global $, axios, i18ndata, Mustache */
/* exported populateData, refreshEpisodesList, refreshSeriesList */

'use strict';

let currentpage,
    context_label,
    series,
    defaultLang = i18ndata['en-US'],
    lang = defaultLang,
    seriesRgbMax = new Array(220, 220, 220), //color range.
    seriesRgbOffset = new Array(20, 20, 20); //darkest possible color;

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

function loadLTIData() {
  return axios.get('/lti');
}

function loadEpisodeSearchInput(q) {
  // render series filter
  let episodesFilterTemplate = $('#template-episodes-filter').html(),
      episodesFilterTplData = {
        query: q,
      };
  $('#episodes-searchfield').html(Mustache.render(episodesFilterTemplate, episodesFilterTplData));
}

function loadSeriesSearchInput(q) {
  // render series filter
  let seriesFilterTemplate = $('#template-series-filter').html(),
      seriesFilterTplData = {
        query: q,
      };
  $('#series-searchfield').html(Mustache.render(seriesFilterTemplate, seriesFilterTplData));
}

function loadEpisodesTab(page, q) {

  let limit = 15,
      offset = (page - 1) * limit,
      series = getSeries(),
      url = '/search/episode.json?limit=' + limit + '&offset=' + offset;

  currentpage = page;

  // attach series query if a series is requested
  if (series) {
    url += '&sid=' + series;
    $('#series-tab').remove();
  } else { // if no series query is found query the episodes based on the context_label
    url += '&q=' + q;
    // no series parameter found, display the search field
    loadEpisodeSearchInput(q);
  }

  // load spinner
  $('#selections').html($('#template-loading').html());

  axios.get(url)
    .then((response) => {
      let data = response.data['search-results'];
      let rendered = '',
          results = [],
          total = parseInt(data.total);

      if (total > 0) {
        results = Array.isArray(data.result) ? data.result : [data.result];
      }

      for (let i = 0; i < results.length; i++) {
        let episode = results[i],
            i18ncreator,
            template = $('#template-episode').html(),
            tpldata,
            attachments;

        if(episode.dcCreator != null) {
          i18ncreator = Mustache.render(i18n('CREATOR'), {creator: episode.dcCreator});
        } else {
          i18ncreator = '';
        }

        tpldata = {
          tool: '/play/' + episode.id,
          displayTitle: episode.dcTitle,
          escapedTitle: episode.dcTitle.replace(/'/g, "\\'"),
          i18ncreator:  i18ncreator.split("by ")[1],
          created: tryLocalDate(episode.dcCreated).split(",")[0],
          seriestitle: episode.mediapackage.seriestitle,
          mpID: episode.id,
          color: generateSeriesColor(episode.mediapackage.series),
          duration: formatDuration(episode.mediapackage.duration)};

        // get preview image
        attachments = episode.mediapackage.attachments.attachment;
        attachments = Array.isArray(attachments) ? attachments : [attachments];
        for (let j = 0; j < attachments.length; j++) {
          if (attachments[j].type.endsWith('/player+preview')) {
            tpldata['image'] = attachments[j].url;
            break;
          }

          if (attachments[j].type.endsWith('/feed+preview')) {
            tpldata['image'] = attachments[j].url;
            break;
          }

          if (attachments[j].type.endsWith('/timeline+preview')) {
            tpldata['image'] = attachments[j].url;
            break;
          }
        }

        // render template
        rendered += Mustache.render(template, tpldata);
      }

      // render episode view
      $('#episodes-results').html(rendered);

      // render pagination
      $('#episodes-pager').pagination({
        dataSource: Array(total),
        pageSize: limit,
        pageNumber: currentpage,
        showNavigator: true,
        formatNavigator: '<%= currentPage %> / <%= totalPage %>, <%= totalNumber %> entries',
        callback: function(data, pagination) {
          if (pagination.pageNumber != currentpage) {
            loadEpisodesTab(pagination.pageNumber, q);
          }
        }
      });
    });
}

function loadSeriesTab(page, q) {
  let limit = 15,
      offset = (page - 1) * limit,
      url = '/search/series.json?limit=' + limit + '&offset=' + offset + '&q=' + q;

  currentpage = page;

  loadSeriesSearchInput();

  axios.get(url)
  .then((response) => {
    let data = response.data['search-results'],
        seriestool = 'ltitools/series/index.html?series=',
        rendered = '',
        results = [],
        total = parseInt(data.total);

    if (total > 0) {
      results = Array.isArray(data.result) ? data.result : [data.result];
    }

    for (let i = 0; i < results.length; i++) {
      let serie = results[i],
          template = $('#template-series').html(),
          tpldata = {
            tool: seriestool + serie.id,
            title: serie.dcTitle,
            created: tryLocalDate(serie.dcCreated),
            image: 'engage/ui/img/logo/opencast-icon.svg',
            color: generateSeriesColor(serie.id)};

      // render template
      rendered += Mustache.render(template, tpldata);
    }

    // render episode view
    $('#series-results').html(rendered);

    // render pagination
    $('#series-pager').pagination({
      dataSource: Array(total),
      pageSize: limit,
      pageNumber: currentpage,
      callback: function(data, pagination) {
        if (pagination.pageNumber != currentpage) {
          loadSeriesTab(pagination.pageNumber, q);
        }
      }
    });
  });
}

function populateData(title, image, created, tool, mpID) {
  // pass required data back to the server
  const urlParams = new URLSearchParams(window.location.search);
  $('#content_item_return_url').val(urlParams.get('content_item_return_url'));
  $('#consumer_key').val(urlParams.get('consumer_key'));
  if (urlParams.has('data')) {
    $('#data').val(urlParams.get('data'));
  }
  if (urlParams.has('test')) {
    $('#test').val(urlParams.get('test'));
  }

  // generate content_items
  let contentItems = {
    '@context': 'http://purl.imsglobal.org/ctx/lti/v1/ContentItem',
    '@graph': [{
      '@type': 'LtiLinkItem',
      '@id': mpID,
      mediaType: 'application/vnd.ims.lti.v1.ltilink',
      title: title,
      text: created,
      thumbnail: {'@id': image},
      url: window.location.protocol + '//' + window.location.host +  '/lti/player/' + mpID,
      custom: {
        tool: tool
      }
    }]
  };

  $('#content_items').val(JSON.stringify(contentItems).replace(/"/g, '"'));
  document.forms[0].submit();
}

function refreshEpisodesList() {
  let value = $('#selected-episodes').val();
  loadEpisodesTab(1, value);
}

function refreshSeriesList() {
  let value = $('#selected-series').val();
  loadSeriesTab(1, value);
}

function generateSeriesColor(id) {

  if (id == null) {
    return '#fff';
  }

  let rgb = new Array(0, 0, 0);

  for (let i = 0; i < id.length; ++i) {
    rgb[(i % 3)] += id.charCodeAt(i);
  }

  for (let i = 0; i < 3; ++i) {
    rgb[i] = ((rgb[i] % seriesRgbMax[i]) + seriesRgbOffset[i]).toString(16);
    if (rgb[i].length < 1) {
      rgb[i] = '0' + rgb[i];
    }
  }

  return '#' + rgb[0] + rgb[1] + rgb[2];
}

function formatDuration(duration) {
  let seconds = Math.floor((duration / 1000) % 60),
      minutes = Math.floor((duration / (1000 * 60) % 60)),
      hours = Math.floor((duration / (1000 * 60 * 60) % 60));

  if (seconds < 10) {
    seconds = '0' + seconds;
  }
  if (minutes < 10) {
    minutes = '0' + minutes;
  }
  if (hours < 10) {
    hours = '0' + hours;
  }

  return hours + ':' + minutes + ':' + seconds;
}

lang = matchLanguage(navigator.language);
axios.all([loadLTIData()])
  .then(axios.spread( function (ltidata) {
    context_label = ltidata.data.context_label;
    series = getSeries();
    loadEpisodesTab(1, context_label);
    if(!series) {
      loadSeriesTab(1, context_label);
    }
  })
  );
