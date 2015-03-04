angular.module('adminNg.controllers')
.controller('JobsCtrl', ['$scope', 'Table', 'JobsResource',
    function ($scope, Table, JobsResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'operation',
                label: 'SYSTEMS.JOBS.TABLE.TITLE'
            }, {
                name:  'workflow',
                label: 'SYSTEMS.JOBS.TABLE.WORKFLOW'
            }, {
                name:  'status',
                label: 'SYSTEMS.JOBS.TABLE.STATUS'
            }, {
                name:  'submitted',
                label: 'SYSTEMS.JOBS.TABLE.SUBMITTED'
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
