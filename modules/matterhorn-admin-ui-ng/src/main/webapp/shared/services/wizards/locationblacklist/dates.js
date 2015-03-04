angular.module('adminNg.services')
.factory('NewLocationblacklistDates', ['BlacklistCountResource', 'NewLocationblacklistItems', 'JsHelper',
function (BlacklistCountResource, NewLocationblacklistItems, JsHelper) {
    var Dates = function () {
        var me = this;

        this.reset = function () {
            me.ud = { fromDate: null, toDate: null };
        };
        this.reset();

        this.isValid = function () {
            return (me.ud.fromDate && me.ud.toDate && me.ud.fromTime && me.ud.toTime) ? true:false;
        };

        this.updateBlacklistCount = function () {
            if (me.isValid()) {
                var from = JsHelper.toZuluTimeString({
                    date: me.ud.fromDate,
                    hour: me.ud.fromTime.split(':')[0],
                    minute: me.ud.fromTime.split(':')[1]
                }),
                    to = JsHelper.toZuluTimeString({
                    date: me.ud.toDate,
                    hour: me.ud.toTime.split(':')[0],
                    minute: me.ud.toTime.split(':')[1]
                });

                me.blacklistCount = BlacklistCountResource.save({
                    type:          'room',
                    blacklistedId: NewLocationblacklistItems.ud.items[0].id,
                    start:         from,
                    end:           to
                });
            }
        };
    };
    return new Dates();
}]);
