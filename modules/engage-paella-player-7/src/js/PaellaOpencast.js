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

/* global OPENCAST_SERVER_URL */
/* global OPENCAST_CONFIG_URL */
/* global OPENCAST_PAELLA_URL */

import { Paella, bindEvent, Events, utils, log } from 'paella-core';
import getBasicPluginContext from 'paella-basic-plugins';
import getSlidePluginContext from 'paella-slide-plugins';
import getZoomPluginContext from 'paella-zoom-plugin';
import getUserTrackingPluginContext from 'paella-user-tracking';

import { loadTrimming, setTrimming } from './TrimmingLoader';
import EpisodeConversor from './EpisodeConversor.js';
import packagePom from '../../pom.xml';

import dictionary from '../default-dictionaries.js';


function getUrlFromBase(base, url) {
  const a = base.endsWith('/') ? base.slice(0, -1) : base;
  const b = url.startsWith('/') ? url.slice(1) : url;
  const fullURL = `${a}/${b}`;
  return fullURL;
}

export function getUrlFromOpencastServer(url) {
  let base = (typeof OPENCAST_SERVER_URL !== 'undefined') ? OPENCAST_SERVER_URL : '/';
  base = utils.getUrlParameter('oc.server') ?? base;

  return getUrlFromBase(base, url);
}

export function getUrlFromOpencastConfig(url) {
  let base = (typeof OPENCAST_CONFIG_URL !== 'undefined') ? OPENCAST_CONFIG_URL : '/ui/config/paella7';
  base = utils.getUrlParameter('oc.config') ?? base;

  return getUrlFromBase(base, url);
}

export function getUrlFromOpencastPaella(url) {
  const base = (typeof OPENCAST_PAELLA_URL !== 'undefined')
    ? OPENCAST_PAELLA_URL
    : getUrlFromOpencastServer('/paella7/ui');

  return getUrlFromBase(base, url);
}


function myWebsiteCheckConsentFunction(type) {
  const cookie_consent_level = utils.getCookie('cookie_consent_level');
  var consent_level = {};
  try {
    consent_level = JSON.parse(cookie_consent_level);
  }
  catch(e) {
    log.debug('Error parsing "cookie_consent_level" cookie');
  }
  return consent_level[type] || false;
}

const initParams = {
  customPluginContext: [
    require.context('../plugins', true, /\.js/),
    getBasicPluginContext(),
    getSlidePluginContext(),
    getZoomPluginContext(),
    getUserTrackingPluginContext()
  ],
  getCookieConsentFunction: (type) => {
    return myWebsiteCheckConsentFunction(type);
  },
  configResourcesUrl: getUrlFromOpencastConfig('/'),
  configUrl: getUrlFromOpencastConfig('/config.json'),
  repositoryUrl: getUrlFromOpencastServer('/search/episode.json'),

  getManifestUrl: (repoUrl, videoId) => {
    return `${repoUrl}?id=${videoId}`;
  },

  getManifestFileUrl: (manifestUrl) => {
    return manifestUrl;
  },

  loadVideoManifest: async function (url, config, player) {
    // check cookie consent (if enabled)
    const cookieConsent = config?.opencast?.cookieConsent?.enable ?? true;
    const cookieConsentConfig = config?.opencast?.cookieConsent?.config ?? {
      'notice_banner_type':'headline',
      'consent_type':'express',
      'palette':'dark',
      'language':'en',
      'page_load_consent_levels':['strictly-necessary'],
      'notice_banner_reject_button_hide':false,
      'preferences_center_close_button_hide':false,
      'page_refresh_confirmation_buttons':false,
      'website_name': 'Paella - opencast player'
    };
    if (cookieConsent == true) {
      window.cookieconsent.run(cookieConsentConfig);
    }

    // Load episode
    const loadEpisode = async () => {
      const response = await fetch(url);

      if (response.ok) {
        const data = await response.json();
        const conversor = new EpisodeConversor(data, config.opencast || {});
        return conversor.data;
      }
      else {
        throw Error('Invalid manifest url');
      }
    };
    const data = await loadEpisode();
    if (data === null) {
      player.log.info('Try to load me.json');
      // Check me.json, if the user is not logged in, redirect to login
      const data = await fetch(getUrlFromOpencastServer('/info/me.json'));
      const me = await data.json();

      if (!me.roles.includes('ROLE_USER')) {
        player.log.info('Video not found and user is not authenticated. Try to log in.');
        location.href = getUrlFromOpencastPaella('auth.html?redirect=' + encodeURIComponent(window.location.href));
      }
      else {
        // TODO: the video does not exist or the user can't see it
        throw Error('The video does not exist or the user can\'t see it');
      }
    }

    // Add event title to browser tab
    const videoTitle = data?.metadata?.title ?? 'Unknown video title';
    const seriesTitle = data?.metadata?.seriestitle ?? 'No series';
    document.title = `${videoTitle} - ${seriesTitle} | Opencast`;

    // userracking service removed from opencast?
    // Load stats
    // const loadStats = async () => {
    //   const videoId = await this.getVideoId(config,player);
    //   const response = await fetch(getUrlFromOpencastServer(`/usertracking/stats.json?id=${videoId}`));
    //   if (response.ok) {
    //     const data = await response.json();
    //     return data.stats;
    //   }
    //   else {
    //     null;
    //   }
    // };
    // const stats = await loadStats();
    // if (stats) {
    //   data.metadata.views = stats.views;
    // }

    return data;
  },

  loadDictionaries: (player) => {
    for (const lang in dictionary) {
      player.addDictionary(lang, dictionary[lang]);
    }
    player.setLanguage(navigator.language.substring(0, 2));
  }
};

