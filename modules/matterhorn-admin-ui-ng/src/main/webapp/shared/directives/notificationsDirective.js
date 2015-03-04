angular.module('adminNg.directives')
.directive('adminNgNotifications', ['Notifications', function (Notifications) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/notifications.html',
        replace: true,
        scope: {
            context: '@',
        },
        link: function (scope) {
            var updateNotifications = function (context) {
                if (angular.isUndefined(scope.context)) {
                    scope.context = 'global';
                }

                if (scope.context === context ) {
                    scope.notifications = Notifications.get(scope.context);
                }
            };

            scope.notifications = Notifications.get(scope.context);

            scope.deregisterAdd = Notifications.$on('added', function (event, context) {
                updateNotifications(context);
            });

            scope.deregisterDelete = Notifications.$on('deleted', function (event, context) {
                updateNotifications(context);
            });

            scope.$on('$destroy', function () {
                scope.deregisterAdd();
                scope.deregisterDelete();
                Notifications.$destroy();
            });
        }
    };
}]);
