/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
'use strict';

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
