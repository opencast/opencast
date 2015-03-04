angular.module('adminNg.services')
.factory('BulkMessageStates', ['BulkMessageRecipients', 'BulkMessageMessage', 'BulkMessageSummary',
        function (BulkMessageRecipients, BulkMessageMessage, BulkMessageSummary) {
    return {
        get: function () {
            return [{
                translation: 'EVENTS.SEND_MESSAGE.TABS.RECIPIENTS',
                name: 'recipients',
                stateController: BulkMessageRecipients
            }, {
                translation: 'EVENTS.SEND_MESSAGE.TABS.MESSAGE',
                name: 'message',
                stateController: BulkMessageMessage
            }, {
                translation: 'EVENTS.SEND_MESSAGE.TABS.SUMMARY',
                name: 'summary',
                stateController: BulkMessageSummary
            }];
        },
        reset: function () {
            BulkMessageRecipients.reset();
            BulkMessageMessage.reset();
        }
    };
}]);
