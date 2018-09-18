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


class Opencast {

  constructor() {
    this._me = undefined,
    this._episode = undefined,
    this._series = undefined,
    this._acl = undefined;
  }

  getUserInfo() {
    var self = this;
    return new Promise((resolve, reject)=>{
      if (self._me) {
        resolve(self._me);
      }
      else {
        base.ajax.get({url:'/info/me.json'},
          function(data,contentType,code) {
            self._me = data;
            resolve(data);
          },
          function(data,contentType,code) { reject(); }
        );
      }
    });
  }

  getEpisode() {
    var self = this;
    return new Promise((resolve, reject)=>{
      if (self._episode) {
        resolve(self._episode);
      }
      else {
        var episodeId = paella.utils.parameters.get('id');
        base.ajax.get({url:'/search/episode.json', params:{'id': episodeId}},
          function(data, contentType, code) {
            if (data['search-results'].result) {
              self._episode = data['search-results'].result;
              resolve(self._episode);
            }
            else {
              reject();
            }
          },
          function(data, contentType, code) {
            reject();
          }
        );
      }
    });
  }


  getSeries() {
    var self = this;
    return this.getEpisode()
    .then(function(episode) {
      return new Promise((resolve, reject)=>{
        if (self._series) {
          resolve(self.series);
        }
        else {
          var serie = episode.mediapackage.series;
          if (serie != undefined) {
            base.ajax.get({url:'/search/series.json', params:{'id': serie}},
              function(data, contentType, code) {
                if (data['search-results'].result) {
                  self._series = data['search-results'].result;
                  resolve(self._series);
                }
                else {
                  reject();
                }
              },
              function(data, contentType, code) {
                reject();
              }
            );
          }
          else {
            reject();
          }
        }
      });
    });
  }

  getACL() {
    var self = this;
    return this.getEpisode()
    .then(function(episode) {
      return new Promise((resolve, reject)=>{
        var serie = episode.mediapackage.series;
        if (serie != undefined) {
          base.ajax.get({url:'/series/'+serie+'/acl.json'},
            function(data,contentType,code) {
              self._acl = data;
              resolve(self._acl);
            },
            function(data,contentType,code) {
              reject();
            }
          );
        }
        else {
          reject();
        }
      });
    });
  }
}

// Patch to work with MH jetty server.
base.ajax.send = function(type,params,onSuccess,onFail) {
  this.assertParams(params);

  var ajaxObj = jQuery.ajax({
    url:params.url,
    data:params.params,
    cache:false,
    type:type
  });

  if (typeof(onSuccess)=='function') {
    ajaxObj.done(function(data,textStatus,jqXHR) {
      var contentType = jqXHR.getResponseHeader('content-type');
      onSuccess(data,contentType,jqXHR.status,jqXHR.responseText);
    });
  }

  if (typeof(onFail)=='function') {
    ajaxObj.fail(function(jqXHR,textStatus,error) {
      var data = jqXHR.responseText;
      var contentType = jqXHR.getResponseHeader('content-type');
      if ( (jqXHR.status == 200) && (typeof(jqXHR.responseText)=='string') ) {
        try {
          data = JSON.parse(jqXHR.responseText);
        }
        catch (e) {
          onFail(textStatus + ' : ' + error,'text/plain',jqXHR.status,jqXHR.responseText);
        }
        onSuccess(data,contentType,jqXHR.status,jqXHR.responseText);
      }
      else{
        onFail(textStatus + ' : ' + error,'text/plain',jqXHR.status,jqXHR.responseText);
      }
    });
  }
};


