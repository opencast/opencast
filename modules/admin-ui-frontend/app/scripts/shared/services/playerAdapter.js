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
'use strict';

angular.module('adminNg.services')
.service('PlayerAdapter', [function () {

  /**
     * Possible player status
     * @readonly
     * @enum {number}
     */
  this.STATUS = {
    INITIALIZING: 0,
    LOADING: 1,
    SEEKING: 2,
    PAUSED: 3,
    PLAYING: 4,
    ENDED: 5,
    ERROR_NETWORK: 6,
    ERROR_UNSUPPORTED_MEDIA: 7
  };

  /**
     * Player adapter event
     * @readonly
     * @enum {string}
     */
  this.EVENTS = {
    PLAY: 'play',
    PAUSE: 'pause',
    SEEKING: 'seeking',
    READY: 'ready',
    TIMEUPDATE: 'timeupdate',
    ERROR: 'error',
    ENDED: 'ended',
    CAN_PLAY: 'canplay',
    DURATION_CHANGE: 'durationchange',
    VOLUMECHANGE: 'volumechange'
  };

  this.eventMapping = function () {
    var EventMapping = function () {
      var mapping = {};

      this.map = function (apiEvent, nativeEvent) {
        mapping[apiEvent] = nativeEvent;
        return this;
      };

      this.resolveNativeName = function (apiEvent) {
        var nativeEvent = mapping[apiEvent];
        if (nativeEvent === undefined) {
          throw Error('native event for [' + apiEvent + '] not found');
        }
        return nativeEvent;
      };
    };

    return new EventMapping();
  };
}]);
