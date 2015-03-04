describe('Series Theme API Resource', function () {
    var SeriesThemeResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _SeriesThemeResource_) {
        $httpBackend = _$httpBackend_;
        SeriesThemeResource = _SeriesThemeResource_;
    }));


    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, response = {
                'id': 1,
                'creationDate': '2014-12-17T14:45:31Z',
                'default': true,
                'usage': 11,
                'description': 'Private Theme of Entwine',
                'name': 'Entwine Private',
                'creator': {
                    'username': 'admin',
                    'email': null,
                    'name': 'Admin User'
                }
            };

            $httpBackend.expectGET('/admin-ng/series/40518/theme.json').respond(JSON.stringify(response));
            result = SeriesThemeResource.get({id: 40518});
            $httpBackend.flush();

            expect(result.id).toEqual(response.id);
        });
    });

    describe('#save', function () {
        it('sends an array of metadata', function () {
            var postRequest = {theme: 12345};

            $httpBackend.expectPUT('/admin-ng/series/40518/theme', function (data) {
                expect(data).toEqual('themeId=12345');
                return true;
            }).respond(200);

            SeriesThemeResource.save({ id: '40518' }, postRequest);
            $httpBackend.flush();
        });

    });

});