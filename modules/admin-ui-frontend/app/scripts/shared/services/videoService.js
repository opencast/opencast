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

/**
 * Utility service bridging video and timeline functionality.
 */
angular.module('adminNg.services')
.factory('VideoService', [
  function () {

    var VideoService = function () {
      this.getCurrentSegment = function (player, video) {
        var matchingSegment,
            position = player.adapter.getCurrentTime() * 1000;
        angular.forEach(video.segments, function (segment) {
          if ((segment.start <= position) && (segment.end >= position)) {
            matchingSegment = segment;
          }
        });
        return matchingSegment;
      };
      this.getPreviousActiveSegment = function (player, video) {
        var matchingSegment,
            previousSegment,
            position = player.adapter.getCurrentTime() * 1000;
        angular.forEach(video.segments, function (segment) {
          if ((segment.start <= position) && (segment.end >= position)) {
            matchingSegment = previousSegment;
          }
          if (!segment.deleted) {
            previousSegment = segment;
          }
        });
        return matchingSegment;
      };
      // get the next active segment including the current segment.
      this.getNextActiveSegment = function (player, video) {
        var matchingSegment,
            foundCurrentSegment,
            position = player.adapter.getCurrentTime() * 1000;
        angular.forEach(video.segments, function (segment) {
          if ((segment.start <= position) && (segment.end >= position)) {
            foundCurrentSegment = true;
          }
          if (foundCurrentSegment && ! matchingSegment && !segment.deleted) {
            matchingSegment = segment;
          }
        });
        return matchingSegment;
      };
    };

    return new VideoService();
  }]);
