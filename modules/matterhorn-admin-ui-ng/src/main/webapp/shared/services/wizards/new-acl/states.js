angular.module('adminNg.services')
.factory('NewAclStates', ['NewAclMetadata', 'NewAclAccess', 'NewAclSummary',
        function (NewAclMetadata, NewAclAccess, NewAclSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.ACLS.NEW.TABS.METADATA',
                name: 'metadata',
                stateController: NewAclMetadata
            }, {
                translation: 'USERS.ACLS.NEW.TABS.ACCESS',
                name: 'access',
                stateController: NewAclAccess
            }, {
                translation: 'USERS.ACLS.NEW.TABS.SUMMARY',
                name: 'summary',
                stateController: NewAclSummary
            }];
        },
        reset: function () {
            NewAclMetadata.reset();
            NewAclAccess.reset();
        }
    };
}]);
