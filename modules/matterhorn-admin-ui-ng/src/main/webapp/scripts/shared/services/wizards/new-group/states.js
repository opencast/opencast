angular.module('adminNg.services')
.factory('NewGroupStates', ['NewGroupMetadata', 'NewGroupRoles', 'NewGroupUsers', 'NewGroupSummary',
        function (NewGroupMetadata, NewGroupRoles, NewGroupUsers, NewGroupSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.GROUPS.DETAILS.TABS.METADATA',
                name: 'metadata',
                stateController: NewGroupMetadata
            }, {
                translation: 'USERS.GROUPS.DETAILS.TABS.ROLES',
                name: 'roles',
                stateController: NewGroupRoles
            }, {
                translation: 'USERS.GROUPS.DETAILS.TABS.USERS',
                name: 'users',
                stateController: NewGroupUsers
            }, {
                translation: 'USERS.GROUPS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewGroupSummary
            }];
        },
        reset: function () {
            NewGroupMetadata.reset();
            NewGroupRoles.reset();
            NewGroupUsers.reset();
        }
    };
}]);
