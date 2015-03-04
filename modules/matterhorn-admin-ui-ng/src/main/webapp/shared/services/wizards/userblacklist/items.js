angular.module('adminNg.services')
.factory('NewUserblacklistItems', ['UsersResource', '$location', function (UsersResource, $location) {
    var Users = function () {
        var me = this;

        this.isEditing = function () {
            var action = $location.search().action;
            return angular.isDefined(action);
        };

        this.reset = function () {
            me.ud = { items: [] };
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.length > 0;
        };

        this.items = UsersResource.query();

        this.addUser = function () {
            var found = false;
            angular.forEach(me.ud.items, function (user) {
                if (user.id === me.ud.userToAdd.id) {
                    found = true;
                }
            });
            if (!found) {
                me.ud.items.push(me.ud.userToAdd);
                me.ud.userToAdd = {};
            }
        };

        // Selecting multiple blacklistedIds is not yet supported by
        // the back end.

        //this.selectAll = function () {
        //    angular.forEach(me.ud.items, function (user) {
        //        user.selected = me.all;
        //    });
        //};

        //this.removeUser = function () {
        //    var items = [];
        //    angular.forEach(me.ud.items, function (user) {
        //        if (!user.selected) {
        //            items.push(user);
        //        }
        //    });
        //    me.ud.items = items;
        //};
    };
    return new Users();
}]);
