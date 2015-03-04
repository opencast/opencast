angular.module('adminNg.services')
.factory('Table', ['$rootScope', '$filter', 'Storage', '$location', '$timeout',
    function ($rootScope, $filter, Storage, $location, $timeout) {
    var TableService = function () {
        var me = this,
            ASC = 'ASC',
            DESC = 'DESC',
            DEFAULT_REFRESH_DELAY = 5000;

        this.rows = [];
        this.sorters = [];
        this.loading = true;

        // Variable related to the pagination
        this.pagination = {
            totalItems  :       100, // the number of items in total
            pages       :        [], // list of pages
            limit       :        10, // the number of items per page
            offset      :         0, // currently selected page
            directAccessibleNo :  3  // number of pages on each side of the current index
        };

        this.updatePagination = function () {
            var p = me.pagination, i;

            p.pages = [];
            for (i = 0; i < (p.totalItems / p.limit); i++) {
                p.pages.push({
                    number : i,
                    active : i === p.offset
                });
            }
        };

        this.refreshColumns = function () {
            var currentTable, currentPath, localStorageColumnConfig;
            currentPath = $location.path();
            currentTable = currentPath.substring(currentPath.lastIndexOf('/') + 1, currentPath.length);
            localStorageColumnConfig = Storage.get('table_column_visibility', currentTable, currentPath);
            if (!$.isEmptyObject(localStorageColumnConfig)) {
                me.columns = localStorageColumnConfig.columns;
                me.columnsConfiguredFromLocalStorage = true;
            } else {
                me.columnsConfiguredFromLocalStorage = false;
            }
        };


        this.getDirectAccessiblePages = function () {
            var startIndex = me.pagination.offset - me.pagination.directAccessibleNo,
                endIndex = me.pagination.offset + me.pagination.directAccessibleNo,
                retVal = [], i, pageToPush;


            if (startIndex < 0) {
                //adjust window if selected window is to low
                endIndex = endIndex - startIndex; // e.g. startIndex: -2 / endIndex: 1 -> endIndex = 1 - (-2) = 3
                startIndex = 0;
            }

            if (endIndex >= me.pagination.pages.length) {
                //adjust window if selected window is to high
                startIndex = startIndex - (endIndex - me.pagination.pages.length) - 1;
                endIndex = me.pagination.pages.length - 1;
            }
            //set endIndex to the highest possible value
            endIndex = Math.min(me.pagination.pages.length - 1, endIndex);
            startIndex = Math.max(0, startIndex);

            for (i = startIndex; i <= endIndex; i++) {

                if (i === startIndex && startIndex !== 0) {
                    // we take the first item if start index is not 0
                    pageToPush = me.pagination.pages[0];
                }
                else if (i === endIndex && endIndex !== me.pagination.pages.length - 1) {
                    // we add the last item if end index is not the real end
                    pageToPush = me.pagination.pages[me.pagination.pages.length - 1];
                }
                else if ((i === startIndex + 1 && startIndex !== 0) || i === endIndex - 1 && endIndex !== me.pagination.pages.length - 1) {
                    // we add the .. at second position or second last position if start- or  end-index is not 0
                    me.pagination.pages[i].label = '..';
                    pageToPush = me.pagination.pages[i];
                }
                else {
                    pageToPush = me.pagination.pages[i];
                }
                retVal.push(pageToPush);
            }
            if (retVal[endIndex]) {
                me.maxLabel = retVal[endIndex].label;
            }
            return retVal;
        };

        this.goToPage = function (page) {
            me.pagination.pages[me.pagination.offset].active = false;
            me.pagination.offset = page;
            me.pagination.pages[me.pagination.offset].active = true;
            Storage.put('pagination', me.resource, 'offset', page);
            me.fetch();
        };

        this.isNavigatePrevious = function () {
            return me.pagination.offset > 0;
        };

        this.isNavigateNext = function () {
            return me.pagination.offset < me.pagination.pages.length - 1;
        };

        /**
         * Changes the number of items on a page to the given value.
         *
         * @param pageSize
         */
        this.updatePageSize = function (pageSize) {
            var p = me.pagination, oldPageSize = p.limit;

            p.limit = pageSize;
            p.offset = 0;

            me.updatePagination();

            Storage.put('pagination', me.resource, 'limit', p.limit);
            Storage.put('pagination', me.resource, 'offset', p.offset);
            if (p.limit !== oldPageSize) {
                me.fetch();
            }
        };

        this.updatePagination = function () {
            var p = me.pagination, i, numberOfPages = p.totalItems / p.limit;

            p.pages = [];
            for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
                p.pages.push({
                    number: i,
                    label: (i + 1).toString(),
                    active: i === p.offset
                });
            }
        };

        this.configure = function (options) {
            var pagination;
            me.all = false;
            me.refreshColumns();

            if (!me.columnsConfiguredFromLocalStorage) {
                // The user has not configured the columns before, hence we will
                // configure them from scratch
                me.columns = options.columns;
                angular.forEach(me.columns, function (column) {
                    column.deactivated = false;
                });
            }
            me.caption = options.caption;
            me.resource = options.resource;
            me.category = options.category;
            me.apiService = options.apiService;
            me.multiSelect = options.multiSelect;
            me.refreshDelay = options.refreshDelay || DEFAULT_REFRESH_DELAY;

            me.predicate = '';
            me.reverse = false;

            me.sorters = [];

            // Load pagination configuration from local storage
            pagination = Storage.get('pagination', me.resource);
            if (angular.isUndefined(pagination)) {
                if (angular.isDefined(pagination.limit)) {
                    me.pagination.limit = pagination.limit;
                }

                if (angular.isDefined(pagination.offset)) {
                    me.pagination.offset = pagination.offset;
                }
            }

            me.updatePageSize(me.pagination.limit);

            // Load sorting criteria from local storage
            angular.forEach(Storage.get('sorter', me.resource), function (values, name) {
                me.sorters[values.priority] = {
                    name: name,
                    priority: values.priority,
                    order: values.order
                };
            });

            if (me.sorters.length > 0) {
                me.predicate = me.sorters[0].name;
                me.reverse = me.sorters[0].order === DESC;
            }
        };

        this.saveSortingCriteria = function (sorterCriteria) {
            angular.forEach(sorterCriteria, function (values, priority) {
                values.priority = priority;
                Storage.put('sorter', me.resource, values.name, values);
            });
        };

        this.sortBy = function (column) {
            // Avoid sorting by action column
            if (angular.isUndefined(column) || column.dontSort) {
                return;
            }

            var newOrder, values;

            values = Storage.get('sorter', me.resource)[column.name];
            newOrder = (values && values.order && values.order === ASC) ? DESC : ASC;

            if (values && angular.isDefined(values.priority)) {
                me.sorters.splice(values.priority, 1);
            }

            me.sorters.splice(0, 0, {
                name     : column.name,
                priority : 0,
                order    : newOrder
            });

            me.saveSortingCriteria(me.sorters);

            me.predicate = column.name;
            me.reverse = newOrder === DESC;

            me.fetch();
        };

        /**
         * Retrieve data from the defined API with the given filter values.
         */
        this.fetch = function () {
            if (angular.isUndefined(me.apiService)) {
                return;
            }

            var query = {},
                filters = [],
                sorters = [];

            me.loading = true;

            angular.forEach(Storage.get('filter', me.resource), function (value, filter) {
                filters.push(filter + ':' + value);
            });

            if (filters.length) {
                query.filter = filters.join(',');
            }

            // Limit temporary to sort by one criteria
            if (me.sorters.length > 0) {
                sorters.push(me.sorters[0].name + ':' + me.sorters[0].order);
            }


            query.limit = me.pagination.limit;
            query.offset = me.pagination.offset * me.pagination.limit;

            if (filters.length) {
                query.filter = filters.join(',');
            }

            if (sorters.length) {
                query.sort = sorters.join(',');
            }

            query.limit = me.pagination.limit;
            query.offset = me.pagination.offset * me.pagination.limit;

            me.apiService.query(query).$promise.then(function (data) {
                var selected = [];
                angular.forEach(me.rows, function (row) {
                    if (row.selected) {
                        selected.push(row.id);
                    }
                });
                angular.forEach(data.rows, function (row) {
                    if (selected.indexOf(row.id) > -1) {
                        row.selected = true;
                    }
                });
                me.rows = data.rows;
                me.loading = false;
                me.pagination.totalItems = data.total;
                me.updatePagination();
            });

            if (me.refreshScheduler.on) {
                me.refreshScheduler.newSchedule();
            }
        };

        /**
         * Scheduler for the refresh of the fetch
         */
        this.refreshScheduler = {
            on: true,
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

        this.toggleAllSelectionFlags = function () {
            angular.forEach(me.rows, function (row) {
                row.selected = me.all;
            });
        };

        this.getSelected = function () {
            var result = [];
            angular.forEach(me.rows, function (row) {
                if (row.selected) {
                    result.push(row);
                }
            });
            return result;
        };

        // Reload the table if the language is changed
        $rootScope.$on('language-changed', function () {
            if (!me.loading) {
                me.fetch();
            }
        });

    };
    return new TableService();
}]);
