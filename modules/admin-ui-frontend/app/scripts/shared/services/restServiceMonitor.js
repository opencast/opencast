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

function monitorService($http, $location, $translate, Storage) {
  var Monitoring = {};
  var services = {
    service: {},
    error: false,
    numErr: 0
  };

  var LATEST_VERSION_NAME = 'Latest Version';
  var REGISTRATION_NAME = 'Registration';
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

    Monitoring.getBasicServiceStats();
    Monitoring.getVersionStats();
    Monitoring.getRegistrationState();
  };

  Monitoring.getRegistrationState = function() {
    $http.get('/admin-ng/adopter/isUpToDate').then(function(data) {
      if (!('true' === data['data'])) {
        $translate('MONITOR.REGISTRATION').then(function(translation) {
          Monitoring.setWarning(REGISTRATION_NAME, translation);
        }).catch(angular.noop);
      }
    });
  };

  Monitoring.getVersionStats = function() {
    $http.get(MY_VERSION_PATH).then(function(response_my_version) {
      $http.get(LATEST_VERSION_PATH).then(function(response_latest_version) {
        Monitoring.populateService(LATEST_VERSION_NAME);
        if (response_latest_version.status === 200 && response_my_version.status === 200
        && response_my_version.data.consistent && response_latest_version.data != '') {
          var my_version = response_my_version.data.version,
              latest_version = response_latest_version.data;
          services.service[LATEST_VERSION_NAME].docs_url =
            'https://docs.opencast.org/r/' + parseInt(latest_version) + '.x/admin/';

          // Check if this is the latest major.minor version available
          if (parseFloat(my_version) >= parseFloat(latest_version)
             || (parseInt(my_version) == parseInt(latest_version) && my_version.endsWith('SNAPSHOT'))) {
            services.service[LATEST_VERSION_NAME].status = OK;
            services.service[LATEST_VERSION_NAME].error = false;

          // Check if this is the latest major but not the latest minor version
          } else if (parseInt(my_version) == parseInt(latest_version)) {
            $translate('UPDATE.MINOR').then(function(translation) {
              Monitoring.setWarning(LATEST_VERSION_NAME, translation);
            }).catch(angular.noop);

          // Check if this is still supported even though it is not the latest major version
          } else if (parseInt(latest_version) - parseInt(my_version) < 2) {
            $translate('UPDATE.MAJOR').then(function(translation) {
              Monitoring.setWarning(LATEST_VERSION_NAME, translation);
            }).catch(angular.noop);

          // This version is no longer supported
          } else {
            $translate('UPDATE.UNSUPPORTED', {'version': my_version}).then(function(translation) {
              Monitoring.setError(LATEST_VERSION_NAME, translation);
            }).catch(angular.noop);
          }

        // Couldn't determine this Opencast's version or what the latest version is
        } else {
          $translate('UPDATE.UNDETERMINED').then(function(translation) {
            Monitoring.setWarning(LATEST_VERSION_NAME, translation);
          }).catch(angular.noop);
          services.service[LATEST_VERSION_NAME].docs_url = 'https://docs.opencast.org';
        }
      });
    }).catch(function(err) {
      Monitoring.setError(LATEST_VERSION_NAME, err.statusText);
      services.service[LATEST_VERSION_NAME].docs_url = 'https://docs.opencast.org';
    });
  };

  Monitoring.setWarning = function(service, text) {
    Monitoring.populateService(service);
    services.service[service].status = text;
    services.service[service].warning = true;
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
    Storage.put('filter', 'services', 'actions', 'true');
    $location.path(SERVICES_FRAGMENT).replace();
  };

  Monitoring.getServiceStatus = function() {
    return services;
  };

  return Monitoring;
}

angular.module('adminNg.services')
.factory('RestServiceMonitor', ['$http', '$location', '$translate', 'Storage', monitorService]);
