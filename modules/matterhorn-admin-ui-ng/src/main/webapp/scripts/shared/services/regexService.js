angular.module('adminNg.services')
.service('RegexService', [function () {
    return {
        translateDateFormatToPattern: function(datePattern){
            if (angular.isString(datePattern) && datePattern.toLowerCase() === 'yyyy-mm-dd') {
                return '[0-9]{4}-[0-9]{2}-[0-9]{2}';
            }
        }
    };
}]);