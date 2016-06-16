angular.module('adminNg.controllers')
.controller('EmailtemplatesCtrl', ['$scope', 'Table', 'Notifications', 'EmailTemplatesResource', 'EmailTemplateResource', 'ResourcesFilterResource',
    function ($scope, Table, Notifications, EmailTemplatesResource, EmailTemplateResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'CONFIGURATION.EMAIL_TEMPLATES.TABLE.NAME'
            }, {
                name:  'creator',
                label: 'CONFIGURATION.EMAIL_TEMPLATES.TABLE.CREATOR'
            }, {
                name:  'created',
                label: 'CONFIGURATION.EMAIL_TEMPLATES.TABLE.CREATED'
            }, {
                template: 'modules/configuration/partials/emailtemplateActionsCell.html',
                label:    'CONFIGURATION.EMAIL_TEMPLATES.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'CONFIGURATION.EMAIL_TEMPLATES.TABLE.CAPTION',
            resource:   'emailtemplates',
            category:   'configuration',
            apiService: EmailTemplatesResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            EmailTemplateResource.delete({ id: row.id }, function () {
                Table.fetch();
                Notifications.add('success', 'EMAIL_TEMPLATE_DELETED');
            }, function () {
                Notifications.add('error', 'EMAIL_TEMPLATE_NOT_DELETED');
            });
        };
    }
]);
