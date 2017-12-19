describe('Series API Resource', function () {
    var SeriesResource, $httpBackend;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            formatDate: function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _SeriesResource_) {
        $httpBackend  = _$httpBackend_;
        SeriesResource = _SeriesResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#query', function () {
        it('calls the series service', function () {
            $httpBackend.expectGET('/admin-ng/series/series.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/series/series.json')));
            SeriesResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/series/series.json')
                .respond(JSON.stringify(getJSONFixture('admin-ng/series/series.json')));
            var data = SeriesResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(2);
            expect(data.rows[0].title).toEqual('•mock• serie 1');
            expect(data.rows[0].creator).toEqual('Teaching Assistant');
            expect(data.rows[0].contributors).toEqual('Heinz Heinzmann, Lars Larsson');
            expect(data.rows[0].createdDateTime).toEqual('2012-12-01T08:59:00Z');
        });
    });

    describe('#create', function () {

        it('sends new series data', function () {
            $httpBackend.expectPOST('/admin-ng/series/new', function (data) {
                expect(angular.fromJson($.deparam(data).metadata).foo)
                    .toEqual('my-series-metadata');
                return true;
            }).respond(200);
            SeriesResource.create({}, { foo: 'my-series-metadata', access: {id: 'test'} });
            $httpBackend.flush();
        });

        it('handles empty requests gracefully', function () {
            $httpBackend.expectPOST('/admin-ng/series/new').respond(200);
            SeriesResource.create();
            $httpBackend.flush();
        });
    });
});
