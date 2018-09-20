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
* A service to manage filter profiles.
*
* Filter profiles will be stored in the localStorage.
*/
angular.module('adminNg.services')
.factory('FilterProfiles', ['localStorageService', function (localStorageService) {
  var FilterProfileStorage = function () {
    var me = this;

    this.getFromStorage = function () {
      this.storage = angular.fromJson(localStorageService.get('filterProfiles')) || {};
    };

    this.get = function (namespace) {
      return angular.copy(me.storage[namespace]) || [];
    };

    this.save = function () {
      localStorageService.add('filterProfiles', angular.toJson(me.storage));
    };

    this.set = function (namespace, value) {
      me.storage[namespace] = angular.copy(value);
      me.save();
    };

    this.getFromStorage();
  };
  return new FilterProfileStorage();
}]);
