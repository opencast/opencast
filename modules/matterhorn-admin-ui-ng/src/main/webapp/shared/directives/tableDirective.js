/**
 * @ngdoc directive
 * @name adminNg.directives.adminNgTable
 * @description
 * Generates a table from the given resource.
 *
 * The generated table has the following features:
 * * Sorts by column without reloading the resource.
 * * Listens to changes to any filter values (see adminNg.directives.adminNgTableFilter).
 *
 * Future features:
 * * Pagination integration with the resource (records per page and offset).
 *
 * @example
 * <admin-ng-table="" table="table" />
 */
angular.module('adminNg.directives')
.directive('adminNgTable', ['Storage', '$translate', function (Storage, $translate) {
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
        templateUrl: 'shared/partials/table.html',
        replace: false,
        scope: {
            table: '='
        },
        link: function (scope, element) {
            scope.table.fetch();

            // Deregister change handler
            scope.$on('$destroy', function () {
                scope.deregisterChange();
            });

            // React on filter changes
            scope.deregisterChange = Storage.scope.$on('change', function (event, type) {
                if (type === 'filter') {
                    scope.table.fetch();
                }
                if (type === 'table_column_visibility') {
                    scope.table.refreshColumns();
                    scope.calculateStyles();
                }
            });

            scope.calculateStyles = function () {
                angular.forEach(scope.table.columns, function (column) {
                    if (angular.isDefined(column.width)) {
                        column.style = {'width': column.width};
                    } else {
                        $translate(column.label).then(function (translation) {
                            setWidth(translation, column, element);
                        });
                    }
                });
            };
        }
    };
}]);
