angular.module('adminNg.directives')
.directive('preSelectFrom', [ 'underscore',
function (_) {

    return {
        restrict: 'A',
        require: ['ngModel'],
        template: '',
        replace: false,
        link: function ($scope, $element, $attr, ngModel) {

            if(angular.isUndefined($attr.preSelectFrom)) {
                console.error('directive preSelectFrom requires a value');
            }

            var unregister = $scope.$watch($attr.preSelectFrom, function (options) {
                if (!_.isUndefined(options)) {

                    // fix angular resource objects
                    if (_.has(options, 'toJSON')) {
                        options = options.toJSON();
                    }

                    if (_.size(options) === 1 && _.size(ngModel) > 0) { // supports objects and arrays
                        ngModel[0].$setViewValue(options[_.keys(options)[0]], 'myevent');
                        unregister();
                    }
                }
            });
        }
    };
}]);
