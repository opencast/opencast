describe('Serie controller', function () {
    var $scope, $httpBackend, $controller, $timeout, SeriesMetadataResource, SeriesAccessResource, SeriesThemeResource, Notifications;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$timeout_, _$httpBackend_, _SeriesMetadataResource_, _SeriesAccessResource_, _SeriesThemeResource_, _Notifications_) {
        $scope = $rootScope.$new();
        $scope.resourceId = '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c';
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        SeriesMetadataResource = _SeriesMetadataResource_;
        SeriesAccessResource = _SeriesAccessResource_;
        SeriesThemeResource = _SeriesThemeResource_;
        Notifications = _Notifications_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata.json')));
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/events.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/events.json')));
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/access.json').respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/access.json')));
        $httpBackend.whenGET('/admin-ng/themes/themes.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/theme.json').respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/theme.json')));
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/participation.json').respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/participation.json')));
        $httpBackend.whenPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/theme').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/THEMES.NAME.json').respond({1001: 'Heinz das Pferd', 1002: 'Full Fledged', 401: 'Doc Test'});
        $httpBackend.whenGET('/admin-ng/resources/THEMES.DESCRIPTION.json').respond({901: 'theme1 description', 902: 'theme2 desc\nsecond line'});
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ACL.ACTIONS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json?filter=role_target:ACL&limit=100&offset=0').respond('{"ROLE_ANONYMOUS": "ROLE_ANONYMOUS"}');
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json?filter=role_target:ACL&limit=100&offset=2').respond('{}');

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
            catalogs = getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata.json');
            $httpBackend.flush();
            $scope.$broadcast('change', '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c');
        });

        it('isolates dublincore/series catalog', function () {
            $scope.$watch('seriesCatalog', function (newCatalog) {
                expect(newCatalog.flavor).toEqual(catalogs[0].flavor);
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

    describe('merges roles correctly', function () {

        beforeEach(function () {
            $httpBackend.flush();
            $scope.$broadcast('change', '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c');
        });

        it('adds external roles appropriately', function() {
            //This is lame, but we have to wait for the series ACL to load, and the external role call to load
            $timeout(function() {
                expect($scope.roles.ROLE_EXTERNAL).toEqual("ROLE_EXTERNAL");
                expect($scope.roles.ROLE_ADMIN_UI).toEqual("ROLE_ADMIN_UI");
                expect($scope.roles.ROLE_ANONYMOUS).toEqual("ROLE_ANONYMOUS");
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

            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'}, {fields: [], attributeToSend: 'test'}, jasmine.any(Function));
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
                $httpBackend.expectPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata').respond(200);
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

            $scope.policies = [];
            $scope.policies[0] = {
                role  : 'admin',
                read  : true,
                write : true,
                actions : {
                    value : []
                }
            };

            spyOn(SeriesAccessResource, 'save');
            $scope.accessSave.call(this);

            expect(SeriesAccessResource.save).toHaveBeenCalledWith({ id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c' },
                {
                    acl : { ace : [ { action : 'read', allow : true, role : 'admin' }, { action : 'write', allow : true, role : 'admin' } ] },
                    override: true
                }
            );
        });
    });

    describe('#themeSave', function () {

        it('displays notification on success', function () {

            $scope.selectedTheme.id = 17;
            spyOn(Notifications, 'add');

            $scope.themeSave();
            $httpBackend.flush();

            expect(Notifications.add).toHaveBeenCalledWith('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
        });

        it('saves the theme record', function () {

            $scope.selectedTheme.id = 17;
            spyOn(SeriesThemeResource, 'save');

            $scope.themeSave();

            expect(SeriesThemeResource.save).toHaveBeenCalledWith({ id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c' }, {theme: 17}, jasmine.any(Function));
        });
    });
});
