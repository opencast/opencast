/**
 * @ngdoc directive
 * @name adminNg.directives.adminNgStats
 * @description
 * Generates a stats bar from the given resource.
 */
angular.module('adminNg.directives')
.directive('adminNgStats', ['Storage', 'HotkeysService', function (Storage, HotkeysService) {
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
            scope.stats.fetch();
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
                  filters.push({namespace: scope.stats.resource, key: filter.name, value: filter.value});
                });
                Storage.replace(filters, 'filter');
            };

            HotkeysService.activateHotkey(scope, "general.select_next_dashboard_filter", "Select Next Dashboard Filter", function(event) {
                event.preventDefault();
                if (scope.statsFilterNumber >= scope.stats.stats.length - 1) {
                  scope.statsFilterNumber = -1;
                }
                scope.showStatsFilter(scope.statsFilterNumber + 1);
            });

            HotkeysService.activateHotkey(scope, "general.select_previous_dashboard_filter", "Select Previous Dashboard Filter", function(event) {
                event.preventDefault();
                if (scope.statsFilterNumber <= 0) {
                  scope.statsFilterNumber = scope.stats.stats.length;
                }
                scope.showStatsFilter(scope.statsFilterNumber - 1);
            });

            HotkeysService.activateHotkey(scope, "general.remove_filters", "Remove Filters", function(event) {
                event.preventDefault();
                Storage.remove('filter');
                scope.statsFilterNumber = -1;
            });
        }
    };
}]);
