angular.module('adminNg.services')
.factory('NewAclAccess', ['ResourcesListResource', 'AclResource', function (ResourcesListResource, AclResource) {
    var Access = function () {

        var me = this,
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false
                };
            };

        me.isAccessState = true;
        me.ud = {};
        me.ud.id = {};
        me.ud.policies = [];
        me.ud.baseAcl = {};

        this.changeBaseAcl = function () {
            var newPolicies = {};
            me.ud.baseAcl = AclResource.get({id: me.ud.id}, function () {
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
            // Is always true, the series can have an empty ACL
            return true;
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
