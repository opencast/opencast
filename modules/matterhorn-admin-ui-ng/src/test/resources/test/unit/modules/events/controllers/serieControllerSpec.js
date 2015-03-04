describe('Serie controller', function () {
    var $scope, $httpBackend, $controller, SeriesMetadataResource, SeriesAccessResource, SeriesThemeResource, Notifications;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _SeriesMetadataResource_, _SeriesAccessResource_, _SeriesThemeResource_, _Notifications_) {
        $scope = $rootScope.$new();
        $scope.resourceId = '4581';
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
        SeriesMetadataResource = _SeriesMetadataResource_;
        SeriesAccessResource = _SeriesAccessResource_;
        SeriesThemeResource = _SeriesThemeResource_;
        Notifications = _Notifications_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/series/4581/metadata.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/4581/metadata.json')));
        $httpBackend.whenGET('/admin-ng/series/4581/events.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/4581/events.json')));
        $httpBackend.whenGET('/admin-ng/series/4581/access.json').respond(200);
        $httpBackend.whenGET('/admin-ng/themes/themes.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/series/4581/theme.json').respond(getJSONFixture('admin-ng/series/4581/theme.json'));
        $httpBackend.whenPUT('/admin-ng/series/4581/theme').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/THEMES.NAME.json').respond({1001: 'Heinz das Pferd', 1002: 'Full Fledged', 401: 'Doc Test'});
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json').respond('{}');


        $controller('SerieCtrl', {$scope: $scope});
    });

    it('fetches series metadata', function () {
        expect($scope.metadata.entries).toBeUndefined();
        $httpBackend.flush();
        expect($scope.metadata.entries).toBeDefined();
        expect($scope.metadata.entries.length).toBe(2);
    });

    it('retrieves the record from the server when the resource ID changes', function () {
        spyOn(SeriesMetadataResource, 'get');
        spyOn(SeriesAccessResource, 'get');
        $scope.$broadcast('change', 7);
        expect(SeriesMetadataResource.get).toHaveBeenCalledWith({ id: 7 }, jasmine.any(Function));
        expect(SeriesAccessResource.get).toHaveBeenCalledWith({ id: 7 }, jasmine.any(Function));
    });

    describe('retrieving metadata catalogs', function () {
        var catalogs;

        beforeEach(function () {
            catalogs = getJSONFixture('admin-ng/series/4581/metadata.json');
            $httpBackend.flush();
            $scope.$broadcast('change', 4581);
        });

        it('isolates dublincore/series catalog', function () {
            $scope.$watch('seriesCatalog', function (newCatalog) {
                expect(newCatalog).toEqual(catalogs[0]);
            });
        });

        it('prepares the extended-metadata catalogs', function () {
            $scope.metadata.$promise.then(function () {
                expect($scope.metadata.entries.length).toBe(2);
                angular.forEach($scope.metadata.entries, function (catalog, index)  {
                    expect(catalog.fields.length).toEqual(catalogs[index + 1].fields.length);
                });
            });
        });

        afterEach(function () {
            $httpBackend.flush();
        });
    });

    describe('#metadataSave', function () {

        it('saves the event record', function () {
            spyOn(SeriesMetadataResource, 'save');
            $scope.metadataSave('test', function () {}, {fields: []});

            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '4581'}, {fields: [], attributeToSend: 'test'}, jasmine.any(Function));
        });

        describe('catalog selection', function () {
            var fn, callbackObject = {
                    callback: function () {}
                };
            beforeEach(function () {
                $httpBackend.flush();
                spyOn($scope, 'metadataSave').and.callThrough();
                spyOn(callbackObject, 'callback');
                spyOn(SeriesMetadataResource, 'save').and.callThrough();
                $httpBackend.expectPUT('/admin-ng/series/4581/metadata').respond(200);
            });

            it('saves fields in the dublincore/series catalog', function () {
                fn = $scope.getSaveFunction('dublincore/series'),
                fn('title', callbackObject.callback);
                $httpBackend.flush();
                expect($scope.metadataSave).toHaveBeenCalledWith('title', callbackObject.callback, $scope.seriesCatalog);
                expect(callbackObject.callback).toHaveBeenCalled();
            });

            it('saves fields in the dublincore/extended-1 catalog', function () {
                fn = $scope.getSaveFunction('dublincore/extended-1'),
                fn('title', callbackObject.callback);
                $httpBackend.flush();
                expect($scope.metadataSave).toHaveBeenCalledWith('title', callbackObject.callback, $scope.metadata.entries[0]);
                expect(callbackObject.callback).toHaveBeenCalled();
            });
        });
    });

    describe('#accessSave', function () {

        it('saves the event record', function () {
            this.policy = {
                role: 'ROLE_TEST',
                read: true,
                write: true
            };

            $scope.access = {
                series_access: {
                    current_acl: 123
                }
            };
            spyOn(SeriesAccessResource, 'save');
            $scope.accessSave.call(this);

            expect(SeriesAccessResource.save).toHaveBeenCalledWith({ id: '4581' }, { acl : { ace : [  ] }, override: true});
        });
    });

    describe('#themeSave', function () {

        it('displays notification on success', function () {

            $scope.theme.current = 17;
            spyOn(Notifications, 'add');

            $scope.themeSave();
            $httpBackend.flush();

            expect(Notifications.add).toHaveBeenCalledWith('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
        });

        it('saves the theme record', function () {

            $scope.theme.current = 17;
            spyOn(SeriesThemeResource, 'save');

            $scope.themeSave();

            expect(SeriesThemeResource.save).toHaveBeenCalledWith({ id: '4581' }, {theme: 17}, jasmine.any(Function));
        });
    });
});
