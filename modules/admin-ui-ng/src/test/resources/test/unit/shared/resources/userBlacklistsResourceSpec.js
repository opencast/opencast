describe('User Blacklists API Resource', function () {
    var UserBlacklistsResource, $httpBackend, JsHelper;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDateTimeRaw:   function (val, date) { return date; },
            formatDateTime:      function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _UserBlacklistsResource_, _JsHelper_) {
        $httpBackend  = _$httpBackend_;
        UserBlacklistsResource = _UserBlacklistsResource_;
        JsHelper = _JsHelper_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#query', function () {

        it('calls the blacklist service with the correct type parameter', function () {
            $httpBackend.expectGET('/blacklist/blacklists.json?type=person')
                .respond(JSON.stringify(getJSONFixture('blacklist/blacklists.json')));
            UserBlacklistsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/blacklist/blacklists.json?type=person')
                .respond(JSON.stringify(getJSONFixture('blacklist/blacklists.json')));
            var data = UserBlacklistsResource.query();
            $httpBackend.flush();

            expect(data.rows.length).toBe(2);
            expect(data.rows[0].resourceName).toEqual('•mock• Hans');
        });
    });

    describe('#get', function () {

        it('calls the blacklist service with the correct type parameter', function () {
            $httpBackend.expectGET('/blacklist/241?type=person')
                .respond(JSON.stringify(getJSONFixture('blacklist/241')));
            UserBlacklistsResource.get({ id: '241' });
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/blacklist/241?type=person')
                .respond(JSON.stringify(getJSONFixture('blacklist/241')));
            var data = UserBlacklistsResource.get({ id: '241' });
            $httpBackend.flush();

            expect(data.resourceName).toEqual('•mock• Hans');
        });
    });

    describe('#save', function () {

        it('calls the blacklist service with the correct type parameter', function () {
            var payload = {
                    items: { items: [{ id: 826}] },
                    dates: {
                        fromDate: '2014-07-01',
                        toDate: '2014-12-01',
                        fromTime: '10:54',
                        toTime: '23:31'
                     },
                        reason: { reason: 'Indisposable', comment: 'Sorry about that' }
                    },
                    fromDate = JsHelper.toZuluTimeString({
                        date    : '2014-07-01',
                        hour   : '10',
                        minute : '54'
                    }),
                    toDate = JsHelper.toZuluTimeString({
                        date    : '2014-12-01',
                        hour   : '23',
                        minute : '31'
                    });

            $httpBackend.expectPOST('/blacklist', function (data) {
                var request = $.deparam(data);
                expect(request.blacklistedId).toEqual('826');
                expect(request.start).toEqual(fromDate);
                expect(request.end).toEqual(toDate);
                expect(request.purpose).toEqual(payload.reason.reason);
                expect(request.comment).toEqual(payload.reason.comment);
                return true;
            }).respond(200);

            UserBlacklistsResource.save({}, payload);
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPOST('/blacklist')
                .respond(203);
            UserBlacklistsResource.save();
            $httpBackend.flush();
        });
    });
});
