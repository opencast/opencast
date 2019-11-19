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

/*global Opencast
         MHAnnotationServiceDefaultDataDelegate
         MHAnnotationServiceTrimmingDataDelegate
         MHFootPrintsDataDelegate
         OpencastTrackCameraDataDelegate
         OpencastToPaellaConverter
         OpencastAccessControl
*/

function initPaellaOpencast() {
  if (!paella.opencast) {
    paella.opencast = new Opencast();

    paella.dataDelegates.MHAnnotationServiceDefaultDataDelegate = MHAnnotationServiceDefaultDataDelegate;
    paella.dataDelegates.MHAnnotationServiceTrimmingDataDelegate = MHAnnotationServiceTrimmingDataDelegate;
    paella.dataDelegates.MHFootPrintsDataDelegate = MHFootPrintsDataDelegate;
    paella.dataDelegates.OpencastTrackCameraDataDelegate = OpencastTrackCameraDataDelegate;
    paella.OpencastAccessControl = OpencastAccessControl;
    window.OpencastAccessControl = OpencastAccessControl;
  }
}

function loadOpencastPaella(containerId) {
  initPaellaOpencast();

  var canRead = false;
  var oacl = new OpencastAccessControl();
  oacl.canRead()
  .then(function(c) {
    canRead = c;
    return oacl.userData();
  })
  .then(function(user) {
    if (!canRead) {
      if (user.isAnonymous) {
        window.location.href = oacl.getAuthenticationUrl();
      }
      else {
        var errorMessage = paella.dictionary
          .translate('Error loading video {id}')
          .replace(/\{id\}/g, paella.utils.parameters.get('id') || '');
        paella.messageBox.showError(errorMessage);
        paella.events.trigger(paella.events.error, {error: errorMessage});
      }
    }
    else {
      paella.lazyLoad(containerId, {
        configUrl:'/ui/config/paella/config.json',
        loadVideo:function() {
          return new Promise((resolve, reject) => {
            paella.opencast.getEpisode()
            .then((episode) => {
              var converter = new OpencastToPaellaConverter();
              var data = converter.convertToDataJson(episode);
              if (data.streams.length < 1) {
                paella.messageBox.showError(paella.dictionary.translate('Error loading video! No video tracks found'));
              }
              else {
                resolve(data);
              }
            });
          });
        }
      });
    }
  });
}
