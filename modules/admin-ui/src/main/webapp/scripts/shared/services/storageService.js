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
* A service to store arbitrary information without use of a backend.
*
* Information can either be stored in the localStorage or as a search
* parameter. Search parameters take precedence over localStorage
* values.
*
*/
angular.module('adminNg.services')
.factory('Storage', ['localStorageService', '$rootScope', '$location', function (localStorageService, $rootScope,
  $location) {
  var Storage = function () {
    var me = this;

    // Create a scope in order to broadcast changes.
    this.scope = $rootScope.$new();

    this.getFromStorage = function () {
      this.storage = angular.fromJson($location.search().storage) ||
                angular.fromJson(localStorageService.get('storage')) ||
                {};
    };

    this.get = function (type, namespace) {
      if (!me.storage[type]) {
        return {};
      }
      return me.storage[type][namespace] || {};
    };

    this.save = function () {
      var params = $location.search();
      params.storage = angular.toJson(me.storage);
      $location.search(params);
      localStorageService.add('storage', angular.toJson(me.storage));
    };

    this.put = function (type, namespace, key, value) {
      if (angular.isUndefined(me.storage[type])) {
        me.storage[type] = {};
      }

      if (angular.isUndefined(me.storage[type][namespace])) {
        me.storage[type][namespace] = {};
      }
      me.storage[type][namespace][key] = value;
      me.save();
      me.scope.$broadcast('change', type, namespace, key, value);
    };

    this.remove = function (type, namespace, key) {
      if (me.storage[type] && me.storage[type][namespace] && key) {
        delete me.storage[type][namespace][key];
      } else if (me.storage[type] && namespace) {
        delete me.storage[type][namespace];
      } else {
        delete me.storage[type];
      }

      me.save();
      me.scope.$broadcast('change', type, namespace, key);
    };

    this.replace = function (entries, type) {
      delete me.storage[type];
      // put for each entry
      angular.forEach(entries, function (entry) {
        if (angular.isUndefined(me.storage[type])) {
          me.storage[type] = {};
        }
        if (angular.isUndefined(me.storage[type][entry.namespace])) {
          me.storage[type][entry.namespace] = {};
        }
        me.storage[type][entry.namespace][entry.key] = entry.value;
      });

      // save and broadcast change
      me.save();
      me.scope.$broadcast('change', type);
    };

    this.getFromStorage();
  };
  return new Storage();
}]);
