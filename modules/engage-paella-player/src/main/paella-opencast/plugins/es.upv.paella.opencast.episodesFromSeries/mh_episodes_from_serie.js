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

/*global Class*/
paella.addPlugin(function() {
  return class EpisodesFromSeries extends paella.ButtonPlugin {
    getSubclass() { return 'EpisodesFromSeries'; }
    getName() { return 'es.upv.paella.opencast.episodesFromSeries'; }

    getIndex() { return 10; }
    getDefaultToolTip() { return paella.dictionary.translate('Related Videos'); }


    getAlignment() { return 'right'; }
    getButtonType() { return paella.ButtonPlugin.type.popUpButton; }


    checkEnabled(onSuccess) {
      var self = this;
      paella.opencast.getEpisode()
      .then(function(episode) {
        self._episode = episode;
        if (episode.mediapackage.series) {
          onSuccess(true);
        }
        else {
          onSuccess(false);
        }
      })
      .catch(function() { onSuccess(false); });
    }

    buildContent(domElement) {
      var self = this;
      var serieId = self._episode.mediapackage.series;
      var serieTitle = self._episode.mediapackage.seriestitle;



      var episodesFromSeriesTitle = document.createElement('div');
      episodesFromSeriesTitle.id = 'episodesFromSeriesTitle';
      episodesFromSeriesTitle.className = 'episodesFromSeriesTitle';
      if (serieId) {
        episodesFromSeriesTitle.innerHTML = '<span class=\'episodesFromSeriesTitle_Bold\'>' +paella.dictionary.translate('Videos in this series:')+'</span> ' + serieTitle;
      }
      else {
        episodesFromSeriesTitle.innerHTML = '<span class=\'episodesFromSeriesTitle_Bold\'>' +paella.dictionary.translate('Available videos:')+'</span>';
      }

      var episodesFromSeriesListing = document.createElement('div');
      episodesFromSeriesListing.id = 'episodesFromSeriesListing';
      episodesFromSeriesListing.className = 'episodesFromSeriesListing';


      domElement.appendChild(episodesFromSeriesTitle);
      domElement.appendChild(episodesFromSeriesListing);


      var params = {limit:5, page:0, sid:serieId};
      var mySearch = new SearchEpisode(paella.player.config, params);
      mySearch.doSearch(params, document.getElementById('episodesFromSeriesListing'));
    }
  };
});



/************************************************************************************/

