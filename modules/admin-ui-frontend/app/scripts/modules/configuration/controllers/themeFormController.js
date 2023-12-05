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

angular.module('adminNg.controllers')
.controller('ThemeFormCtrl', ['$scope', '$timeout', 'FormNavigatorService', 'Notifications', 'ThemeResource',
  'NewThemeResource', 'ThemeUsageResource', 'Table',
  function ($scope, $timeout, FormNavigatorService, Notifications, ThemeResource, NewThemeResource, ThemeUsageResource,
    Table) {
    var action = $scope.$parent.action;

    $scope.currentForm = 'generalForm';

    $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
      // We have to set the currentForm property here in the controller.
      // The reason for that is that the footer sections in the partial are decorated with ng-if, which
      // creates a new scope each time they are activated.
      $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
    };

    $scope.cancel = function () {
      $scope.close();
    };

    $scope.valid = function () {
      if (angular.isDefined($scope.themeForm)) {
        return $scope.themeForm.$valid;
      }
      return false;
    };

    if (action === 'add') {
      // Lets set some defaults first
      $scope.general = {
        'default': false
      };

      $scope.bumper = {
        'active': false
      };

      $scope.trailer = {
        'active': false
      };

      $scope.license = {
        'active': false
      };

      $scope.watermark = {
        'active': false,
        position: 'topRight'
      };

      $scope.titleslide = {
        'mode':'extract'
      };
    }

    if (action === 'edit') {
      // load resource
      fetchChildResources($scope.resourceId);
    }

    $scope.submit = function () {
      var messageId, userdata = {}, success, failure;
      success = function () {
        Notifications.add('success', 'THEME_CREATED');
        Notifications.remove(messageId);
        $timeout(function () {
          Table.fetch();
        }, 1000);
      };

      failure = function () {
        Notifications.add('error', 'THEME_NOT_CREATED');
        Notifications.remove(messageId);
      };

      // add message that never disappears
      messageId = Notifications.add('success', 'THEME_UPLOAD_STARTED', 'global', -1);
      userdata = {
        general: $scope.general,
        bumper: $scope.bumper,
        trailer: $scope.trailer,
        license: $scope.license,
        titleslide: $scope.titleslide,
        watermark: $scope.watermark
      };
      if (action === 'add') {
        NewThemeResource.save({}, userdata, success, failure);
      }
      if (action === 'edit') {
        ThemeResource.update({id: $scope.resourceId}, userdata, success, failure);
      }
      // close will not fetch content yet....
      $scope.close(false);
    };

    /**
         * This private function updates the scope with the data of a given theme.
         *
         * @param id the id of the theme
         */
    function fetchChildResources(id) {
      ThemeResource.get({id: id, format: '.json'}, function (response) {
        angular.forEach(response, function (obj, name) {
          $scope[name] = obj;
        });
        $scope.themeLoaded = true;
      });

      $scope.usage = ThemeUsageResource.get({themeId: id});
    }

    $scope.$on('change', function (event, id) {
      fetchChildResources(id);
    });

  }]);

