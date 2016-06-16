describe('Theme API Resource', function () {

    var $httpBackend, ThemeResource, ThemesResource, ThemeUsageResource, theme = {
            'id':1,
            'creationDate': '2014-12-17T14:45:31Z',
            'default': true,
            'description': 'Private Theme of Entwine',
            'name':'Theme Entwine Private',
            'creator':'Admin User',
            'bumperActive':true,
            'bumperFile':'uuid1',
            'bumperFileName':'bumperFile.mp4',
            'bumperFileUrl':'http://localhost:9001/staticfiles/uuid1',
            'trailerActive':true,
            'trailerFile':'uuid2',
            'trailerFileName':'trailerFile.mp4',
            'trailerFileUrl':'http://localhost:9001/staticfiles/uuid2',
            'titleSlideActive':true,
            'titleSlideBackground':'uuid3',
            'titleSlideBackgroundName':'titleslideFile.mp4',
            'titleSlideBackgroundUrl':'http://localhost:9001/staticfiles/uuid3',
            'licenseSlideActive':true,
            'licenseSlideDescription':'250 words',
            'licenseSlideBackground':'uuid4',
            'licenseSlideBackgroundName':'licenseFile.mp4',
            'licenseSlideBackgroundUrl':'http://localhost:9001/staticfiles/uuid4',
            'watermarkActive':true,
            'watermarkFile':'uuid5',
            'watermarkFileName':'watermarkFile.mp4',
            'watermarkFileUrl':'http://localhost:9001/staticfiles/uuid5',
            'watermarkPosition':'topRight'
        };

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.services.language'));
    beforeEach(module('pascalprecht.translate'));
    beforeEach(module('LocalStorageModule'));
    beforeEach(module('ngResource'));


    beforeEach(inject(function (_$httpBackend_, _ThemeResource_, _ThemesResource_, _ThemeUsageResource_) {
        $httpBackend = _$httpBackend_;
        ThemeResource = _ThemeResource_;
        ThemesResource = _ThemesResource_;
        ThemeUsageResource = _ThemeUsageResource_;
    }));

    afterEach(function () {
        $httpBackend.verifyNoOutstandingExpectation();
    });

    describe('ThemeResource', function () {

        describe('#delete', function(){

            it('executes delete request on delete()', function () {
                $httpBackend.expectDELETE('/admin-ng/themes/1').respond({});

                ThemeResource.delete({id: 1});
            });
        });


        describe('#get', function(){
            var response;

            beforeEach(function(){
                $httpBackend.whenGET('/admin-ng/themes/1.json').respond(JSON.stringify(theme));
                response = ThemeResource.get({id: 1, format: '.json'});
                $httpBackend.flush();
            });


            it('sets name', function (){
                expect(response.general.name).toEqual('Theme Entwine Private');
            });

            it('sets description', function (){
                expect(response.general.description).toEqual('Private Theme of Entwine');
            });

            it('sets bumper.active', function (){
                expect(response.bumper.active).toEqual(true);
            });

            it('sets bumper.file.id', function (){
                expect(response.bumper.file.id).toEqual('uuid1');
            });

            it('sets bumper.file.name', function (){
                expect(response.bumper.file.name).toEqual('bumperFile.mp4');
            });

            it('sets bumper.file.url', function (){
                expect(response.bumper.file.url).toEqual('http://localhost:9001/staticfiles/uuid1');
            });

            it('sets trailer.active', function (){
                expect(response.trailer.active).toEqual(true);
            });

            it('sets trailer.file.id', function (){
                expect(response.trailer.file.id).toEqual('uuid2');
            });

            it('sets trailer.file.name', function (){
                expect(response.trailer.file.name).toEqual('trailerFile.mp4');
            });

            it('sets trailer.file.url', function (){
                expect(response.trailer.file.url).toEqual('http://localhost:9001/staticfiles/uuid2');
            });

            it('sets titleslide.active', function (){
                expect(response.titleslide.active).toEqual(true);
            });

            it('sets titleslide.file.id', function (){
                expect(response.titleslide.file.id).toEqual('uuid3');
            });

            it('sets titleslide.file.name', function (){
                expect(response.titleslide.file.name).toEqual('titleslideFile.mp4');
            });

            it('sets titleslide.file.url', function (){
                expect(response.titleslide.file.url).toEqual('http://localhost:9001/staticfiles/uuid3');
            });

            it('sets license.active', function (){
                expect(response.license.active).toEqual(true);
            });

            it('sets license.description', function (){
                expect(response.license.description).toEqual('250 words');
            });

            it('sets license.file.id', function (){
                expect(response.license.file.id).toEqual('uuid4');
            });

            it('sets license.file.name', function (){
                expect(response.license.file.name).toEqual('licenseFile.mp4');
            });

            it('sets license.file.url', function (){
                expect(response.license.file.url).toEqual('http://localhost:9001/staticfiles/uuid4');
            });

            it('sets watermark.file.active', function (){
                expect(response.watermark.active).toEqual(true);
            });

            it('sets watermark.file.name', function (){
                expect(response.watermark.file.name).toEqual('watermarkFile.mp4');
            });

            it('sets watermark.file.url', function (){
                expect(response.watermark.file.url).toEqual('http://localhost:9001/staticfiles/uuid5');
            });

            it('sets watermark.file.position', function (){
                expect(response.watermark.position).toEqual('topRight');
            });
        });

    });

    describe('ThemesResource', function () {

        var themesResponse = {
            'total': 33,
            'limit': 0,
            'count': 2,
            'results': [theme]
        };

        it('executes get request on query()', function () {
            $httpBackend.expectGET('/admin-ng/themes/themes.json').respond(JSON.stringify(themesResponse));

            ThemesResource.query();
        });
    });

    describe('ThemeUsageResource', function () {

        var seriesResponse = {
                'series': [
                    {
                        'id': '4581',
                        'title': 'Mock Serie 1'
                    },
                    {
                        'id': '4582',
                        'title': 'Mock Serie 2'
                    }
                ]
            };

        it('executes get request on query()', function () {
            $httpBackend.expectGET('/admin-ng/themes/2/usage.json').respond(JSON.stringify(seriesResponse));

            ThemeUsageResource.query({themeId: 2});
        });
    });


});
