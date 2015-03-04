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
                console.log('proceeding to ', targetForm);
                return targetForm;
            }
            else {
                console.log('staying at ', currentForm);
                return currentForm;
            }
        };
    };

    return new FormNavigator();
});
