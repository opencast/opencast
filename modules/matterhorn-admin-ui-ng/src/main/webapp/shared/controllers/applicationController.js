// The main controller that all other scopes inherit from (except isolated scopes).
angular.module('adminNg.controllers')
.controller('ApplicationCtrl', ['$scope', '$rootScope', '$location', '$window', 'AuthService', 'Notifications', 'ResourceModal',
    function ($scope, $rootScope, $location, $window, AuthService, Notifications, ResourceModal) {

        $scope.bodyClicked = function () {
            angular.element('[old-admin-ng-dropdown]').removeClass('active');
        };

        var FEEDBACK_URL_PROPERTY = 'org.opencastproject.admin.feedback.url',
            DOCUMENTATION_URL_PROPERTY = 'org.opencastproject.admin.documentation.url';

        $scope.currentUser  = null;
        $scope.feedbackUrl = undefined;
        $scope.documentationUrl = undefined;

        AuthService.getUser().$promise.then(function (user) {
            $scope.currentUser = user;

            if (angular.isDefined(user.org.properties[FEEDBACK_URL_PROPERTY])) {
                $scope.feedbackUrl = user.org.properties[FEEDBACK_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[DOCUMENTATION_URL_PROPERTY])) {
                $scope.documentationUrl = user.org.properties[DOCUMENTATION_URL_PROPERTY];
            }
        });

        $scope.toDoc = function () {
            if ($scope.documentationUrl) {
                $window.location.href = $scope.documentationUrl;
            } else {
                console.warn('Documentation Url is not set.');
            }
        };

        $rootScope.userIs = AuthService.userIsAuthorizedAs;

        // Restore open modals if any
        var params = $location.search();
        if (params.modal && params.resourceId) {
            ResourceModal.show(params.modal, params.resourceId, params.tab, params.action);
        }
    }
]);
