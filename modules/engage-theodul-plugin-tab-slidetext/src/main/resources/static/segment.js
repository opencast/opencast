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
/*jslint browser: true, nomen: true*/
/*global define*/
define(function() {
  'use strict';

  /**
     * Segment
     *
     * @param time
     * @param image_url
     */
  var Segment = function(time, image_url, text) {
    this.time = time;
    this.humanReadableTime = formatSeconds(time);
    this.image_url = image_url;
    this.text = text;
  };

  /**
     * Returns the formatted seconds
     *
     * @param seconds seconds to format
     * @return formatted seconds
     */
  function formatSeconds (seconds) {
    if (!seconds) {
      seconds = 0;
    }
    seconds = (seconds < 0) ? 0 : seconds;
    var result = '';
    if (parseInt(seconds / 3600) < 10) {
      result += '0';
    }
    result += parseInt(seconds / 3600);
    result += ':';
    if ((parseInt(seconds / 60) - parseInt(seconds / 3600) * 60) < 10) {
      result += '0';
    }
    result += parseInt(seconds / 60) - parseInt(seconds / 3600) * 60;
    result += ':';
    if (seconds % 60 < 10) {
      result += '0';
    }
    result += seconds % 60;
    if (result.indexOf('.') != -1) {
      result = result.substring(0, result.lastIndexOf('.')); // get rid of the .ms
    }
    return result;
  }

  return Segment;
});
