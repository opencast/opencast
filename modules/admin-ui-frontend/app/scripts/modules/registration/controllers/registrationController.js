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

// Controller for creating a new event. This is a wizard, so it implements a state machine design pattern.
angular.module('adminNg.controllers')
.controller('RegistrationCtrl', ['$scope', '$timeout', 'Table', 'AdopterRegistrationStates',
  'AdopterRegistrationResource', 'CountryResource', 'NewEventStates', 'NewEventResource', 'EVENT_TAB_CHANGE',
  'Notifications', 'Modal', 'AuthService',
  function ($scope, $timeout, Table, AdopterRegistrationStates, AdopterRegistrationResource, CountryResource,
    NewEventStates, NewEventResource, EVENT_TAB_CHANGE, Notifications, Modal, AuthService) {

    $scope.state = AdopterRegistrationStates.getInitialState($scope.$parent.resourceId);
    $scope.states = AdopterRegistrationStates.get($scope.$parent.resourceId);
    $scope.countries = CountryResource.getCountries();
    $scope.adopter = new AdopterRegistrationResource();

    document.getElementById('help-dd').classList.remove('active');

    // Filling the form fields
    AdopterRegistrationResource.get({}, function (adopter) {
      for (var field in adopter) {
        if(field === 'registered') {
          $scope.registered = adopter[field];
          continue;
        }
        $scope.adopter[field] = adopter[field];
      }
    });


    $scope.nextState = function (inputAction) {
      if ($scope.state === 'form' && (inputAction === 1 || inputAction === 3)) { // 1:Save, 3:Update
        if ($scope.adopterRegistrationForm.$invalid || !$scope.adopter.agreedToPolicy) {
          return;
        }
      }

      $scope.state = $scope.states[$scope.state]['nextState'][inputAction];
      if($scope.state === 'close'){
        $scope.close();
      } else if($scope.state === 'skip') {
        $scope.notNow();
      } else if($scope.state === 'save') {
        $scope.save();
      } else if($scope.state === 'update') {
        $scope.updateProfile();
      } else if($scope.state === 'delete') {
        $scope.deleteProfile();
      }
    };


    $scope.save = function () {
      if($scope.adopterRegistrationForm.$valid) {
        AuthService.getUser().$promise.then(function() {
          $scope.adopter.registered = true;
          AdopterRegistrationResource.create({}, $scope.adopter,
            function ($response, header) {
              // success callback
              $scope.nextState(0);
            }, function(error) {
              // error callback
              $scope.nextState(1);
            });
        }).catch(angular.noop);
      }
    };


    $scope.notNow = function () {
      AuthService.getUser().$promise.then(function() {
        $scope.registered = false;
        AdopterRegistrationResource.create({}, $scope.adopter,
          function ($response, header) {
            // success callback
          }, function(error) {
            // error callback
          });
      }).catch(angular.noop);
    };


    $scope.deleteProfile = function () {
      AuthService.getUser().$promise.then(function() {
        $scope.adopter.registered = true;
        AdopterRegistrationResource.delete(
          function ($response, header) {
            $scope.nextState(0);
          }, function(error) {
            $scope.nextState(1);
          });
      }).catch(angular.noop);
    };


    $scope.close = function () {
      Modal.$scope.close();
    };


  }]);
