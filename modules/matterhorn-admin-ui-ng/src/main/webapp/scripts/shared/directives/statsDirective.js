/**
 * @ngdoc directive
 * @name adminNg.directives.adminNgStats
 * @description
 * Generates a stats bar from the given resource.
 */
angular.module('adminNg.directives')
.directive('adminNgStats', ['Storage', function (Storage) {
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
            scope.statsFilterFields = [].concat.apply([],
                                          scope.stats.stats
                                              .map(function(stat) {
                                                  return stat.filters;
                                              })
                                      )
                                      .reduce(function(fieldArr, current) {
                                          if (fieldArr.indexOf(current.name) === -1) {
                                              fieldArr.push(current.name);
                                          }
                                          return fieldArr;
                                      }, []);

            scope.showStatsFilter = function (index) {
                angular.forEach(scope.statsFilterFields, function(field) {
                    Storage.remove('filter', scope.stats.resource, field);
                });
                angular.forEach(scope.stats.stats[index].filters, function (filter) {
                    Storage.put('filter', scope.stats.resource, filter.name, filter.value);
                });
            };
        }
    };
}]);
