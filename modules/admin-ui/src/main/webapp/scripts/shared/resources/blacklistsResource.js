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

angular.module('adminNg.resources')
.factory('BlacklistsResource', ['$resource', '$filter', 'Language', 'JsHelper',
function ($resource, $filter, Language, JsHelper) {

    var parse = function (r, type) {
        var row = {};
        row.id            = r.id;
        row.resourceName  = r.resource.name;
        row.resourceId    = r.resource.id;
        row.date_from     = Language.formatDateTime('short', r.start);
        row.date_to       = Language.formatDateTime('short', r.end);
        row.date_from_raw = Language.formatDateTimeRaw('short', r.start);
        row.date_to_raw   = Language.formatDateTimeRaw('short', r.end);
        row.reason        = r.purpose;
        row.comment       = r.comment;
        row.type          = $filter('uppercase')(type);

        return row;
    };

    return function (type) {
        return {
            query: {
                method: 'GET',
                isArray: false,
                params: { type: type, id: 'blacklists.json' },
                transformResponse: function (data) {
                    var result = [];
                    angular.forEach(JSON.parse(data), function (item, type) {
                        result.push(parse(item, type));
                    });

                    return {
                        rows   : result,
                        total  : data.total,
                        offset : data.offset,
                        count  : data.count,
                        limit  : data.limit
                    };
                }
            },
            get: {
                method: 'GET',
                params: { type: type },
                transformResponse: function (data) {
                    return parse(JSON.parse(data));
                }
            },
            save: {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {
                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    var request = {};

                    request.type = type;
                    if (data.items.items.length) {
                        request.blacklistedId = data.items.items[0].id;
                    }
                    request.start = JsHelper.toZuluTimeString({
                        date: data.dates.fromDate,
                        hour: data.dates.fromTime.split(':')[0],
                        minute: data.dates.fromTime.split(':')[1]
                    });
                    request.end = JsHelper.toZuluTimeString({
                        date: data.dates.toDate,
                        hour: data.dates.toTime.split(':')[0],
                        minute: data.dates.toTime.split(':')[1]
                    });
                    request.purpose = data.reason.reason;
                    request.comment = data.reason.comment;

                    return $.param(request);
                }
            },
            update: {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {
                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    var request = {};

                    request.type = type;
                    if (data.items.items.length) {
                        request.blacklistedId = data.items.items[0].id;
                    }
                    request.start = JsHelper.toZuluTimeString({
                        date: data.dates.fromDate,
                        hour: data.dates.fromTime.split(':')[0],
                        minute: data.dates.fromTime.split(':')[1]
                    });
                    request.end = JsHelper.toZuluTimeString({
                        date: data.dates.toDate,
                        hour: data.dates.toTime.split(':')[0],
                        minute: data.dates.toTime.split(':')[1]
                    });
                    request.purpose = data.reason.reason;
                    request.comment = data.reason.comment;

                    return $.param(request);
                }
            }
        };
    };
}])
.factory('UserBlacklistsResource', ['$resource', 'BlacklistsResource',
function ($resource, BlacklistsResource) {
    return $resource('/blacklist/:id', {}, BlacklistsResource('person'));
}])
.factory('LocationBlacklistsResource', ['$resource', 'BlacklistsResource',
function ($resource, BlacklistsResource) {
    return $resource('/blacklist/:id', {}, BlacklistsResource('room'));
}]);
