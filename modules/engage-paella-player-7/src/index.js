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
import { Paella, bindEvent, Events, utils } from 'paella-core';
import getBasicPluginContext from 'paella-basic-plugins';
import getSlidePluginContext from 'paella-slide-plugins';
import getZoomPluginContext from 'paella-zoom-plugin';
import getUserTrackingPluginContext from 'paella-user-tracking';
import { loadTrimming, setTrimming } from './js/TrimmingLoader';

import packagePom from '../pom.xml';

import EpisodeConversor from './js/EpisodeConversor.js';

// Customized icons
import BackwardIcon from './icons/backward-icon.svg';
import ForwardIcon from './icons/forward-icon.svg';
import CaptionsIcon from './icons/captions-icon.svg';
import CloseIcon from './icons/close.svg';
import FullscreenIcon from './icons/fullscreen-icon.svg';
import FullscreenExitIcon from './icons/fullscreen-exit.svg';
import MaximizeIcon from './icons/maximize.svg';
import MinimizeIcon from './icons/minimize.svg';
import PlayIcon from './icons/play-icon.svg';
// import SettingsIcon from './icons/settings-icon.svg';
import NextIcon from './icons/slide-next-icon.svg';
import PrevIcon from './icons/slide-prev-icon.svg';
import SlidesIcon from './icons/slides-icon.svg';
import ViewModeIcon from './icons/view-mode.svg';
import VolumeHighIcon from './icons/volume-base-icon.svg';
import VolumeMidIcon from './icons/volume-mid-icon.svg';
import VolumeLowIcon from './icons/volume-low-icon.svg';
import VolumeMuteIcon from './icons/volume-mute-icon.svg';


