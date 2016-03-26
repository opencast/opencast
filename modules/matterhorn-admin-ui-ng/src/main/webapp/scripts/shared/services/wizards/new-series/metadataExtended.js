angular.module('adminNg.services')
.factory('NewSeriesMetadataExtended', ['NewSeriesMetadataResource', function (NewSeriesMetadataResource) {
    var MetadataExtended = function () {
        var me = this, i;

        // As soon as the required metadata fields arrive from the backend,
        // we check which are mandatory.
        // This information will be needed in order to tell if we can move
        // on to the next page of the wizard.
        this.postProcessMetadata = function (data) {
            var fields = [], chunk;
            for (chunk in data) {
                if (data.hasOwnProperty(chunk)) {
                    // extended metadata is every object in the returned data which
                    // does not start with a dollar sign and which isn't dublincore/episode
                    if (chunk !== 'dublincore/series' && chunk.charAt(0) !== '$') {
                        me.ud[chunk] = {fields: data[chunk].fields};
                        me.ud[chunk].flavor = data[chunk].flavor;
                        me.ud[chunk].title = data[chunk].title;
                        fields = fields.concat(data[chunk].fields);
                    }
                }
            }
            // we go for the extended metadata here
            if (fields.length > 0) {
                for (i = 0; i < fields.length; i++) {
                    if (fields[i].required) {
                        me.requiredMetadata[fields[i].id] = false;
                        if (fields[i].type === 'boolean') {
                            // set all boolean fields to false by default
                            fields[i].value = false;
                        }
                    }
                }
                me.visible = true;
            }
            else {
                me.visible = false;
            }
        };

        // Checks if the current state of this wizard is valid and we are
        // ready to move on.
        this.isValid = function () {
            var result = true;
            //FIXME: The angular validation should rather be used,
            // unfortunately it didn't work in this context.
            angular.forEach(me.requiredMetadata, function (item) {
                if (item === false) {
                    result = false;
                }
            });
            return result;
        };

        this.save = function (scope) {
            //FIXME: This should be nicer, rather propagate the id and values
            //instead of looking for them in the parent scope.
            var params = scope.$parent.params,
                target = scope.$parent.target,
                fieldId = params.id,
                value = params.value;

            if (params.collection) {
                if (angular.isArray(value)) {
                    params.presentableValue = value;
                } else {
                    params.presentableValue = params.collection[value];
                }
            } else {
                params.presentableValue = value;
            }

            me.ud[target].fields[fieldId] = params;

            if (!angular.isUndefined(me.requiredMetadata[fieldId])) {
                if (angular.isDefined(value) && value.length > 0) {
                    // we have received a required value
                    me.requiredMetadata[fieldId] = true;
                } else {
                    // the user has deleted the value
                    me.requiredMetadata[fieldId] = false;
                }
            }
        };

        this.getFiledCatalogs = function () { 
            var catalogs = [];

            angular.forEach(me.ud, function(catalog) {
                var empty = true;
                angular.forEach(catalog.fields, function (field) {
                    if (angular.isDefined(field.presentableValue) && field.presentableValue !=='') {
                        empty = false;
                    }
                });

                if (!empty) {
                    catalogs.push(catalog);
                }
            });

            return catalogs;
        };

        this.reset = function () {
            me.ud = {};
            me.requiredMetadata = {};
            me.metadata = NewSeriesMetadataResource.get(me.postProcessMetadata);
        };

        this.getUserEntries = function () {
            return me.ud;
        };

        this.reset();
    };

    return new MetadataExtended();
}]);
