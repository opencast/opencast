angular.module('adminNg.controllers')
.controller('AclsCtrl', ['$scope', 'Table', 'AclsResource', 'AclResource', 'Notifications',
    function ($scope, Table, AclsResource, AclResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'USERS.ACLS.TABLE.NAME'
            //}, {
            //    name:  'created',
            //    label: 'USERS.ACLS.TABLE.CREATED'
            //}, {
            //    name:  'creator',
            //    label: 'USERS.ACLS.TABLE.CREATOR'
            //}, {
            //    name:  'in_use',
            //    label: 'USERS.ACLS.TABLE.IN_USE'
            }, {
               template: 'modules/users/partials/aclActionsCell.html',
               label:    'USERS.ACLS.TABLE.ACTION',
               dontSort: true
            }],
            caption:    'USERS.ACLS.TABLE.CAPTION',
            resource:   'acls',
            category:   'users',
            apiService: AclsResource
        });

        $scope.table.delete = function (id) {
            AclResource.delete({id: id}, function () {
                Notifications.add('success', 'ACL_DELETED');
            }, function () {
                Notifications.add('error', 'ACL_NOT_DELETED');
            });
        };
    }
]);
