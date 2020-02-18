describe('adminNg.modules.events.validators.notEmptySelectionValidator', function () {
    var $compile, $rootScope, $httpBackend, element;
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
    beforeEach(inject(function (_$rootScope_, _$compile_, _$httpBackend_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $rootScope.listoptions = getJSONFixture('admin-ng/event/new/processing');
        $rootScope.somemodel = {};
        $rootScope.processing = {
            ud: {}
        };
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        element = '<form name="testform"><select chosen data-disable-search-threshold="0" not-empty-selection ng-model="somemodel"' +
                  'ng-model-options="{ allowInvalid: true }" ng-options="w.title for (obj, w) in listoptions"/></form>';
        $compile(element)($rootScope);
    });


    it('instantiates', function () {
        $rootScope.$digest();
    });

    it('is invalid if no selection was made', function () {
        $rootScope.$digest();
        expect($rootScope.testform.$valid).toBeFalsy();
    });

    it('is valid as soon as an item is selected', function () {
        $rootScope.processing.ud.workflow = {
            selection: {
                id: 'an item'
            }
        };
        $rootScope.$digest();
        expect($rootScope.testform.$valid).toBeTruthy();
    });

    it('is invalid if now workflow is selected', function () {
        $rootScope.processing.ud.workflow = {};
        $rootScope.$digest();
        expect($rootScope.testform.$valid).toBeFalsy();
    });
});
