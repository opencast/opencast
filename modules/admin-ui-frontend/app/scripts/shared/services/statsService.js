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
.factory('Stats', ['$rootScope', '$filter', 'Storage', '$location', '$timeout', 'RelativeDatesService',
  function ($rootScope, $filter, Storage, $location, $timeout, RelativeDatesService) {
    var StatsService = function () {
      var me = this,
          DEFAULT_REFRESH_DELAY = 5000;

      this.stats = [];

      this.configure = function (options) {
        me.resource = options.resource;
        me.apiService = options.apiService;
        me.stats = options.stats;
        me.refreshDelay = options.refreshDelay || DEFAULT_REFRESH_DELAY;

        me.stats.sort(function(a, b) {
          return a.order - b.order;
        });
        me.fetch();
      };

      /**
       * Retrieve data from the defined API with the given filter values.
       */
      this.fetch = function () {
        me.runningQueries = 0;

        angular.forEach(me.stats, function (stat) {

          var query = {};
          var filters = [];

          angular.forEach(stat.filters, function (filter) {

            var name = filter.name;
            var value = filter.value;

            if (Object.prototype.hasOwnProperty.call(value, 'relativeDateSpan')) {
              value = RelativeDatesService.relativeDateSpanToFilterValue(value.relativeDateSpan.from,
                value.relativeDateSpan.to,
                value.relativeDateSpan.unit);
            }

            filters.push(name + ':' + value);
          });

          if (filters.length) {
            query.filter = filters.join(',');
          }

          /* Workaround:
           * We don't want actual data here, but limit 0 does not work (retrieves all data)
           * See MH-11892 Implement event counters efficiently
           */
          query.limit = 1;
          me.runningQueries++;

          me.apiService.query(query).$promise.then(function (data) {
            stat.counter = data.total;
            stat.index = me.stats.indexOf(stat);

            me.runningQueries--;
            me.refreshScheduler.restartSchedule();
          }).catch(function () {
            me.runningQueries--;
            me.refreshScheduler.restartSchedule();
          });
        });
      };

      /**
       * Scheduler for the refresh of the fetch
       */
      this.refreshScheduler = {
        on: true,
        restartSchedule: function () {
          if (me.refreshScheduler.on && (angular.isUndefined(me.runningQueries) || me.runningQueries <= 0)) {
            me.refreshScheduler.newSchedule();
          }
        },
        newSchedule: function () {
          me.refreshScheduler.cancel();
          me.refreshScheduler.nextTimeout = $timeout(me.fetch, me.refreshDelay);
        },
        cancel: function () {
          if (me.refreshScheduler.nextTimeout) {
            $timeout.cancel(me.refreshScheduler.nextTimeout);
          }
        }
      };

    };
    return new StatsService();
  }]);
