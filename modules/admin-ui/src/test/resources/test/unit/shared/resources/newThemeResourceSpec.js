describe('New Theme API Resource', function () {

    var NewThemeResource, $httpBackend, request = {
        bumper: {
            active: true,
            file: {
                id: 111111,
                name: 'bumper.mp4'
            }
        },
        'general': {
            name: 'test-name',
            description: 'test-description',
            default: false
        },
        license: {
            active: true,
            backgroundImage: true,
            description: 'test-license-description',
            file: {
                id: 444444,
                name: 'license.mp4'
            }
        },
        summary: {},
        titleslide: {
            active: true,
            file: {
                id: 333333,
                name: 'slides-v.mp4'
            },
            metadata: {
                title: {
                    label: 'CONFIGURATION.THEMES.DETAILS.TITLE.METADATA.ITEM.TITLE',
                    enabled: true
                },
                speaker: {
                    label: 'CONFIGURATION.THEMES.DETAILS.TITLE.METADATA.ITEM.SPEAKER',
                    enabled: true
                },
                date: {
                    label: 'CONFIGURATION.THEMES.DETAILS.TITLE.METADATA.ITEM.DATE',
                    enabled: false
                },
                room: {
                    label: 'CONFIGURATION.THEMES.DETAILS.TITLE.METADATA.ITEM.ROOM',
                    enabled: false
                }
            },
            mode: 'upload'
        },
        trailer: {
            active: true,
            file: {
                id: 222222,
                name: 'trailer.mp4'
            }
        },
        watermark: {
            active: true,
            file: {
                id: 555555,
                name: 'slides-v.mp4'
            },
            position: 'topLeft'
        }
    };


    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {
            }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _NewThemeResource_) {
        NewThemeResource = _NewThemeResource_;
        $httpBackend = _$httpBackend_;
    }));

    describe('save requst transformation', function () {

        // temporarily disabled, the backend is not yet ready
        xit('sets default', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).default).toEqual('false');
                return true;
            }).respond(200);
        });

        it('sets description', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).description).toEqual('test-description');
                return true;
            }).respond(200);
        });

        it('sets name', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).name).toEqual('test-name');
                return true;
            }).respond(200);
        });

        it('sets bumperActive', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).bumperActive).toEqual('true');
                return true;
            }).respond(200);
        });

        it('sets bumperFile', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).bumperFile).toEqual('111111');
                return true;
            }).respond(200);
        });

        it('sets trailerActive', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).trailerActive).toEqual('true');
                return true;
            }).respond(200);
        });

        it('sets trailerFile', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).trailerFile).toEqual('222222');
                return true;
            }).respond(200);
        });

        it('sets titleActive', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).titleSlideActive).toEqual('true');
                return true;
            }).respond(200);
        });

        it('sets titleSlideBackground', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).titleSlideBackground).toEqual('333333');
                return true;
            }).respond(200);
        });

        it('sets licenseSlideActive', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).licenseSlideActive).toEqual('true');
                return true;
            }).respond(200);
        });

        it('sets licenseSlideDescription', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).licenseSlideDescription).toEqual('test-license-description');
                return true;
            }).respond(200);
        });

        it('sets licenseSlideBackground', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).licenseSlideBackground).toEqual('444444');
                return true;
            }).respond(200);
        });

        it('sets watermarkActive', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).watermarkActive).toEqual('true');
                return true;
            }).respond(200);
        });

        it('sets watermarkFile', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).watermarkFile).toEqual('555555');
                return true;
            }).respond(200);
        });

        it('sets watermarkPosition', function () {
            $httpBackend.expectPOST('/admin-ng/themes', function (data) {
                expect($.deparam(data).watermarkPosition).toEqual('topLeft');
                return true;
            }).respond(200);
        });
      
        afterEach(function () {
            NewThemeResource.save(request);
            $httpBackend.flush();
        });
    });

});
