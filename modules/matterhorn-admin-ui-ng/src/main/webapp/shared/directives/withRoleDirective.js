/**
 * @ngdoc directive
 * @name adminNg.directives.withRole
 * @description
 * Remove the element if the current user does not have the given role.
 *
 * @example
 * <a with-role="ROLE_USER">New event</a>
 */
angular.module('adminNg.directives')
.directive('withRole', ['AuthService', function (AuthService) {
    return {
        priority: 1000,
        link: function ($scope, element, attr) {
            element.addClass('hide');

            AuthService.userIsAuthorizedAs(attr.withRole, function () {
                element.removeClass('hide');
            }, function () {
                element.remove();
            });
        }
    };
}]);
