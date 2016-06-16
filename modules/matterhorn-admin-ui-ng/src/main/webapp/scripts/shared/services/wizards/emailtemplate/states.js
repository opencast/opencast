angular.module('adminNg.services')
.factory('EmailtemplateStates', ['EmailtemplateMessage', 'EmailtemplateSummary',
        function (EmailtemplateMessage, EmailtemplateSummary) {
    return {
        get: function () {
            return [{
                translation: 'CONFIGURATION.EMAIL_TEMPLATES.DETAILS.TABS.MESSAGE',
                name: 'message',
                stateController: EmailtemplateMessage
            }, {
                translation: 'CONFIGURATION.EMAIL_TEMPLATES.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: EmailtemplateSummary
            }];
        },
        reset: function () {
            EmailtemplateMessage.reset();
        }
    };
}]);
