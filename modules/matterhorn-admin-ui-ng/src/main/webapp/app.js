/**
* Copyright 2009-2013 The Regents of the University of California
* Licensed under the Educational Community License, Version 2.0
* (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.osedu.org/licenses/ECL-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS"
* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*
*/
'use strict';

// File purpose: Declare app level module which depends on filters, and services

angular.module('LocalStorageModule').value('prefix', 'adminNg');
angular.module('adminNg', [
    'angularFileUpload',
    'pascalprecht.translate',
    'localytics.directives',
    'ui.sortable',
    'LocalStorageModule',
    'ngRoute',
    'ngResource',
    'ngAnimate',
    'ngMessages',
    'adminNg.controllers',
    'adminNg.services',
    'adminNg.filters',
    'adminNg.resources',
    'adminNg.services.language',
    'adminNg.services.table',
    'adminNg.services.modal',
    'adminNg.directives'
]).config(['$routeProvider', function ($routeProvider) {
    var firstCharToUpper = function (string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    };
    $routeProvider.when('/:category/:resource/:itemId/:subresource/:tab', {
        templateUrl: function (params) {
            return 'modules/' + params.resource + '/subresources/partials/' + params.subresource + '.html';
        },
        controller: function ($scope, $routeParams, $controller) {
            var capitalizedName = firstCharToUpper($routeParams.subresource);
            $controller(capitalizedName + 'Ctrl', {$scope: $scope});
        }
    });
    $routeProvider.when('/:category/:resource', {
        templateUrl: function (params) {
            return 'modules/' + params.category + '/partials/index.html';
        },
        controller: function ($scope, $routeParams, $controller) {
            var capitalizedName = $routeParams.resource.charAt(0).toUpperCase() + $routeParams.resource.slice(1);
            $controller(capitalizedName + 'Ctrl', {$scope: $scope});
        },
        reloadOnSearch: false
    });
    $routeProvider.otherwise({redirectTo: '/events/events'});
}])
.config(['LanguageProvider', '$translateProvider', function (LanguageProvider, $translateProvider) {
    LanguageProvider.setTranslateProvider($translateProvider);
}])
.config(['$translateProvider', function ($translateProvider) {
    var options = {
        'prefix': 'org/opencastproject/adminui/languages/lang-',
        'suffix': '.json'
    };
    $translateProvider.useLoader('customLanguageLoader', options);
    $translateProvider.preferredLanguage('en_US'); // This triggers the configuration process of our custom loader
    $translateProvider.useMissingTranslationHandler('customMissingTranslationHandler');
}])
.config(['$logProvider', function ($logProvider) {
    $logProvider.debugEnabled(false);
}])
.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

    // Try to anticipate a session logout and reload if it applies. When reloading,
    // Spring Security will redirect to the login page.
    $httpProvider.interceptors.push(['$q', function ($q) {
        return {
            response: function (response) {
                try {
                    if (response.data.indexOf('login-container') >= 0) {
                        if (window.location.pathname.indexOf('login.html') === -1) {
                            window.location.reload();
                        }
                    }
                } catch (e) {}
                return response || $q.when(response);
            }
        };
    }]);
}])
.run(['$rootScope', function ($rootScope) {
    // Define wrappers around non-mockable native functions.
    $rootScope.location = {};
    $rootScope.location.reload = window.location.reload;
}]);
