describe('Serie controller', function () {
    var $scope, $httpBackend, $controller, $timeout, SeriesMetadataResource, SeriesAccessResource, SeriesThemeResource,
      Notifications;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$timeout_, _$httpBackend_, _SeriesMetadataResource_,
      _SeriesAccessResource_, _SeriesThemeResource_, _Notifications_) {
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
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata.json')));
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/events.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/events.json')));
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/access.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/access.json')));
        $httpBackend.whenGET('/admin-ng/themes/themes.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/theme.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/theme.json')));
        $httpBackend.whenPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/theme').respond('{}');
        $httpBackend.whenGET('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/tobira/pages').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/THEMES.NAME.json')
            .respond({1001: 'Heinz das Pferd', 1002: 'Full Fledged', 401: 'Doc Test'});
        $httpBackend.whenGET('/admin-ng/resources/THEMES.DESCRIPTION.json')
            .respond({901: 'theme1 description', 902: 'theme2 desc\nsecond line'});
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/feeds/feeds').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ACL.ACTIONS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/acl/roles.json?target=ACL&limit=-1').respond('[{"name": "ROLE_ANONYMOUS"}]');
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
        // Until we're actually testing the statistics endpoint, just return an empty set here
        $httpBackend.whenGET(/\/admin-ng\/statistics.*/).respond('[]');
        $httpBackend.whenPOST(/\/admin-ng\/statistics.*/).respond('[]');

        $controller('SerieCtrl', {$scope: $scope});
    });

    it('fetches series metadata', function () {
        expect($scope.metadata.entries).toBeUndefined();
        expect($scope.commonMetadataCatalog).toBeUndefined();
        expect($scope.extendedMetadataCatalogs).toBeUndefined();
        $httpBackend.flush();
        expect($scope.metadata.entries).toBeDefined();
        expect($scope.metadata.entries.length).toBe(3);
        expect($scope.commonMetadataCatalog).toBeDefined();
        expect($scope.extendedMetadataCatalogs).toBeDefined();
        expect($scope.extendedMetadataCatalogs.length).toBe(2);
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

        it('extracts the common metadata catalog', function () {
            $scope.$watch('commonMetadataCatalog', function (newCatalog) {
                expect(newCatalog.flavor).toEqual(catalogs[0].flavor);
            });
        });

        it('extracts the extended metadata catalogs', function () {
            $scope.metadata.$promise.then(function () {
                expect($scope.extendedMetadataCatalogs.length).toBe(2);
                angular.forEach($scope.extendedMetadataCatalogs, function (catalog, index)  {
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

    describe('#unsavedChanges', function() {
        it('has unsaved changes', function () {

            expect($scope.unsavedChanges([{fields: [{dirty: true}]}])).toBe(true);
            expect($scope.unsavedChanges([{fields: [{dirty: false}, {dirty: true}]},
              {fields: [{dirty: false}]}])).toBe(true);
           expect($scope.unsavedChanges([{fields: [{dirty: true}]},
              {fields: [{dirty: true}]}])).toBe(true);
        });

        it('doesn\'t have unsaved changes', function () {
            expect($scope.unsavedChanges([{fields: [{dirty: false}]}])).toBe(false);
            expect($scope.unsavedChanges([{fields: [{dirty: false}, {dirty: false}]},
              {fields: [{dirty: false}]}])).toBe(false);
        });
    });

    describe('#metadataChanged', function () {
        var fn, callbackObject = {
            callback: function () {}
        };

        beforeEach(function () {
            spyOn($scope, 'metadataChanged').and.callThrough();
            spyOn(callbackObject, 'callback');
            $httpBackend.flush();
        });

        it('does\'t mark fields dirty when value hasn\'t changed', function () {
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(false);
            fn = $scope.getMetadataChangedFunction('dublincore/series'),
            fn('title', callbackObject.callback);
            expect($scope.metadataChanged).toHaveBeenCalledWith('title', callbackObject.callback,
              $scope.commonMetadataCatalog);
            expect(callbackObject.callback).toHaveBeenCalled();
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(false);
            expect($scope.commonMetadataCatalog.fields[0].dirty).toBe(false);
        });

        it('marks field in the common metadata catalog as dirty', function () {
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(false);
            $scope.commonMetadataCatalog.fields[0].value = "New Title";
            fn = $scope.getMetadataChangedFunction('dublincore/series'),
            fn('title', callbackObject.callback);
            expect($scope.metadataChanged).toHaveBeenCalledWith('title', callbackObject.callback,
              $scope.commonMetadataCatalog);
            expect(callbackObject.callback).toHaveBeenCalled();
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(true);
            expect($scope.commonMetadataCatalog.fields[0].dirty).toBe(true);
        });

        it('marks field in the extended metadata catalog as dirty', function () {
            expect($scope.unsavedChanges($scope.extendedMetadataCatalogs)).toBe(false);
            $scope.extendedMetadataCatalogs[0].fields[0].value = "New Title";
            fn = $scope.getMetadataChangedFunction('dublincore/extended-1'),
            fn('title', callbackObject.callback);
            expect($scope.metadataChanged).toHaveBeenCalledWith('title', callbackObject.callback,
              $scope.extendedMetadataCatalogs[0]);
            expect(callbackObject.callback).toHaveBeenCalled();
            expect($scope.unsavedChanges($scope.extendedMetadataCatalogs)).toBe(true);
            expect($scope.extendedMetadataCatalogs[0].fields[0].dirty).toBe(true);
        });
    });

    describe('#metadataSave', function () {
        beforeEach(function () {
            spyOn(SeriesMetadataResource, 'save').and.callThrough();
        });

        it('doesn\'t save when no field marked as dirty', function () {
            var catalog = {fields: [{ dirty: false }]};
            $scope.metadataSave([catalog]);
            expect(SeriesMetadataResource.save).not.toHaveBeenCalled();
        });

        it('saves when field marked as dirty', function () {
            var catalog = {fields: [{ dirty: true }]};
            $scope.metadataSave([catalog]);
            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'},
              catalog, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].dirty).toBe(false);
        });

        it('resets old value when saving', function () {
            var catalog = {fields: [{ dirty: true, value: 'blub', oldValue: 'blah' }]};
            $scope.metadataSave([catalog]);
            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'},
              catalog, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].value).toBe('blub');
            expect(catalog.fields[0].oldValue).toBe('blub');
        });

        it('saves multiple catalogs', function () {
            var catalog = {fields: [{ dirty: true }]};
            var catalog2 = {fields: [{ dirty: true }]};
            $scope.metadataSave([catalog, catalog2]);
            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'},
              catalog, jasmine.any(Function));
            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'},
              catalog2, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata').respond(200);
            $httpBackend.expectPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].dirty).toBe(false);
            expect(catalog2.fields[0].dirty).toBe(false);
        });

        it('saves one of multiple catalogs', function () {
            var catalog = {fields: [{ dirty: true, value: 'blub', oldValue: 'blah' }]};
            var catalog2 = {fields: [{ dirty: false, value: 'blub', oldValue: 'blah' }]};
            $scope.metadataSave([catalog, catalog2]);
            expect(SeriesMetadataResource.save).toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'},
              catalog, jasmine.any(Function));
            expect(SeriesMetadataResource.save).not.toHaveBeenCalledWith({id: '73f9b7ab-1d8f-4c75-9da1-ceb06736d82c'},
              catalog2, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/series/73f9b7ab-1d8f-4c75-9da1-ceb06736d82c/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].dirty).toBe(false);
            expect(catalog.fields[0].oldValue).toBe('blub');
            expect(catalog2.fields[0].oldValue).toBe('blah');
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
                    override: false
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
