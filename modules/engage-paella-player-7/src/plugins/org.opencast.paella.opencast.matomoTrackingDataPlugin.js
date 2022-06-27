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
import { DataPlugin, Events, bindEvent } from 'paella-core';

export default class OpencastMatomoTrackingDataPlugin extends DataPlugin {

  async isEnabled() {
      // This is the CORRECT WAY to extend isEnabled() default behavior
      if (!(await super.isEnabled())) {
        return false;
      }
      else {
        // Extend the isEnabled() funcionality
        const response = await fetch('/usertracking/detailenabled');
        const data = await response.text();
        const enabled = /true/i.test(data);
        return enabled;
      }
  }

  async load(){
    const client_id = this.config.client_id;
    const heartbeat = this.config.heartbeat;
    const tracking_client = this.config.tracking_client;
    var server = this.config.server;
    const site_id = this.config.site_id;



    let matomoPromise = null;
    const matomoScript = (path) => {
      if (!matomoPromise){
        matomoPromise = new Promise((resolve) => {
          const script = document.createElement('script');
          script.type = 'text/javascript';
          script.src = path;
          let loaded = false;
          script.onload = script.onreadystatechange = () => {
            if (!loaded) {
              loaded = true;
              resolve();
            }
          };
          document.head.appendChild(script);
        });
      }
      return matomoPromise;
    };

    if (server.substr(-1) != '/') server += '/';
    await matomoScript(server + tracking_client + '.js');
    this.player.matomotracker = Matomo.getAsyncTracker( server + tracking_client + '.php', site_id );
    //this.player.log.debug('Matomo Analytics Initialized: ' + Matomo.initialized);
    this.player.matomotracker.client_id = client_id;
    if (heartbeat && heartbeat > 0) this.player.matomotracker.enableHeartBeatTimer(heartbeat);
    if (Matomo && Matomo.MediaAnalytics) {
      bindEvent(this.player,
        Events.STREAM_LOADED,
        () => {
          Matomo.MediaAnalytics.scanForMedia();
        });
      this.registerVisit();
    }
  }

  async write(context, { id }, data) {
    const currentTime = await this.player.videoContainer.currentTime();
    this.player.log.debug(`Logging event for video id ${ id } at time: ${ currentTime }`);

    switch (data.event) {
    case Events.PLAY:
      this.player.matomotracker.trackEvent('Player.Controls', 'Play', this.loadTitle());
      break;
    case Events.PAUSE:
      this.player.matomotracker.trackEvent('Player.Controls', 'Pause', this.loadTitle());
      break;
    case Events.ENDED:
      this.player.matomotracker.trackEvent('Player.Status', 'Ended', this.loadTitle());
      break;
    case (Events.FULLSCREEN_CHANGED):
      if (data.params.status == true){
        this.player.matomotracker.trackEvent('Player.View', 'Fullscreen', this.loadTitle());
      } else {
        this.player.matomotracker.trackEvent('Player.View', 'ExitFullscreen', this.loadTitle());
      }
      break;
    case Events.PLAYER_LOADED:
      this.player.matomotracker.trackEvent('Player.Status', 'LoadComplete', this.loadTitle());
      break;
    case Events.SHOW_POPUP:
      this.player.matomotracker.trackEvent('Player.PopUp', 'Show', data.params.plugin.name);
      break;
    case Events.HIDE_POPUP:
      this.player.matomotracker.trackEvent('Player.PopUp', 'Hide', data.params.plugin.name);
      break;
    case Events.SEEK:
      this.player.matomotracker.trackEvent('Player.Controls', 'Seek', Math.round(data.params.newTime));
      break;
    case Events.VOLUME_CHANGED:
      this.player.matomotracker.trackEvent('Player.Settings', 'Volume', data.params.volume);
      break;
    case Events.RESIZE_END:
      this.player.matomotracker.trackEvent('Player.View', 'Resize', `${ data.params.size.w }x${ data.params.size.h }`);
      break;
    case Events.CAPTIONS_CHANGED:
      this.player.matomotracker.trackEvent('Player.Captions', 'Enabled', data);
      break;

    // Bug: Playback rate can't be triggered
    case Events.PLAYBACK_RATE_CHANGED:
      this.player.log.info('Playbackrate changed!');
      this.player.matomotracker.trackEvent('Player.Controls', 'PlaybackRate', data.params.newPlaybackRate);
      break;
    }
  }

  registerVisit() {
    var opencastData,
        title,
        eventId,
        seriesTitle,
        seriesId,
        presenter;

    opencastData = this.player.videoManifest.metadata;

    if (opencastData.UID != undefined && opencastData.title != undefined){
      title = opencastData.title;
      eventId = opencastData.UID;
      presenter = opencastData.presenters;
      this.player.matomotracker.setCustomVariable (5, 'client',
        (this.player.matomotracker.client_id || 'Paella Opencast'));
      /* eslint-disable no-unused-vars */
      Matomo.MediaAnalytics.setMediaTitleFallback(function (mediaElement) {return title;});
      /* eslint-disable no-unused-vars */
    }

    if (opencastData != undefined){
      seriesId = opencastData.series;
      seriesTitle = opencastData.seriestitle;
    }

    if (title) this.player.matomotracker.setCustomVariable(1, 'event', title + ' (' + eventId + ')');
    if (seriesTitle) this.player.matomotracker.setCustomVariable(2, 'series' + seriesTitle + ' (' + seriesId + ')');
    if (presenter) this.player.matomotracker.setCustomVariable(3, 'presenter', presenter);
    if (title && presenter){
      this.player.matomotracker.setDocumentTitle(title + ' - ' + (presenter || 'Unknown'));
      this.player.matomotracker.trackPageView(title + ' - ' + (presenter || 'Unknown'));
    } else{
      this.player.matomotracker.trackPageView();
    }
  }

  loadTitle() {
    var title = this.player.videoManifest.metadata.title;
    return title;
  }


}
