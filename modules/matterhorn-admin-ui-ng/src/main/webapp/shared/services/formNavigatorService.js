angular.module('adminNg.services')
.factory('FormNavigatorService', function () {
    var FormNavigator = function () {
        this.navigateTo = function (targetForm, currentForm, requiredForms) {
            var valid = true;
            angular.forEach(requiredForms, function (form) {
                if (!form.$valid) {
                    valid = false;
                }
            });
            if (valid) {
                return targetForm;
            }
            else {
                return currentForm;
            }
        };
    };

    return new FormNavigator();
});
