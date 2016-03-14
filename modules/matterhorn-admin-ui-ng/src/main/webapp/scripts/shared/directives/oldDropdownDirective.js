angular.module('adminNg.directives')
.directive('oldAdminNgDropdown', function () {
    return {
        scope: {
            fn: '='
        },
        link: function ($scope, element) {
            //FIXME: Replace with proper angular-ui, or a global event
            element.on('click', function (event) {

                event.preventDefault();
                event.stopPropagation();

                var tagName = event.target.tagName, value = event.target.innerHTML;


                if (tagName === 'DIV' || tagName === 'SPAN' || tagName === 'I') {
                    angular.element('.drop-down-container, .nav-dd').not(this).removeClass('active');
                    angular.element(this).toggleClass('active');
                } else if (tagName === 'A') {
                    // makes the dropdown invisible after selection
                    angular.element(this).toggleClass('active');
                    if (angular.isDefined(value) && value.length > 0 && angular.isDefined($scope.fn)) {
                        $scope.fn.apply(event, [event.target.innerHTML]);
                    }
                }
            });

            $scope.$on('$destroy', function () {
                element.off('click');
            });
        }
    };
});
