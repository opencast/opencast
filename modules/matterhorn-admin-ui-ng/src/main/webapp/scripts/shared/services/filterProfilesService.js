/**
* A service to manage filter profiles.
*
* Filter profiles will be stored in the localStorage.
*/
angular.module('adminNg.services')
.factory('FilterProfiles', ['localStorageService', function (localStorageService) {
    var FilterProfileStorage = function () {
        var me = this;

        this.getFromStorage = function () {
            this.storage = angular.fromJson(localStorageService.get('filterProfiles')) || {};
        };

        this.get = function (namespace) {
            return angular.copy(me.storage[namespace]) || [];
        };

        this.save = function () {
            localStorageService.add('filterProfiles', angular.toJson(me.storage));
        };

        this.set = function (namespace, value) {
            me.storage[namespace] = angular.copy(value);
            me.save();
        };

        this.getFromStorage();
    };
    return new FilterProfileStorage();
}]);
