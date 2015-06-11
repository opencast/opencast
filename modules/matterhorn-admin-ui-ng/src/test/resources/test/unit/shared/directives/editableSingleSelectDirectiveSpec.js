describe('adminNg.directives.adminNgEditableSingleSelect', function () {
    var $compile, $rootScope, $timeout, element;

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

    beforeEach(inject(function (_$rootScope_, _$timeout_, _$compile_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $timeout = _$timeout_;
    }));

    beforeEach(function () {
        $rootScope.params = { value: 'item2' };
        $rootScope.collection = { item1: 'Value 1', item2: 'Value 2', item3: 'Value 3' };
        $rootScope.save = jasmine.createSpy();
        element = $compile('<div admin-ng-editable-single-select params="params" collection="collection" save="save"></div>')($rootScope);
        $rootScope.$digest();
    });

    it('displays the label', function () {
        expect(element.find('span').text()).toContain('Value 2');
    });

    it('becomes editable when clicked', function () {
        expect(element.find('div')).toHaveClass('ng-hide');
        expect(element.find('span')).not.toHaveClass('ng-hide');
        element.click();
        $timeout.flush();
        expect(element.find('select')).not.toHaveClass('ng-hide');
        expect(element.find('div')).toHaveClass('ng-hide');
    });

    it('saves the value when it changes', function () {
        element.click();
        element.find('select').val('item3').change();
        $timeout.flush();
        expect($rootScope.save).toHaveBeenCalled();
    });

    it('does not save when no changes have been made', function () {
        element.click();
        element.find('select').change();
        $timeout.flush();
        expect($rootScope.save).not.toHaveBeenCalled();
    });

    describe('#submit', function () {
        var callbackContext = {};
        beforeEach(function () {
            callbackContext.scope = element.find('div').scope();
        });

        it('saves the value', function () {
            element.find('div').scope().submit();
            $timeout.flush();
            expect($rootScope.save).toHaveBeenCalled();
        });
    });
});
