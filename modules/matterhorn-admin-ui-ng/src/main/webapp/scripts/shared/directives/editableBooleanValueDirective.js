angular.module('adminNg.directives')
.directive('adminNgEditableBooleanValue', function () {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableBooleanValue.html',
        replace: true,
        scope: {
            params:     '=',
            save:       '='
        },

        link: function (scope) {

            scope.submit = function () {
                // Prevent submission if value has not changed.
                if (scope.params.value === scope.original) { return; }

                scope.save(scope.params.id, function () {
                    scope.original = scope.params.value;
                });
            };
        }
    };
});
