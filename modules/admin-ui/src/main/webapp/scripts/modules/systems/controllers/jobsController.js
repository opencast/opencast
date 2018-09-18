angular.module('adminNg.controllers')
.controller('JobsCtrl', ['$scope', 'Table', 'JobsResource', 'ResourcesFilterResource',
    function ($scope, Table, JobsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'id',
                label: 'SYSTEMS.JOBS.TABLE.ID'
            }, {
                name:  'status',
                label: 'SYSTEMS.JOBS.TABLE.STATUS',
                translate: true
            }, {
                name:  'operation',
                label: 'SYSTEMS.JOBS.TABLE.OPERATION'
            }, {
                name:  'type',
                label: 'SYSTEMS.JOBS.TABLE.TYPE'
            }, {
                name:  'processingHost',
                label: 'SYSTEMS.JOBS.TABLE.HOST_NAME'
            }, {
                name:  'submitted',
                label: 'SYSTEMS.JOBS.TABLE.SUBMITTED'
            }, {
                name:  'started',
                label: 'SYSTEMS.JOBS.TABLE.STARTED'
            }, {
                name:  'creator',
                label: 'SYSTEMS.JOBS.TABLE.CREATOR'
            //}, {
            //    template: 'modules/systems/partials/jobActionsCell.html',
            //    label:    'SYSTEMS.JOBS.TABLE.ACTION',
            //    dontSort: true
            }],
            caption:    'SYSTEMS.JOBS.TABLE.CAPTION',
            resource:   'jobs',
            category:   'systems',
            apiService: JobsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
    }
]);
