// The main controller that all other scopes inherit from (except isolated scopes).
angular.module('adminNg.controllers')
.controller('ApplicationCtrl', ['$scope', '$rootScope', '$location', '$window', 'AuthService', 'Notifications', 'ResourceModal', 'VersionResource',
    function ($scope, $rootScope, $location, $window, AuthService, Notifications, ResourceModal, VersionResource) {

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
    }
]);
