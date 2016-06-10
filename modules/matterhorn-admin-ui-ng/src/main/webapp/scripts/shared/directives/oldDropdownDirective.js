angular.module('adminNg.directives')
.directive('oldAdminNgDropdown', function () {
    return {
        scope: {
            fn: '='
        },
        link: function ($scope, element) {
            element.on('click', function (event) {
                event.stopPropagation();
                angular.element(this).toggleClass('active');
            });

            $scope.$on('$destroy', function () {
                element.off('click');
            });
        }
    };
});
