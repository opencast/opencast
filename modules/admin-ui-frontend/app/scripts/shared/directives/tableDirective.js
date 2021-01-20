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
 * @ngdoc directive
 * @name adminNg.directives.adminNgTable
 * @description
 * Generates a table from the given resource.
 *
 * The generated table has the following features:
 * * Sorts by column without reloading the resource.
 * * Listens to changes to any filter values (see adminNg.directives.adminNgTableFilter).
 *
 * Future features:
 * * Pagination integration with the resource (records per page and offset).
 *
 * @example
 * <admin-ng-table="" table="table" />
 */
angular.module('adminNg.directives')
.directive('adminNgTable', ['Storage', '$translate', function (Storage, $translate) {
  var calculateWidth, setWidth;
  calculateWidth = function (label, element) {
    var testDiv, width;
    testDiv = element.find('#length-div').append(label).append('<i class="sort"></i>');
    width = testDiv.width();
    testDiv.html('');
    return width;
  };

  setWidth = function (translation, column, element) {
    var width;
    if (angular.isUndefined(translation)) {
      width = calculateWidth(column.label, element);
    } else {
      width = calculateWidth(translation, element);
    }
    column.style = column.style || {};
    column.style['min-width'] = (width + 40) + 'px';
  };

  return {
    templateUrl: 'shared/partials/table.html',
    replace: false,
    scope: {
      table: '=',
      highlight: '<'
    },
    link: function (scope, element) {
      scope.table.fetch(true);

      // Deregister change handler
      scope.$on('$destroy', function () {
        scope.deregisterChange();
      });

      // React on filter changes
      scope.deregisterChange = Storage.scope.$on('change', function (event, type) {
        if (type === 'filter') {
          scope.table.fetch();
        }
        if (type === 'table_column_visibility') {
          scope.table.refreshColumns();
          scope.calculateStyles();
        }
      });

      scope.calculateStyles = function () {
        angular.forEach(scope.table.columns, function (column) {
          if (angular.isDefined(column.width)) {
            column.style = {'width': column.width};
          } else {
            $translate(column.label).then(function (translation) {
              setWidth(translation, column, element);
            }).catch(angular.noop);
          }
        });
      };
    }
  };
}]);
