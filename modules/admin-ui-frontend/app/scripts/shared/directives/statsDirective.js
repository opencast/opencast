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
 * @name adminNg.directives.adminNgStats
 * @description
 * Generates a stats bar from the given resource.
 */
angular.module('adminNg.directives')
.directive('adminNgStats', ['Storage', 'HotkeysService', 'RelativeDatesService',
  function (Storage, HotkeysService, RelativeDatesService) {

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
      column.style['min-width'] = (width + 22) + 'px';
    };

    return {
      templateUrl: 'shared/partials/stats.html',
      replace: false,
      scope: {
        stats: '='
      },
      link: function (scope) {
        scope.statsFilterNumber = -1;

        scope.showStatsFilter = function (index) {
          var filters = [];

          if (index >= scope.stats.stats.length - 1) {
            index = scope.stats.stats.length - 1;
          } else if (index < 0) {
            index = 0;
          }

          scope.statsFilterNumber = index;

          angular.forEach(scope.stats.stats[index].filters, function (filter) {

            var value = filter.value;
            var name = filter.name;

            if (Object.prototype.hasOwnProperty.call(value, 'relativeDateSpan')) {
              value = RelativeDatesService.relativeDateSpanToFilterValue(value.relativeDateSpan.from,
                value.relativeDateSpan.to,
                value.relativeDateSpan.unit);
            }
            filters.push({namespace: scope.stats.resource, key: name, value: value});
          });
          Storage.replace(filters, 'filter');
        };

        HotkeysService.activateHotkey(scope, 'general.select_next_dashboard_filter', function (event) {
          event.preventDefault();
          if (scope.statsFilterNumber >= scope.stats.stats.length - 1) {
            scope.statsFilterNumber = -1;
          }
          scope.showStatsFilter(scope.statsFilterNumber + 1);
        });

        HotkeysService.activateHotkey(scope, 'general.select_previous_dashboard_filter', function (event) {
          event.preventDefault();
          if (scope.statsFilterNumber <= 0) {
            scope.statsFilterNumber = scope.stats.stats.length;
          }
          scope.showStatsFilter(scope.statsFilterNumber - 1);
        });

        HotkeysService.activateHotkey(scope, 'general.remove_filters', function (event) {
          event.preventDefault();
          Storage.remove('filter');
          scope.statsFilterNumber = -1;
        });

        scope.$on('$destroy', function() {
          scope.stats.refreshScheduler.cancel();
        });
      }
    };
  }]);
