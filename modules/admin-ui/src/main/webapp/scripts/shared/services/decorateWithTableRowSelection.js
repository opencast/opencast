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

angular.module('adminNg.services')
    .factory('decorateWithTableRowSelection', function () {
      /**
       * A decorator that adds all necessary methods for row selection functionality.
       * Usage: Inject this service and invoke it on the object you want to decorate, note that the object must
       * store its rows in the variable this.rows.
       * @type {{}}
       */
      var decorateWithTableRowSelection = function (target) {
        var each = function (callback) {
          angular.forEach(target.rows, function (row) {
            callback(row);
          });
        };

        /**
         * Fires when the select all checkbox has changed. This usually works fine with ng-model and ng-change,
         * but in the case of schedule-task-modal.html it didn't!. The model parameter is a workaround that worked.
         *
         * @param model Optional paramater, can be set if ng-change fires before the new value is written to the model.
         */
        target.allSelectedChanged = function (model) {
          var newState = target.allSelected;
          if (angular.isDefined(model)) {
            newState = model;
          }
          each(function (row) {
            row.selected = newState;
          });
        };

        target.getSelected = function () {
          var result = [];
          each(function (row) {
            if (row.selected) {
              result.push(row);
            }
          });
          return result;
        };

        target.getSelectedIds = function () {
          var result = [];
          each(function (row) {
            if(row.selected) {
              result.push(row.id);
            }
          });
          return result;
        };

        target.hasAnySelected = function () {
          return target.getSelected().length > 0;
        };

        /**
         * Indicates that one of the rows has changed the selection flag. Reevaluation of the select all flag
         * will be needed.
         *
         * @param rowId
         */
        target.rowSelectionChanged = function (rowId) {
          if (target.rows[rowId].selected !== true) {
            // untick select all if the new value is undefined or false
            target.allSelected = false;
          } else if (target.getSelected().length === target.rows.length) {
            // tick select all if all selected boxes are active now.
            target.allSelected = true;
            target.allSelectedChanged();
          }
        };

        /**
         * Looks at all rows and updates the allSelected flag accordingly.
         */
        target.updateAllSelected = function () {
          target.allSelected = target.getSelectedIds().length === target.rows.length;
        };

        target.deselectAll = function () {
          target.allSelected = false;
          each(function (row) {
            row.selected = false;
          });
        };

        /**
         * Returns copies of the currently selected rows, changing them will not affect the table data.
         *
         * @returns {Array} A copy of the selected rows.
         */
        target.copySelected = function () {
          var copy = [];
          angular.forEach(target.getSelected(), function (row) {
            copy.push(angular.extend({}, row ));
          });
          return copy;
        };

        if (target.rowsPromise) {
          target.rowsPromise.then(function () {
            target.allSelected = target.getSelected().length === target.rows.length;
          }).catch(angular.noop);
        }

        return target;
      };

      return decorateWithTableRowSelection;
    });
