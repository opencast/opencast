/**
 * @ngdoc directive
 * @name adminNg.modules.events.validators.taskStartable
 * @description
 * Checks if the given value equals ARCHIVE.
 *
 */
angular.module('adminNg.directives')
.directive('taskStartable', ['Notifications', function (Notifications) {
    var link = function (scope, elm, attrs, ctrl) {
        scope, elm, attrs, ctrl, Notifications;
        ctrl.$validators.taskStartable = function (modelValue, viewValue) {
            if (viewValue) {
                if (angular.isDefined(attrs.taskStartable) &&
                    attrs.taskStartable.toUpperCase().indexOf('ARCHIVE') === 0) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return true;
            }
        };
    };

    return {
        require: 'ngModel',
        link: link
    };
}]);
