angular.module('adminNg.controllers')
.controller('ServersCtrl', ['$scope', 'Table', 'ServersResource', 'ServiceResource',
    function ($scope, Table, ServersResource, ServiceResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:     'online',
                template: 'modules/systems/partials/serverStatusCell.html',
                label:    'SYSTEMS.SERVERS.TABLE.STATUS'
            }, {
                name:  'name',
                label: 'SYSTEMS.SERVERS.TABLE.HOST_NAME'
            }, {
                name:  'cores',
                label: 'SYSTEMS.SERVERS.TABLE.CORES'
            }, {
                name:  'completed',
                label: 'SYSTEMS.SERVERS.TABLE.COMPLETED'
            }, {
                name:  'running',
                label: 'SYSTEMS.SERVERS.TABLE.RUNNING'
            }, {
                name:  'queued',
                label: 'SYSTEMS.SERVERS.TABLE.QUEUED'
            }, {
                name:  'meanRunTime',
                label: 'SYSTEMS.SERVERS.TABLE.MEAN_RUN_TIME'
            }, {
                name:  'meanQueueTime',
                label: 'SYSTEMS.SERVERS.TABLE.MEAN_QUEUE_TIME'
            }, {
                name:     'maintenance',
                template: 'modules/systems/partials/serverMaintenanceCell.html',
                label:    'SYSTEMS.SERVERS.TABLE.MAINTENANCE'
            //}, {
            //    template: 'modules/systems/partials/serverActionsCell.html',
            //    label:    'SYSTEMS.SERVERS.TABLE.ACTION',
            //    dontSort: true
            }],
            caption:    'SYSTEMS.SERVERS.TABLE.CAPTION',
            resource:   'servers',
            category:   'systems',
            apiService: ServersResource
        });

        $scope.table.setMaintenanceMode = function (host, maintenance) {
            ServiceResource.setMaintenanceMode({
                host: host,
                maintenance: maintenance
            }, function () {
                $scope.table.fetch();
            });
        };
    }
]);
