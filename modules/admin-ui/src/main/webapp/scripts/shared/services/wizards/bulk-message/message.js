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

angular.module('adminNg.services')
.factory('BulkMessageMessage', ['EmailPreviewResource', 'EmailVariablesResource', 'EmailTemplatesResource',
  'EmailTemplateResource', 'BulkMessageRecipients',
  function (EmailPreviewResource, EmailVariablesResource, EmailTemplatesResource, EmailTemplateResource,
    BulkMessageRecipients) {
    var Message = function () {
      var me = this;

      this.reset = function () {
        me.ud = {};
        this.templates = EmailTemplatesResource.query();
      };
      this.reset();

      this.variables = EmailVariablesResource.query();

      this.applyTemplate = function () {
        EmailTemplateResource.get({ id: me.ud.email_template.id }, function (template) {
          me.ud.message = template.message;
          me.ud.subject = template.subject;
        });
      };

      this.isValid = function () {
        return (angular.isDefined(me.ud) &&
                    angular.isDefined(me.ud.message) &&
                    angular.isDefined(me.ud.subject));
      };

      this.updatePreview = function () {
        if (me.isValid()) {
          me.preview = EmailPreviewResource.save({
            templateId: me.ud.email_template.id
          }, {
            recordingIds: BulkMessageRecipients.ud.items.recordings
                        .map(function (item) { return item.id; }).join(','),
            personIds:    BulkMessageRecipients.ud.items.recipients
                        .map(function (item) { return item.id; }).join(','),
            signature:    me.ud.include_signature ? true : false,
            body:         me.ud.message
          });
        }
      };

      this.insertVariable = function (variable) {
        var message  = me.ud.message || '',
            position = angular.element('textarea[ng-model="wizard.step.ud.message"]')[0].selectionStart;

        me.ud.message = message.substr(0, position) + variable + message.substr(position);
      };
    };
    return new Message();
  }]);
