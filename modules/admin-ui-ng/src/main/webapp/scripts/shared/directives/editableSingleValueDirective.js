/**
 * @ngdoc directive
 * @name ng.directive:adminNgEditableSingleValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field
 * which will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required`, `value` and optionally `type` (defaults to 'text').
 * The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-single-value params="params" save="save"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableSingleValue', ['$timeout', 'JsHelper', function ($timeout, JsHelper) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableSingleValue.html',
        replace: true,
        scope: {
            params:     '=',
            save:       '='
        },
        link: function (scope, element) {
                // Parse the given time string (HH:mm) to separate the minutes / hours
            var parseTime = function (dateStr) {
                    var date = JsHelper.parseUTCTimeString(dateStr);
                    if (angular.isDate(date)) {
                        scope.params.hours = date.getHours();
                        scope.params.minutes = date.getMinutes();
                    }
                },
                // Check if the given value has two digits otherwise add a 0
                ensureTwoDigits = function (intValue) {
                    return (intValue < 10 ? '0' : '') + intValue;
                },
                // Format the value to be presented as string
                present = function (params) {
                    switch (params.type) {
                        case 'time':
                            if (angular.isUndefined(params.hours)) {
                                parseTime(params.value);
                            }
                            return ensureTwoDigits(params.hours) + ':' + ensureTwoDigits(params.minutes);
                        default:
                            return params.value;
                    }
                };

            scope.editMode = false;

            if (scope.params.type === 'time') {
                scope.hours = JsHelper.initArray(24);
                scope.minutes = JsHelper.initArray(60);
                parseTime(scope.params.value);
            }

            scope.presentableValue = present(scope.params);

            scope.enterEditMode = function () {
                // Store the original value for later comparision or undo
                if (!angular.isDefined(scope.original)) {
                    scope.original = scope.params.value;
                }
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                  if ((scope.params.type === 'text_long') && (element.find('textarea'))) {
                    element.find('textarea').focus();
                  } else if (element.find('input')) {
                    element.find('input').focus();
                  }
                });
            };

            scope.keyDown = function (event) {
		if (event.keyCode === 13 && !event.shiftKey) {
		    event.stopPropagation();
		    event.preventDefault();
		}
	    }


            scope.keyUp = function (event) {
                if (event.keyCode === 13 && !event.shiftKey) {
                    // Submit the form on ENTER
                    // Leaving the edit mode causes a blur which in turn triggers
                    // the submit action.
                    scope.editMode = false;
                    angular.element(event.target).blur();
                } else if (event.keyCode === 27) {
                    // Restore original value on ESC
                    scope.params.value = scope.original;
                    if (scope.params.type === 'time') {
                        parseTime(scope.params.value);
                    }
                    scope.editMode = false;
                    // Prevent the modal from closing.
                    event.stopPropagation();
                }
            };

            scope.submit = function () {
                if (scope.params.type === 'time') {
                    var newDate = new Date(0);
                    newDate.setHours(scope.params.hours, scope.params.minutes);
                    scope.params.value = ensureTwoDigits(newDate.getUTCHours()) + ':' +
                                         ensureTwoDigits(newDate.getUTCMinutes());
                }

                // Prevent submission if value has not changed.
                if (scope.params.value === scope.original) {
                    scope.editMode = false;
                    return;
                 }

                scope.presentableValue = present(scope.params);
                scope.editMode = false;

                if (!_.isUndefined(scope.params)) {
                    scope.save(scope.params.id);
                    scope.original = scope.params.value;
                    if (scope.params.type === 'time') {
                        parseTime(scope.params.value);
                    }
                }
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
       }
    };
}]);
