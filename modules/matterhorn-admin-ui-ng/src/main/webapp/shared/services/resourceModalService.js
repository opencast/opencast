/**
 * @ngdoc service
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying details of table records.
 */
angular.module('adminNg.services.modal')
.factory('ResourceModal', ['$location', '$compile', '$injector', 'Table', 'Modal', '$timeout',
    function ($location, $compile, $injector, Table, Modal, $timeout) {
    var ResourceModal = function () {
        var me = this,
            DEFAULT_REFRESH_DELAY = 5000;

        /**
         * Scheduler for the refresh of the fetch
         */
        this.refreshScheduler = {
            on: true,
            newSchedule: function () {
                me.refreshScheduler.cancel();
                me.refreshScheduler.nextTimeout = $timeout(me.loadSubNavData, DEFAULT_REFRESH_DELAY);
            },
            cancel: function () {
                if (me.refreshScheduler.nextTimeout) {
                    $timeout.cancel(me.refreshScheduler.nextTimeout);
                }
            }
        };

        /**
         * @ngdoc function
         * @name Modal.openTab
         * @methodOf adminNg.modal.Modal
         * @description
         * Opens the given or first tab and any sub tabs.
         *
         * The tab will be determined either by the Event object (when
         * called from a click event) or from the tabId parameter. All
         * bread crumbs will be deleted unless the `keepBreadcrumbs` flag
         * is true.
         *
         * Updates the URL with the `tab` search parameter.
         *
         * @param {string} tabId Tab ID to open.
         * @param {boolean} keepBreadcrumbs Deletes breadcrumbs if true.
         */
        this.openTab = function (tabId, keepBreadcrumbs) {
            // If no tab is defined, open the first tab by default.
            if (angular.isUndefined(tabId)) {
                tabId = Modal.modal.find('#modal-nav > a').first().data('modal-tab');
            }

            var params = $location.search();

            // Clean up breadcrumbs if appropriate
            if (!keepBreadcrumbs) {
                delete params.breadcrumbs;
                me.$scope.breadcrumbs = [];
            }

            Modal.modal.find('#breadcrumb').hide();
            Modal.modal.find('.modal-content').removeClass('active');
            Modal.modal.find('[data-modal-tab="' + tabId + '"]').addClass('active');
            Modal.modal.find('[data-modal-tab-content="' + tabId + '"]').addClass('active');

            me.$scope.tab = tabId;

            params.tab = tabId;
            $location.search(params);
        };

        /**
         * @ngdoc function
         * @name Modal.openSubTab
         * @methodOf adminNg.modal.Modal
         * @description
         * Switch tabs or open the first one.
         *
         * The tab will be determined either by the Event object (when
         * called from a click event) or from the tabId parameter.
         *
         * Updates the URL with the `tab` search parameter.
         *
         * @param {string} subTabId Sub tab ID to open.
         * @param {string} apiServiceName (optional) Service name of the $resource wrapper to use.
         * @param {Array} subIds (optional) resource sub-IDs of the current sub tab.
         * @param {boolean} sibling (optional) Indicates if this sub tab is a sibling to its parent.
         */
        this.openSubTab = function (subTabId, apiServiceName, subId, sibling) {
            var params = $location.search(), previous;

            // Determine if any sub tabs need to be restored
            try {
                previous = JSON.parse(params.breadcrumbs);
            } catch (e) {}
            me.generateBreadcrumbs(subTabId, previous, apiServiceName, subId, sibling);

            Modal.modal.find('.modal-content').removeClass('active');

            // When displaying a subNavigation Item, we need to visually
            // activate the parent tab as well.
            me.$scope.tab = Modal.modal.find('[data-modal-tab-content="' + subTabId + '"]')
                .addClass('active').data('parent');

            if (me.$scope.breadcrumbs.length > 0) {
                me.loadSubNavData();
            }

            params.breadcrumbs = JSON.stringify(me.$scope.breadcrumbs);
            $location.search(params);

            if (me.refreshScheduler.on) {
                me.refreshScheduler.cancel();
                me.refreshScheduler.newSchedule();
            }
        };

        /**
         * @ngdoc function
         * @name Modal.loadSubNavData
         * @methodOf adminNg.modal.Modal
         * @description
         * Finds the service name in the current breadcrumb information and calls its
         * GET method, with the appropriate id.
         *
         * The loaded data is stored in $scope.subNavData.
         */
        this.loadSubNavData = function () {
            var apiService, previousBreadcrumb = {},
                params = [me.$scope.resourceId];

            angular.forEach(me.$scope.breadcrumbs, function (breadcrumb) {
                if (!breadcrumb.sibling) {
                    params.push(breadcrumb.subId);
                }
                apiService = breadcrumb.api;
                previousBreadcrumb = breadcrumb;
            });
            apiService = $injector.get(apiService);

            params = params.reduce(function (prevValue, currentValue, index) {
                prevValue['id' + index] = currentValue;
                return prevValue;
            }, {});

            apiService.get(params, function (data) {
                me.$scope.subNavData = data;
            });

            if (me.refreshScheduler.on) {
                me.refreshScheduler.newSchedule();
            }
        };

        /**
         * @ngdoc function
         * @name Modal.generateBreadcrumbs
         * @methodOf adminNg.modal.Modal
         * @description
         * Render navigation history for sub tabs.
         *
         * Determines all relevant information from the data attributes
         * of the target tab, such as the depth of the sub tab and its
         * label. The label attribute will be translated.
         *
         * If the `previous` parameter contains breadcrumbs they will be
         * restored.
         *
         * @example
         * <div data-modal-tab-content="quick-actions-details" data-level="2" data-label="QUICK_ACTION_DETAILS_TITLE">
         *
         * @param {string} tabId Tab ID of the current tab.
         * @param {Array} previous breadcrumbs to restore.
         * @param {string} api (optional) Service name of the $resource wrapper to use.
         * @param {Array} subIds (optional) resource sub-IDs of the current sub tab.
         * @param {Boolean} sibling (optional) flag indicating that this sub tab is a sibling to the last.
         */
        this.generateBreadcrumbs = function (tabId, previous, api, subId, sibling) {
            var subNavLevel, subNavLabel, tab, subNav;
            tab = Modal.modal.find('[data-modal-tab-content="' + tabId + '"]');
            subNavLevel = tab.data('level');
            subNavLabel = tab.data('label');
            subNav      = Modal.modal.find('#breadcrumb');

            subNav.empty();

            // Restore previous breadcrumbs from URL
            if (previous && previous.length && previous[previous.length - 1].id === tabId) {
                me.$scope.breadcrumbs = previous;
            }

            // Create a new sub tab or navigate back
            if (me.$scope.breadcrumbs.length + 1 < subNavLevel) {
                me.$scope.breadcrumbs.push({
                    level:   subNavLevel,
                    label:   subNavLabel,
                    id:      tabId,
                    api:     api,
                    sibling: sibling,
                    subId:   subId
                });
            } else {
                me.$scope.breadcrumbs.splice(subNavLevel - 1);
            }

            // Populate the breadcrumbs container with links
            angular.forEach(me.$scope.breadcrumbs, function (item) {
                subNav.append('<a ' +
                    'class="breadcrumb-link active" ' +
                    'data-level="' + item.level + '" ' +
                    'ng-click="openSubTab(\'' + item.id + '\')" ' +
                    'translate>' +
                    item.label +
                    '</a>');
            });
            $compile(subNav)(me.$scope);
            subNav.show();
        };

        /**
         * @ngdoc function
         * @name Modal.showAdjacent
         * @methodOf adminNg.modal.Modal
         * @description
         * Determine and set the next or previous resource ID.
         *
         * @param {boolean} reverse Choose the previous instead of the next record.
         */
        this.showAdjacent = function (reverse) {
            var adjacentId, adjacentIndex, params = $location.search();

            angular.forEach(Table.rows, function (row, index) {
                if (row.id === me.$scope.resourceId) {
                    adjacentIndex = index;
                    return;
                }
            });

            if (reverse) { adjacentIndex -= 1; }
            else         { adjacentIndex += 1; }

            if (Table.rows[adjacentIndex]) {
                adjacentId = Table.rows[adjacentIndex].id;
            }

            if (!angular.isUndefined(adjacentId)) {
                me.$scope.resourceId = adjacentId;
                params.resourceId = adjacentId;
                $location.search(params);
                me.$scope.$broadcast('change', adjacentId);
            }
        };


        /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @param {string} resourceId Identifier for the resource used in the modal.
         * @param {string} tab Identifier for the currently active tab in a modal (optional)
         * @param {string} action Name of the type of content (e.g. add, edit)
         */
        this.show = function (modalId, resourceId, tab, action) {
            var $scope, modalNav, params, http = Modal.show(modalId), subTab;

            this.$scope = Modal.$scope;
            $scope = this.$scope;

            if (!http) { return; }

            $scope.showAdjacent = me.showAdjacent;
            $scope.openTab = me.openTab;
            $scope.openSubTab = me.openSubTab;
            $scope.breadcrumbs = [];
            $scope.resourceId = resourceId;
            $scope.action = action;

            http.then(function () {
                params = $location.search();

                // Set content (by tab or otherwise)
                modalNav = Modal.modal.find('#modal-nav > a');
                if (!modalNav.hasClass('active')) {
                    if (modalNav.length === 0) {
                        Modal.modal.find('> .modal-content').show();
                    } else {
                        $scope.openTab(tab, true);
                        try {
                            subTab = JSON.parse(params.breadcrumbs);
                            subTab = subTab[subTab.length - 1];
                            $scope.openSubTab(subTab.id, subTab.api, subTab.subId, subTab.sibling);
                        } catch (e) {}
                    }
                }

                // Set location
                params.resourceId = resourceId;
                params.action = action;
                $location.search(params);
            });
        };
    };

    return new ResourceModal();
}]);
