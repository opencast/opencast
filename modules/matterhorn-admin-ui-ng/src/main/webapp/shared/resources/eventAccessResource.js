angular.module('adminNg.resources')
.factory('EventAccessResource', ['$resource', function ($resource) {
    var transform = function (data) {
            var metadata = {};
            try {
                metadata = JSON.parse(data);
            } catch (e) { }
            return metadata;
        },
        eventResource,
        accessResource,
        managedAclResource;

    eventResource = $resource('/admin-ng/event/:id/access.json', { id: '@id' }, {
        get: { method: 'GET', transformResponse: transform }
    });

    accessResource = $resource('/admin-ng/event/:id/access', { id: '@id' }, {
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
        get: eventResource.get,
        save: accessResource.save
    };
}]);
