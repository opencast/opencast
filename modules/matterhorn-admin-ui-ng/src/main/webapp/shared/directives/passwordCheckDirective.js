/**
 * @ngdoc directive
 * @name adminNg.modal.adminNgPwCheck
 * @description
 * Checks a password for correct repetition.
 *
 * Usage: Set this directive in the input element of the password.
 *
 * @example
 * <input admin-ng-pw-check="model.repeatedPassword"/>
 */
angular.module('adminNg.directives')
.directive('adminNgPwCheck', function () {
    return {
        require: 'ngModel',
        link: function (scope, elem, attrs, ctrl) {
            scope.deregisterWatch = scope.$watch(attrs.adminNgPwCheck, function (confirmPassword) {
                var isValid = ctrl.$viewValue === confirmPassword;
                ctrl.$setValidity('pwmatch', isValid);
            });

            scope.$on('$destroy', function () {
                scope.deregisterWatch();
            });
        }
    };
});
