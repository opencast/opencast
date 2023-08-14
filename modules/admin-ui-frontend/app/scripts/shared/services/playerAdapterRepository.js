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
.factory('PlayerAdapterRepository', ['$injector', function ($injector) {


  /**
     * A repository containing the adapter instances for given adapter type (may be html5, videojs) and given element.
     * The sole purpose of this implementation is to be able to manage adapters on a per instance basis - if there is
     * a better solution for this problem, this class becomes obvious.
     *
     * @constructor
     */
  var PlayerAdapterRepository = function () {
    var adapters = {};

    /**
         * Returns the given adapter instance per adapterType and elementId. If the adapter does not exist,
         * it will be created.
         *
         * @param adapterType
         * @param element of the player
         * @returns {*}
         */
    this.findByAdapterTypeAndElementId = function (adapterType, element) {
      if (typeof adapters[adapterType] === 'undefined') {
        // create entry for adapterType if not existent
        adapters[adapterType] = {};
      }

      if (typeof adapters[adapterType][element.id] === 'undefined') {
        adapters[adapterType][element.id] = this.createNewAdapter(adapterType, element);
      }
      return adapters[adapterType][element.id];
    };

    this.createNewAdapter = function (adapterType, element) {
      var factory, adapter;
      factory = $injector.get('PlayerAdapterFactory' + adapterType);
      adapter = factory.create(element);
      return adapter;
    };

  };

  return new PlayerAdapterRepository();
}]);
