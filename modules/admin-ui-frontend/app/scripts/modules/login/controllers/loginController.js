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
 * Controller for the login screen.
 *
 * The actual authentication is handled by the login form submit which
 * POSTs to `j_spring_security_check`. If the request succeeds, the
 * response will cause a redirect to the application. The URL for the
 * redirect is not configured in this Opencast module.
 *
 * If the request fails, the response will redirect to the current
 * page but add a `error` search parameter (without a value).
 */
angular.module('adminNg.controllers')
.controller('LoginCtrl', ['$scope', '$location', 'VersionResource', 'Language', '$rootScope',
  function ($scope, $location, VersionResource, Language, $rootScope) {

    $scope.isError = false;

    $scope.changeLanguage = function (language) {
      Language.changeLanguage(language);
    };


    // If authentication fails, the `error` search parameter will be
    // set.
    if ($location.absUrl().match(/\?error$/)) {
      $scope.isError = true;
    } else {
      VersionResource.query(function(response){
        $scope.version = response.version ? response : (angular.isArray(response.versions) ? response.versions[0] : {});
        if (!response.consistent) {
          $scope.version.buildNumber = 'inconsistent';
        }
      });
    }

    $rootScope.$on('language-changed', function () {
      $scope.currentLanguageCode = Language.getLanguageCode();
      $scope.currentLanguageName = Language.getLanguage().displayLanguage;
      $scope.availableLanguages = Language.getAvailableLanguages();
    });

    // For the logout please check the navigationController.
  }]);
