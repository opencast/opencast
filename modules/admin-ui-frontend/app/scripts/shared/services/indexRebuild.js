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

function indexRebuildStates($http) {
  var RebuildStates = {};
  var rebuildServices = {
    service: {},
    showRebuildStates: false,
  };

  var BACKEND_NAME = 'Rebuild Services';

  var WAITING_STATE = 'PENDING';
  var EXECUTED_STATE = 'RUNNING';
  var COMPLETED_STATE = 'OK';
  var FAILED_STATE = 'ERROR';

  RebuildStates.run = function () {
    // Clear existing data
    RebuildStates.resetService();
    RebuildStates.getRebuildStates();
  };

  RebuildStates.resetService = function () {
    rebuildServices.service = {};
    rebuildServices.showRebuildStates = false;
  };

  RebuildStates.setState = function (service, state, order) {
    RebuildStates.populateService(order);
    rebuildServices.service[order].status = state;
    rebuildServices.service[order].service = service;
    if (state === WAITING_STATE) {
      rebuildServices.service[order].pending = true;
    }
    else if (state === EXECUTED_STATE) {
      rebuildServices.service[order].running = true;
    }
    else if (state === COMPLETED_STATE) {
      rebuildServices.service[order].ok = true;
    }
    else {
      rebuildServices.service[order].error = true;
      rebuildServices.service[order].status = FAILED_STATE;
    }
  };

  RebuildStates.getRebuildStates = function () {
    $http.get('/index/rebuild/states.json').then(function (data) {
      if (undefined === data.data || undefined === data.data.services) {
        RebuildStates.setState(BACKEND_NAME, FAILED_STATE, 0);
        return;
      }
      angular.forEach(data.data.services.service, function (service) {
        var name = service.type;
        var state = service.state;
        var order = service.executionOrder;
        RebuildStates.setState(name, state, order);
        if (state != COMPLETED_STATE) {
          rebuildServices.showRebuildStates = true;
        }
      });
    }).catch(function (err) {
      rebuildServices.service = {};
      RebuildStates.setState(BACKEND_NAME, FAILED_STATE, 0);
    });
  };

  RebuildStates.populateService = function (order) {
    if (rebuildServices.service[order] === undefined) {
      rebuildServices.service[order] = {};
    }
  };

  RebuildStates.sortService = function () {
    var i = 0;
    var sortedService = {};
    angular.forEach(rebuildServices.service, function () {
      if (rebuildServices.service[i] != undefined) {
        sortedService.push(rebuildServices.service[i]);
        i++;
      }
    });
    rebuildServices.service = sortedService;
  };

  RebuildStates.getRebuildStatus = function () {
    RebuildStates.sortService();
    return rebuildServices;
  };

  return RebuildStates;
}

angular.module('adminNg.services')
    .factory('IndexRebuildStates', ['$http', indexRebuildStates]);
