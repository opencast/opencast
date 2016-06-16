angular.module('adminNg.controllers')
.controller('ThemesCtrl', ['$scope', 'Table', 'ThemesResource', 'ThemeResource', 'ResourcesFilterResource', 'Notifications',
    function ($scope, Table, ThemesResource, ThemeResource, ResourcesFilterResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'CONFIGURATION.THEMES.TABLE.NAME'
            }, {
                name:  'description',
                label: 'CONFIGURATION.THEMES.TABLE.DESCRIPTION'
            },  {
                name:  'creator',
                label: 'CONFIGURATION.THEMES.TABLE.CREATOR'
            }, {
                name:  'creation_date',
                label: 'CONFIGURATION.THEMES.TABLE.CREATED'
            },
            /* 
             * Temporarily disabled
             *
             *{
             *    name:  'default',
             *    label: 'CONFIGURATION.THEMES.TABLE.DEFAULT'
             *},
             */
            {
                template: 'modules/configuration/partials/themesActionsCell.html',
                label:    'CONFIGURATION.THEMES.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'CONFIGURATION.THEMES.TABLE.CAPTION',
            resource:   'themes',
            category:   'configuration',
            apiService: ThemesResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (id) {
            ThemeResource.delete({id: id}, function () {
                Table.fetch();
                Notifications.add('success', 'THEME_DELETED');
            }, function () {
                Notifications.add('error', 'THEME_NOT_DELETED', 'user-form');
            });
        };

    }
]);
