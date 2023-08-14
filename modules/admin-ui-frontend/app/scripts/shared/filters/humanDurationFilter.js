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

angular.module('adminNg.filters')
    .filter('humanDuration', [function () {
      return function (durationInMsecValue) {

        if(angular.isUndefined(durationInMsecValue)) {
          return;
        }

        // best effort, independent on type
        var durationInMsec = parseInt(durationInMsecValue);

        if(isNaN(durationInMsec)) {
          return durationInMsecValue;
        }

        var durationInSec = parseInt(durationInMsec / 1000);

        var min = 60;
        var hour = min * min;

        var hours = parseInt(durationInSec / hour);
        var rest = durationInSec % hour;
        var minutes = parseInt(rest / min);
        rest = rest % min;
        var seconds =  parseInt(rest % min);

        if(seconds < 10) {
          // add leading zero
          seconds = '0' + seconds;
        }

        if (hours > 0 && minutes < 10) {
          minutes = '0' + minutes;
        }

        var minWithSec = minutes + ':' + seconds;
        if (hours > 0) {
          return hours + ':' + minWithSec;
        }
        return minWithSec;
      };
    }]);
