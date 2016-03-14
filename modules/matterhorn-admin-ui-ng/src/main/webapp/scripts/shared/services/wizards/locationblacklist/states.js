angular.module('adminNg.services')
.factory('NewLocationblacklistStates', ['NewLocationblacklistItems', 'NewLocationblacklistDates', 'NewLocationblacklistReason', 'NewLocationblacklistSummary',
        function (NewLocationblacklistItems, NewLocationblacklistDates, NewLocationblacklistReason, NewLocationblacklistSummary) {
    return {
        get: function () {
            return [{
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.LOCATIONS',
                name: 'items',
                stateController: NewLocationblacklistItems
            }, {
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.DATES',
                name: 'dates',
                stateController: NewLocationblacklistDates
            }, {
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.REASON',
                name: 'reason',
                stateController: NewLocationblacklistReason
            }, {
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewLocationblacklistSummary
            }];
        },
        reset: function () {
            NewLocationblacklistItems.reset();
            NewLocationblacklistDates.reset();
            NewLocationblacklistReason.reset();
        }
    };
}]);
