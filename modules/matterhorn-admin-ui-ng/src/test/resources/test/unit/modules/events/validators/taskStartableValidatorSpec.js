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
        $rootScope.row = {
            source: 'ARCHIVE',
            selected: true
        };
        element = '<form name="testform"><input name="selection" type="checkbox" task-startable="{{row.source}}" ng-model="row.selected" class="child-cbox"></form>';
    });

    describe('source === ARCHIVE', function () {
        beforeEach(function () {
            $compile(element)($rootScope);
            $rootScope.$digest();
        });

        it('instantiates', function () {
            expect($rootScope.row.selected).toBeTruthy();
        });

        it('is valid if it is selected and source equals ARCHIVE', function () {
            expect($rootScope.testform.$valid).toBeTruthy();
        });

    });

    describe('source === PROCESSING', function () {
        it('is invalid if source does not equal ARCHIVE', function () {
            $rootScope.row.source = 'PROCESSING';
            $compile(element)($rootScope);
            $rootScope.$digest();
            expect($rootScope.testform.$valid).toBeFalsy();
        });
    });

});

