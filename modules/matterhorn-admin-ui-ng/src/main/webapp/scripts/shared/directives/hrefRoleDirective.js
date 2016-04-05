/**
 * @ngdoc directive
 * @name adminNg.directives.adminNgHrefRole
 * @description
 * Overrides the 'href' attribute of an element depending on the user's roles.
 *
 * @example
 * <a admin-ng-href-role="{'STUDENT': '/student', 'TEACHER': '/teacher'}" href="/general" />
 */
angular.module('adminNg.directives')
.directive('adminNgHrefRole', ['AuthService', function (AuthService) {
    return {
        scope: {
            rolesHref: '@adminNgHrefRole'
        },
        link: function ($scope, element) {
            var href;
            angular.forEach($scope.$eval($scope.rolesHref), function(value, key) {
                if (AuthService.userIsAuthorizedAs(key) && angular.isUndefined(href)) {
                    href = value;
                }
            });

            if (angular.isDefined(href)) {
                element.attr('href', href);
            }
        }
    };
}]);
