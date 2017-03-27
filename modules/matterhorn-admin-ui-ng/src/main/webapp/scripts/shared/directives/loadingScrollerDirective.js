/**
 * @ngdoc directive
 * @name adminNg.modal.adminNgLoadingScroller
 * @description
 * Checks for the scoll position of a multi-select element and calls a method to load additional data if needed
 *
 * Usage: Set this directive in the select element you need to load data for
 */
angular.module('adminNg.directives')
.directive('loadingScroller', [function () {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            var raw = element[0];

            element.bind('scroll', function () {
                if (raw.scrollTop + raw.offsetHeight > raw.scrollHeight) {
                    scope.$apply(attrs.adminNgLoadingScroller);
                }
            });
        }
    };
}]);
