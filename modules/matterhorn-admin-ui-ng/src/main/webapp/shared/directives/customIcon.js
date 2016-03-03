angular.module('adminNg.directives')
    .directive('customIcon', function() {
    return function($scope, element, attrs) {
        attrs.$observe('customIcon', function(url) {
            if (angular.isDefined(url) && url !== '') {
                element.css({
                    'background-image': 'url(' + url +')'
                });
            }
        });
    };
});
