describe('Dates Step in User Blacklist Wizard', function () {
    var NewUserblacklistDates, NewUserblacklistItems, BlacklistCountResource, $httpBackend, JsHelper;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_NewUserblacklistDates_, _NewUserblacklistItems_, _BlacklistCountResource_, _$httpBackend_, _JsHelper_) {
        NewUserblacklistDates = _NewUserblacklistDates_;
        NewUserblacklistItems = _NewUserblacklistItems_;
        BlacklistCountResource = _BlacklistCountResource_;
        $httpBackend = _$httpBackend_;
        JsHelper = _JsHelper_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/users/users.json').respond('{"results":[]}');
    });

    describe('#isValid', function () {
        beforeEach(function () {
            NewUserblacklistItems.ud.items = [{ id: 81294 }];
        });

        it('is invalid by default', function () {
            expect(NewUserblacklistDates.isValid()).toBeFalsy();
        });

        it('becomes valid when from- and to dates and times are set', function () {
            NewUserblacklistDates.ud.fromDate = '2014-07-07';
            NewUserblacklistDates.ud.fromTime = '10:12';
            expect(NewUserblacklistDates.isValid()).toBe(false);
            NewUserblacklistDates.ud.toDate = '2014-09-02';
            NewUserblacklistDates.ud.toTime = '22:23';
            expect(NewUserblacklistDates.isValid()).toBe(true);
        });
    });

    describe('#updateBlacklistCount', function () {

        describe('with valid parameters', function () {

            it('retrieves the black list count for the current parameters', function () {
                var startDate =  JsHelper.toZuluTimeString({
                        date   : '2014-07-07',
                        hour   : '10',
                        minute : '12'
                    }),
                    endDate = JsHelper.toZuluTimeString({
                        date   : '2014-08-01',
                        hour   : '22',
                        minute : '23'
                    });

                NewUserblacklistDates.ud.fromDate = '2014-07-07';
                NewUserblacklistDates.ud.fromTime = '10:12';
                NewUserblacklistDates.ud.toDate = '2014-08-01';
                NewUserblacklistDates.ud.toTime = '22:23';
                NewUserblacklistItems.ud.items = [{ id: 81294 }];
                $httpBackend.expectGET('/blacklist/blacklistCount?blacklistedId=81294&end=' + endDate + '&start=' + startDate + '&type=person').respond(201);
                NewUserblacklistDates.updateBlacklistCount();
                $httpBackend.flush();
            });
        });

        describe('with invalid parameters', function () {

            it('does not retrieve the black list count', function () {
                NewUserblacklistDates.ud.toDate = '2014-08-01';
                NewUserblacklistItems.ud.items = [{ id: 81294 }];
                spyOn(BlacklistCountResource, 'save');
                NewUserblacklistDates.updateBlacklistCount();

                expect(BlacklistCountResource.save).not.toHaveBeenCalled();
            });
        });
    });
});
