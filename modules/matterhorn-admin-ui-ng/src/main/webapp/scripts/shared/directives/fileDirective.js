angular.module('adminNg.directives')
.directive('adminNgFileUpload', ['$parse', function ($parse) {
    return {
        link: function (scope, element, attrs) {
            var model = $parse(attrs.file),
                modelSetter = model.assign;

            element.bind('change', function () {
                scope.$apply(function () {
                    modelSetter(scope, element[0].files[0]);
                });
            });

            scope.$on('$destroy', function () {
                element.unbind('change');
            });
       }
    };
}]);
