describe('Theme Form Controller', function () {
    var $scope, $controller, $parentScope, $timeout, ThemeResourceMock, ThemeUsageResourceMock, NotificationsMock, FormNavigatorServiceMock,
        NewThemeResourceMock, TableServiceMock, userdata;

    userdata = {
        general: {
            name: 'Heinz name',
            description: 'heinzys description',
            default: false
        },
        bumper: {
            active: false
        },
        trailer: {
            active: false
        },
        watermark: {
            active: false
        },
        titleslide: {
            active: false
        },
        license: {
            active: false
        }
    };

    ThemeResourceMock = jasmine.createSpyObj('ThemeResource', ['get', 'delete', 'update']);
    ThemeUsageResourceMock = jasmine.createSpyObj('ThemeUsageResource', ['get']);
    NotificationsMock = jasmine.createSpyObj('Notifications', ['add', 'remove']);
    FormNavigatorServiceMock = jasmine.createSpyObj('FormNavigatorService', ['navigateTo']);
    NewThemeResourceMock = jasmine.createSpyObj('NewThemeResource', ['save']);
    TableServiceMock = jasmine.createSpyObj('TableService', ['fetch']);

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));
    beforeEach(module(function ($provide) {
        $provide.value('ThemeResource', ThemeResourceMock);
        $provide.value('ThemeUsageResource', ThemeUsageResourceMock);
        $provide.value('Notifications', NotificationsMock);
        $provide.value('FormNavigatorService', FormNavigatorServiceMock);
        $provide.value('NewThemeResource', NewThemeResourceMock);
        $provide.value('Table', TableServiceMock);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$timeout_) {
        $controller = _$controller_;
        $timeout = _$timeout_;

        $parentScope = $rootScope.$new();
        $scope = $parentScope.$new();
    }));

    describe('instantiation', function () {
        beforeEach(function () {
            $controller('ThemeFormCtrl', {$scope: $scope});
        });
        
        it('sets the current form', function () {
            expect($scope.currentForm).toEqual('generalForm');
        });

        it('overwrites the navigateTo method', function () {
            expect($scope.navigateTo).toBeDefined();
            $scope.navigateTo('somewhere', 'hereIamNow', []);
            expect(FormNavigatorServiceMock.navigateTo).toHaveBeenCalledWith('somewhere', 'hereIamNow', []);
        });

        it('implements cancel', function () {
            expect($scope.cancel).toBeDefined();
            $scope.close = jasmine.createSpy('close');
            $scope.cancel();
            expect($scope.close).toHaveBeenCalled();
        });

        it('implements submit', function () {
            expect($scope.submit).toBeDefined();
        });

        describe('#valid', function () {

            it('is defined', function () {
                expect($scope.valid).toBeDefined();
            });

            it('returns false if no theme-form is in the scope', function () {
                expect($scope.valid()).toBeFalsy();
            });

            it('returns true if theme-form is in the scope', function () {
                $scope['theme-form'] = {
                    $valid: true
                };
                expect($scope.valid()).toBeTruthy();
            });
        });

    });

    describe('add theme', function () {
        beforeEach(function () {
            $parentScope.action = 'add';
            $controller('ThemeFormCtrl', {$scope: $scope});
            $scope.close = jasmine.createSpy('close');
        });

        it('initializes for the new theme usecase', function () {
            expect($scope.general).toBeDefined();
            expect($scope.bumper).toBeDefined();
            expect($scope.trailer).toBeDefined();
            expect($scope.titleslide).toBeDefined();
            expect($scope.license).toBeDefined();
            expect($scope.watermark).toBeDefined();
        });

        describe('save', function () {
        
            beforeEach(function () {
                $scope.general = userdata.general;
                $scope.bumper = userdata.bumper;
                $scope.trailer = userdata.trailer;
                $scope.license = userdata.license;
                $scope.titleslide = userdata.titleslide;
                $scope.watermark = userdata.watermark;
                $scope.submit();
            });

            it('closes modal upon submit', function () {
                expect($scope.close).toHaveBeenCalled();
            });

            it('adds theme upload notification', function () {
                expect(NotificationsMock.add).toHaveBeenCalledWith('success', 'THEME_UPLOAD_STARTED', 'global', -1);
            });

            it('saves upon submit', function () {
                TableServiceMock.fetch.and.returnValue(null);
                expect(NewThemeResourceMock.save).toHaveBeenCalledWith({}, userdata, jasmine.any(Function), jasmine.any(Function));
            });

            it('reacts correctly on success', function () {
                NewThemeResourceMock.save.calls.mostRecent().args[2].call($scope);
                $timeout.flush();
                expect(TableServiceMock.fetch).toHaveBeenCalled();
                expect(NotificationsMock.add).toHaveBeenCalledWith('success', 'THEME_CREATED');
                expect(NotificationsMock.remove).toHaveBeenCalled();
            });

            it('reacts correctly on failure', function () {
                NewThemeResourceMock.save.calls.mostRecent().args[3].call($scope);
                expect(NotificationsMock.add).toHaveBeenCalledWith('error', 'THEME_NOT_CREATED');
                expect(NotificationsMock.remove).toHaveBeenCalled();
            });
        });

    });

    describe('edit theme', function () {
        beforeEach(function () {
            $parentScope.action = 'edit';
            $parentScope.resourceId = 144;
            $controller('ThemeFormCtrl', {$scope: $scope});
            $scope.close = jasmine.createSpy('close');
        });

        it('initializes for the edit theme usecase', function () {
            expect(ThemeResourceMock.get).toHaveBeenCalledWith({id: 144, format: '.json'}, jasmine.any(Function));
        });

        it('loads the series for showing the usage', function () {
            expect(ThemeUsageResourceMock.get).toHaveBeenCalledWith({themeId: 144});
        });

        it('reacts correctly upon having loaded the theme successfully', function () {
            ThemeResourceMock.get.calls.mostRecent().args[1].call($scope, userdata);
            expect($scope.general).toEqual(userdata.general);
            expect($scope.bumper).toEqual(userdata.bumper);
        });

        it('sets the loaded flag', function () {
            ThemeResourceMock.get.calls.mostRecent().args[1].call($scope);
            expect($scope.themeLoaded).toBeTruthy();
        });

        describe('#update', function () {
            beforeEach(function () {
                $scope.general = userdata.general;
                $scope.bumper = userdata.bumper;
                $scope.trailer = userdata.trailer;
                $scope.license = userdata.license;
                $scope.titleslide = userdata.titleslide;
                $scope.watermark = userdata.watermark;
                $scope.submit();
            });

            it('calls the update service', function () {
                expect(ThemeResourceMock.update).toHaveBeenCalledWith(
                    {id: $parentScope.resourceId}, userdata, jasmine.any(Function), jasmine.any(Function));
                expect($scope.close).toHaveBeenCalled();
            });
        });

    });
});
