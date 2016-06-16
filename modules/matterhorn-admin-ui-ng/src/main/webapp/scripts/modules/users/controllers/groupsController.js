angular.module('adminNg.controllers')
.controller('GroupsCtrl', ['$scope', 'Table', 'Modal', 'GroupsResource', 'GroupResource', 'ResourcesFilterResource', 'Notifications',
    function ($scope, Table, Modal, GroupsResource, GroupResource, ResourcesFilterResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'USERS.GROUPS.TABLE.NAME'
            }, {
                name:  'description',
                label: 'USERS.GROUPS.TABLE.DESCRIPTION'
            }, {
                name:  'role',
                label: 'USERS.GROUPS.TABLE.ROLE'
            }, {
               template: 'modules/users/partials/groupActionsCell.html',
               label:    'USERS.USERS.TABLE.ACTION',
               dontSort: true
            }],
            caption:    'USERS.GROUPS.TABLE.CAPTION',
            resource:   'groups',
            category:   'users',
            apiService: GroupsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
          GroupResource.delete({id: row.id}, function () {
              Table.fetch();
              Modal.$scope.close();
              Notifications.add('success', 'GROUP_DELETED');
          }, function () {
              Notifications.add('error', 'GROUP_NOT_DELETED', 'group-form');
          });
        };
    }
]);
