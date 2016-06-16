angular.module('adminNg.resources')
.factory('SeriesAccessResource', ['$resource', function ($resource) {
    var seriesResource,
        managedAclResource,
        accessResource;

    seriesResource = $resource('/admin-ng/series/:id/access.json', { id: '@id' }, {
        get: {  method: 'GET' }
    });

    accessResource = $resource('/admin-ng/series/:id/access', { id: '@id' }, {
        save: { method: 'POST',
                isArray: true,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {

                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    return $.param({
                        acl      : angular.toJson({acl: data.acl}),
                        override : true
                    });
                }
        }
    });

    managedAclResource = $resource('/acl-manager/acl/:id', { id: '@id'}, {
        get: { method: 'GET'}
    });

    return {
        getManagedAcl : managedAclResource.get,
        get           : seriesResource.get,
        save          : accessResource.save,
    };
}]);
