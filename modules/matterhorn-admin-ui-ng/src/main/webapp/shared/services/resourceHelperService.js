/**
 * @ngdoc service
 * @name adminNg.modal
 * @description
 * Provides a service for displaying details of table records.
 */
angular.module('adminNg.services')
    .factory('ResourceHelper',
    function () {

        /**
         * Returns input at it is. Can be used if no result transformation is necessary.
         * @param input
         * @returns {*}
         */
        var defaultMapper = function (input) {
            return input;
        };

        return {
            parseResponse: function (responseBody, mapper) {

                var effectiveMapper = mapper || defaultMapper;

                var result = [], data = {};

                try {
                    data = JSON.parse(responseBody);     

                    // Distinguish between single and multiple values
                    if ($.isArray(data.results)) {
                        angular.forEach(data.results, function (event) {
                            result.push(effectiveMapper(event));
                        });
                    } else {
                        result.push(effectiveMapper(data.results));
                    }
                } catch (e) {
                    console.warn(e);
                }

                // guard against empty dataset
                if (angular.isUndefined(data.results)) {
                    return;
                }


                return {
                    rows: result,
                    total: data.total,
                    offset: data.offset,
                    count: data.count,
                    limit: data.limit
                };
            }
        };
    });

