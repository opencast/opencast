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

/////////////////////////////////////////////////
// OCR Segments Search
/////////////////////////////////////////////////
paella.addPlugin(function() {
  return class searchPlugin extends paella.SearchServicePlugIn {
    constructor() {
      super();
    }

    getName() { return 'es.upv.paella.opencast.searchPlugin'; }

    search(text, next) {

      if ((text === '') || (text === undefined)) {
        next(false,[]);
      }
      else {
        var episodeId = paella.utils.parameters.get('id');

        paella.ajax.get({url:'/search/episode.json', params:{id:episodeId, q:text, limit:1000}},
          function(data, contentType, returnCode) {
            paella.debug.log('Searching episode=' + episodeId + ' q='+text);
            var segmentsAvailable = (data !== undefined) && (data['search-results'] !== undefined) &&
                        (data['search-results'].result !== undefined) &&
                        (data['search-results'].result.segments !== undefined) &&
                        (data['search-results'].result.segments.segment.length > 0);

            var searchResult = [];

            if (segmentsAvailable) {
              var segments = data['search-results'].result.segments;
              var i, segment;

              for (i =0; i < segments.segment.length; ++i ) {
                segment = segments.segment[i];
                var relevance = parseInt(segment.relevance);

                if (relevance > 0) {
                  searchResult.push({
                    content: segment.text,
                    scote: segment. relevance,
                    time: parseInt(segment.time)/1000
                  });
                }
              }
              next(false, searchResult);
            }
            else {
              paella.debug.log('No Revelance');
              next(false, []);
            }
          },
          function(data, contentType, returnCode) {
            next(true);
          }
        );
      }
    }
  };
});
