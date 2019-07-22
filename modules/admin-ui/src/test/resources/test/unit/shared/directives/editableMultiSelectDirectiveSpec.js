describe('adminNg.directives.adminNgEditableMultiSelect', function () {
    var $compile, $rootScope, $timeout, element, $httpBackend;

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

    beforeEach(inject(function (_$rootScope_, _$compile_, _$httpBackend_, _$timeout_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $timeout = _$timeout_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
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

    function enterValue(value) {
        element.click();
        element.find('input').val(value).trigger('change');

        var enter = $.Event('keyup');
        enter.keyCode = 13;
        element.find('input').trigger(enter);
    }

    it('saves when adding values', function () {
        expect(element.scope().params.value).not.toContain('Item 2');

        enterValue('item2');

        expect($rootScope.save).toHaveBeenCalled();
        expect(element.scope().params.value).toContain('Item 2');
    });

    it('saves when removing values', function () {
        expect(element.scope().params.value).toContain('Item 1');

        element.find('span.ng-multi-value:first a').mousedown();

        expect($rootScope.save).toHaveBeenCalled();
        expect(element.scope().params.value).not.toContain('Item 1');
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

    it('allows entering multiple values at once separated by a given delimiter', function () {
        element.scope().params.value = ['item1'];
        element.scope().params.delimiter = ';';

        enterValue('item2;item3');

        expect(element.scope().params.value).toContain('Item 2');
        expect(element.scope().params.value).toContain('Item 3');
        expect(element.scope().params.value).not.toContain('item2;item3');
    });

    it('ignores duplicate values', function () {
        function count() {
            var count = 0;
            angular.forEach(element.scope().params.value, function (value) {
                if (value == 'Item 2') {
                    ++count;
                }
            });
            return count;
        }
        expect(count()).toBe(0);

        enterValue('item2');
        enterValue('item2');

        expect(count()).toBe(1);
    });

    describe('#leaveEditMode', function () {

        it('leaves edit mode', function () {
            var scope = element.find('ul').scope();  // the div & input elements don't exist when editMode = false
            scope.editMode = true;
            scope.data.value = 'edited';

            scope.leaveEditMode();

            expect(element.scope().params.value).not.toContain('edited');
            expect(scope.editMode).toBe(false);
            expect(scope.data.value).toEqual('');
        });
    });

    describe('#onBlur', function () {

        var scope;

        beforeEach(function () {
            scope = element.find('ul').scope();  // the div & input elements don't exist when editMode = false
            scope.editMode = true;
            scope.mixed = true;
            scope.data.value = 'edited';
        });

        it('saves and leaves edit mode', function () {
            scope.onBlur();

            expect(scope.editMode).toBe(false);
            expect(scope.data.value).toEqual('');
            expect(scope.params.value).toContain('edited')
        });
    })

    describe('#addValue', function () {

        it('does not save duplicate values', function () {
            var model = ['unique'];
            element.find('ul').scope().addValue(model, 'unique');
            expect(model).toEqual(['unique']);
        });
    });

    describe('#keyUp', function () {
        var event,
            scope;

        beforeEach(function () {
            event = { target: { value: 'a' } };
            event.stopPropagation = jasmine.createSpy();
            scope = element.find('ul').scope();
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

            it('clears value', function () {
              expect(scope.data.value).toBe('');
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