window.onload = async () => {

  const dictionaries = require.context('./i18n/', true, /\.json$/);
  const languages = {};
  function addDictionaries(player) {
    dictionaries.keys().forEach(k => {
      const reResult = /([a-z-]+[A-Z_]+)\.json/.exec(k);
      const localization = reResult && reResult[1];
      if (localization) {
        const dict = dictionaries(k);
        player.addDictionary(localization,dict);

        const lang = localization.substr(0,2);
        if (!languages[lang]) {
          languages[lang] = true;
          player.addDictionary(lang,dict);
        }
      }
    });
  }

  function myWebsiteCheckConsentFunction(type) {
    const cookie_consent_level = utils.getCookie('cookie_consent_level');
    var consent_level = {};
    try {
      consent_level = JSON.parse(cookie_consent_level);
    }
    catch(e) {
      paella.log.debug('Error parsing "cookie_consent_level" cookie');
    }
    return consent_level[type] || false;
  }

  const initParams = {
    customPluginContext: [
      require.context('./plugins', true, /\.js/),
      getBasicPluginContext(),
      getSlidePluginContext(),
      getZoomPluginContext(),
      getUserTrackingPluginContext()
    ],
    getCookieConsentFunction: (type) => {
      return myWebsiteCheckConsentFunction(type);
    },
    configResourcesUrl: '/ui/config/paella7/',
    configUrl: '/ui/config/paella7/config.json',

    repositoryUrl: '/search/episode.json',

    getManifestUrl: (repoUrl,videoId) => {
      return `${repoUrl}?id=${videoId}`;
    },

    getManifestFileUrl: (manifestUrl) => {
      return manifestUrl;
    },

    loadVideoManifest: async function (url, config, player) {
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

      const loadStats = async () => {
        const videoId = await this.getVideoId(config,player);
        const response = await fetch(`/usertracking/stats.json?id=${videoId}`);
        if (response.ok) {
          const data = await response.json();
          return data.stats;
        }
        else {
          null;
        }
      };

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
      const data = await loadEpisode();
      if (data === null) {
        player.log.info('Try to load me.json');
        // Check me.json, if the user is not logged in, redirect to login
        const data = await fetch('/info/me.json');
        const me = await data.json();

        if (me.userRole === 'ROLE_USER_ANONYMOUS') {
          player.log.info('Video not found and user is not authenticated. Try to log in.');
          location.href = 'auth.html?redirect=' + encodeURIComponent(window.location.href);
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

      // Load stats
      const stats = await loadStats();
      if (stats) {
        data.metadata.views = stats.views;
      }

      return data;
    },

    loadDictionaries: player => {
      const lang = navigator.language;
      player.setLanguage(lang);
      addDictionaries(player);
    }
  };

  class PaellaOpencast extends Paella {
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
  }

  let paella = new PaellaOpencast('player-container', initParams);

  paella.loadManifest()
  .then(()=>{ return utils.loadStyle('style.css');})
  .then(()=>{ return utils.loadStyle('/ui/config/paella7/custom_theme.css');})
  .then(()=>{
    //// Customized icons
    //// fullscreen
    paella.addCustomPluginIcon('es.upv.paella.fullscreenButton','fullscreenIcon',FullscreenIcon);
    paella.addCustomPluginIcon('es.upv.paella.fullscreenButton','windowedIcon', FullscreenExitIcon);

    //// volume
    paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeHighIcon',VolumeHighIcon);
    paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeMidIcon',VolumeMidIcon);
    paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeLowIcon',VolumeLowIcon);
    paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeMuteIcon',VolumeMuteIcon);

    //// layout icons
    paella.addCustomPluginIcon('es.upv.paella.dualVideoDynamic','iconMaximize', MaximizeIcon);
    paella.addCustomPluginIcon('es.upv.paella.dualVideoDynamic','iconMimimize', MinimizeIcon);
    paella.addCustomPluginIcon('es.upv.paella.dualVideoDynamic','iconClose', CloseIcon);
    //// play button
    paella.addCustomPluginIcon('es.upv.paella.playPauseButton','play', PlayIcon);
    //// quality selector
    //paella.addCustomPluginIcon("es.upv.paella.qualitySelector","screenIcon",screenIcon);
    //// playback rate
    //paella.addCustomPluginIcon("es.upv.paella.playbackRateButton","screenIcon",screenIcon);

    //// layout selector
    paella.addCustomPluginIcon('es.upv.paella.layoutSelector','layoutIcon', ViewModeIcon);

    //// backward 30 segonds
    paella.addCustomPluginIcon('es.upv.paella.backwardButtonPlugin','backwardIcon', BackwardIcon);
    //// forward 30 segonds
    paella.addCustomPluginIcon('es.upv.paella.forwardButtonPlugin','forwardIcon', ForwardIcon);

    //// keyboard icon
    //paella.addCustomPluginIcon("es.upv.paella.keyboardShortcutsHelp","keyboardIcon",keyboardIcon);
    //// audio selector
    //paella.addCustomPluginIcon("es.upv.paella.audioSelector","screenIcon",screenIcon);
    //// download icon
    //paella.addCustomPluginIcon("es.upv.paella.downloadsPlugin","downloadIcon",downloadIcon);

    //// find captions icon
    //paella.addCustomPluginIcon("es.upv.paella.findCaptionsPlugin","findCaptionsIcon",findCaptionsIcon);

    //// captions icon
    paella.addCustomPluginIcon('es.upv.paella.captionsSelectorPlugin','captionsIcon',CaptionsIcon);

    //// slides icon
    paella.addCustomPluginIcon('es.upv.paella.frameControlButtonPlugin','photoIcon',SlidesIcon);

    //// slide navigation
    paella.addCustomPluginIcon('es.upv.paella.nextSlideNavigatorButton','arrowRightIcon', NextIcon);
    paella.addCustomPluginIcon('es.upv.paella.prevSlideNavigatorButton','arrowLeftIcon', PrevIcon);
  })
  .then(() => paella.log.info('Paella player load done'))
  .catch(e => paella.log.error(e));

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
    let trimmingData = await loadTrimming(paella, paella.videoId);
    // Check for trimming param in URL: ?trimming=1m2s;2m
    const trimming = utils.getHashParameter('trimming') || utils.getUrlParameter('trimming');
    if (trimming) {
      const trimmingSplit = trimming.split(';');
      if (trimmingSplit.length == 2) {
        const startTrimming = trimmingData.start + humanTimeToSeconds(trimmingSplit[0]);
        const endTrimming = Math.min(trimmingData.start + humanTimeToSeconds(trimmingSplit[1]), trimmingData.end);

        if (startTrimming < endTrimming && endTrimming > 0 && startTrimming >= 0) {
          trimmingData = {
            start: startTrimming,
            end: endTrimming,
            enabled: true
          };
        }
      }
    }
    await setTrimming(paella, trimmingData);

    // Check time param in URL and seek:  ?time=1m2s
    const timeString = utils.getHashParameter('time') || utils.getUrlParameter('time');
    if (timeString) {
      const totalTime = humanTimeToSeconds(timeString);
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
};
