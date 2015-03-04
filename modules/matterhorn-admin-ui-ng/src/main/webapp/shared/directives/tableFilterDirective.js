angular.module('adminNg.directives')
.directive('adminNgTableFilter', ['Storage', 'FilterProfiles', 'Language', 'underscore', function (Storage, FilterProfiles, Language, _) {
    return {
        templateUrl: 'shared/partials/tableFilters.html',
        replace: true,
        scope: {
            filters:   '=',
            namespace: '='
        },
        link: function (scope) {
            scope.formatDateRange = Language.formatDateRange;

            scope.restoreFilters = function () {
                angular.forEach(scope.filters.filters, function (filter, name) {
                    filter.value = Storage.get('filter', scope.namespace)[name];
                });
                scope.textFilter = Storage.get('filter', scope.namespace).textFilter;
            };

            scope.filters.$promise.then(function () {
                scope.restoreFilters();
            });

            scope.removeFilters = function () {
                angular.forEach(scope.filters.filters, function (filter) {
                    delete filter.value;
                });
                Storage.remove('filter');
            };

            scope.removeFilter = function (name, filter) {
                delete filter.value;
                Storage.remove('filter', scope.namespace, name);
            };

            scope.selectFilterTextValue = _.debounce(function (filterName, filterValue) {
                scope.showFilterSelector = false;
                scope.selectedFilter = null;
                Storage.put('filter', scope.namespace, filterName, filterValue);
            }, 250);

            scope.selectFilterSelectValue = function (filterName, filter) {
                scope.showFilterSelector = false;
                scope.selectedFilter = null;
                Storage.put('filter', scope.namespace, filterName, filter.value);
            };

            scope.selectFilterPeriodValue = function (filterName, filter) {
                // Merge from-to values of period filter)
                if (filter.period.to && filter.period.from) {
                    filter.value = new Date(filter.period.from).toISOString() + '/' + new Date(filter.period.to).toISOString();
                }

                if (filter.value) {
                    scope.showFilterSelector = false;
                    scope.selectedFilter = null;
                    Storage.put('filter', scope.namespace, filterName, filter.value);
                }
            };

            // Restore filter profiles
            scope.profiles = FilterProfiles.get(scope.namespace);

            scope.validateProfileName = function () {
                var profileNames = FilterProfiles.get(scope.namespace).map(function (profile) {
                    return profile.name;
                });
                scope.profileForm.name.$setValidity('uniqueness',
                        profileNames.indexOf(scope.profile.name) > -1 ? false:true);
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
                var i, filter;
                for (i in scope.filters.filters) {
                    if (FilterProfiles.get(scope.namespace)[index]) {
                        filter = scope.filters.filters[i];
                        filter.value = FilterProfiles.get(scope.namespace)[index].filter[i];
                        Storage.put('filter', scope.namespace, i, filter.value);
                    }
                }
                scope.mode = 0;
                scope.activeProfile = index;
            };
        }
    };
}]);
