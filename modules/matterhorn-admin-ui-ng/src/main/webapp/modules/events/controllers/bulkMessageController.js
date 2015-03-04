angular.module('adminNg.controllers')
.controller('BulkMessageCtrl', ['$scope', 'BulkMessageStates', 'EmailResource', 'Table', 'Notifications', 'Modal', 'EVENT_TAB_CHANGE',
function ($scope, BulkMessageStates, EmailResource, Table, Notifications, Modal, EVENT_TAB_CHANGE) {
    BulkMessageStates.reset();
    $scope.states = BulkMessageStates.get();

    // Render the preview only when the summary tab has been reached.
    $scope.$on(EVENT_TAB_CHANGE, function (event, args) {
        if (args.current.name === 'summary') {
            $scope.states[1].stateController.updatePreview();
        }
    });

    $scope.submit = function () {
        var userdata = {};
        angular.forEach($scope.states, function (state) {
            userdata[state.name] = state.stateController.ud;
        });

        EmailResource.save({ templateId: userdata.message.email_template.id }, userdata, function () {
            Table.fetch();
            Notifications.add('success', 'EMAIL_SENT');
            Modal.$scope.close();
        }, function () {
            Notifications.add('error', 'EMAIL_NOT_SENT', 'bulk-message-form');
        });
    };
}]);
