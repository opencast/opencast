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

  var LATEST_VERSION_NAME = 'Latest Version';
  var AMQ_NAME = 'ActiveMQ';
  var STATES_NAME = 'Service States';
  var BACKEND_NAME = 'Backend Services';
  var MALFORMED_DATA = 'Malformed Data';
  var OK = 'OK';
  var SERVICES_FRAGMENT = '/systems/services';
  var SERVICE_NAME_ATTRIBUTE = 'service-name';
  var LATEST_VERSION_PATH = 'oc-version/version.json';
  var MY_VERSION_PATH = '/sysinfo/bundles/version?prefix=opencast';

  Monitoring.run = function() {
    //Clear existing data
    services.service = {};
    services.error = false;
    services.numErr = 0;

    Monitoring.getActiveMQStats();
    Monitoring.getBasicServiceStats();
    Monitoring.getVersionStats();
  };

  Monitoring.getVersionStats = function() {
    $http.get(MY_VERSION_PATH).then(function(response_my_version) {
      $http.get(LATEST_VERSION_PATH).then(function(response_latest_version) {
        Monitoring.populateService(LATEST_VERSION_NAME);

        if (response_latest_version.status === 200 && response_my_version.status === 200
        && response_my_version.data.consistent) {
          var my_version = response_my_version.data.version;
          var latest_version = response_latest_version.data;
          services.service[LATEST_VERSION_NAME].docs_url =
            'https://docs.opencast.org/r/' + parseInt(latest_version) + '.x/admin/';

          if (parseFloat(my_version) >= parseFloat(latest_version)
             || (my_version[0] == latest_version[0] && my_version.endsWith('SNAPSHOT'))) {
            services.service[LATEST_VERSION_NAME].status = OK;
            services.service[LATEST_VERSION_NAME].error = false;
          } else if (my_version[0] == latest_version[0]) {
            services.service[LATEST_VERSION_NAME].status = 'There is a minor update available.';
            services.service[LATEST_VERSION_NAME].error = true;
          } else if (parseInt(latest_version[0]) - parseInt(my_version[0]) < 2) {
            Monitoring.setError(LATEST_VERSION_NAME, 'There is a major update available.');
          } else {
            Monitoring.setError(LATEST_VERSION_NAME,
              'Version ' + my_version + ' of Opencast is no longer supported. Please update.');
          }
        } else {
          Monitoring.setError(LATEST_VERSION_NAME, 'Version not found');
          services.service[LATEST_VERSION_NAME].docs_url = 'https://docs.opencast.org';
        }
      });
    }).catch(function(err) {
      Monitoring.setError(LATEST_VERSION_NAME, err.statusText);
      services.service[LATEST_VERSION_NAME].docs_url = 'https://docs.opencast.org';
    });
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
      Monitoring.setError(AMQ_NAME, err.statusText);
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
