angular.module('adminNg.services')
.factory('NewSeriesAccess', ['ResourcesListResource', 'SeriesAccessResource', 'Notifications', '$timeout',
    function (ResourcesListResource, SeriesAccessResource, Notifications, $timeout) {
    var Access = function () {

        var me = this,
            NOTIFICATION_CONTEXT = 'series-acl',
            aclNotification,
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false
                };
            },
            checkNotification = function () {
                if (me.unvalidRule) {
                    if (!angular.isUndefined(me.notificationRules)) {
                        Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                    }
                    me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
                } else if (!angular.isUndefined(me.notificationRules)) {
                    Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                    me.notificationRules = undefined;
                }

                if (!me.hasRights) {
                    if (!angular.isUndefined(me.notificationRights)) {
                        Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                    }
                    me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
                } else if (!angular.isUndefined(me.notificationRights)) {
                    Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                    me.notificationRights = undefined;
                }

                $timeout(function () {
                    checkNotification();
                 }, 200);
            };

        me.ud = {};
        me.ud.id = {};
        me.ud.policies = [];
        me.ud.baseAcl = {};

        this.changeBaseAcl = function () {
            var newPolicies = {};
            me.ud.baseAcl = SeriesAccessResource.getManagedAcl({id: me.ud.id}, function () {
                angular.forEach(me.ud.baseAcl.acl.ace, function (acl) {
                    var policy = newPolicies[acl.role];

                    if (angular.isUndefined(policy)) {
                        newPolicies[acl.role] = createPolicy(acl.role);
                    }
                    newPolicies[acl.role][acl.action] = acl.allow;
                });

                me.ud.policies = [];
                angular.forEach(newPolicies, function (policy) {
                    me.ud.policies.push(policy);
                });

                me.ud.id = '';
            });
        };

        this.addPolicy = function () {
            me.ud.policies.push(createPolicy());
        };

        this.deletePolicy = function (policyToDelete) {
            var index;

            angular.forEach(me.ud.policies, function (policy, idx) {
                if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write && 
                    policy.read === policyToDelete.read) {
                    index = idx;
                }
            });

            if (angular.isDefined(index)) {
                me.ud.policies.splice(index, 1);
            }
        };

        this.isValid = function () {
             var hasRights = false,
                rulesValid = true;

             angular.forEach(me.ud.policies, function (policy) {
                rulesValid = false;

                if (policy.read && policy.write) {
                    hasRights = true;
                }

                if ((policy.read || policy.write) && !angular.isUndefined(policy.role)) {
                    rulesValid = true;
                }
             });

            me.unvalidRule = !rulesValid;
            me.hasRights = hasRights;

            if (hasRights && angular.isDefined(aclNotification)) {
                Notifications.remove(aclNotification, 'series-acl');
            }

            if (!hasRights && !angular.isDefined(aclNotification)) {
                aclNotification = Notifications.add('warning', 'SERIES_ACL_MISSING_READWRITE_ROLE', 'series-acl', -1);
            }
            
            return rulesValid && hasRights;
        };

        checkNotification();
        
        me.acls  = ResourcesListResource.get({ resource: 'ACL' });
        me.roles = ResourcesListResource.get({ resource: 'ROLES' }); 

        this.reset = function () {
            me.ud = {
                id: {},
                policies: []
            };
        };

        this.reload = function () {
            me.acls  = ResourcesListResource.get({ resource: 'ACL' });
            me.roles = ResourcesListResource.get({ resource: 'ROLES' }); 
        };

        this.reset();
    };

    return new Access();
}]);
