/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
'use strict';

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
                    //console.warn(e);
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

