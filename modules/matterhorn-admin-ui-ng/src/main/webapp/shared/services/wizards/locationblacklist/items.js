angular.module('adminNg.services')
.factory('NewLocationblacklistItems', ['CaptureAgentsResource', function (CaptureAgentsResource) {
    var Locations = function () {
        var me = this;

        this.reset = function () {
            me.ud = { items: [] };
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.length > 0;
        };

        this.items = CaptureAgentsResource.query();

        this.addItem = function () {
            var found = false;
            angular.forEach(me.ud.items, function (item) {
                if (item.id === me.ud.itemToAdd.id) {
                    found = true;
                }
            });
            if (!found) {
                me.ud.items.push(me.ud.itemToAdd);
                me.ud.itemToAdd = {};
            }
        };

        // Selecting multiple blacklistedIds is not yet supported by
        // the back end.

        //this.selectAll = function () {
        //    angular.forEach(me.ud.items, function (item) {
        //        item.selected = me.all;
        //    });
        //};

        //this.removeItem = function () {
        //    var items = [];
        //    angular.forEach(me.ud.items, function (item) {
        //        if (!item.selected) {
        //            items.push(item);
        //        }
        //    });
        //    me.ud.items = items;
        //};
    };
    return new Locations();
}]);
