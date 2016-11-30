// The main controller that all other scopes inherit from (except isolated scopes).
angular.module('adminNg.controllers')
.controller('ApplicationCtrl', ['$scope', '$rootScope', '$location', '$window', 'AuthService', 'Notifications',
            'ResourceModal', 'VersionResource', 'HotkeysService', '$interval', 'RestServiceMonitor',
    function ($scope, $rootScope, $location, $window, AuthService, Notifications, ResourceModal,
              VersionResource, HotkeysService, $interval, RestServiceMonitor){

        $scope.bodyClicked = function () {
            angular.element('[old-admin-ng-dropdown]').removeClass('active');
        };

        var FEEDBACK_URL_PROPERTY = 'org.opencastproject.admin.feedback.url',
            DOCUMENTATION_URL_PROPERTY = 'org.opencastproject.admin.help.documentation.url',
            RESTDOCS_URL_PROPERTY = 'org.opencastproject.admin.help.restdocs.url',
            MEDIA_MODULE_URL_PROPERTY = 'org.opencastproject.admin.mediamodule.url';

        $scope.currentUser  = null;
        $scope.feedbackUrl = undefined;
        $scope.documentationUrl = undefined;
        $scope.restdocsUrl = undefined;
        $scope.mediaModuleUrl = undefined;
        RestServiceMonitor.setService('ActiveMQ');
        $scope.service = RestServiceMonitor.getServiceStatus();

        AuthService.getUser().$promise.then(function (user) {
            $scope.currentUser = user;

            if (angular.isDefined(user.org.properties[FEEDBACK_URL_PROPERTY])) {
                $scope.feedbackUrl = user.org.properties[FEEDBACK_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[DOCUMENTATION_URL_PROPERTY])) {
                $scope.documentationUrl = user.org.properties[DOCUMENTATION_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[RESTDOCS_URL_PROPERTY])) {
                $scope.restdocsUrl = user.org.properties[RESTDOCS_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[MEDIA_MODULE_URL_PROPERTY])) {
                $scope.mediaModuleUrl = user.org.properties[MEDIA_MODULE_URL_PROPERTY];
            }
        });

        //Running RestService on loop - updating $scope.service
        $interval(function(){
            RestServiceMonitor.run();
            $scope.service = RestServiceMonitor.getServiceStatus();
        }, 60000);

        $scope.toDoc = function () {
            if ($scope.documentationUrl) {
                $window.open ($scope.documentationUrl);
            } else {
                console.warn('Documentation Url is not set.');
            }
        };

        $scope.toRestDoc = function () {
          if ($scope.restdocsUrl) {
              $window.open ($scope.restdocsUrl);
          } else {
              console.warn('REST doc Url is not set.');
          }
        };

        $rootScope.userIs = AuthService.userIsAuthorizedAs;

        // Restore open modals if any
        var params = $location.search();
        if (params.modal && params.resourceId) {
            ResourceModal.show(params.modal, params.resourceId, params.tab, params.action);
        }

        if (angular.isUndefined($rootScope.version)) {
            VersionResource.query(function(response) {
                $rootScope.version = response.version ? response : (angular.isArray(response.versions)?response.versions[0]:{});
                if (!response.consistent) {
                    $rootScope.version.buildNumber = 'inconsistent';
                }
            });
        }

        HotkeysService.activateUniversalHotkey("general.event_view", "Open Events Table", function(event) {
            event.preventDefault();
            $location.path('/events/events').replace();
        });

        HotkeysService.activateUniversalHotkey("general.series_view", "Open Series Table", function(event) {
            event.preventDefault();
            $location.path('/events/series').replace();
        });

        HotkeysService.activateUniversalHotkey("general.new_event", "Create New Event", function(event) {
            event.preventDefault();
            ResourceModal.show("new-event-modal");
        });

        HotkeysService.activateUniversalHotkey("general.new_series", "Create New Series", function(event) {
            event.preventDefault();
            ResourceModal.show("new-series-modal");
        });

        HotkeysService.activateUniversalHotkey("general.help", "Show Help", function(event) {
            event.preventDefault();
            if(angular.element('#help-dd').hasClass('active')) {
              angular.element('#help-dd').removeClass('active');
            } else {
              angular.element('#help-dd').addClass('active');
            }
        })
    }
]);
