angular.module('adminNg.resources')
.factory('NewEventResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    return $resource('/admin-ng/event/new', {}, {
        save: {
            method: 'POST',

            // By setting ‘Content-Type’: undefined, the browser sets the
            // Content-Type to multipart/form-data for us and fills in the
            // correct boundary. Manually setting ‘Content-Type’:
            // multipart/form-data will fail to fill in the boundary parameter
            // of the request.
            headers: { 'Content-Type': undefined },

            responseType: 'text',

            transformResponse: [],

            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }

                // The end point expects a multipart request payload with two fields
                // 1. A form field called 'metadata' containing all userdata
                // 2. A non-form field either called 'presenter', 'presentation' or
                //    'audio' which contains a File object

                var fd = new FormData(), source, sourceType = data.source.type;

                source = {
                    type: sourceType
                };

                if (sourceType !== 'UPLOAD') {
                    source.metadata = {
                        start: JsHelper.toZuluTimeString(data.source[sourceType].start),
                        device: data.source[sourceType].device.name,
                        inputs: (function (inputs) {
                            var result = '';
                            angular.forEach(inputs, function (enabled, inputId) {
                                if (enabled) {
                                    result += inputId + ',';
                                }
                            });
                            // Remove the trailing comma for the last input
                            result = result.substring(0, result.length - 1);
                            return result;
                        })(data.source[sourceType].device.inputMethods)
                    };
                }

                if (sourceType === 'SCHEDULE_SINGLE') {
                    source.metadata.end = JsHelper.toZuluTimeString(data.source.SCHEDULE_SINGLE.start, data.source.SCHEDULE_SINGLE.duration);
                }

                if (sourceType === 'SCHEDULE_MULTIPLE') {
                    // We need to set it to the end time and day so the last day will be used in the recurrance and the correct end time is used
                    // for the rest of the recordings.
                    var endParts = JsHelper.getDateParts(data.source.SCHEDULE_MULTIPLE.end);
                    var end = {
                        date : data.source.SCHEDULE_MULTIPLE.end,
                        hour : parseInt(data.source.SCHEDULE_MULTIPLE.start.hour) + parseInt(data.source.SCHEDULE_MULTIPLE.duration.hour),
                        minute : parseInt(data.source.SCHEDULE_MULTIPLE.start.minute) + parseInt(data.source.SCHEDULE_MULTIPLE.duration.minute)
                    };
                    var endDate = new Date(endParts.year, endParts.month, endParts.day, end.hour , end.minute);
                    end.hour = endDate.getHours();
                    end.minute = endDate.getMinutes();
                    source.metadata.end = JsHelper.toZuluTimeString(end);
                    source.metadata.duration = (
                        parseInt(data.source.SCHEDULE_MULTIPLE.duration.hour, 10) * 60 * 60 * 1000 +
                        parseInt(data.source.SCHEDULE_MULTIPLE.duration.minute, 10) * 60 * 1000
                    ).toString();
                    source.metadata.rrule = (function (src) {
                        return JsHelper.assembleRrule(src.SCHEDULE_MULTIPLE);
                    })(data.source);
                }

                // Remove useless information for the request
                angular.forEach(data.metadata, function (catalog) {
                    angular.forEach(catalog.fields, function (field) {
                            delete field.collection;                        
                            delete field.label;
                            delete field.presentableValue;
                            delete field.readOnly;
                            delete field.required;
                    });
                });

                // Add metadata form field
                fd.append('metadata', JSON.stringify({
                    metadata: data.metadata,
                    processing: {
                        workflow: data.processing.workflow.id,
                        configuration: data.processing.workflow.selection.configuration
                    },
                    access: data.access,
                    source: source
                }));

                if (sourceType === 'UPLOAD') {
                    // Add file field, depending on its source
                    if (data.source.upload.audioOnly) {
                        fd.append('audio', data.source.upload.audioOnly);
                    }

                    if (data.source.upload.segmentable) {
                        fd.append('presentation', data.source.upload.segmentable);
                    }

                    if (data.source.upload.nonSegmentable) {
                        fd.append('presenter', data.source.upload.nonSegmentable);
                    }
                }

                return fd;
            }
        }
    });
}]);
