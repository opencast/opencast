angular.module('adminNg.services')
.factory('Stats', ['$rootScope', '$filter', 'Storage', '$location', '$timeout',
    function ($rootScope, $filter, Storage, $location, $timeout) {
    var StatsService = function () {
        var me = this,
            DEFAULT_REFRESH_DELAY = 5000;

        this.stats = [];
        this.loading = true;

        this.configure = function (options) {
            me.resource = options.resource;
            me.apiService = options.apiService;
            me.stats = options.stats;
            me.refreshDelay = options.refreshDelay || DEFAULT_REFRESH_DELAY;
        };

        /**
         * Retrieve data from the defined API with the given filter values.
         */
        this.fetch = function () {
            if (angular.isUndefined(me.apiService)) {
                return;
            }

            me.loading = true;
            me.runningQueries = 0;

            angular.forEach(me.stats, function (stat) {
                var query = {};
                var filters = [];
                angular.forEach(stat.filters, function (filter) {
                    filters.push(filter.name + ':' + filter.value);
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
                    me.loading = false;
                    stat.counter = data.total;
                    stat.index = me.stats.indexOf(stat);

                    me.runningQueries--;
                    me.refreshScheduler.restartSchedule();
                }, function () {
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

        // Reload the stats if the language is changed
        $rootScope.$on('language-changed', function () {
            if (!me.loading) {
                me.fetch();
            }
        });

    };
    return new StatsService();
}]);
