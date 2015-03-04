/**
 * Copyright 2009-2013 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the 'License'); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an 'AS IS'
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
'use strict';

// A controller for global page navigation
angular.module('adminNg.controllers')
.controller('NavCtrl', ['$scope', '$rootScope', '$location', '$window', '$resource', '$routeParams', 'Language',
    function ($scope, $rootScope, $location, $window, $resource, $routeParams, Language) {
        // FIXME Move this information to the Language service so it can be
        // fetched via Language.getAvailableLanguages().

        $scope.category = $routeParams.category || $rootScope.category;

        $scope.availableLanguages = [];

        $scope.changeLanguage = function (key) {
            Language.changeLanguage(key);
        };

        $scope.toLanguageClass = function (language) {
            return Language.$convertLanguageToCode(language.code);
        };

        $rootScope.$on('language-changed', function () {
            $scope.currentLanguageCode = Language.getLanguageCode();
            $scope.currentLanguageName = Language.getLanguage().displayLanguage;
            $scope.availableLanguages = Language.getAvailableLanguages();
        });

        $scope.logout = function () {
            $window.location.href = $window.location.origin + '/j_spring_security_logout';
        };
    }
]);
