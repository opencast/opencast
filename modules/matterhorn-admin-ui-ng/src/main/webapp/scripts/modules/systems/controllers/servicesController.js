angular.module('adminNg.controllers')
.controller('ServicesCtrl', ['$scope', 'Table', 'ServicesResource', 'ServiceResource',
    function ($scope, Table, ServicesResource, ServiceResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'status',
                label: 'SYSTEMS.SERVICES.TABLE.STATUS'
            }, {
                name:  'name',
                label: 'SYSTEMS.SERVICES.TABLE.NAME'
            }, {
                name:  'completed',
                label: 'SYSTEMS.SERVICES.TABLE.COMPLETED'
            }, {
                name:  'running',
                label: 'SYSTEMS.SERVICES.TABLE.RUNNING'
            }, {
                name:  'queued',
                label: 'SYSTEMS.SERVICES.TABLE.QUEUED'
            }, {
                name:  'meanRunTime',
                label: 'SYSTEMS.SERVICES.TABLE.MEAN_RUN_TIME'
            }, {
                name:  'meanQueueTime',
                label: 'SYSTEMS.SERVICES.TABLE.MEAN_QUEUE_TIME'
            }, {
                template: 'modules/systems/partials/serviceActionsCell.html',
                label:    'SYSTEMS.SERVICES.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'SYSTEMS.SERVICES.TABLE.CAPTION',
            resource:   'services',
            category:   'systems',
            apiService: ServicesResource
        });

        $scope.table.sanitize = function (host, serviceType) {
            ServiceResource.sanitize({
                host: host,
                serviceType: serviceType
            }, function () {
                $scope.table.fetch();
            });
        };
    }
]);
