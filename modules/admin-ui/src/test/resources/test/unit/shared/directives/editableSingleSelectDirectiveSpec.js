describe('adminNg.directives.adminNgEditableSingleSelect', function () {
    var $compile, $rootScope, $timeout, element, $filter, $httpBackend;

    beforeEach(module('adminNg'));
    beforeEach(module('adminNg.filters'));
    beforeEach(module('shared/partials/editableSingleSelect.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$filter_, _$httpBackend_, _$rootScope_, _$timeout_, _$compile_) {
        $filter = _$filter_;
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $rootScope.params = { value: 'item2' };
        $rootScope.collection = { item1: 'Value 1', item2: 'Value 2', item3: 'Value 3' };
        $rootScope.save = jasmine.createSpy();
        element = $compile('<div admin-ng-editable-single-select params="params" collection="collection" save="save"></div>')($rootScope);
        $rootScope.$digest();
    });

    describe('displays the label', function () {
        it('when no option selected', function () {
            $rootScope.params.value = '';
            $rootScope.$digest();
            expect(element.find('span').text()).toContain('SELECT_NO_OPTION_SELECTED');
        });

        it('when option selected', function () {
            $rootScope.params.value = 'Value 3';
            $rootScope.$digest();
            expect(element.find('span').text()).toContain('item3');
        });

        it('when no options available', function () {
            $rootScope.collection = [];
            $rootScope.$digest();
            expect(element.find('span').text()).toContain('SELECT_NO_OPTIONS_AVAILABLE');
        });
    });

    describe('on init', function () {
        it('is not editable', function () {
            expect(element.find('i').scope().editMode).toBe(false);
            expect(element.find('div.editable-select').length).toBe(0);
            expect(element.find('span')).not.toHaveClass('ng-hide');
            expect(element.find('select').length).toBe(0);
        });
    });

    describe('enters edit mode', function () {
        it('when calling directly', function () {
            element.find('i').scope().enterEditMode();
            $timeout.flush();
            expect(element.find('i').scope().editMode).toBe(true);
            expect(element.find('div.editable-select').length).toBe(1);
            expect(element.find('span')).toHaveClass('ng-hide');
            expect(element.find('select').length).toBe(1);
        });

        it('when clicking anywhere into the div', function () {
            spyOn(element.find('i').scope(), 'enterEditMode').and.callThrough();
            element.mousedown();
            $timeout.flush();
            expect(element.find('i').scope().enterEditMode).toHaveBeenCalled();
            expect(element.find('i').scope().editMode).toBe(true);
            expect(element.find('div.editable-select').length).toBe(1);
            expect(element.find('span')).toHaveClass('ng-hide');
            expect(element.find('select').length).toBe(1);
        });
    });

    describe('leaves edit mode', function () {
        beforeEach(function () {
            element.find('i').scope().enterEditMode();
            $timeout.flush();
            spyOn(element.find('i').scope(), 'leaveEditMode').and.callThrough();
        });

        it('when calling directly', function () {
            element.find('i').scope().leaveEditMode();
            $rootScope.$digest();
            expect(element.find('i').scope().editMode).toBe(false);
            expect(element.find('div.editable-select').length).toBe(0);
            expect(element.find('span')).not.toHaveClass('ng-hide');
            expect(element.find('select').length).toBe(0);
        });
    });

    describe('saves', function () {
        beforeEach(function () {
            element.find('i').scope().enterEditMode();
            $timeout.flush();
            spyOn(element.find('i').scope(), 'submit').and.callThrough();
        });

        it('when value changes', function () {
            element.find('select').val('item3').change();
            $timeout.flush();
            expect(element.find('i').scope().submit).toHaveBeenCalled();
            expect($rootScope.save).toHaveBeenCalled();
        });
    });
});
