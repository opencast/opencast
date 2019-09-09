describe('adminNg.directives.myTable', function () {
    var $compile, $rootScope, element, Storage;

    beforeEach(module('adminNg'));
    beforeEach(module('LocalStorageModule'));
    beforeEach(module('shared/partials/table.html'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_, _Storage_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        Storage = _Storage_;
    }));

    beforeEach(function () {
        $rootScope.table = { fetch: jasmine.createSpy() };
        element = $compile('<div data-admin-ng-table="" table="table"></div>')($rootScope);
    });

    it('fetches the table records', function () {
        $rootScope.$digest();
        expect($rootScope.table.fetch).toHaveBeenCalled();
    });

    it('reacts on filter changes', function () {
        $rootScope.$digest();
        expect($rootScope.table.fetch.calls.count()).toBe(1);

        Storage.scope.$emit('change', 'filter');
        expect($rootScope.table.fetch.calls.count()).toBe(2);
    });

    it('does not react on non-filter changes', function () {
        $rootScope.$digest();
        expect($rootScope.table.fetch.calls.count()).toBe(1);

        Storage.scope.$emit('change', 'sorter');
        expect($rootScope.table.fetch.calls.count()).toBe(1);
    });
});
