describe('Dates Step in Location Blacklist Wizard', function () {
    var NewLocationblacklistDates, NewLocationblacklistItems, BlacklistCountResource, $httpBackend, JsHelper;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_NewLocationblacklistDates_, _NewLocationblacklistItems_, _BlacklistCountResource_, _$httpBackend_, _JsHelper_) {
        NewLocationblacklistDates = _NewLocationblacklistDates_;
        NewLocationblacklistItems = _NewLocationblacklistItems_;
        BlacklistCountResource = _BlacklistCountResource_;
        $httpBackend = _$httpBackend_;
        JsHelper = _JsHelper_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond('{"results":[]}');
    });

    describe('#isValid', function () {
        beforeEach(function () {
            NewLocationblacklistItems.ud.items = [{ id: 81294 }];
        });

        it('is invalid by default', function () {
            expect(NewLocationblacklistDates.isValid()).toBeFalsy();
        });

        it('becomes valid when from- and to dates and times are set', function () {
            NewLocationblacklistDates.ud.fromDate = '2014-07-07';
            NewLocationblacklistDates.ud.fromTime = '10:12';
            expect(NewLocationblacklistDates.isValid()).toBe(false);
            NewLocationblacklistDates.ud.toDate = '2014-09-02';
            NewLocationblacklistDates.ud.toTime = '22:23';
            expect(NewLocationblacklistDates.isValid()).toBe(true);
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

                NewLocationblacklistDates.ud.fromDate = '2014-07-07';
                NewLocationblacklistDates.ud.fromTime = '10:12';
                NewLocationblacklistDates.ud.toDate = '2014-08-01';
                NewLocationblacklistDates.ud.toTime = '22:23';
                NewLocationblacklistItems.ud.items = [{ id: 81294 }];
                $httpBackend.expectGET('/blacklist/blacklistCount?blacklistedId=81294&end=' + endDate + '&start=' + startDate + '&type=room').respond(201);
                NewLocationblacklistDates.updateBlacklistCount();
                $httpBackend.flush();
            });
        });

        describe('with invalid parameters', function () {

            it('does not retrieve the black list count', function () {
                NewLocationblacklistDates.ud.toDate = '2014-08-01';
                NewLocationblacklistItems.ud.items = [{ id: 81294 }];
                spyOn(BlacklistCountResource, 'save');
                NewLocationblacklistDates.updateBlacklistCount();

                expect(BlacklistCountResource.save).not.toHaveBeenCalled();
            });
        });
    });
});
