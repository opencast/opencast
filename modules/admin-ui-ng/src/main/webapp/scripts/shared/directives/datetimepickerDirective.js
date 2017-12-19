angular.module('adminNg.directives')
.directive('datetimepicker', ['Language', function (Language) {

    function getCurrentLanguageCode() {
        var lc = Language.getLanguageCode() || 'en';
        lc = lc.replace(/\_.*/, ''); // remove long locale, as the datepicker does not support this
        return lc;
    }


    return {
        // Enforce the angularJS default of restricting the directive to // attributes only
        restrict: 'A',
        // Always use along with an ng-model
        require: '?ngModel',
        scope: {
            // This method needs to be defined and
            // passed in to the directive from the view controller
            select: '&' // Bind the select function we refer to the
            // right scope
        },
        link: function (scope, element, attrs, ngModel) {
            var dateValue;

            if (scope.params) {
                dateValue = new Date(scope.params);
            } else if (scope.$parent && scope.$parent.params && scope.$parent.params.value) {
                dateValue = new Date(scope.$parent.params.value);
            }


            if (ngModel) {

                var updateModel = function (date) {
                    scope.$apply(function () {
                        // Call the internal AngularJS helper to
                        // update the two-way binding
                        ngModel.$setViewValue(date.toISOString());
                    });
                };


                var optionsObj = {};
                optionsObj.timeInput = true;
                optionsObj.showButtonPanel = true;
                optionsObj.onClose = function () {
                    console.log('onClose');
                    var newDate = element.datetimepicker('getDate');
                    setTimeout(function(){
                        updateModel(newDate);
                        if (scope.select) {
                            scope.$apply(function () {
                                scope.select({date: newDate});
                            });
                        }
                    });
                };

                var lc = getCurrentLanguageCode();
                $.datepicker.setDefaults($.datepicker.regional[lc]);
                $.timepicker.setDefaults($.timepicker.regional[lc === 'en' ? '' : lc]);

                element.datetimepicker(optionsObj);
                element.datetimepicker('setDate', dateValue);
            }

            scope.$on('$destroy', function () {
                try {
                    element.datetimepicker('destroy');
                } catch (e) {}
            });
        }
    };
}]);