export class PaellaOpencast extends Paella {
  get version() {
    const player = packagePom?.project?.parent?.version || packagePom?.project?.version || 'unknown';
    const coreLibrary = super.version;
    const pluginModules = this.pluginModules.map(m => `${ m.moduleName }: ${ m.moduleVersion }`);
    return {
      player,
      coreLibrary,
      pluginModules
    };
  }

  constructor(containerElement) {
    super(containerElement, initParams);

    const paella = this;
    function humanTimeToSeconds(humanTime) {
      let hours = 0;
      let minutes = 0;
      let seconds = 0;
      const hoursRE = /([0-9]+)h/i.exec(humanTime);
      const minRE = /([0-9]+)m/i.exec(humanTime);
      const secRE = /([0-9]+)s/i.exec(humanTime);
      if (hoursRE) {
        hours = parseInt(hoursRE[1]) * 60 * 60;
      }
      if (minRE) {
        minutes = parseInt(minRE[1]) * 60;
      }
      if (secRE) {
        seconds = parseInt(secRE[1]);
      }
      const totalTime =  hours + minutes + seconds;
      return totalTime;
    }

    bindEvent(paella, Events.PLAYER_LOADED, async () => {
      // Enable trimming
      // Retrieve video duration in case a default trim end time is needed
      const videoDuration = paella.videoManifest?.metadata?.duration;
      // Retrieve trimming data from a data delegate
      let trimmingData = await loadTrimming(paella, paella.videoId);
      // Retrieve trimming data in URL param: ?trimming=1m2s;2m
      const trimming = utils.getHashParameter('trimming') || utils.getUrlParameter('trimming');
      // Retrieve trimming data in URL start-end params in seconds: ?start=12&end=345
      // Allow the 'end' param to overrule the end in trimming data,
      // Allow a 'start' or an 'end' URL parameter to be passed alone
      const startTrimVal = utils.getHashParameter('start') || utils.getUrlParameter('start');
      const endTrimVal = utils.getHashParameter('end') || utils.getUrlParameter('end');

      if (trimming || startTrimVal || endTrimVal) {
        let startTrimming = 0;  // default start time
        let endTrimming = videoDuration; // raw video duration;
        if (trimming) {
          const trimmingSplit = trimming.split(';');
          if (trimmingSplit.length == 2) {
            startTrimming = trimmingData.start + humanTimeToSeconds(trimmingSplit[0]);
            endTrimming = (trimmingData.end == 0)
              ? trimmingData.start + humanTimeToSeconds(trimmingSplit[1])
              : Math.min(trimmingData.start + humanTimeToSeconds(trimmingSplit[1]), trimmingData.end);
          }
        } else {
          if (startTrimVal) {
            startTrimming = trimmingData.start + Math.floor(startTrimVal);
          }
          if (endTrimVal) {
            endTrimming = Math.min(trimmingData.start + Math.floor(endTrimVal), videoDuration);
          }
        }
        if (startTrimming < endTrimming && endTrimming > 0 && startTrimming >= 0) {
          trimmingData = {
            start: startTrimming,
            end: endTrimming,
            enabled: true
          };
        }
        paella.log.debug(`Setting trim to ${JSON.stringify(trimmingData)}`);
        await setTrimming(paella, trimmingData);
      }

      // Check time param in URL and seek:  ?time=1m2s
      const timeString = utils.getHashParameter('time') || utils.getUrlParameter('time');
      // Check t param, which is seek time in seconds, to be passed as a query or hash: #t=12002
      const timeStringInSecs = utils.getHashParameter('t') || utils.getUrlParameter('t');

      if (timeString || timeStringInSecs) {
        let totalTime = 0;
        if (timeString) {
          totalTime = humanTimeToSeconds(timeString);
        } else {
          totalTime = Math.floor(timeStringInSecs);
        }
        paella.log.debug(`Setting initial seek to '${totalTime}' seconds`);
        await paella.videoContainer.setCurrentTime(totalTime);
      }

      // Check captions param in URL:  ?captions  / ?captions=<lang>
      const captions = utils.getHashParameter('captions') || utils.getUrlParameter('captions');
      if (captions != null) {
        let captionsIndex = 0;
        if (captions !== '') {
          paella.captionsCanvas.captions.some((c, idx) => {
            if (c.language == captions) {
              captionsIndex = idx;
              return true;
            }
            return false;
          });
        }
        const captionSelected = paella?.captionsCanvas?.captions[captionsIndex];
        if (captionSelected) {
          paella.log.info(`Enabling captions: ${captionSelected?.label} (${captionSelected?.language})`);
          paella.captionsCanvas.enableCaptions({ index: captionsIndex });
        }
      }
    });
  }
}
