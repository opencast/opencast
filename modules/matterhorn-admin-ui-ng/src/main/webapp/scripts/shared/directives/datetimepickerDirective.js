angular.module('adminNg.directives')
.directive('datetimepicker', ['Language', function (Language) {

    function getCurrentLanguageCode() {
        var lc = Language.getLanguageCode();
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

            var defaultDate;

            if (scope.params) {
                defaultDate = new Date(scope.params);
            } else if (scope.$parent && scope.$parent.params && scope.$parent.params.input) {
                defaultDate = scope.$parent.params.input;
            }


            if (ngModel) {

                var updateModel = function (dateTxt) {
                    scope.$apply(function () {
                        // Call the internal AngularJS helper to
                        // update the two-way binding
                        ngModel.$setViewValue(dateTxt);
                    });
                };


                var optionsObj = {};
                optionsObj.defaultDate = defaultDate;
                optionsObj.dateFormat = "dd/mm/yy','";
                optionsObj.timeFormat = "HH:mm:ss";
                optionsObj.showButtonPanel = true;
                optionsObj.onSelect = function (dateTxt) {
                    console.log("DateTxt: " + dateTxt);
                    var day = parseInt(dateTxt.substring(0, dateTxt.indexOf("/"))),
                        month = parseInt(dateTxt.substring(dateTxt.indexOf("/")+1, dateTxt.lastIndexOf("/"))) - 1,
                        year = parseInt(dateTxt.substring(dateTxt.lastIndexOf("/")+1, dateTxt.indexOf(","))),
                        hour = parseInt(dateTxt.substring(dateTxt.indexOf(" ")+1, dateTxt.indexOf(":"))),
                        minute = parseInt(dateTxt.substring(dateTxt.indexOf(":")+1, dateTxt.lastIndexOf(":"))),
                        second = parseInt(dateTxt.substring(dateTxt.lastIndexOf(":")+1)),
                        newDate = new Date(year, month, day, hour, minute, second);
                    console.log("Date: " + day +"."+month+"."+year + ", " + hour + ":"+minute+":" + second);
                    console.log(newDate.toISOString());
                    setTimeout(function(){
                        updateModel(newDate);
                        if (scope.select) {
                            scope.$apply(function () {
                                scope.select({date: newDate});
                            });
                        }
                    });

                    $.datepicker._hideDatepicker();
                };

                $.datepicker.setDefaults($.datepicker.regional[getCurrentLanguageCode()]);

                element.datetimepicker(optionsObj);
            }

            scope.$on('$destroy', function () {
                try {
                    element.datepicker('destroy');
                } catch (e) { }
            });
        }
    };
}]);
