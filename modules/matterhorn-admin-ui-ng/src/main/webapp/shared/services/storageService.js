/**
* A service to store arbitrary information without use of a backend.
*
* Information can either be stored in the localStorage or as a search
* parameter. Search parameters take precedence over localStorage
* values.
*
*/
angular.module('adminNg.services')
.factory('Storage', ['localStorageService', '$rootScope', '$location', function (localStorageService, $rootScope, $location) {
    var Storage = function () {
        var me = this;

        // Create a scope in order to broadcast changes.
        this.scope = $rootScope.$new();

        this.getFromStorage = function () {
            this.storage = angular.fromJson($location.search().storage) ||
                angular.fromJson(localStorageService.get('storage')) ||
                {};
        };

        this.get = function (type, namespace) {
            if (!me.storage[type]) {
                return {};
            }
            return me.storage[type][namespace] || {};
        };

        this.save = function () {
            var params = $location.search();
            params.storage = angular.toJson(me.storage);
            $location.search(params);
            localStorageService.add('storage', angular.toJson(me.storage));
        };

        this.put = function (type, namespace, key, value) {
            if (angular.isUndefined(me.storage[type])) {
                me.storage[type] = {};
            }
            if (angular.isUndefined(me.storage[type][namespace])) {
                me.storage[type][namespace] = {};
            }
            me.storage[type][namespace][key] = value;
            me.save();
            me.scope.$broadcast('change', type, namespace, key, value);
        };

        this.remove = function (type, namespace, key) {
            if (me.storage[type] && me.storage[type][namespace] && key) {
                delete me.storage[type][namespace][key];
            } else if (me.storage[type] && namespace) {
                delete me.storage[type][namespace];
            } else {
                delete me.storage[type];
            }

            me.save();
            me.scope.$broadcast('change', type, namespace, key);
        };

        this.getFromStorage();
    };
    return new Storage();
}]);
