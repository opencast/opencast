describe('adminNg.directives.adminNgEditableSingleValue', function () {
    var $compile, $rootScope, $timeout, element;

    beforeEach(module('adminNg.directives'));
    beforeEach(module('adminNg.filters'));
    beforeEach(module('shared/partials/editableSingleValue.html'));

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
        $rootScope.params = { value: 'original value' };
        $rootScope.save = jasmine.createSpy();
        element = $compile('<div admin-ng-editable-single-value params="params" save="save"></div>')($rootScope);
        $rootScope.$digest();
    });

    it('displays the label', function () {
        expect(element.html()).toContain('original value');
    });

    it('becomes editable when clicked', function () {
        expect(element.find('input')).toHaveClass('ng-hide');
        expect(element.find('span')).not.toHaveClass('ng-hide');
        element.click();
        $timeout.flush();
        expect(element.find('input')).not.toHaveClass('ng-hide');
        expect(element.find('span')).toHaveClass('ng-hide');
    });

    it('saves the value when exiting edit mode', function () {
        element.click();
        $rootScope.params.value = 'new value';
        element.find('input').blur();
        expect($rootScope.save).toHaveBeenCalled();
    });

    it('stores the original values when activated', function () {
        element.click();
        element.find('input').val('changed value');
        element.find('input').blur();
        expect(element.find('span').scope().original).toEqual('original value');
        element.click();
        element.find('input').val('another value');
        element.find('input').blur();
        expect(element.find('span').scope().original).toEqual('original value');
    });

    it('does not save when no changes have been made', function () {
        element.click();
        element.find('input').blur();
        expect($rootScope.save).not.toHaveBeenCalled();
    });

    describe('#keyUp', function () {
        var event = {};

        beforeEach(function () {
            element.find('span').scope().editMode = true;
        });

        it('does nothing by default', function () {
            element.find('span').scope().keyUp(event);
            expect(element.find('span').scope().editMode).toBe(true);
        });

        describe('when pressing ESC', function () {
            beforeEach(function () {
                event.stopPropagation = jasmine.createSpy();
                event.keyCode = 27;
                element.find('span').scope().keyUp(event);
            });

            it('leaves edit mode', function () {
                expect(element.find('span').scope().editMode).toBe(false);
            });

            it('stops propagation', function () {
                expect(event.stopPropagation).toHaveBeenCalled();
            });
        });

        describe('when pressing ENTER', function () {
            beforeEach(function () {
                event.stopPropagation = jasmine.createSpy();
                event.keyCode = 13;
                element.find('span').scope().keyUp(event);
            });

            it('leaves edit mode', function () {
                expect(element.find('span').scope().editMode).toBe(false);
            });

            it('does not stop propagation', function () {
                expect(event.stopPropagation).not.toHaveBeenCalled();
            });
        });
    });

    describe('#submit', function () {
        var callbackContext = {};
        beforeEach(function () {
            callbackContext.scope = element.find('span').scope();
            $rootScope.save.and.callFake(function (id, callback) {
                callback.apply(callbackContext);
            });
        });

        it('saves the value', function () {
            element.find('span').scope().submit();
            expect($rootScope.save).toHaveBeenCalled();
        });
    });
});
