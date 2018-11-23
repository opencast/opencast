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
        expect(element.find('div').length).toBe(0);
        element.click();
        $timeout.flush();
        expect(element.find('div').length).toBe(1);
    });

    it('saves when adding values', function () {
        expect(element.scope().params.value).not.toContain('item2');

        element.click();
        element.find('input').val('item2').trigger('change');

        var enter = $.Event('keyup');
        enter.keyCode = 13;
        element.find('input').trigger(enter);

        expect($rootScope.save).toHaveBeenCalled();
        expect(element.scope().params.value).toContain('Item 2');
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

    describe('#enterEditMode', function () {

        it('enter edit mode', function () {
            var scope = element.find('ul').scope();
            scope.editMode = false;

            scope.enterEditMode();

            expect(scope.editMode).toBe(true);
        });
    });

    describe('#leaveEditMode', function () {

        it('leaves edit mode', function () {
            var scope = element.find('ul').scope();  // the div & input elements doesn't exist when editMode = false
            scope.editMode = true;
            scope.data.value = 'edited';

            scope.leaveEditMode();

            expect(element.scope().params.value).not.toContain('edited')
            expect(scope.editMode).toBe(false);
            expect(scope.data.value).toEqual('edited');
        });
    });

    describe('#addValue', function () {

        it('does not save duplicate values', function () {
            var model = ['unique'];
            element.find('ul').scope().addValue(model, 'unique');
            expect(model).toEqual(['unique']);
        });
    });

    describe('#keyUp', function () {
        var event = { target: { value: 'a' } };
        var scope;

        beforeEach(function () {
            event.stopPropagation = jasmine.createSpy();
            scope = element.find('ul').scope()
            scope.editMode = true;

        });

        it('does nothing by default', function () {
            scope.keyUp(event);
            expect(scope.editMode).toBe(true);
        });

        it('stops propagation', function () {
            scope.keyUp(event);
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        describe('when pressing ESC', function () {
            beforeEach(function () {
                event.keyCode = 27;
                scope.keyUp(event);
            });

            it('leaves edit mode', function () {
                expect(scope.editMode).toBe(false);
            });
        });

        describe('when pressing ENTER', function () {
            beforeEach(function () {
                event.keyCode = 13;
            });

            describe('with an item included in the collection', function () {
                beforeEach(function () {
                    scope.data.value = 'item2';
                    scope.keyUp(event);
                });

                it('does not leave edit mode', function () {
                    expect(scope.editMode).toBe(true);
                });
            });

            describe('with an item not included in the collection', function () {
                beforeEach(function () {
                    scope.keyUp(event);
                });

                it('does not leave edit mode', function () {
                    expect(scope.editMode).toBe(true);
                });
            });
        });
    });

    describe('#submit', function () {

        it('saves the value', function () {
            element.find('ul').scope().submit();
            expect($rootScope.save).toHaveBeenCalled();
        });
    });
});
