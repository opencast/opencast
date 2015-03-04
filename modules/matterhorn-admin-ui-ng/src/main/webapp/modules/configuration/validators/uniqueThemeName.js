/**
 * @ngdoc directive
 * @name adminNg.modules.configuration.validators.uniquerThemeName
 * @description
 * Makes sure that the model name is unique.
 *
 */
angular.module('adminNg.directives')
.directive('uniqueThemeName', ['ThemesResource', 'Notifications', function (ThemesResource, Notifications) {
    var existingModelValue, link;
    link = function (scope, elm, attrs, ctrl) {
        var existingThemes;
        if (angular.isUndefined(existingThemes)) {
            existingThemes = ThemesResource.get();
        }
        ctrl.$validators.uniqueTheme = function (modelValue, viewValue) {
            var result = true;
            if (!ctrl.$dirty) {
                if (angular.isDefined(ctrl.$modelValue)) {
                    existingModelValue = ctrl.$modelValue;
                }
                return true;
            }
            if (ctrl.$isEmpty(viewValue)) {
                Notifications.add('error', 'THEME_NAME_EMPTY', 'new-theme-general');
                // consider empty models to be invalid
                result = false;
            }
            else {
                if (angular.isDefined(existingModelValue)) {
                    if (existingModelValue === viewValue) {
                        return true; // thats ok
                    }
                }
                angular.forEach(existingThemes.results, function (theme) {
                    if (theme.name === viewValue) {
                        Notifications.add('error', 'THEME_NAME_ALREADY_TAKEN', 'new-theme-general');
                        result = false;
                    }
                });
            }

            // it is invalid
            return result;
        };
    };
    return {
        require: 'ngModel',
        link: link
    };
}]);
