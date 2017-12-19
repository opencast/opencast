angular.module('adminNg.resources')
.factory('SeriesMetadataResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
    var transform = function (data) {
        var metadata = {};
        try {
            metadata = JSON.parse(data);
            JsHelper.replaceBooleanStrings(metadata);
        } catch (e) { }
        return { entries: metadata };
    };

    return $resource('/admin-ng/series/:id/metadata:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transform
        },
        save: { method: 'PUT',
            isArray: true,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (catalog) {
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
            },
            tranformResponse: transform
        }
    });
}]);
