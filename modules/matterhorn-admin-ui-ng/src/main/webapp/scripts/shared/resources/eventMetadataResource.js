angular.module('adminNg.resources')
.factory('EventMetadataResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
    var transformResponse = function (data) {
            var metadata = {};
            try {
                metadata = JSON.parse(data);
                angular.forEach(metadata, function (catalog) {
                    if (angular.isDefined(catalog.locked)) {
                        angular.forEach(catalog.fields, function (field) {
                            field.locked = catalog.locked;
                            field.readOnly = true;
                        });
                    }
                });
                JsHelper.replaceBooleanStrings(metadata);
            } catch (e) { }
            return { entries: metadata };
        },
        transformRequest = function (catalog) {
            if (catalog.attributeToSend) {
                var catalogToSave = {
                    flavor: catalog.flavor,
                    title: catalog.title,
                    fields: []
                };

                angular.forEach(catalog.fields, function (entry) {
                    if (entry.id === catalog.attributeToSend) {
                        catalogToSave.fields.push(entry);
                    }
                });
                return $.param({metadata: angular.toJson([catalogToSave])});
            }
            return $.param({metadata: angular.toJson([catalog])});
        };

    return $resource('/admin-ng/event/:id/metadata:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        },
        save: {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformResponse: transformResponse,
            transformRequest:  transformRequest
        }
    });
}]);
