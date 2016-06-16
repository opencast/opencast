angular.module('adminNg.services')
    .factory('EmailtemplateMessage', ['$location', 'ResourcesListResource', 'EmailTemplateDemoResource',
        'EmailVariablesResource', 'Notifications',
        function ($location, ResourcesListResource, EmailTemplateDemoResource, EmailVariablesResource, Notifications) {
            var Message = function () {
                var me = this, 
                    isNameUnique = function () {
                        me.unique = true;
                        angular.forEach(me.names, function (name) {
                            if (name === me.ud.name) {
                                me.unique = false;
                            }
                        });
                        return me.unique;
                    },
                    NOTIFICATION_CONTEXT = 'emailtemplate-form';

                me.unique = true;

                this.reset = function () {
                    me.ud = {};
                    me.names = ResourcesListResource.get({ resource: 'EMAIL_TEMPLATE_NAMES' });
                };
                this.reset();

                this.variables = EmailVariablesResource.query();

                this.isValid = function () {

                    if (!me.names.$resolved) {
                        // Make sure the template name can be validated at all
                        return false;
                    }

                    var nameIsUnique = isNameUnique();


                    if ($location.search().action === 'add' && me.ud.name && me.ud.name.length > 2) {
                        if(!nameIsUnique){
                            if (me.notification) {
                                Notifications.remove(me.notification, NOTIFICATION_CONTEXT);
                            }
                            me.notification = Notifications.add('error', 'THEME_NAME_ALREADY_TAKEN', NOTIFICATION_CONTEXT);
                        }
                    }

                    if (me.ud.message && me.ud.subject && me.ud.name && nameIsUnique) {
                        return true;
                    }
                    return false;
                };

                this.updatePreview = function () {
                    if (me.isValid()) {
                        me.preview = EmailTemplateDemoResource.save({
                            body:    me.ud.message,
                            subject: me.ud.subject
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
