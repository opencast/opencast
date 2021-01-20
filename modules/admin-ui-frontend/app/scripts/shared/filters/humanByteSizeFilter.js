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

/*
 Transforms numbers of bytes into human readable format
 */
angular.module('adminNg.filters')
    .filter('humanBytes', [function () {
      return function (bytesValue) {

        if (angular.isUndefined(bytesValue)) {
          return;
        }

        // best effort, independent on type
        var bytes = parseInt(bytesValue);

        if(isNaN(bytes)) {
          return bytesValue;
        }

        // from http://stackoverflow.com/a/14919494
        var thresh = 1000;
        if (Math.abs(bytes) < thresh) {
          return bytes + ' B';
        }
        var units = ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
        var u = -1;
        do {
          bytes /= thresh;
          ++u;
        } while (Math.abs(bytes) >= thresh && u < units.length - 1);

        return bytes.toFixed(1) + ' ' + units[u];

      };
    }]);
