angular.module('adminNg.services')
.factory('NewUserblacklistStates', ['NewUserblacklistItems', 'NewUserblacklistDates', 'NewUserblacklistReason', 'NewUserblacklistSummary',
        function (NewUserblacklistItems, NewUserblacklistDates, NewUserblacklistReason, NewUserblacklistSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.USERS',
                name: 'items',
                stateController: NewUserblacklistItems
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.DATES',
                name: 'dates',
                stateController: NewUserblacklistDates
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.REASON',
                name: 'reason',
                stateController: NewUserblacklistReason
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewUserblacklistSummary
            }];
        },
        reset: function () {
            NewUserblacklistItems.reset();
            NewUserblacklistDates.reset();
            NewUserblacklistReason.reset();
        }
    };
}]);
