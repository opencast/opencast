describe('adminNg.directives.adminNgEditableMultiSelect', function () {
    var $compile, $rootScope, $timeout, element;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/editableMultiSelect.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_, _$timeout_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $timeout = _$timeout_;
    }));

    beforeEach(function () {
        $rootScope.params = { value: ['item1', 'item3'] };
        $rootScope.collection = { item1: 'Item 1', item2: 'Item 2', item3: 'Item 3' };
        $rootScope.save = jasmine.createSpy();
        element = $compile('<div admin-ng-editable-multi-select params="params" collection="collection" save="save"></div>')($rootScope);
        $rootScope.$digest();
    });

    it('creates an editable element', function () {
        expect(element).toContainElement('i.edit');
        expect(element.html()).toContain('Item 1');
        expect(element.html()).toContain('Item 3');
    });

    it('becomes editable when clicked', function () {
        expect(element.find('div')).toHaveClass('ng-hide');
        element.click();
        $timeout.flush();
        expect(element.find('div')).not.toHaveClass('ng-hide');
    });

    it('saves when adding values', function () {
        expect(element.scope().params.value).not.toContain('item2');

        element.click();
        element.find('input').val('item2').trigger('change');

        var enter = $.Event('keyup');
        enter.keyCode = 13;
        element.find('input').trigger(enter);

        expect($rootScope.save).toHaveBeenCalled();
        expect(element.scope().params.value).toContain('item2');
    });

    it('saves when removing values', function () {
        expect(element.scope().params.value).toContain('item1');

        element.click();
        element.find('span.ng-multi-value:first a').mousedown();

        expect($rootScope.save).toHaveBeenCalled();
        expect(element.scope().params.value).not.toContain('item1');
    });

    xit('autocompletes after entering three characters', function () {
        expect(element.find('datalist option').length).toBe(0);
        expect(element.find('datalist').html()).not.toContain('Item 1');

        element.find('input').val('som').trigger('change');
        var keys = $.Event('keyup');
        element.find('input').trigger(keys);

        expect(element.find('datalist option').length).toBe(3);
        expect(element.find('datalist').html()).toContain('Item 1');
    });

    describe('#leaveEditMode', function () {

        it('leaves edit mode', function () {
            var scope = element.find('div').scope();
            scope.editMode = true;
            scope.value = 'edited';

            scope.leaveEditMode();

            expect(scope.editMode).toBe(false);
            expect(scope.value).toEqual('');
        });
    });

    describe('#addValue', function () {

        it('does not save duplicate values', function () {
            var model = ['unique'];
            element.find('input').scope().addValue(model, 'unique');
            expect(model).toEqual(['unique']);
        });
    });

    describe('#keyUp', function () {
        var event = { target: { value: 'a' } };

        beforeEach(function () {
            event.stopPropagation = jasmine.createSpy();
            element.find('input').scope().editMode = true;
        });

        it('does nothing by default', function () {
            element.find('input').scope().keyUp(event);
            expect(element.find('input').scope().editMode).toBe(true);
        });

        it('stops propagation', function () {
            element.find('input').scope().keyUp(event);
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        describe('when pressing ESC', function () {
            beforeEach(function () {
                event.keyCode = 27;
                element.find('input').scope().keyUp(event);
            });

            it('leaves edit mode', function () {
                expect(element.find('input').scope().editMode).toBe(false);
            });
        });

        describe('when pressing ENTER', function () {
            beforeEach(function () {
                event.keyCode = 13;
            });

            describe('with an item included in the collection', function () {
                beforeEach(function () {
                    element.find('input').scope().value = 'item2';
                    element.find('input').scope().keyUp(event);
                });

                it('leaves edit mode', function () {
                    expect(element.find('input').scope().editMode).toBe(false);
                });
            });

            describe('with an item not included in the collection', function () {
                beforeEach(function () {
                    element.find('input').scope().keyUp(event);
                });

                it('does not leave edit mode', function () {
                    expect(element.find('input').scope().editMode).toBe(true);
                });
            });
        });

        describe('when entering more than one character', function () {
            beforeEach(function () {
                event.keyCode = 45;
                event.target.value = 'ab';
                element.find('input').scope().keyUp(event);
            });

            it('sets the collection', function () {
                expect(element.find('input').scope().collection).toBeDefined();
            });
        });
    });

    describe('#submit', function () {

        it('saves the value', function () {
            element.find('input').scope().submit();
            expect($rootScope.save).toHaveBeenCalled();
        });
    });
});
