describe('adminNg.modules.events.validators.taskStartableValidator', function () {
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
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $rootScope.row = {
            event_status_raw: 'PROCESSED',
            selected: true
        };
        element = '<form name="testform"><input name="selection" type="checkbox" task-startable="{{row.event_status_raw}}" ' +
            'ng-model="row.selected" ng-change="rowSelectionChanged($index)" class="child-cbox"></form>';
    });

    describe('event_status_raw === PROCESSED', function () {
        beforeEach(function () {
            $compile(element)($rootScope);
            $rootScope.$digest();
        });

        it('instantiates', function () {
            expect($rootScope.row.selected).toBeTruthy();
        });

        it('is valid if it is selected and event_status_raw equals PROCESSED', function () {
            expect($rootScope.testform.$valid).toBeTruthy();
        });
    });

    describe('event_status_raw === PROCESSING_CANCELED', function () {
        beforeEach(function () {
            $rootScope.row.event_status_raw = 'PROCESSING_CANCELED';
            $compile(element)($rootScope);
            $rootScope.$digest();
        });

        it('instantiates', function () {
            expect($rootScope.row.selected).toBeTruthy();
        });

        it('is valid if it is selected and event_status_raw equals PROCESSING_CANCELED', function () {
            expect($rootScope.testform.$valid).toBeTruthy();
        });
    });

    describe('event_status_raw === PROCESSING_FAILURE', function () {
        beforeEach(function () {
            $rootScope.row.event_status_raw = 'PROCESSING_FAILURE';
            $compile(element)($rootScope);
            $rootScope.$digest();
        });

        it('instantiates', function () {
            expect($rootScope.row.selected).toBeTruthy();
        });

        it('is valid if it is selected and event_status_raw equals PROCESSING_FAILURE', function () {
            expect($rootScope.testform.$valid).toBeTruthy();
        });
    });


    describe('event_status_raw === RECORDING_FAILURE', function () {
        it('is invalid if source does not equal PROCESSED, nor PROCESSING_CANCELED, nor PROCESSING_FAILURE', function () {
            $rootScope.row.event_status_raw = 'RECORDING_FAILURE';
            $compile(element)($rootScope);
            $rootScope.$digest();
            expect($rootScope.testform.$valid).toBeFalsy();
        });
    });

});

