/**
 * @ngdoc directive
 * @name ng.directive:adminNgEditableDateValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field
 * which will be supported by a date picker.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`. The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
 <doc:example>
 <doc:source>
 <div admin-ng-editable-date params="params" save="save"></div>
 </doc:source>
 </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableDateValue', ['$timeout', function ($timeout) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableDateValue.html',
        replace: true,
        scope: {
            params: '=',
            save: '='
        },
        link: function (scope, element) {
            scope.enterEditMode = function () {
                // Store the original value for later comparision or undo
                if (!angular.isDefined(scope.original)) {
                    scope.original = scope.params.value;
                }
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                    element.find('input').focus();
                });
            };

            scope.keyUp = function (event) {
                if (event.keyCode === 27) {
                    // Restore original value on ESC
                    scope.params.value = scope.original;
                    scope.editMode = false;
                    // Prevent the modal from closing.
                    event.stopPropagation();
                }
            };

            scope.submit = function () {
                // Prevent submission if value has not changed.
                if (scope.params.value === scope.original) { return; }

                scope.editMode = false;

                scope.save(scope.params.id, function () {
                    scope.original = scope.params.value;
                });
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
        }
    };
}]);
