/**
 * Copyright 2009-2013 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the 'License'); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an 'AS IS'
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
'use strict';

// A controller for global page navigation
angular.module('adminNg.controllers')
.controller('TablePreferencesCtrl', ['$scope', '$timeout', 'Table', 'Modal', 'Storage', '$translate',
    function ($scope, $timeout, Table, Modal, Storage, $translate) {
        var filteredColumns, cloneColumns;

        cloneColumns = function () {
            return JSON.parse(JSON.stringify(Table.columns));
        };

        filteredColumns = function (isDeactivated) {
            var result = [];
            angular.forEach($scope.columnsClone, function (column) {
                if (column.deactivated === isDeactivated) {
                    result.push(column);
                }
            });
            return result;
        };

        $scope.table = Table;
        $scope.keyUp = Modal.keyUp;

        $scope.changeColumn = function (column, deactivate) {
            if (deactivate) {
                $scope.activeColumns.splice($scope.activeColumns.indexOf(column), 1);
                $scope.deactivatedColumns.push(column);
            }
            else {
                $scope.deactivatedColumns.splice($scope.deactivatedColumns.indexOf(column), 1);
                $scope.activeColumns.push(column);
            }
            column.deactivated = deactivate;
        };

        $scope.save = function () {
            var type = 'table_column_visibility', namespace = Table.resource, prefs = 'columns',
                settings = $scope.deactivatedColumns.concat($scope.activeColumns);
            Storage.put(type, namespace, prefs, settings);
        };

        $scope.initialize = function () {
            $scope.columnsClone = cloneColumns();
            $scope.deactivatedColumns = filteredColumns(true);
            $scope.activeColumns = filteredColumns(false);
            $translate(Table.caption).then(function (translation) {
                $scope.tableName = translation;
            });
            $translate('RESET').then(function (translation) {
                $scope.resetTranslation = translation;
            });
        };
        $scope.initialize();
    }
]);
