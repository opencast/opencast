/**
 * @ngdoc directive
 * @name adminNg.modules.events.validators.taskStartableValidator
 * @description
 * Checks if the chosen selection is valid. We would have expected ng-required to suffice, but with this chosen component, it doesn't.
 *
 */
angular.module('adminNg.directives')
.directive('notEmptySelection', ['Notifications', function (Notifications) {
    var link = function (scope, elm, attrs, ctrl) {
        scope, elm, attrs, ctrl, Notifications;
        var $scope = scope;
        ctrl.$validators.notEmptySelection = function () {
            var workflow = $scope.processing.ud.workflow;
            if (angular.isDefined(workflow)) {
                if (angular.isObject(workflow.selection)) {
                    return angular.isDefined(workflow.selection.id) && workflow.selection.id.length > 0;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        };
    };

    return {
        require: 'ngModel',
        link: link
    };
}]);
