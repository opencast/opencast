angular.module('adminNg.controllers')
.controller('JobsCtrl', ['$scope', 'Table', 'JobsResource',
    function ($scope, Table, JobsResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'operation',
                label: 'SYSTEMS.JOBS.TABLE.OPERATION'
            }, {
                name:  'type',
                label: 'SYSTEMS.JOBS.TABLE.TYPE'
            }, {
                name:  'submitted',
                label: 'SYSTEMS.JOBS.TABLE.SUBMITTED'
            }, {
                name:  'started',
                label: 'SYSTEMS.JOBS.TABLE.STARTED'
            }, {
                name:  'creator',
                label: 'SYSTEMS.JOBS.TABLE.CREATOR',
            }, {
                name:  'processingHost',
                label: 'SYSTEMS.JOBS.TABLE.HOST_NAME'
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
    }
]);
