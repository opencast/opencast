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

angular.module('adminNg.directives')
.directive('adminNgTableFilter', ['Storage', 'FilterProfiles', 'Language', 'underscore', '$translate', '$timeout',
  function (Storage, FilterProfiles, Language, _, $translate, $timeout) {
    return {
      templateUrl: 'shared/partials/tableFilters.html',
      replace: true,
      scope: {
        filters:   '=',
        namespace: '='
      },
      link: function (scope) {
        scope.formatDateRange = Language.formatDateRange;

        scope.getOptionLabel = function (filter) {
          var optionLabel;

          angular.forEach(filter.options, function (id, label) {
            if (id === filter.value) {
              optionLabel = label;
            }
          });

          return optionLabel;
        };

        scope.displayFilterSelector = function() {
          scope.showFilterSelector = true;
          $timeout(function(){
            angular.element('.main-filter').trigger('chosen:open');
          });
        };

        scope.initializeMap = function() {
          for (var key in scope.filters.filters) {
            scope.filters.map[key] = {
              options: {},
              type: scope.filters.filters[key].type,
              label: scope.filters.filters[key].label,
              translatable: scope.filters.filters[key].translatable
            };
            var options = scope.filters.filters[key].options;
            // eslint-disable-next-line
            angular.forEach(options, function(option) {
              scope.filters.map[key].options[option.value] = option.label;
            });
          }
        };

        scope.restoreFilters = function () {
          scope.filters.map = {};
          if (Object.keys(scope.filters.map).length === 0) {
            scope.initializeMap();
          }
          angular.forEach(scope.filters.filters, function (filter, name) {
            filter.value = Storage.get('filter', scope.namespace)[name];

            if (scope.filters.map[name]) {
              scope.filters.map[name].value = filter.value;
            }
          });
          scope.textFilter = Storage.get('filter', scope.namespace).textFilter;
        };

        scope.filters.$promise.then(function () {
          scope.restoreFilters();
        }).catch(angular.noop);

        scope.removeFilters = function () {
          angular.forEach(scope.filters.map, function (filter) {
            delete filter.value;
          });

          scope.selectedFilter = null;
          scope.showFilterSelector = false;
          Storage.remove('filter');
          angular.element('.main-filter').val('').trigger('chosen:updated');
        };

        scope.removeFilter = function (name, filter) {
          delete filter.value;
          Storage.remove('filter', scope.namespace, name);
        };

        scope.selectFilterTextValue = _.debounce(function (filterName, filterValue) {
          scope.showFilterSelector = false;
          scope.selectedFilter = null;
          scope.addFilterToStorage('filter', scope.namespace, filterName, filterValue);
        }, 250);

        scope.getFilterName = function(){
          if (angular.isDefined(scope.selectedFilter) && angular.isDefined(scope.selectedFilter.label)) {
            for(var i in scope.filters.filters) {
              if (angular.equals(scope.filters.filters[i].label, scope.selectedFilter.label)) {
                return i;
              }
            }
          }
        };

        scope.selectFilterSelectValue = function (filter)  {
          scope.restoreFilters();
          var filterName = scope.getFilterName();
          scope.showFilterSelector = false;
          scope.selectedFilter = null;
          scope.filters.map[filterName].value = filter.value;
          scope.addFilterToStorage('filter', scope.namespace, filterName , filter.value);
        };

        scope.toggleFilterSettings = function () {
          scope.mode = scope.mode ? 0 : 1;
        };

        scope.selectFilterPeriodValue = function (filter, dateField, otherDateField) {

          if (!Object.prototype.hasOwnProperty.call(filter, 'prefilled')) {
            filter.prefilled = {};
            filter.prefilled[otherDateField] = false;
          }

          filter.prefilled[dateField] = false;

          //pre-fill second date field if empty
          if (!filter.period[otherDateField]) {
            filter.period[otherDateField] = filter.period[dateField];
            filter.prefilled[otherDateField] = true;

            scope.showDatepicker(otherDateField);
          }
          else if (filter.period[otherDateField] && filter.prefilled[otherDateField]) {
            scope.showDatepicker(otherDateField);
          }
          else {
            var filterName = scope.getFilterName();
            // "filter.period.to" and "filter.period.from" are local Date formated into string "YY-MM-DD"
            // Parse back into local Date obj by adding timezone offset
            // -- Get the current local timezone offset in MS --
            var localTimeZoneOffSetInMs = new Date().getTimezoneOffset() * 60 * 1000;
            var localOffsetFrom = new Date(Date.parse(filter.period.from) + localTimeZoneOffSetInMs);
            var localOffsetTo = new Date(Date.parse(filter.period.to) + localTimeZoneOffSetInMs);
            // -- Set "From" date to start of day (00:01) and "To" date to end of day (23:59) --
            var from = new Date(localOffsetFrom.setHours(0, 0, 0, 1));
            var to = new Date(localOffsetTo.setHours(23, 59, 59, 999));
            // -- Transform to IOS string UTC Z for filter value --
            filter.value = from.toISOString() + '/' + to.toISOString();

            scope.showFilterSelector = false;
            scope.selectedFilter = null;

            if (!scope.filters.map[filterName]) {
              scope.filters.map[filterName] = {};
            }
            scope.filters.map[filterName].value = filter.value;
            scope.addFilterToStorage('filter', scope.namespace, filterName, filter.value);
          }
        };

        scope.showDatepicker = function(dateField) {

          var element;

          switch (dateField) {
          case 'from':
            element = '.small-search.start-date';
            break;
          case 'to':
            element = '.small-search.end-date';
            break;
          default:
            return;
          }

          $timeout(function(){
            angular.element(element).datepicker('show');
          });
        };

        scope.addFilterToStorage = function(type, namespace, filterName, filterValue) {
          Storage.put(type, namespace, filterName, filterValue);
          angular.element('.main-filter').val('').trigger('chosen:updated');
        };

        // Restore filter profiles
        scope.profiles = FilterProfiles.get(scope.namespace);

        scope.validateProfileName = function () {
          var profileNames = FilterProfiles.get(scope.namespace).map(function (profile) {
            return profile.name;
          });
          scope.profileForm.name.$setValidity('uniqueness',
            profileNames.indexOf(scope.profile.name) <= -1);
        };

        scope.saveProfile = function () {
          if (angular.isDefined(scope.currentlyEditing)) {
            scope.profiles[scope.currentlyEditing] = scope.profile;
          } else {
            scope.profile.filter = angular.copy(Storage.get('filter', scope.namespace));
            scope.activeProfile = scope.profiles.push(scope.profile) - 1;
          }

          FilterProfiles.set(scope.namespace, scope.profiles);
          scope.profile = {};
          scope.mode = 0;
          delete scope.currentlyEditing;
        };

        scope.cancelProfileEditing = function () {
          scope.profiles = FilterProfiles.get(scope.namespace);
          scope.profile = {};
          scope.mode = 1;
          delete scope.currentlyEditing;
        };

        scope.closeProfile = function () {
          scope.mode = 0;
          scope.profile = {};
          delete scope.currentlyEditing;
        };

        scope.removeFilterProfile = function (index) {
          scope.profiles.splice(index, 1);
          FilterProfiles.set(scope.namespace, scope.profiles);
        };

        scope.editFilterProfile = function (index) {
          scope.mode = 2;
          scope.profile = scope.profiles[index];
          scope.currentlyEditing = index;
        };

        scope.loadFilterProfile = function (index) {
          if (FilterProfiles.get(scope.namespace)[index]) {
            var newFilters = [];
            var filtersFromProfile = FilterProfiles.get(scope.namespace)[index].filter;
            angular.forEach(filtersFromProfile, function (fvalue, fkey) {
              newFilters.push({namespace: scope.namespace, key: fkey, value: fvalue});
            });
            Storage.replace(newFilters, 'filter');
          }
          scope.mode = 0;
          scope.activeProfile = index;
        };

        scope.onChangeSelectMainFilter = function(selectedFilter) {
          scope.filter = selectedFilter;

          if (selectedFilter.type === 'period') {
            scope.showDatepicker('from');
          }
          else {
            $timeout(function(){
              angular.element('.second-filter').trigger('chosen:open');
            });
          }
        };

        // Deregister change handler
        scope.$on('$destroy', function () {
          scope.deregisterChange();
        });

        // React on filter changes
        scope.deregisterChange = Storage.scope.$on('change', function (event, type) {
          if (type === 'filter') {
            scope.restoreFilters();
          }
        });

      }
    };
  }]);
