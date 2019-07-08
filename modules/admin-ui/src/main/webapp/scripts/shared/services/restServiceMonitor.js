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
.factory('RestServiceMonitor', ['$http', '$location', 'Storage', function($http, $location, Storage) {
  var Monitoring = {};
  var services = {
    service: {},
    error: false,
    numErr: 0
  };

  var AMQ_NAME = 'ActiveMQ';
  var STATES_NAME = 'Service States';
  var BACKEND_NAME = 'Backend Services';
  var MALFORMED_DATA = 'Malformed Data';
  var OK = 'OK';
  var SERVICES_FRAGMENT = '/systems/services';
  var SERVICE_NAME_ATTRIBUTE = 'service-name';

  Monitoring.run = function() {
    //Clear existing data
    services.service = {};
    services.error = false;
    services.numErr = 0;

    Monitoring.getActiveMQStats();
    Monitoring.getBasicServiceStats();
  };

  Monitoring.getActiveMQStats = function() {
    $http.get('/broker/status').then(function(data) {
      Monitoring.populateService(AMQ_NAME);
      if (data.status === 204) {
        services.service[AMQ_NAME].status = OK;
        services.service[AMQ_NAME].error = false;
      } else {
        services.service[AMQ_NAME].status = data.statusText;
        services.service[AMQ_NAME].error = true;
      }
    }).catch(function(err) {
      Monitoring.populateService(AMQ_NAME);
      services.service[AMQ_NAME].status = err.statusText;
      services.service[AMQ_NAME].error = true;
      services.error = true;
      services.numErr++;
    });
  };

  Monitoring.setError = function(service, text) {
    Monitoring.populateService(service);
    services.service[service].status = text;
    services.service[service].error = true;
    services.error = true;
    services.numErr++;
  };

  Monitoring.getBasicServiceStats = function() {
    $http.get('/services/health.json').then(function(data) {
      if (undefined === data.data || undefined === data.data.health) {
        Monitoring.setError(STATES_NAME, MALFORMED_DATA);
        return;
      }
      var abnormal = 0;
      abnormal = data.data.health['warning'] + data.data.health['error'];
      if (abnormal == 0) {
        Monitoring.populateService(BACKEND_NAME);
        services.service[BACKEND_NAME].status = OK;
      } else {
        Monitoring.getDetailedServiceStats();
      }
    }).catch(function(err) {
      Monitoring.setError(STATES_NAME, err.statusText);
    });
  };

  Monitoring.getDetailedServiceStats = function() {
    $http.get('/services/services.json').then(function(data) {
      if (undefined === data.data || undefined === data.data.services) {
        Monitoring.setError(BACKEND_NAME, MALFORMED_DATA);
        return;
      }
      angular.forEach(data.data.services.service, function(service, key) {
        var name = service.type.split('opencastproject.')[1];
        if (service.service_state != 'NORMAL') {
          Monitoring.populateService(name);
          services.service[name].status = service.service_state;
          services.service[name].error = true;
          services.error = true;
          services.numErr++;
        }
      });
    }).catch(function(err) {
      Monitoring.setError(BACKEND_NAME, err.statusText);
    });
  };

  Monitoring.populateService = function(name) {
    if (services.service[name] === undefined) {
      services.service[name] = {};
    }
  };

  Monitoring.jumpToServices = function(event) {
    var serviceName = null;
    if (event.target.tagName == 'a')
      serviceName = event.target.getAttribute(SERVICE_NAME_ATTRIBUTE);
    else
      serviceName = event.target.parentNode.getAttribute(SERVICE_NAME_ATTRIBUTE);

    if (serviceName != AMQ_NAME) {
      Storage.put('filter', 'services', 'actions', 'true');
      $location.path(SERVICES_FRAGMENT).replace();
    }
  };

  Monitoring.getServiceStatus = function() {
    return services;
  };

  return Monitoring;
}]);
