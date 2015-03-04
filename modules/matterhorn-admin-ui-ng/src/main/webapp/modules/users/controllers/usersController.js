angular.module('adminNg.controllers')
.controller('UsersCtrl', ['$scope', 'Table', 'UsersResource', 'UserResource', 'ResourcesFilterResource', 'Notifications', 'Modal',
    function ($scope, Table, UsersResource, UserResource, ResourcesFilterResource, Notifications, Modal) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'USERS.USERS.TABLE.NAME'
            }, {
                name:  'username',
                label: 'USERS.USERS.TABLE.USERNAME'
            }, {
                name:  'roles',
                label: 'USERS.USERS.TABLE.ROLES'
            }, {
                name:  'provider',
                label: 'USERS.USERS.TABLE.TYPE'
            }, {
            //     name:  'blacklist_from',
            //     label: 'USERS.USERS.TABLE.BLACKLIST_FROM'
            // }, {
            //     name:  'blacklist_to',
            //     label: 'USERS.USERS.TABLE.BLACKLIST_TO'
            // }, {
               template: 'modules/users/partials/userActionsCell.html',
               label:    'USERS.USERS.TABLE.ACTION',
               dontSort: true
            }],
            caption:    'USERS.USERS.TABLE.CAPTION',
            resource:   'users',
            category:   'users',
            apiService: UsersResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            UserResource.delete({username: row}, function () {
                Table.fetch();
                Modal.$scope.close();
                Notifications.add('success', 'USER_DELETED');
            }, function () {
                Notifications.add('error', 'USER_NOT_DELETED');
            });
        };
    }
]);
