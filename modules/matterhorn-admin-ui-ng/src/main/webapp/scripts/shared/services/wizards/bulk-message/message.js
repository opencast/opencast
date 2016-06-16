angular.module('adminNg.services')
.factory('BulkMessageMessage', ['EmailPreviewResource', 'EmailVariablesResource', 'EmailTemplatesResource', 'EmailTemplateResource', 'BulkMessageRecipients',
function (EmailPreviewResource, EmailVariablesResource, EmailTemplatesResource, EmailTemplateResource, BulkMessageRecipients) {
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
                    signature:    me.ud.include_signature ? true:false,
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
