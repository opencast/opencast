angular.module('adminNg.controllers')
.controller('ServicesCtrl', ['$scope', 'Table', 'ServicesResource', 'ServiceResource', 'ResourcesFilterResource',
    function ($scope, Table, ServicesResource, ServiceResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'status',
                label: 'SYSTEMS.SERVICES.TABLE.STATUS',
                translate: true
            }, {
                name:  'name',
                label: 'SYSTEMS.SERVICES.TABLE.NAME'
            }, {
                name:  'hostname',
                label: 'SYSTEMS.SERVICES.TABLE.HOST_NAME'
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
            apiService: ServicesResource,
            sorter:     {"sorter":{"services":{"status":{"name":"status","priority":0,"order":"DESC"}}}}
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.sanitize = function (hostname, serviceType) {
            ServiceResource.sanitize({
                host: hostname,
                serviceType: serviceType
            }, function () {
                $scope.table.fetch();
            });
        };
    }
]);
