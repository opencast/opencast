// todo: table component has to provide context information
let TableService = function(location) {
    let ASC = 'ASC',
        DESC = 'DESC',
        DEFAULT_REFRESH_DELAY = 5000;

    this.rows = [];
    this.allSelected = false;
    this.sorters = [];
    this.loading = true;

    this.pagination = {
        totalItems : 100,
        pages : [],
        limit: 10,
        offset: 0,
        directAccessibleNo: 3
    };

    this.updatePagination = function() {
        let p = this.pagination,
            i,
            numberOfPages = p.totalItems / p.limit;

        p.pages = [];
        for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
            p.pages.push({
                number: i,
                label: (i + 1).toString(),
                active: i === p.offset
            });
        }
    };

    this.refreshColumns = function () {
        let currentTable, currentPath, localStorageColumnConfig;
        currentPath = location.path();
        currentTable = currentPath.substr(currentPath.lastIndexOf('/') + 1, currentPath.length);
        //todo: how to access storage
        localStorageColumnConfig = null;
        if (!!localStorageColumnConfig) {
            this.column = localStorageColumnConfig.columns;
            this.columnsConfiguredFromLocalStorage = true;
        } else {
            this.columnsConfiguredFromLocalStorage = false;
        }
    };

    this.getDirectAccessiblePages = function () {
        let startIndex = this.pagination.offset - this.pagination.directAccessibleNo,
            endIndex = this.pagination.offset + this.pagination.directAccessibleNo,
            retVal = [],
            i, pageToPush;

        if (startIndex < 0) {
            endIndex = endIndex - startIndex;
            startIndex = 0;
        }

        if (endIndex >= this.pagination.pages.length) {
            startIndex = startIndex - (endIndex - this.pagination.pages.length) - 1;
            endIndex = endIndex - this.pagination.pages.length - 1;
        }

        endIndex = Math.min(this.pagination.pages.length - 1, endIndex);
        startIndex = Math.max(0, startIndex);

        for (i = startIndex; i <= endIndex; i++) {

            if(i === startIndex && startIndex !==0) {

                pageToPush = this.pagination.pages[0];
            }
            else if (i === endIndex && endIndex !== this.pagination.pages.length - 1) {

                pageToPush = this.pagination.pages[this.pagination.pages.length -1];
            }
            else if ((i === startIndex + 1 && startIndex !== 0)
            || (i === endIndex - 1 && endIndex !== this.pagination.pages.length - 1)) {
                this.pagination.pages[i].label = '..'
                pageToPush = this.pagination.pages[i];
            }
            else {
                pageToPush = this.pagination.pages[i];
            }
            retVal.push(pageToPush);
        }
        if (retVal[endIndex]) {
            this.maxLabel = retVal[endIndex].label;
        }
        return retVal;
    };

    this.goToPage = function (page) {
        this.allSelected = false;
        this.pagination.pages[this.pagination.offset].active = false;
        this.pagination.offset = page;
        this.pagination.pages[this.pagination.offset].active = true;
        // todo: put this in storage and then this.fetch()
    };

    this.isNavigatePrevious = function () {
        return this.pagination.offset > 0;
    };

    this.isNavigateNext = function () {
        return this.pagination.offset < this.pagination.pages.length - 1;
    };

    this.updatePageSize = function (pageSize) {
        let p = this.pagination,
            oldPageSize = p.limit;

        p.limit = pageSize;
        if (!p.resume) {
            p.offset = 0;
        }
        p.resume = false;
        // todo: put resume in Storage

        this.updatePagination();

        //todo: put limit in Storage
        //todo: put offset in Storage
        if(p.limit !== oldPageSize) {
            //todo: this.fetch()
        }
    };

    this.configure = function (options) {
        let pagination;
        this.allSelected = false;
        this.options = options;
        this.refreshColumns();

        if(!this.columnsConfiguredFromLocalStorage) {
            this.columns = options.columns;
            this.columns.map((column) => {
                column.deactivated = column.name === 'technical_end';
            });
        }

        this.caption = options.caption;
        this.resource = options.resource;
        this.category = options.category;
        this.apiService = options.apiService;
        this.multiSelect = options.multiSelect;
        this.refreshDelay = options.refreshDelay || DEFAULT_REFRESH_DELAY;
        this.postProcessRow = options.postProcessRow;

        this.predicate = '';
        this.reverse = false;

        this.sorters = [];

        // todo: load pagination config from local storage
        pagination = '';
        if (typeof pagination !== 'undefined') {
            if (typeof pagination.limit !== 'undefined') {
                this.pagination.limit = pagination.limit;
            }

            if (typeof pagination.offset !== 'undefined') {
                this.pagination.offset = pagination.offset;
            }

            if (typeof pagination.resume !== 'undefined') {
                this.pagination.resume = pagination.resume;
            }
        }

        this.updatePageSize(this.pagination.limit);

        // todo: loading sorting criteria from local storage
        // todo: replace this.resource with Storage.get('sorter', this.resource)
        this.resource.map((values, name) => {
            this.sorters[values.priority] = {
                name: name,
                priority: values.priority,
                order: values.order
            }
        });

        if (this.sorters.length > 0) {
            this.predicate = this.sorters[0].name;
            this.reverse = this.sorters[0].order === DESC;
        } else {
            for(let i = 0; i < this.columns.length; i++) {
                let column = this.columns[i];

                if (column.sortable) {
                    this.sortBy(column);
                    break;
                }
            }
        }
    };

    this.saveSortingCriteria = function (sorterCriteria) {
        sorterCriteria.map((values, priority) => {
            values.priority = priority;
            // todo: save this to storage
        });
    };

    this.sortBy = function (column) {
        if(typeof column === 'undefined' || !column.sortable) {
            return;
        }

        let newOrder, values;

        //todo: get values out of storage
        values = '';
        newOrder = (values && values.order && values.order === ASC) ? DESC : ASC;

        if (values && typeof values.priority !== 'undefined') {
            this.sorters.splice(values.priority, 1);
        }

        this.sorters.splice(0, 0, {
            name : column.name,
            priority: 0,
            order : newOrder
        });

        this.saveSortingCriteria(this.sorters);

        this.predicate = column.name;
        this.reverse = newOrder === DESC;

        // todo: this.fetch();
    };

    this.fetch = function (reset) {
        if (typeof this.apiService === 'undefined') {
            return;
        }

        if(reset) {
            this.rows = [];
            this.pagination.totalItems = 0;
            this.updatePagination();
            this.updateAllSelected();
        }

        let query = {},
            filters = [],
            sorters = [];

        this.loading = true;

        // todo: this.filter durch storage.get('filter', this.resource) ersetzen
        this.resource.map((value, filter) => {
            filters.push(filter + ':' + value);
        });

        if (filters.length) {
            query.filter = filters.join(',');
        }

        if (this.sorters.length > 0) {
            sorters.push(this.sorters[0].name + ':' + this.sorters[0].order);
        }

        if (sorters.length) {
            query.sort = sorters.join(',');
        }

        query.limit = this.pagination.limit;
        query.offset = this.pagination.offset * this.pagination.limit;

        (function(resource) {
            let startTime = new Date();
            // todo: here promise in admin-ui-frontend, is this here needed to?
            this.apiService.query(query).then(function (data) {

                if(this.lastStartTime && this.lastStartTime > startTime) {
                    return;
                }
                this.lastStartTime = startTime;

                if(resource !== this.resource) {
                    return;
                }

                let selected = [];
                this.rows.map((row) => {
                    if(row.selected) {
                        selected.push(row.id);
                    }
                });
                data.rows.map((row) => {
                    if (selected.indexOf(row.id) > -1) {
                        row.selected = true;
                    }
                });
                if (typeof this.postProcessRow !== 'undefined') {
                    data.rows.map((row) => {
                       this.postProcessRow(row);
                    });
                }
                this.rows = data.rows;
                this.loading = false;
                this.pagination.totalItems = data.total;

                if (this.pagination.offset !== 0 && data.count == 0) {
                    this.goToPage(0);
                }
                this.updatePagination();
                this.updateAllSelected();
            }).catch(function () {});
        })(this.resource);

        if(this.refreshScheduler.on) {
            this.refreshScheduler.newSchedule();
        }
    };

    this.refreshScheduler = {
        on: true,
        newSchedule: function () {
            this.refreshScheduler.cancel();
            // todo: react equivalent of timeout
            this.refreshScheduler.nextTimeout = $timeout(this.fetch, this.refreshDelay);
        },
        cancel: function () {
            if (this.refreshScheduler.nextTimeout) {
                // todo: react equivalent of timeout
                $timeout.cancel(this.refreshScheduler.nextTimeout);
            }
        }
    };

    this.addFilterToStorage = function (column, filter) {
        //todo: Storage.put('filter', this.resource, column, filter)
    }

    this.addFilterToStorageForResource = function (resource, column, filter) {
        // todo: Storage.put('filter', resource, column, filter);
    }

    this.goToRoute = function(path) {
        // todo: react equivalent of location
        $location.path(path);
    }
}
let tableService = new TableService();
