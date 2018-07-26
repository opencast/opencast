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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EditStatusCtrl', ['$scope', 'Modal', 'Table', 'OptoutsResource', 'Notifications',
    'decorateWithTableRowSelection',
    function ($scope, Modal, Table, OptoutsResource, Notifications, decorateWithTableRowSelection) {

    $scope.rows = Table.copySelected();
    $scope.allSelected = true; // by default, all records are selected

    $scope.changeStatus = function (newStatus) {
        $scope.status = newStatus;
        if (angular.isDefined($scope.status)) {
            $scope.status += '';            
        }
    };

    $scope.valid = function () {
        return $scope.TableForm.$valid && angular.isDefined($scope.status);
    };

    $scope.submit = function () {
        var resource = Table.resource.indexOf('series') >= 0 ? 'series' : 'event',
            sourceNotification = resource === 'series' ? 'SERIES' : 'EVENTS'; 
        if ($scope.valid()) {
            OptoutsResource.save({
                resource: resource,
                eventIds: $scope.getSelectedIds(),
                optout: $scope.status === 'true'
            }, function (data) {
                var nbErrors = data.error ? data.error.length : 0,
                    nbOK = data.ok ? data.ok.length : 0,
                    nbNotFound = data.notFound ? data.notFound.length : 0,
                    nbUnauthorized = data.unauthorized ? data.unauthorized.length : 0,
                    nbBadRequest = data.badRequest ? data.badRequest.length : 0;
                Table.deselectAll();
                Modal.$scope.close();
                if (nbErrors === 0 && nbBadRequest === 0 && nbNotFound === 0 && nbUnauthorized === 0) {
                    Notifications.add('success', sourceNotification + '_UPDATED_ALL');
                } else {
                    if (nbOK > 0) {
                        Notifications.addWithParams('success', sourceNotification + '_UPDATED_NB', {number : nbOK});
                    }

                    var errors = [];

                    if (data.error) {
                        errors = errors.concat(data.err);
                    }

                    if (data.notFound) {
                        errors = errors.concat(data.notFound);
                    }                    

                    if (data.badRequest) {
                        errors = errors.concat(data.badRequest);
                    }

                    if (data.unauthorized) {
                        errors = errors.concat(data.unauthorized);
                    }

                    if (data.forbidden) {
                        errors = errors.concat(data.forbidden);
                    }                 

                    angular.forEach(errors, function (error) {
                        Notifications.addWithParams('error', sourceNotification + '_NOT_UPDATED_ID', {id: error});
                    });

                }
            }, function () {
                Modal.$scope.close();
                Notifications.add('error', sourceNotification + '_NOT_UPDATED_ALL');
            });
        }
    };
    decorateWithTableRowSelection($scope);
}]);