var SearchEpisode = Class.create({
  config:null,
  proxyUrl:'',
  recordingEntryID:'',
  useJsonp:false,
  divLoading:null,
  divResults:null,

  AsyncLoaderPublishCallback: Class.create(paella.AsyncLoaderCallback,{
    config:null,
    recording:null,

    initialize:function(config, recording) {
      this.parent('AsyncLoaderPublishCallback');
      this.config = config;
      this.recording = recording;
    },

    load:function(onSuccess,onError) {
      var thisClass = this;

      paella.data.read('publish',{id:this.recording.id},function(data,status) {
        if (status == true) {
          if ((data == true) || (data == 'True')) {
            thisClass.recording.entry_published_class = 'published';
          }
          else if ((data == false) || (data == 'False')) {
            thisClass.recording.entry_published_class = 'unpublished';
          }
          else if (data == 'undefined'){
            thisClass.recording.entry_published_class = 'pendent';
          }
          else {
            thisClass.recording.entry_published_class = 'no_publish_info';
          }
          onSuccess();
        }
        else {
          thisClass.recording.entry_published_class = 'no_publish_info';
          onSuccess();
        }
      });
    }
  }),

  createDOMElement:function(type, id, className) {
    var elem = document.createElement(type);
    elem.id = id;
    elem.className = className;
    return elem;
  },

  doSearch:function(params, domElement) {
    var thisClass = this;
    this.recordingEntryID =	 domElement.id + '_entry_';


    domElement.innerHTML = '';
    // loading div
    this.divLoading = this.createDOMElement('div', thisClass.recordingEntryID + '_loading', 'recordings_loading');
    this.divLoading.innerHTML = paella.dictionary.translate('Searching...');
    domElement.appendChild(this.divLoading);

    // header div
    var divHeader = this.createDOMElement('div', thisClass.recordingEntryID + '_header', 'recordings_header');
    domElement.appendChild(divHeader);
    this.divResults = this.createDOMElement('div', thisClass.recordingEntryID + '_header_results', 'recordings_header_results');
    divHeader.appendChild(this.divResults);
    var divNavigation = this.createDOMElement('div', thisClass.recordingEntryID + '_header_navigation', 'recordings_header_navigation');
    divHeader.appendChild(divNavigation);

    // loading results
    thisClass.setLoading(true);
    paella.ajax.get({url:'/search/episode.json', params:params},
      function(data, contentType, returnCode, dataRaw) {
        thisClass.processSearchResults(data, params, domElement, divNavigation);
      },
      function(data, contentType, returnCode) {
      }
    );
  },


  processSearchResults:function(response, params, divList, divNavigation) {
    var thisClass = this;
    if (typeof(response)=='string') {
      response = JSON.parse(response);
    }

    var resultsAvailable = (response !== undefined) &&
      (response['search-results'] !== undefined) &&
      (response['search-results'].total !== undefined);

    if (resultsAvailable === false) {
      paella.debug.log('Seach failed, respons:  ' + response);
      return;
    }


    var totalItems = parseInt(response['search-results'].total);

    if (totalItems === 0) {
      if (params.q === undefined) {
        thisClass.setResults('No recordings');
      } else {
        thisClass.setResults('No recordings found: "' + params.q + '"');
      }
    } else {
      var offset = parseInt(response['search-results'].offset);
      var limit = parseInt(response['search-results'].limit);

      var startItem = offset;
      var endItem = offset + limit;
      if (startItem < endItem) {
        startItem = startItem + 1;
      }

      if (params.q === undefined) {
        thisClass.setResults('Results ' + startItem + '-' + endItem + ' of ' + totalItems);
      } else {
        thisClass.setResults('Results ' + startItem + '-' + endItem + ' of ' + totalItems + ' for "' + params.q + '"');
      }


      // *******************************
      // *******************************
      // TODO
      var asyncLoader = new paella.AsyncLoader();
      var results = response['search-results'].result;
      if (!(results instanceof Array)) { results = [results]; }
      //There are annotations of the desired type, deleting...
      for (var i =0; i < results.length; ++i ){
        asyncLoader.addCallback(new thisClass.AsyncLoaderPublishCallback(thisClass.config, results[i]));
      }

      asyncLoader.load(function() {
        var i;
        // create navigation div
        if (results.length < totalItems) {
          // current page
          var currentPage = 1;
          if (params.offset !== undefined) {
            currentPage = (params.offset / params.limit) + 1;
          }

          // max page
          var maxPage = parseInt(totalItems / params.limit);
          if (totalItems % 10 != 0) maxPage += 1;
          maxPage =  Math.max(1, maxPage);


          // previous link
          var divPrev = document.createElement('div');
          divPrev.id = thisClass.recordingEntryID + '_header_navigation_prev';
          divPrev.className = 'recordings_header_navigation_prev';
          if (currentPage > 1) {
            var divPrevLink = document.createElement('a');
            divPrevLink.param_offset = (currentPage - 2) * params.limit;
            divPrevLink.param_limit	= params.limit;
            divPrevLink.param_q = params.q;
            divPrevLink.param_sid = params.sid;
            $(divPrevLink).click(function(event) {
              var params = {};
              params.offset = this.param_offset;
              params.limit = this.param_limit;
              params.q = this.param_q;
              params.sid = this.param_sid;
              thisClass.doSearch(params, divList);
            });
            divPrevLink.innerHTML = paella.dictionary.translate('Previous');
            divPrev.appendChild(divPrevLink);
          } else {
            divPrev.innerHTML = paella.dictionary.translate('Previous');
          }
          divNavigation.appendChild(divPrev);

          var divPage = document.createElement('div');
          divPage.id = thisClass.recordingEntryID + '_header_navigation_page';
          divPage.className = 'recordings_header_navigation_page';
          divPage.innerHTML = paella.dictionary.translate('Page:');
          divNavigation.appendChild(divPage);

          // take care for the page buttons
          var spanBeforeSet = false;
          var spanAfterSet = false;
          var offsetPages = 2;
          for (i = 1; i <= maxPage; i++)	{
            var divPageId = document.createElement('div');
            divPageId.id = thisClass.recordingEntryID + '_header_navigation_pageid_'+i;
            divPageId.className = 'recordings_header_navigation_pageid';

            if (!spanBeforeSet && currentPage >= 5 && i > 1 && (currentPage - (offsetPages + 2) != 1)) {
              divPageId.innerHTML = '...';
              i = currentPage - (offsetPages + 1);
              spanBeforeSet = true;
            }
            else if (!spanAfterSet && (i - offsetPages) > currentPage && maxPage - 1 > i && i > 4) {
              divPageId.innerHTML = '...';
              i = maxPage - 1;
              spanAfterSet = true;
            }
            else {
              if (i !== currentPage) {
                var divPageIdLink = document.createElement('a');
                divPageIdLink.param_offset = (i -1) * params.limit;
                divPageIdLink.param_limit = params.limit;
                divPageIdLink.param_q = params.q;
                divPageIdLink.param_sid = params.sid;
                $(divPageIdLink).click(function(event) {
                  var params = {};
                  params.offset = this.param_offset;
                  params.limit = this.param_limit;
                  params.q = this.param_q;
                  params.sid = this.param_sid;
                  thisClass.doSearch(params, divList);
                });
                divPageIdLink.innerHTML = i;
                divPageId.appendChild(divPageIdLink);
              } else {
                divPageId.innerHTML = i;
              }
            }
            divNavigation.appendChild(divPageId);
          }

          // next link
          var divNext = document.createElement('div');
          divNext.id = thisClass.recordingEntryID + '_header_navigation_next';
          divNext.className = 'recordings_header_navigation_next';
          if (currentPage < maxPage) {
            var divNextLink = document.createElement('a');
            divNextLink.param_offset = currentPage * params.limit;
            divNextLink.param_limit	= params.limit;
            divNextLink.param_q = params.q;
            divNextLink.param_sid = params.sid;
            $(divNextLink).click(function(event) {
              var params = {};
              params.offset = this.param_offset;
              params.limit = this.param_limit;
              params.q = this.param_q;
              params.sid = this.param_sid;
              thisClass.doSearch(params, divList);
            });
            divNextLink.innerHTML = paella.dictionary.translate('Next');
            divNext.appendChild(divNextLink);
          } else {
            divNext.innerHTML = paella.dictionary.translate('Next');
          }
          divNavigation.appendChild(divNext);

        }

        // create recording divs
        for (i=0; i < results.length; ++i ){
          var recording = results[i];

          var divRecording = thisClass.createRecordingEntry(i, recording);
          divList.appendChild(divRecording);
        }
      }, null);
    }
    // finished loading
    thisClass.setLoading(false);
  },


  setLoading:function(loading) {
    if (loading == true) {
      this.divLoading.style.display='block';
    } else {
      this.divLoading.style.display='none';
    }
  },

  setResults:function(results) {
    this.divResults.innerHTML = results;
  },

  getUrlOfAttachmentWithType:function(recording, type) {
    for (var i =0; i < recording.mediapackage.attachments.attachment.length; ++i ){
      var attachment = recording.mediapackage.attachments.attachment[i];
      if (attachment.type === type) {
        return attachment.url;
      }
    }

    return '';
  },

  createRecordingEntry:function(index, recording) {
    var rootID = this.recordingEntryID + index;


    var divEntry = document.createElement('div');
    divEntry.id = rootID;


    divEntry.className='recordings_entry ' + recording.entry_published_class;
    if (index % 2 == 1) {
      divEntry.className=divEntry.className+' odd_entry';
    } else {
      divEntry.className=divEntry.className+' even_entry';
    }

    var previewUrl = this.getUrlOfAttachmentWithType(recording, 'presentation/search+preview');
    if (previewUrl == '') {
      previewUrl = this.getUrlOfAttachmentWithType(recording, 'presenter/search+preview');
    }

    var divPreview = document.createElement('div');
    divPreview.id = rootID+'_preview_container';
    divPreview.className = 'recordings_entry_preview_container';
    var imgLink = document.createElement('a');
    imgLink.setAttribute('tabindex', '-1');
    imgLink.id = rootID+'_preview_link';
    imgLink.className = 'recordings_entry_preview_link';
    imgLink.href = 'watch.html?id=' + recording.id;
    var imgPreview = document.createElement('img');
    imgPreview.setAttribute('alt', '');
    imgPreview.setAttribute('title', recording.dcTitle);
    imgPreview.setAttribute('aria-label', recording.dcTitle);
    imgPreview.id = rootID+'_preview';
    imgPreview.src = previewUrl;
    imgPreview.className = 'recordings_entry_preview';
    imgLink.appendChild(imgPreview);
    divPreview.appendChild(imgLink);
    divEntry.appendChild(divPreview);

    var divResultText = document.createElement('div');
    divResultText.id = rootID+'_text_container';
    divResultText.className = 'recordings_entry_text_container';


    // title
    var divResultTitleText = document.createElement('div');
    divResultTitleText.id = rootID+'_text_title_container';
    divResultTitleText.className = 'recordings_entry_text_title_container';
    var titleResultText = document.createElement('a');
    titleResultText.setAttribute('tabindex', '-1');
    titleResultText.id = rootID+'_text_title';
    titleResultText.innerHTML = recording.dcTitle;
    titleResultText.className = 'recordings_entry_text_title';
    titleResultText.href = 'watch.html?id=' + recording.id;
    divResultTitleText.appendChild(titleResultText);
    divResultText.appendChild(divResultTitleText);


    // author
    var author = '&nbsp;';
    var author_search = '';
    if(recording.dcCreator) {
      author = 'by ' + recording.dcCreator;
      author_search = recording.dcCreator;
    }
    var divResultAuthorText = document.createElement('div');
    divResultAuthorText.id = rootID+'_text_author_container';
    divResultAuthorText.className = 'recordings_entry_text_author_container';
    var authorResultText = document.createElement('a');
    authorResultText.setAttribute('tabindex', '-1');
    authorResultText.id = rootID+'_text_title';
    authorResultText.innerHTML = author;
    authorResultText.className = 'recordings_entry_text_title';
    if (author_search != '') {
      authorResultText.href = 'index.html?q=' + encodeURIComponent(author_search);
    }
    divResultAuthorText.appendChild(authorResultText);
    divResultText.appendChild(divResultAuthorText);


    // date time
    //var timeDate = recording.mediapackage.start;
    var timeDate = recording.dcCreated;
    if (timeDate) {
      var offsetHours = parseInt(timeDate.substring(20, 22), 10);
      var offsetMinutes = parseInt(timeDate.substring(23, 25), 10);
      if (timeDate.substring(19,20) == '-') {
        offsetHours = - offsetHours;
        offsetMinutes = - offsetMinutes;
      }
      var sd = new Date();
      sd.setUTCFullYear(parseInt(timeDate.substring(0, 4), 10));
      sd.setUTCMonth(parseInt(timeDate.substring(5, 7), 10) - 1);
      sd.setUTCDate(parseInt(timeDate.substring(8, 10), 10));
      sd.setUTCHours(parseInt(timeDate.substring(11, 13), 10) - offsetHours);
      sd.setUTCMinutes(parseInt(timeDate.substring(14, 16), 10) - offsetMinutes);
      sd.setUTCSeconds(parseInt(timeDate.substring(17, 19), 10));
      timeDate = sd.toLocaleString();
    } else {
      timeDate = 'n.a.';
    }


    var divResultDateText = document.createElement('div');
    divResultDateText.id = rootID+'_text_date';
    divResultDateText.className = 'recordings_entry_text_date';
    divResultDateText.innerHTML = timeDate;
    divResultText.appendChild(divResultDateText);

    divEntry.appendChild(divResultText);

    divEntry.setAttribute('tabindex','10000');
    $(divEntry).keyup(function(event) {
      if (event.keyCode == 13) { window.location.href='watch.html?id=' + recording.id; }
    });

    divEntry.setAttribute('alt', '');
    divEntry.setAttribute('title', recording.dcTitle);
    divEntry.setAttribute('aria-label', recording.dcTitle);

    return divEntry;
  }
});
