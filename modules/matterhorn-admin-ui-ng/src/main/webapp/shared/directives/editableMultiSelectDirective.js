/**
 * @ngdoc directive
 * @name ng.directive:adminNgEditableMultiSelect
 *
 * @description
 * Upon click on its label, this directive will display an <input> field and
 * currently selected values. They will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`.
 * The "collection" attribute contains a hash of objects (or a promise thereof)
 * which maps values to their labels.
 * The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-multi-select params="params" save="save" collection="collection"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableMultiSelect', ['$timeout', function ($timeout) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableMultiSelect.html',
        replace: true,
        scope: {
            params:     '=',
            collection: '=',
            mixed:      '=',
            save:       '='
        },
        link: function (scope, element) {
            scope.enterEditMode = function () {
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                    element.find('input').focus();
                });
            };

            scope.leaveEditMode = function () {
                scope.editMode = false;
                scope.value = '';
            };

            scope.addValue = function (model, value) {
                if (value && model.indexOf(value) === -1) {
                    model.push(value);
                    scope.editMode = false;
                }
                scope.submit();
            };

            scope.removeValue = function (model, value) {
                model.splice(model.indexOf(value), 1);
                scope.submit();
            };

            scope.keyUp = function (event) {
                var value = event.target.value;
                if (event.keyCode === 13) {
                    // ENTER
                    if (scope.mixed || scope.collection[scope.value]) {
                        scope.addValue(scope.params.value, scope.value);
                    }
                } else if (event.keyCode === 27) {
                    // ESC
                    scope.editMode = false;
                } else if (value.length >= 2) {
                    // TODO update the collection
                    scope.collection = scope.collection;
                }
                event.stopPropagation();
            };

            scope.getText = function (value) {
                if (angular.isDefined(scope.collection[value])) {
                    return scope.collection[value];
                } else {
                    return value;
                }
            };

            scope.submit = function () {
                scope.save(scope.params.id);
                scope.editMode = false;
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
       }
    };
}]);
