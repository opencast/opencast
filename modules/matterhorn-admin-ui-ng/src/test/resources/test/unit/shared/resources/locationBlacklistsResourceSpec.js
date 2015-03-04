describe('Location Blacklists API Resource', function () {
    var LocationBlacklistsResource, $httpBackend, JsHelper;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDateTimeRaw:   function (val, date) { return date; },
            formatDateTime:      function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _LocationBlacklistsResource_, _JsHelper_) {
        $httpBackend  = _$httpBackend_;
        LocationBlacklistsResource = _LocationBlacklistsResource_;
        JsHelper = _JsHelper_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#query', function () {

        it('calls the blacklist service with the correct type parameter', function () {
            $httpBackend.expectGET('/blacklist/blacklists.json?type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/blacklists.json')));
            LocationBlacklistsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/blacklist/blacklists.json?type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/blacklists.json')));
            var data = LocationBlacklistsResource.query();
            $httpBackend.flush();

            expect(data.rows.length).toBe(2);
            expect(data.rows[0].resourceName).toEqual('•mock• Hans');
        });
    });

    describe('#get', function () {

        it('calls the blacklist service with the correct type parameter', function () {
            $httpBackend.expectGET('/blacklist/241?type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/241')));
            LocationBlacklistsResource.get({ id: '241' });
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/blacklist/241?type=room')
                .respond(JSON.stringify(getJSONFixture('blacklist/241')));
            var data = LocationBlacklistsResource.get({ id: '241' });
            $httpBackend.flush();

            expect(data.resourceName).toEqual('•mock• Hans');
        });
    });

    describe('#save', function () {
        var payload, startDate, endDate;
        beforeEach(function () {
            startDate = {
                date   : '2014-07-01',
                hour   : '10',
                minute : '54'
            };            
            endDate = {
                date   : '2014-12-01',
                hour   : '23',
                minute : '31'
            };
            payload = {
                items: { items: [{ id: 826}] },
                dates: {
                    fromDate: startDate.date,
                    toDate: endDate.date,
                    fromTime: '10:54',
                    toTime: '23:31'
                },
                reason: { reason: 'Indisposable', comment: 'Sorry about that' }
            };
        });

        it('calls the blacklist service with the correct type parameter', function () {
            $httpBackend.expectPOST('/blacklist', function (data) {
                var request = $.deparam(data);
                expect(request.blacklistedId).toEqual('826');
                expect(request.start).toEqual(JsHelper.toZuluTimeString(startDate));
                expect(request.end).toEqual(JsHelper.toZuluTimeString(endDate));
                expect(request.purpose).toEqual(payload.reason.reason);
                expect(request.comment).toEqual(payload.reason.comment);
                return true;
            }).respond(200);

            LocationBlacklistsResource.save({}, payload);
            $httpBackend.flush();
        });

        it('handles empty items gracefully', function () {
            payload.items.items = [];

            $httpBackend.expectPOST('/blacklist', function (data) {
                var request = $.deparam(data);
                expect(request.start).toEqual(JsHelper.toZuluTimeString(startDate));
                expect(request.end).toEqual(JsHelper.toZuluTimeString(endDate));
                expect(request.purpose).toEqual(payload.reason.reason);
                expect(request.comment).toEqual(payload.reason.comment);
                return true;
            }).respond(200);

            LocationBlacklistsResource.save({}, payload);
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPOST('/blacklist')
                .respond(203);
            LocationBlacklistsResource.save();
            $httpBackend.flush();
        });
    });

    describe('#update', function () {
        var payload, startDate, endDate;
        beforeEach(function () {
            startDate = {
                date   : '2014-07-01',
                hour   : '10',
                minute : '54'
            };            
            endDate = {
                date   : '2014-12-01',
                hour   : '23',
                minute : '31'
            };
            payload = {
                items: { items: [{ id: 826}] },
                dates: {
                    fromDate : startDate.date,
                    toDate   : endDate.date,
                    fromTime : '10:54',
                    toTime   : '23:31'
                },
                reason: { reason: 'Indisposable', comment: 'Sorry about that' }
            };
        });

        it('calls the blacklist service with the correct type parameter', function () {
            $httpBackend.expectPUT('/blacklist', function (data) {
                var request = $.deparam(data);
                expect(request.blacklistedId).toEqual('826');
                expect(request.start).toEqual(JsHelper.toZuluTimeString(startDate));
                expect(request.end).toEqual(JsHelper.toZuluTimeString(endDate));
                expect(request.purpose).toEqual(payload.reason.reason);
                expect(request.comment).toEqual(payload.reason.comment);
                return true;
            }).respond(200);

            LocationBlacklistsResource.update({}, payload);
            $httpBackend.flush();
        });

        it('handles empty items gracefully', function () {
            payload.items.items = [];

            $httpBackend.expectPUT('/blacklist', function (data) {
                var request = $.deparam(data);
                expect(request.start).toEqual(JsHelper.toZuluTimeString(startDate));
                expect(request.end).toEqual(JsHelper.toZuluTimeString(endDate));
                expect(request.purpose).toEqual(payload.reason.reason);
                expect(request.comment).toEqual(payload.reason.comment);
                return true;
            }).respond(200);

            LocationBlacklistsResource.update({}, payload);
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPUT('/blacklist')
                .respond(203);
            LocationBlacklistsResource.update();
            $httpBackend.flush();
        });
    });
});
