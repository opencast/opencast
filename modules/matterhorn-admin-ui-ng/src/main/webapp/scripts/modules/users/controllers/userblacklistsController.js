angular.module('adminNg.controllers')
.controller('UserblacklistsCtrl', ['$scope', 'Table', 'Notifications', 'UserBlacklistsResource', 'ResourcesFilterResource',
    function ($scope, Table, Notifications, UserBlacklistsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'resourceName',
                label: 'USERS.BLACKLISTS.TABLE.NAME'
            }, {
                name:  'date_from',
                label: 'USERS.BLACKLISTS.TABLE.DATE_FROM',
                dontSort: true
            }, {
                name:  'date_to',
                label: 'USERS.BLACKLISTS.TABLE.DATE_TO',
                dontSort: true
            }, {
                name:  'reason',
                label: 'USERS.BLACKLISTS.TABLE.REASON',
                translate: true
            }, {
                template: 'modules/users/partials/userblacklistActionsCell.html',
                label:    'USERS.BLACKLISTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'USERS.BLACKLISTS.TABLE.CAPTION',
            resource:   'userblacklists',
            category:   'users',
            apiService: UserBlacklistsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            row.$delete({ id: row.id }, function () {
                Table.fetch();
                Notifications.add('success', 'USER_BLACKLIST_DELETED');
            }, function () {
                Notifications.add('error', 'USER_BLACKLIST_NOT_DELETED');
            });
        };
    }
]);
