angular.module('adminNg.services')
.factory('NewEventAccess', ['ResourcesListResource', 'EventAccessResource', 'SeriesAccessResource', function (ResourcesListResource, EventAccessResource, SeriesAccessResource) {
    var Access = function () {

        var me = this,
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false
                };
            },
            changePolicies = function (access, loading) {
                var newPolicies = {};
                angular.forEach(access, function (acl) {
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

                if (!loading) {
                    me.ud.accessSave();
                }
            };

        me.isAccessState = true;
        me.ud = {};
        me.ud.id = {};
        me.ud.policies = [];
        me.ud.baseAcl = {};

        this.setMetadata = function (metadata) {
            me.metadata = metadata;
            me.loadSeriesAcl();
        };

        this.loadSeriesAcl = function () {
            angular.forEach(me.metadata.getUserEntries(), function (m) {
                if (m.id === 'isPartOf' && angular.isDefined(m.value) && m.value !== '') {
                    SeriesAccessResource.get({ id: m.value }, function (data) {
                        if (angular.isDefined(data.series_access)) {
                            var json = angular.fromJson(data.series_access.acl);
                            changePolicies(json.acl.ace, true);
                        }
                    });
                }
            });
        };

        this.changeBaseAcl = function () {
            var newPolicies = {};
            me.ud.baseAcl = EventAccessResource.getManagedAcl({id: me.ud.id}, function () {
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
             var hasRights = false;

             angular.forEach(me.ud.policies, function (policy) {
                if (policy.read && policy.write) {
                    hasRights = true;
                }
             });
            
            return hasRights;
        };
        
        me.acls  = ResourcesListResource.get({ resource: 'ACL' });
        me.roles = ResourcesListResource.get({ resource: 'ROLES' }); 

        this.reset = function () {
            me.ud = {
                id: {},
                policies: []
            };
        };

        this.reset();
    };
    return new Access();
}]);
