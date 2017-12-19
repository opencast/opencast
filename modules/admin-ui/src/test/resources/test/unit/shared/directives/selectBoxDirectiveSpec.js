describe('adminNg.directives.adminNgSelectBox', function () {
    var $compile, $rootScope, element;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/selectContainer.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
    }));

    beforeEach(function () {
        $rootScope.resource = {
            searchable: true,
            available: [{ name: 'rabbit', color: 'white' }, { name: 'crow', color: 'black' }],
            selected: []
        };
        element = $compile('<admin-ng-select-box resource="resource" group-by="color" />')($rootScope);
        $rootScope.$digest();
    });

    it('renders a two-pane select box', function () {
        expect(element).toContainElement('div.multi-select-container');
        expect(element).toContainElement('select.available');
        expect(element).toContainElement('select.selected');
        expect(element.find('select.available').html()).toContain('crow');
    });

    it('groups entries by color', function () {
        expect(element.find('select.available optgroup').length).toBe(2);
    });

    it('disables buttons until values are selected', function () {
        expect(element.find('a.submit')).toHaveClass('disabled');
        element.find('select.available option:first').prop({ selected: true });
        element.find('select.available').change();
        expect(element.find('a.submit')).not.toHaveClass('disabled');
    });

    it('allows adding values to the selected pane', function () {
        element.find('select.available option:last').prop({ selected: true });
        element.find('select.available').change();
        element.find('a.submit').click();
        expect(element.find('select.selected').html()).toContain('rabbit');
        expect(element.find('select.available').html()).not.toContain('rabbit');
    });

    it('allows removing values from the selected pane', function () {
        element.find('select.available option:first').prop({ selected: true });
        element.find('select.available').change();
        element.find('a.submit').click();

        element.find('select.selected option:first').prop({ selected: true });
        element.find('select.selected').change();
        element.find('a.remove').click();
        expect(element.find('select.selected').html()).not.toContain('rabbit');
        expect(element.find('select.available').html()).toContain('rabbit');
    });

    it('handles empty sources gracefully', function () {
        var scope = element.find('select').scope();
        spyOn(scope, 'move');

        delete scope.markedForAddition;
        delete scope.markedForRemoval;
        scope.add();
        scope.remove();

        expect(scope.move).not.toHaveBeenCalled();
    });
});
