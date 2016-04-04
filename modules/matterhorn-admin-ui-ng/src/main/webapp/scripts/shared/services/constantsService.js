// TODO: Refactor this to use $resource, this was an experiment
angular.module('adminNg.services')
.provider('ConstantsService', function () {
    var me = this;
    this.$get = ['$http', function ($http) {
        var result = {};
        me.promise = $http({method: 'GET', url: '/admin-ng/resources/components.json'});
        me.promise.success(function (data) {
            me.constants = data;
            angular.forEach(data, function (value, key) {
                result[key] = value;
            });
        });
        return result;
    }];
});
