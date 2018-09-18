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
paella.addPlugin(function() {
  return class TranscriptionTabBarPlugin extends paella.TabBarPlugin {
    get divContainer() { return this._divContainer; }
    set divContainer(v) { this._divContainer = v; }
    get divSearchBar() { return this._divSearchBar; }
    set divSearchBar(v) { this._divSearchBar = v; }
    get divLoading() { return this._divLoading; }
    set divLoading(v) { this._divLoading = v; }
    get divResults() { return this._divResults; }
    set divResults(v) { this._divResults = v; }
    get divSearch() { return this._divSearch; }
    set divSearch(v) { this._divSearch = v; }
    get divSearchBarRelevance() { return this._divSearchBarRelevance; }
    set divSearchBarRelevance(v) { this._divSearchBarRelevance = v; }

    get resultsEntryID() { return this._resultsEntryID; }
    set resultsEntryID(v) { this._resultsEntryID = v; }
    get foundAlready() { return this._foundAlready; }
    set foundAlready(v) { this._foundAlready = v; }
    get lastHit() { return this._lastHit; }
    set lastHit(v) { this._lastHit = v; }
    get proxyUrl() { return this._proxyUrl; }
    set proxyUrl(v) { this._proxyUrl = v; }
    get useJsonp() { return this._useJsonp; }
    set useJsonp(v) { this._useJsonp = v; }

    constructor() {
      super();
      this._resultsEntryID = '',
      this._foundAlready = false, // flag if something has already been found
      this._lastHit = '',         // storage for latest successful search hit
      this._proxyUrl = '',
      this._useJsonp = false;
    }

    getSubclass() { return 'searchTabBar'; }
    getName() { return 'es.upv.paella.opencast.transcriptionTabBarPlugin'; }
    getTabName() { return paella.dictionary.translate('Transcription'); }
    getIndex() { return 20; }
    getDefaultToolTip() { return paella.dictionary.translate('Transcription'); }


    checkEnabled(onSuccess) {
      var self = this;
      paella.opencast.getEpisode()
      .then(
        function(episode) {
          self._episode = episode;
          onSuccess(episode.segments != undefined);
        },
        function() { onSuccess(false); }
      );
    }

    buildContent(domElement) {
      this.domElement = domElement;
      this.loadContent();
    }

    action(tab) {}

    loadContent() {
      this.divContainer = document.createElement('div');
      this.divContainer.className = 'searchTabBarContainer';

      this.divSearchBar = document.createElement('div');
      this.divSearchBar.className = 'searchTabBarSearchBar';

      this.divLoading = document.createElement('div');
      this.divLoading.className = 'searchTabBarLoading';

      this.divResults = document.createElement('div');
      this.divResults.className = 'searchTabBarResults';

      this.divSearch = document.createElement('div');
      this.divSearch.className = 'searchTabBarSearch';


      this.divContainer.appendChild(this.divSearchBar);
      this.divContainer.appendChild(this.divLoading);
      this.divContainer.appendChild(this.divSearch);
      this.divContainer.appendChild(this.divResults);
      this.domElement.appendChild(this.divContainer);

      this.prepareSearchBar();
      this.loadSegmentText();
    }


    setLoading(b) {
      if (b == true){
        this.divLoading.style.display='block';
        this.divResults.style.display='none';
      }
      else{
        this.divLoading.style.display='none';
        this.divResults.style.display='block';
      }
    }


    prepareSearchBar(){
      var thisClass = this;

      var divSearchBarLeft = document.createElement('div');
      divSearchBarLeft.className = 'searchBar';

      this.divSearchBarRelevance = document.createElement('div');
      this.divSearchBarRelevance.className = 'relevanceInfo';


      // -------  Left
      var inputElement = document.createElement('input');
      inputElement.type = 'text';
      inputElement.value = paella.dictionary.translate('Search in this event');
      inputElement.setAttribute('size', '30');
      inputElement.setAttribute('dir','lrt');
      inputElement.setAttribute('spellcheck','true');
      inputElement.setAttribute('x-webkit-speech','');
      inputElement.setAttribute('tabindex','4000');
      inputElement.onfocus = function(){this.value=''; this.onfocus=undefined;};
      inputElement.onkeyup = function(){thisClass.doSearch(this.value);};

      divSearchBarLeft.appendChild(inputElement);

      // -------  Right
      var r1 = document.createElement('div');
      var r2 = document.createElement('div');
      var r3 = document.createElement('div');
      var r4 = document.createElement('div');
      r1.className = 'text';
      r2.className = 'lt30';
      r3.className = 'lt70';
      r4.className = 'gt70';

      r1.innerHTML = paella.dictionary.translate('Search relevance:');
      r2.innerHTML = '&lt; 30%';
      r3.innerHTML = '&lt; 70%';
      r4.innerHTML = '&gt; 70%';

      this.divSearchBarRelevance.appendChild(r1);
      this.divSearchBarRelevance.appendChild(r2);
      this.divSearchBarRelevance.appendChild(r3);
      this.divSearchBarRelevance.appendChild(r4);

      this.divSearchBar.appendChild(divSearchBarLeft);
      this.divSearchBar.appendChild(this.divSearchBarRelevance);
    }

    loadSegmentText() {
      var self = this;
      this.setLoading(true);
      this.divResults.innerHTML = '';

      if (self._episode.segments === undefined) {
        paella.debug.log('Segment Text data not available');
      }
      else {
        var segments = self._episode.segments;
        for (var i =0; i < segments.segment.length; ++i ){
          var segment = segments.segment[i];
          this.appendSegmentTextEntry(segment);
        }
      }
      this.setLoading(false);
    }

    appendSegmentTextEntry(segment) {
      var divEntry = document.createElement('div');
      divEntry.className='searchTabBarResultEntry';
      divEntry.id='searchTabBarResultEntry_' + segment.index;
      divEntry.setAttribute('tabindex', 4100 + parseInt(segment.index));
      $(divEntry).click(function(event){
        $(document).trigger( paella.events.seekToTime, {time: segment.time/1000});
      });
      $(divEntry).keyup(function(event) {
        if (event.keyCode == 13) { $(document).trigger( paella.events.seekToTime, {time: segment.time/1000}); }
      });

      var divPreview = document.createElement('div');
      divPreview.className = 'searchTabBarResultEntryPreview';
      if (segment && segment.previews && segment.previews.preview) {
        var imgPreview = document.createElement('img');
        imgPreview.src = segment.previews.preview.$;
        divPreview.appendChild(imgPreview);
      }
      divEntry.appendChild(divPreview);


      var divResultText  = document.createElement('div');
      divResultText.className = 'searchTabBarResultEntryText';


      var textResultText = document.createElement('a');
      textResultText.innerHTML = '<span class=\'time\'>' + paella.utils.timeParse.secondsToTime(segment.time/1000) + '</span> ' + segment.text;
      divResultText.appendChild(textResultText);
      divEntry.appendChild(divResultText);

      this.divResults.appendChild(divEntry);
    }

    doSearch(value) {
      var thisClass = this;
      if (value != '') {
        this.divSearchBarRelevance.style.display='none'; //"block";
      }
      else {
        this.divSearchBarRelevance.style.display='none';
      }
      this.setLoading(true);


      var segmentsAvailable = false;
      paella.ajax.get({url:'/search/episode.json', params:{id:thisClass._episode.id, q:value, limit:1000}},
        function(data, contentType, returnCode) {
          paella.debug.log('Searching episode='+thisClass._episode.id + ' q='+value);

          segmentsAvailable = (data !== undefined) && (data['search-results'] !== undefined) &&
                      (data['search-results'].result !== undefined) &&
                      (data['search-results'].result.segments !== undefined) &&
                      (data['search-results'].result.segments.segment.length > 0);

          if (value === '') {
            thisClass.setNotSearch();
          }
          else {
            thisClass.setResultAvailable(value);
          }

          if (segmentsAvailable) {
            var segments = data['search-results'].result.segments;
            var maxRelevance = 0;
            var i, segment;

            for (i =0; i < segments.segment.length; ++i ){
              segment = segments.segment[i];
              if (maxRelevance < parseInt(segment.relevance)) {
                maxRelevance = parseInt(segment.relevance);
              }
            }
            paella.debug.log('Search Max Revelance ' + maxRelevance);


            for (i =0; i < segments.segment.length; ++i ){
              segment = segments.segment[i];
              var relevance = parseInt(segment.relevance);

              var relevanceClass = '';
              if (value !== '') {
                if (relevance <= 0) {
                  relevanceClass = 'none_relevance';
                } else if (relevance <  Math.round(maxRelevance * 30 / 100)) {
                  relevanceClass = 'low_relevance';
                } else if (relevance < Math.round(maxRelevance * 70 / 100)) {
                  relevanceClass = 'medium_relevance';
                } else {
                  relevanceClass = 'high_relevance';
                }
              }

              var divEntry = $('#searchTabBarResultEntry_'+segment.index);
              divEntry[0].className = 'searchTabBarResultEntry ' + relevanceClass;
            }

            if (!thisClass.foundAlready) {
              thisClass.foundAlready = true;
            }
            thisClass.lastHit = value;
          }
          else {
            paella.debug.log('No Revelance');
            if (thisClass.foundAlready){
              thisClass.setNoActualResultAvailable(value);
            }
          }
          thisClass.setLoading(false);
        },
        function(data, contentType, returnCode) {
          thisClass.setLoading(false);
        }
      );
    }


    setNoActualResultAvailable(searchValue) {
      this.divSearch.innerHTML = paella.dictionary.translate('Results for \'{0}\' (no actual results for \'{1}\' found)').replace(/\{0\}/g,this.lastHit).replace(/\{1\}/g,searchValue);

    }

    setResultAvailable(searchValue) {
      this.divSearch.innerHTML =  paella.dictionary.translate('Results for \'{0}\'').replace(/\{0\}/g,searchValue);
    }

    setNotSearch() {
      this.divSearch.innerHTML='';
    }
  };
});
