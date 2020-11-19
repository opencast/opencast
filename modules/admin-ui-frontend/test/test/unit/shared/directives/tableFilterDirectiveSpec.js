describe('adminNg.directives.adminNgTableFilter', function () {
    var $compile, $rootScope, $httpBackend, Storage, FilterProfiles, ResourcesFilterResource, element,
        profilesStub = [{ name: 'myFilter', filter: { type: 'chair' }}],
        newProfile   = { name: 'myNewFilter', filter: { type: 'table' }};

    beforeEach(module('adminNg'));
    beforeEach(module('pascalprecht.translate'));
    beforeEach(module('LocalStorageModule'));
    beforeEach(module('shared/partials/tableFilters.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_, _$httpBackend_, _Storage_, _FilterProfiles_, _ResourcesFilterResource_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        Storage = _Storage_;
        FilterProfiles = _FilterProfiles_;
        ResourcesFilterResource = _ResourcesFilterResource_;
    }));

    beforeEach(function () {
        $rootScope.resource = 'furniture';
        $rootScope.category = 'furniture';

        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $httpBackend.whenGET('/admin-ng/resources/furniture/filters.json').respond(JSON.stringify({
            color: {
                label: 'COLOR',
                type: 'select',
                options: {
                    'brown': 'BROWN',
                    'white': 'WHITE',
                    'black': 'BLACK'
                },
                translatable: true
            },
            type: {
                label: 'TYPE',
                type: 'select',
                options: {
                    'chair': 'CHAIR',
                    'table': 'TABLE'
                },
                translatable: true
            }
        }));

        $rootScope.filters = ResourcesFilterResource.get({ resource: $rootScope.category });

        Storage.put('filter', 'furniture', 'type', 'CHAIR');
        element = $compile('<div admin-ng-table-filter data-filters="filters" data-namespace="resource"></div>')($rootScope);
        $rootScope.$digest();

        $httpBackend.flush();
    });

    afterEach(function () {
        window.localStorage.clear();
    });

    it('renders the table filter element', function () {
        expect(element).toEqual('div.filters-container');
        expect(element).toContainElement('.fa-filter');
        expect(element.html()).toBeMatchedBy('div.table-filter');
    });

    it('restores filters after fetching them', function () {
        expect(element.scope().filters.filters.type.label).toEqual('TYPE');
        expect(element.scope().filters.filters.color.label).toEqual('COLOR');
    });

    it('restores filter profiles', function () {
        expect(element.find('div').scope().profiles).toBeDefined();
    });

    it('allows selecting a filter', function () {
        expect(element.find('select[ng-model="filterValue"]')).not.toExist();
        expect(element.find('div').scope().filter).toBeUndefined();

        element.find('select[ng-model="selectedFilter"] option[value="color"]').prop({selected: true});
        element.find('select[ng-model="selectedFilter"]').change();

        expect(element.find('select[ng-model="filterValue"] option').length).toBe(4);
        expect(element.find('div').scope().filter.label).toEqual('COLOR');
    });

    it('overwrites existing filters when selecting a new value', function () {
        element.find('select[ng-model="selectedFilter"] option[value="color"]').prop({selected: true});
        element.find('select[ng-model="selectedFilter"]').change();

        element.find('select[ng-model="filterValue"] option:last').prop({selected: true});
        element.find('select[ng-model="filterValue"]').change();

        expect(element.scope().filters.map.color.value).toEqual('white');
    });

    describe('#removeFilter', function () {
        beforeEach(function () {
            spyOn(Storage, 'remove');
            element.scope().filters.filters.type.value = 'chair';
            element.find('div').scope().removeFilter('type', $rootScope.filters.filters.type);
        });

        it('removes all filters', function () {
            expect(element.scope().filters.filters.type.value).toBeUndefined();
            expect(Storage.remove).toHaveBeenCalledWith('filter', 'furniture', 'type');
        });
    });

    describe('#selectFilterTextValue', function () {

        it('sets the text filter values', function (done) {
            spyOn(Storage, 'put');
            element.find('div').scope().selectFilterTextValue('filter-name', 'abc');
            // The request is only send after 250ms due to the debounce function
            setTimeout(function () {
                expect(Storage.put).toHaveBeenCalledWith('filter', 'furniture', 'filter-name', 'abc');
                done();
            }, 500);
        });
    });

    describe('#selectFilterPeriodValue', function () {

        describe('with only the from date', function () {

            var filter;

            beforeEach(function () {
                filter = { period: { from: 'from-date' } };
                spyOn(Storage, 'put');
                element.find('div').scope().selectFilterPeriodValue(filter, 'from', 'to');
            });

            it('does not set a filter value', function () {
                expect(Storage.put).not.toHaveBeenCalled();
            });

            it('pre-fills to date', function () {
                expect(filter.period.to).toEqual(filter.period.from);
                expect(filter.prefilled.from).toBe(false);
                expect(filter.prefilled.to).toBe(true);
            });
        });

        describe('with only the to date', function () {

            var filter;

            beforeEach(function() {
                filter = { period: { to: 'to-date' } };
                spyOn(Storage, 'put');
                element.find('div').scope().selectFilterPeriodValue(filter, 'to', 'from');
            });

            it('does not set a filter value', function () {
                expect(Storage.put).not.toHaveBeenCalled();
            });

            it('pre-fills from date', function () {
                expect(filter.period.from).toEqual(filter.period.to);
                expect(filter.prefilled.to).toBe(false);
                expect(filter.prefilled.from).toBe(true);
            });
        });

        describe('with both from- and to dates', function () {

            var filter, fromDate, toDate, fromDateIsoStr, toDateIsoStr;

            beforeEach(function() {
                // Set dates with ISO string (vs localized 2015-01-01)
                fromDateIsoStr = '2015-01-01T00:00:00.001Z';
                toDateIsoStr = '2015-01-02T23:59:59.999Z';
                fromDate = new Date(fromDateIsoStr);
                toDate = new Date(toDateIsoStr);
                filter = { period: { type: "period", from: fromDate, to: toDate } };
                spyOn(Storage, 'put');
                element.find('div').scope().selectFilterPeriodValue(filter, 'from', 'to');
            });

            it('does not pre-fill dates', function () {
                expect(filter.prefilled.to).toBe(false);
                expect(filter.prefilled.from).toBe(false);
            })

            it('sets the time period filter value (accounting for localized day)', function () {
                // Get local time zone offset to verify user requested day range
                var timeOffset = toDate.getTimezoneOffset();
                var expectedFromDate = new Date(fromDateIsoStr);
                // Adjust to local day
                expectedFromDate =  new Date(expectedFromDate.getTime() + timeOffset * 60 * 1000);
                expectedFromDate.setHours(0, 0, 0, 001);
                var expectedToDate = new Date(toDateIsoStr);
                // Adjust to local day
                expectedToDate =  new Date(expectedToDate.getTime() + timeOffset * 60 * 1000);
                expectedToDate.setHours(23, 59, 59, 999);
                expect(Storage.put).toHaveBeenCalledWith('filter', 'furniture', undefined, expectedFromDate.toISOString() + '/' + expectedToDate.toISOString());
            });
        });
    });

    describe('#removeFilters', function () {
        beforeEach(function () {
            element.scope().filters.filters.type.value = 'chair';
            element.scope().filters.filters.color.value = 'brown';
            element.find('div').scope().removeFilters();
        });

        it('removes all filters', function () {
            expect(element.scope().filters.filters.type.value).toBeUndefined();
            expect(element.scope().filters.filters.color.value).toBeUndefined();
        });
    });

    describe('#saveProfile', function () {
        beforeEach(function () {
            spyOn(FilterProfiles, 'set');
            element.find('div').scope().profiles = angular.copy(profilesStub);
            Storage.put('filter', 'furniture', 'type', 'table');
        });

        describe('with a new profile', function () {
            beforeEach(function () {
                element.find('div').scope().profile = angular.copy(newProfile);
                element.find('div').scope().saveProfile();
            });

            it('sets the filter profile', function () {
                var expectedResult = angular.copy(profilesStub);
                expectedResult.push(newProfile);
                expect(FilterProfiles.set).toHaveBeenCalledWith('furniture', expectedResult);
            });
        });

        describe('with an existing profile', function () {
            beforeEach(function () {
                element.find('div').scope().editFilterProfile(0);
                element.find('div').scope().profile = angular.copy(newProfile);
                element.find('div').scope().saveProfile();
            });

            it('sets the profile', function () {
                var expectedResult = [newProfile];
                expect(FilterProfiles.set).toHaveBeenCalledWith('furniture', expectedResult);
            });
        });
    });

    describe('#cancelProfileEditing', function () {
        beforeEach(function () {
            element.find('div').scope().currentlyEditing = 9;
            element.find('div').scope().cancelProfileEditing();
        });

        it('empties the profile model', function () {
            expect(element.find('div').scope().currentlyEditing).toBeUndefined();
        });

        it('deletes the ID of the currently edited profile', function () {
            expect(element.find('div').scope().profile).toEqual({});
        });
    });

    describe('#removeFilterProfile', function () {
        beforeEach(function () {
            spyOn(FilterProfiles, 'set');
            element.find('div').scope().profiles = angular.copy(profilesStub);
            element.find('div').scope().removeFilterProfile(0);
        });

        it('removes the filter profile', function () {
            expect(element.find('div').scope().profiles.length).toBe(0);
        });

        it('saves the profile', function () {
            expect(FilterProfiles.set).toHaveBeenCalled();
        });
    });

    describe('#editFilterProfile', function () {
        beforeEach(function () {
            element.find('div').scope().profiles = angular.copy(profilesStub);
            element.find('div').scope().editFilterProfile(0);
        });

        it('populates the profile', function () {
            expect(element.find('div').scope().profile).toEqual(profilesStub[0]);
        });

        it('sets the ID of the currently edited profile', function () {
            expect(element.find('div').scope().currentlyEditing).toBe(0);
        });
    });

    describe('#loadFilterProfile', function () {
        beforeEach(function () {
            spyOn(Storage, 'replace');
            element.find('div').scope().profiles = angular.copy(profilesStub);
        });

        describe('without stored filters', function () {
            beforeEach(function () {
                element.find('div').scope().loadFilterProfile(0);
            });

            it('does nothin', function () {
                expect(Storage.replace).not.toHaveBeenCalled();
            });
        });

        describe('with stored filters', function () {
            beforeEach(function () {
                FilterProfiles.set('furniture', profilesStub);
                element.find('div').scope().loadFilterProfile(0);
            });

            it('stores the loaded filter', function () {
                expect(Storage.replace).toHaveBeenCalledWith([ { namespace : 'furniture', key : 'type', value : 'chair' } ], 'filter');
            });
        });
    });

    describe('#validateProfileName', function () {
        var $scope;

        beforeEach(function () {
            $scope = element.find('div').scope();
            $scope.profiles = [{ name: 'Profile A' }, { name: 'Profile B' }];
            FilterProfiles.set($scope.namespace, $scope.profiles);
            $scope.profileForm = {
                name: {
                    $setValidity: jasmine.createSpy()
                }
            };
            $scope.profile = {};
        });

        describe('with an new profile name', function () {
            beforeEach(function () {
                $scope.profile.name = 'Profile C';
                $scope.validateProfileName();
            });

            it('accepts validity of the form field', function () {
                expect($scope.profileForm.name.$setValidity).toHaveBeenCalledWith('uniqueness', true);
            });
        });

        describe('with an already existing profile name', function () {
            beforeEach(function () {
                $scope.profile.name = 'Profile A';
                $scope.validateProfileName();
            });

            it('rejects validity of the form field', function () {
                expect($scope.profileForm.name.$setValidity).toHaveBeenCalledWith('uniqueness', false);
            });
        });
    });
});
